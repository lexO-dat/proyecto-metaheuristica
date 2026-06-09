package spp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Optimizacion por Enjambre de Particulas (PSO) DISCRETO para el 2D Strip
 * Packing Problem.
 *
 * Como el PSO clasico es continuo, se adapta al espacio discreto del problema
 * (un ORDEN de objetos) mediante:
 *
 *   (A) Codificacion por "random keys": cada particula tiene una clave continua
 *       por objeto; el orden se obtiene ordenando esas claves. La heuristica
 *       Bottom-Left evalua ese orden y devuelve la altura (fitness a minimizar).
 *
 *   (B) Funcion de transferencia en V (arco tangente) que convierte la
 *       velocidad continua en una PROBABILIDAD DE CAMBIO de estado.
 *
 *   (C) Actualizacion discreta de la posicion por INVERSION DE BIT: con dicha
 *       probabilidad, la clave de la dimension se "invierte" (complemento en
 *       [0,1]); en caso contrario, conserva su estado previo.
 */
public class EnjambrePSO {

    // ---- Parametros del PSO ----
    private final int numParticulas;
    private final int maxIteraciones;
    private final double c1;        // coeficiente cognitivo (atraccion a pBest)
    private final double c2;        // coeficiente social    (atraccion a gBest)
    private final double wInicial;  // inercia inicial
    private final double wFinal;    // inercia final (decae linealmente)
    private final double vMax;      // limite de velocidad

    // ---- Datos del problema ----
    private final List<Objeto> objetos;
    private final double anchoContenedor;
    private final int dimension;            // = numero de objetos

    private final Random rand;

    // ---- Mejor global ----
    private double[] gBestPosicion;
    private double   gBestFitness = Double.POSITIVE_INFINITY;

    public EnjambrePSO(List<Objeto> objetos, double anchoContenedor,
                       int numParticulas, int maxIteraciones, long semilla) {
        this.objetos         = objetos;
        this.anchoContenedor = anchoContenedor;
        this.dimension       = objetos.size();
        this.numParticulas   = numParticulas;
        this.maxIteraciones  = maxIteraciones;
        this.c1 = 2.0;
        this.c2 = 2.0;
        this.wInicial = 0.9;
        this.wFinal   = 0.4;
        this.vMax     = 6.0;
        this.rand     = new Random(semilla);
    }

    /**
     * Ejecuta el ciclo principal del PSO y devuelve el mejor resultado (la
     * colocacion Bottom-Left correspondiente al mejor orden encontrado).
     */
    public ResultadoBL optimizar() {
        List<Particula> enjambre = inicializarEnjambre();

        // Evaluacion inicial y determinacion del gBest.
        for (Particula p : enjambre) {
            p.fitnessActual = evaluar(p.posicion);
            p.actualizarMejorPersonal();
            considerarGlobal(p);
        }

        // ---- Ciclo principal ----
        for (int iter = 0; iter < maxIteraciones; iter++) {
            // Inercia con decaimiento lineal.
            double w = wInicial - (wInicial - wFinal) * iter / (double) maxIteraciones;

            for (Particula p : enjambre) {
                for (int k = 0; k < dimension; k++) {
                    // --- Ecuacion estandar de velocidad del PSO ---
                    // v = w*v + c1*r1*(pBest - x) + c2*r2*(gBest - x)
                    double r1 = rand.nextDouble();
                    double r2 = rand.nextDouble();
                    double cognitivo = c1 * r1 * (p.mejorPosicion[k] - p.posicion[k]);
                    double social    = c2 * r2 * (gBestPosicion[k]   - p.posicion[k]);
                    double v = w * p.velocidad[k] + cognitivo + social;

                    // Acotar velocidad a [-vMax, vMax].
                    if (v >  vMax) v =  vMax;
                    if (v < -vMax) v = -vMax;
                    p.velocidad[k] = v;

                    // ---FUNCION DE TRANSFERENCIA EN V ---
                    // V(v) = | (2/pi) * arctan( (pi/2) * v ) |
                    double prob = Math.abs((2.0 / Math.PI)
                            * Math.atan((Math.PI / 2.0) * v));

                    // --- ACTUALIZACION DISCRETA POR INVERSION DE BIT ---
                    // si rand < V(v): x = (x)^-1  (complemento de la clave en [0,1])
                    // si rand >= V(v): x conserva su estado previo
                    if (rand.nextDouble() < prob) {
                        p.posicion[k] = 1.0 - p.posicion[k];   // inversion
                    }
                    // (else) -> se mantiene p.posicion[k]
                }

                // Evaluar la nueva posicion con Bottom-Left.
                p.fitnessActual = evaluar(p.posicion);

                // Actualizar pBest y gBest.
                if (p.fitnessActual < p.mejorFitness) {
                    p.actualizarMejorPersonal();
                }
                considerarGlobal(p);
            }
        }

        // Reconstruir y devolver la mejor solucion global.
        return BottomLeft.colocar(decodificarOrden(gBestPosicion), anchoContenedor);
    }

