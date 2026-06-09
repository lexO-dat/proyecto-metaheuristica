package spp;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Punto de entrada: procesa todos los .csv de la carpeta "testcases",
 * resuelve cada uno con PSO discreto + Bottom-Left e imprime el resultado.
 */
public class Main {

    // Parametros de ejecucion del PSO.
    private static final int  NUM_PARTICULAS  = 30;
    private static final int  MAX_ITERACIONES = 300;
    private static final long SEMILLA         = 12345L;

    public static void main(String[] args) throws IOException {
        java.util.Locale.setDefault(java.util.Locale.US);  // punto decimal
        // El argumento puede ser una CARPETA (procesa todos sus .csv) o un
        // ARCHIVO .csv individual. Por defecto, la carpeta "testcases".
        Path ruta = Paths.get(args.length > 0 ? args[0] : "testcases");

        List<Path> archivos = new ArrayList<>();
        if (Files.isRegularFile(ruta)) {
            archivos.add(ruta);
        } else if (Files.isDirectory(ruta)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(ruta, "*.csv")) {
                for (Path p : ds) archivos.add(p);
            }
            archivos.sort((a, b) -> a.getFileName().toString()
                    .compareToIgnoreCase(b.getFileName().toString()));
        } else {
            System.err.println("No existe la ruta: " + ruta.toAbsolutePath());
            return;
        }

        if (archivos.isEmpty()) {
            System.err.println("No hay archivos .csv en: " + ruta.toAbsolutePath());
            return;
        }

        for (Path archivo : archivos) {
            procesarCaso(archivo);
        }
    }

    private static void procesarCaso(Path archivo) throws IOException {
        LectorCSV.CasoPrueba caso = LectorCSV.leer(archivo);

        EnjambrePSO pso = new EnjambrePSO(
                caso.objetos, caso.anchoContenedor,
                NUM_PARTICULAS, MAX_ITERACIONES, SEMILLA);

        long t0 = System.currentTimeMillis();
        ResultadoBL mejor = pso.optimizar();
        long t1 = System.currentTimeMillis();

        imprimirResultado(caso, mejor, t1 - t0);
    }

    private static void imprimirResultado(LectorCSV.CasoPrueba caso,
                                          ResultadoBL mejor, long ms) {
        // OPT de referencia: el optimo conocido si esta disponible (>0);
        // en caso contrario (p.ej. -999 = desconocido), una cota inferior.
        boolean optConocido = caso.optimoEsperado > 0;
        double ref = optConocido ? caso.optimoEsperado : cotaInferior(caso);

        String sep = "============================================================";
        System.out.println(sep);
        System.out.println("ARCHIVO            : " + caso.nombreArchivo);
        System.out.printf ("Ancho contenedor   : %.3f%n", caso.anchoContenedor);
        System.out.println("N. de objetos      : " + caso.objetos.size());
        System.out.printf ("OPT (referencia)   : %.3f%s%n", ref,
                optConocido ? "  (optimo conocido)" : "  (cota inferior)");
        System.out.printf ("Mejor altura (PSO) : %.3f%n", mejor.getAltura());

        if (ref > 0) {
            double razon = mejor.getAltura() / ref;            // metrica H/OPT
            System.out.printf ("Razon H/OPT        : %.4f%n", razon);
            double gap = 100.0 * (mejor.getAltura() - ref) / ref;
            System.out.printf ("Gap                : %.2f%%%n", gap);
        }
        System.out.println("Tiempo             : " + ms + " ms");

        System.out.println("------------------------------------------------------------");
        System.out.println("Orden final y coordenadas (esquina inferior-izquierda):");
        System.out.printf ("  %-6s %-9s %-9s %-9s %-9s%n", "ID", "Alto", "Ancho", "X", "Y");
        for (Objeto o : mejor.getColocados()) {
            System.out.printf("  %-6d %-9.3f %-9.3f %-9.3f %-9.3f%n",
                    o.getId(), o.getAlto(), o.getAncho(), o.getX(), o.getY());
        }
        System.out.println(sep);
        System.out.println();
    }

    /**
     * Cota inferior continua para la altura del SPP cuando no se conoce el
     * optimo: el maximo entre (1) la altura del objeto mas alto y (2) el area
     * total dividida por el ancho de la tira (empaquetamiento ideal sin huecos).
     */
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
}
