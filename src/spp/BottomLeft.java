package spp;

import java.util.ArrayList;
import java.util.List;

/**
 * Heuristica de colocacion Bottom-Left con un "skyline" (perfil superior)
 * representado por SEGMENTOS de coordenada continua. Soporta dimensiones
 * enteras y decimales.
 *
 * El perfil se guarda como una lista de segmentos ordenados por X, donde cada
 * segmento {xIni, altura} cubre [xIni, xIniDelSiguiente) y el ultimo llega
 * hasta el ancho del contenedor.
 *
 * Para colocar un objeto de ancho w (orden Bottom-Left):
 *   - Las posiciones X candidatas son los inicios de los segmentos del perfil
 *     (puntos donde el perfil "escalona"); para cada X tal que el objeto cabe
 *     [X, X+w] dentro del ancho, la Y de apoyo es el MAXIMO del perfil en esa
 *     ventana.
 *   - Se elige la X con menor Y de apoyo y, a igualdad, la X mas a la izquierda.
 *   - Tras colocar, el perfil sube a (Y + alto) en [X, X+w).
 *
 * La altura final alcanzada es el FITNESS que el PSO minimiza.
 */
public final class BottomLeft {

    private BottomLeft() { }

    private static final double EPS = 1e-7;

    public static ResultadoBL colocar(List<Objeto> ordenObjetos, double anchoContenedor) {
        // Perfil inicial: un unico segmento a altura 0 que cubre todo el ancho.
        // xInicios.get(k) -> inicio del segmento k ; alturas.get(k) -> su altura.
        List<Double> xInicios = new ArrayList<>();
        List<Double> alturas  = new ArrayList<>();
        xInicios.add(0.0);
        alturas.add(0.0);

        List<Objeto> colocados = new ArrayList<>(ordenObjetos.size());
        double alturaTotal = 0.0;

        for (Objeto original : ordenObjetos) {
            Objeto obj = original.copiaLimpia();
            double w = obj.getAncho();

            double mejorX = 0.0;
            double mejorY;

            if (w >= anchoContenedor - EPS) {
                // Degenerado: tan ancho como la tira -> se apoya sobre todo el perfil.
                mejorX = 0.0;
                mejorY = maxEnRango(xInicios, alturas, anchoContenedor, 0.0, anchoContenedor);
            } else {
                mejorY = Double.POSITIVE_INFINITY;
                int n = xInicios.size();
                // Probar cada inicio de segmento como X candidata (de izq. a der.).
                for (int k = 0; k < n; k++) {
                    double x = xInicios.get(k);
                    if (x + w > anchoContenedor + EPS) {
                        break; // los siguientes inicios estan aun mas a la derecha
                    }
                    double y = maxEnRango(xInicios, alturas, anchoContenedor, x, x + w);
                    if (y < mejorY - EPS) {   // menor Y; empate -> conserva X menor (ya recorremos de izq a der)
                        mejorY = y;
                        mejorX = x;
                        if (y <= EPS) break;  // nada mas bajo que el suelo
                    }
                }
            }

            obj.setPosicion(mejorX, mejorY);
            colocados.add(obj);

            double tope = mejorY + obj.getAlto();
            double fin = Math.min(mejorX + w, anchoContenedor);
            elevarPerfil(xInicios, alturas, anchoContenedor, mejorX, fin, tope);

            if (tope > alturaTotal) alturaTotal = tope;
        }

        return new ResultadoBL(alturaTotal, colocados);
    }

    /** Maximo del perfil en el intervalo [a, b). */
    private static double maxEnRango(List<Double> xInicios, List<Double> alturas,
                                     double W, double a, double b) {
        double m = 0.0;
        int n = xInicios.size();
        for (int k = 0; k < n; k++) {
            double segIni = xInicios.get(k);
            double segFin = (k + 1 < n) ? xInicios.get(k + 1) : W;
            if (segFin <= a + EPS) continue;   // segmento totalmente a la izquierda
            if (segIni >= b - EPS) break;      // segmento totalmente a la derecha
            if (alturas.get(k) > m) m = alturas.get(k);
        }
        return m;
    }

    /** Eleva el perfil a 'nuevaAltura' en el intervalo [a, b) y fusiona segmentos iguales. */
    private static void elevarPerfil(List<Double> xInicios, List<Double> alturas,
                                     double W, double a, double b, double nuevaAltura) {
        insertarCorte(xInicios, alturas, W, a);
        insertarCorte(xInicios, alturas, W, b);

        int n = xInicios.size();
        for (int k = 0; k < n; k++) {
            double segIni = xInicios.get(k);
            double segFin = (k + 1 < n) ? xInicios.get(k + 1) : W;
            if (segFin <= a + EPS) continue;
            if (segIni >= b - EPS) break;
            alturas.set(k, nuevaAltura);
        }
        fusionar(xInicios, alturas);
    }

    /** Inserta un corte (frontera de segmento) en x, dividiendo el segmento que lo contiene. */
    private static void insertarCorte(List<Double> xInicios, List<Double> alturas,
                                      double W, double x) {
        if (x <= EPS || x >= W - EPS) return;   // 0 y W son fronteras implicitas
        int n = xInicios.size();
        for (int k = 0; k < n; k++) {
            double segIni = xInicios.get(k);
            double segFin = (k + 1 < n) ? xInicios.get(k + 1) : W;
            if (Math.abs(segIni - x) <= EPS) return;       // ya existe un corte ahi
            if (x > segIni + EPS && x < segFin - EPS) {     // x cae dentro de este segmento
                xInicios.add(k + 1, x);
                alturas.add(k + 1, alturas.get(k));         // misma altura del segmento dividido
                return;
            }
        }
    }

    /** Une segmentos adyacentes con la misma altura para mantener la lista compacta. */
    private static void fusionar(List<Double> xInicios, List<Double> alturas) {
        for (int k = xInicios.size() - 1; k >= 1; k--) {
            if (Math.abs(alturas.get(k) - alturas.get(k - 1)) <= EPS) {
                xInicios.remove(k);
                alturas.remove(k);
            }
        }
    }
}