    // Inicializacion

    private List<Particula> inicializarEnjambre() {
        List<Particula> enjambre = new ArrayList<>(numParticulas);

        for (int i = 0; i < numParticulas; i++) {
            Particula p = new Particula(dimension);
            for (int k = 0; k < dimension; k++) {
                p.posicion[k]  = rand.nextDouble();              // clave en [0,1]
                p.velocidad[k] = (rand.nextDouble() * 2 - 1);    // en [-1,1]
            }
            enjambre.add(p);
        }

        // SOLUCION INICIAL HEURISTICA: se siembra la primera particula con el
        // orden por AREA descendente (orden tipico que da buenos resultados a
        // Bottom-Left). Las claves se fijan segun el rango de cada objeto.
        sembrarOrdenHeuristico(enjambre.get(0));

        return enjambre;
    }

    /**
     * Fija las claves de una particula para que su orden decodificado sea
     * exactamente el de los objetos ordenados por area (de mayor a menor).
     */
    private void sembrarOrdenHeuristico(Particula p) {
        Integer[] indices = new Integer[dimension];
        for (int i = 0; i < dimension; i++) indices[i] = i;

        // Mayor area primero.
        Arrays.sort(indices, Comparator.comparingDouble(
                (Integer idx) -> objetos.get(idx).getArea()).reversed());

        // Asignar claves crecientes segun la posicion en el ranking, de modo
        // que al ordenar por clave se recupere este mismo orden.
        for (int rank = 0; rank < dimension; rank++) {
            int idxObjeto = indices[rank];
            p.posicion[idxObjeto] = (rank + 1.0) / (dimension + 1.0);
        }
    }

    // Evaluacion / decodificacion

    /** Fitness = altura alcanzada por Bottom-Left sobre el orden decodificado. */
    private double evaluar(double[] posicion) {
        return BottomLeft.colocar(decodificarOrden(posicion), anchoContenedor).getAltura();
    }

    /**
     * Decodifica una posicion continua en una permutacion de objetos
     * (regla "smallest position value": ordenar por clave ascendente).
     */
    private List<Objeto> decodificarOrden(double[] posicion) {
        Integer[] indices = new Integer[dimension];
        for (int i = 0; i < dimension; i++) indices[i] = i;

        Arrays.sort(indices, Comparator.comparingDouble(idx -> posicion[idx]));

        List<Objeto> orden = new ArrayList<>(dimension);
        for (int idx : indices) {
            orden.add(objetos.get(idx));
        }
        return orden;
    }

    private void considerarGlobal(Particula p) {
        if (p.fitnessActual < gBestFitness) {
            gBestFitness  = p.fitnessActual;
            gBestPosicion = Arrays.copyOf(p.posicion, p.posicion.length);
        }
    }

    public double getGBestFitness() { return gBestFitness; }
}
