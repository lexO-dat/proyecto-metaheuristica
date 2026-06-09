package spp;

import java.util.Arrays;

/**
 * Particula del enjambre (PSO).
 *
 * CODIFICACION (random keys):
 *  - 'posicion[k]' es una clave continua en [0,1] asociada al objeto k.
 *  - El ORDEN (permutacion) que se pasa a Bottom-Left se obtiene ordenando los
 *    objetos por su clave de menor a mayor. Asi, una posicion continua
 *    representa INDIRECTAMENTE un orden discreto de objetos.
 *
 * El PSO discreto aplica sobre estas claves:
 *  - una velocidad continua (ecuacion estandar de PSO), y
 *  - una funcion de transferencia en V + operacion de inversion de bit
 *    (ver clase EnjambrePSO).
 */
public class Particula {

    public double[] posicion;       // x_i (claves continuas, dimension = nObjetos)
    public double[] velocidad;      // v_i

    public double[] mejorPosicion;  // pBest (mejor posicion personal encontrada)
    public double   mejorFitness;   // fitness (altura) de pBest

    public double   fitnessActual;  // fitness de la posicion actual

    public Particula(int dimension) {
        this.posicion      = new double[dimension];
        this.velocidad     = new double[dimension];
        this.mejorPosicion = new double[dimension];
        this.mejorFitness  = Double.POSITIVE_INFINITY;
        this.fitnessActual = Double.POSITIVE_INFINITY;
    }

    /** Guarda la posicion actual como mejor personal (pBest). */
    public void actualizarMejorPersonal() {
        this.mejorFitness  = this.fitnessActual;
        this.mejorPosicion = Arrays.copyOf(this.posicion, this.posicion.length);
    }
}
