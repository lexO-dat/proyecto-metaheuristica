package spp;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Ejecuta TODO un conjunto de instancias de una carpeta y produce un resumen
 * agregado (no imprime coordenadas, solo una linea por instancia).
 *
 * Para cada instancia reporta la metrica H/OPT de:
 *   - BL    : heuristica Bottom-Left pura con orden por area descendente (baseline).
 *   - PSO   : el mejor orden hallado por el PSO discreto.
 * Asi se puede juzgar si la metaheuristica realmente mejora a la heuristica base.
 *
 * El presupuesto del PSO (particulas x iteraciones) se adapta al numero de
 * items para que las instancias grandes terminen en tiempo razonable.
 *
 * Uso:  java -cp out spp.BatchRunner [carpeta] [salida.csv]
 */
public class BatchRunner {

    private static final long SEMILLA = 12345L;

    public static void main(String[] args) throws IOException {
        // Punto decimal con '.' (evita que el locale use coma y rompa el CSV).
        java.util.Locale.setDefault(java.util.Locale.US);

        Path carpeta = Paths.get(args.length > 0 ? args[0] : "testcases");
        Path salida  = Paths.get(args.length > 1 ? args[1] : "resumen_resultados.csv");

        if (!Files.isDirectory(carpeta)) {
            System.err.println("No es una carpeta: " + carpeta.toAbsolutePath());
            return;
        }

        List<Path> archivos = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(carpeta, "*.csv")) {
            for (Path p : ds) archivos.add(p);
        }
        archivos.sort(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()));

        System.out.printf("%-32s %7s %9s %9s %9s %9s %9s %8s%n",
                "instancia", "items", "OPT", "H_BL", "H_PSO", "HoptBL", "HoptPSO", "ms");

        List<Double> hoptBL  = new ArrayList<>();
        List<Double> hoptPSO = new ArrayList<>();
        int psoMejor = 0, psoIgual = 0, psoPeor = 0, fallos = 0;

        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(salida))) {
            csv.println("instancia,items,ancho,opt_conocido,OPT_ref,H_BL,H_PSO,Hopt_BL,Hopt_PSO,ms");

            for (Path archivo : archivos) {
                try {
                    LectorCSV.CasoPrueba caso = LectorCSV.leer(archivo);
                    int n = caso.objetos.size();
                    if (n == 0) continue;

                    boolean optConocido = caso.optimoEsperado > 0;
                    double ref = optConocido ? caso.optimoEsperado : cotaInferior(caso);

                    // --- Baseline: Bottom-Left con orden por area descendente ---
                    List<Objeto> ordenArea = new ArrayList<>(caso.objetos);
                    ordenArea.sort(Comparator.comparingDouble(Objeto::getArea).reversed());
                    double hBL = BottomLeft.colocar(ordenArea, caso.anchoContenedor).getAltura();

                    // --- PSO discreto (presupuesto adaptativo al tamano) ---
                    int[] pres = presupuesto(n);
                    long t0 = System.currentTimeMillis();
                    EnjambrePSO pso = new EnjambrePSO(caso.objetos, caso.anchoContenedor,
                            pres[0], pres[1], SEMILLA);
                    double hPSO = pso.optimizar().getAltura();
                    long ms = System.currentTimeMillis() - t0;

                    double rBL  = hBL  / ref;
                    double rPSO = hPSO / ref;
                    hoptBL.add(rBL);
                    hoptPSO.add(rPSO);

                    if      (hPSO < hBL - 1e-6) psoMejor++;
                    else if (hPSO > hBL + 1e-6) psoPeor++;
                    else                        psoIgual++;

                    String nombre = archivo.getFileName().toString();
                    System.out.printf("%-32s %7d %9.2f %9.2f %9.2f %9.4f %9.4f %8d%n",
                            recortar(nombre, 32), n, ref, hBL, hPSO, rBL, rPSO, ms);

                    csv.printf("%s,%d,%.4f,%b,%.4f,%.4f,%.4f,%.6f,%.6f,%d%n",
                            nombre, n, caso.anchoContenedor, optConocido, ref,
                            hBL, hPSO, rBL, rPSO, ms);
                } catch (Exception e) {
                    fallos++;
                    System.out.printf("%-32s  ERROR: %s%n",
                            recortar(archivo.getFileName().toString(), 32), e.getMessage());
                }
            }
        }

        imprimirResumen(hoptBL, hoptPSO, psoMejor, psoIgual, psoPeor, fallos, salida);
    }

    // ------------------------------------------------------------------

    /** Presupuesto {particulas, iteraciones} segun el numero de items. */
    private static int[] presupuesto(int n) {
        if (n <= 200)  return new int[]{20, 200};
        if (n <= 1000) return new int[]{15, 80};
        if (n <= 3000) return new int[]{10, 40};
        return new int[]{8, 20};
    }

    private static double cotaInferior(LectorCSV.CasoPrueba caso) {
        double areaTotal = 0;
        double altoMax = 0;
        for (Objeto o : caso.objetos) {
            areaTotal += o.getArea();
            altoMax = Math.max(altoMax, o.getAlto());
        }
        double porArea = areaTotal / caso.anchoContenedor;
        return Math.max(altoMax, porArea);
    }

    private static String recortar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "~";
    }

    private static void imprimirResumen(List<Double> bl, List<Double> pso,
                                        int mejor, int igual, int peor,
                                        int fallos, Path salida) {
        System.out.println();
        System.out.println("==================== RESUMEN GLOBAL ====================");
        System.out.println("Instancias procesadas : " + pso.size() + "   (errores: " + fallos + ")");
        System.out.printf ("H/OPT promedio  BL  : %.4f   (mediana %.4f)%n",
                promedio(bl), mediana(bl));
        System.out.printf ("H/OPT promedio  PSO : %.4f   (mediana %.4f)%n",
                promedio(pso), mediana(pso));
        System.out.println("--------------------------------------------------------");
        System.out.println("PSO mejor que BL   : " + mejor);
        System.out.println("PSO igual que BL   : " + igual);
        System.out.println("PSO peor que BL    : " + peor);
        System.out.println("--------------------------------------------------------");
        System.out.println("Detalle por instancia guardado en: " + salida.toAbsolutePath());
        System.out.println("========================================================");
    }

    private static double promedio(List<Double> xs) {
        if (xs.isEmpty()) return 0;
        double s = 0; for (double x : xs) s += x;
        return s / xs.size();
    }

    private static double mediana(List<Double> xs) {
        if (xs.isEmpty()) return 0;
        List<Double> c = new ArrayList<>(xs);
        Collections.sort(c);
        int m = c.size() / 2;
        return c.size() % 2 == 0 ? (c.get(m - 1) + c.get(m)) / 2 : c.get(m);
    }
}
