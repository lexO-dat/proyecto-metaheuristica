package spp;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Lectura de los casos de prueba en formato CSV.
 *
 * Formato esperado:
 *   Linea 1 (cabecera): [Ancho contenedor],[Solucion optima esperada],[NA]
 *   Lineas siguientes : [ID_Objeto],[Alto],[Ancho]
 *
 * Ejemplo:
 *   20,20,NA
 *   1,14,4
 *   2,2,5
 */
public class LectorCSV {

    /** Estructura simple que agrupa los datos leidos de un caso de prueba. */
    public static class CasoPrueba {
        public final String nombreArchivo;
        public final double anchoContenedor;
        public final double optimoEsperado;
        public final List<Objeto> objetos;

        public CasoPrueba(String nombreArchivo, double anchoContenedor,
                          double optimoEsperado, List<Objeto> objetos) {
            this.nombreArchivo = nombreArchivo;
            this.anchoContenedor = anchoContenedor;
            this.optimoEsperado = optimoEsperado;
            this.objetos = objetos;
        }
    }

    public static CasoPrueba leer(Path archivo) throws IOException {
        List<Objeto> objetos = new ArrayList<>();
        double anchoContenedor = 0;
        double optimoEsperado = -1;

        try (BufferedReader br = Files.newBufferedReader(archivo)) {
            String linea;
            boolean cabecera = true;

            int numLinea = 0;
            while ((linea = br.readLine()) != null) {
                numLinea++;
                linea = linea.trim();
                if (linea.isEmpty()) {
                    continue;
                }
                // Separador flexible: coma, punto y coma, tabulador o espacios.
                String[] campos = linea.split("[,;\\t ]+");

                if (cabecera) {
                    // [Ancho contenedor],[Optimo esperado],[NA]
                    if (campos.length < 1) {
                        throw new IOException("Cabecera invalida en " + archivo.getFileName()
                                + " (linea " + numLinea + "): \"" + linea + "\"");
                    }
                    anchoContenedor = Double.parseDouble(campos[0].trim());
                    optimoEsperado = campos.length >= 2 ? parsearDecimal(campos[1].trim(), -1) : -1;
                    cabecera = false;
                } else {
                    // [ID],[Alto],[Ancho]
                    if (campos.length < 3) {
                        // Linea de objeto incompleta: se ignora.
                        continue;
                    }
                    int    id    = Integer.parseInt(campos[0].trim());
                    double alto  = Double.parseDouble(campos[1].trim());
                    double ancho = Double.parseDouble(campos[2].trim());
                    objetos.add(new Objeto(id, alto, ancho));
                }
            }
        }

        String nombre = archivo.getFileName().toString();
        return new CasoPrueba(nombre, anchoContenedor, optimoEsperado, objetos);
    }

    private static double parsearDecimal(String s, double porDefecto) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return porDefecto; // p.ej. "NA"
        }
    }
}
