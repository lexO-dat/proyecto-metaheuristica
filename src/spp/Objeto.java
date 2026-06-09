package spp;

/**
 * Representa un objeto/rectangulo a empaquetar en el contenedor.
 *
 * Las dimensiones son de tipo double porque varias instancias benchmark
 * (p.ej. Wang-Valenzuela) usan medidas decimales.
 *
 * Campos geometricos:
 *  - alto, ancho : dimensiones del rectangulo (NO rota).
 *  - x, y        : coordenadas de la esquina inferior-izquierda asignadas
 *                  por la heuristica Bottom-Left. El origen (0,0) es la
 *                  esquina inferior-izquierda del contenedor.
 */
public class Objeto {

    private final int id;
    private final double alto;
    private final double ancho;

    // Coordenadas asignadas por Bottom-Left (NaN = sin colocar)
    private double x = Double.NaN;
    private double y = Double.NaN;

    public Objeto(int id, double alto, double ancho) {
        this.id = id;
        this.alto = alto;
        this.ancho = ancho;
    }

    /** Copia "limpia" (sin coordenadas) para reutilizar el objeto en una nueva colocacion. */
    public Objeto copiaLimpia() {
        return new Objeto(id, alto, ancho);
    }

    public int    getId()    { return id; }
    public double getAlto()  { return alto; }
    public double getAncho() { return ancho; }
    public double getArea()  { return alto * ancho; }

    public double getX() { return x; }
    public double getY() { return y; }

    public void setPosicion(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /** Bordes utiles para los tests de solapamiento. */
    public double getDerecha() { return x + ancho; }   // borde derecho (eje X)
    public double getTope()    { return y + alto;  }    // borde superior (eje Y)

    @Override
    public String toString() {
        return String.format("Obj[%d] (alto=%.3f, ancho=%.3f) -> (x=%.3f, y=%.3f)",
                id, alto, ancho, x, y);
    }
}
