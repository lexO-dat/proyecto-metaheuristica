package spp;

/**
 * Contenedor del 2D Strip Packing Problem.
 *
 * En el SPP el ancho es FIJO (la "tira"/strip) y la altura es ilimitada.
 * El objetivo del problema es MINIMIZAR la altura total utilizada
 * (altoActual) una vez colocados todos los objetos.
 */
public class Contenedor {

    private final int anchoFijo;   // ancho de la tira (constante)
    private int altoActual;        // altura alcanzada tras la colocacion (a minimizar)

    public Contenedor(int anchoFijo) {
        this.anchoFijo = anchoFijo;
        this.altoActual = 0;
    }

    public int getAnchoFijo()  { return anchoFijo; }
    public int getAltoActual() { return altoActual; }

    public void setAltoActual(int altoActual) { this.altoActual = altoActual; }
}
