package spp;

import java.util.List;

/**
 * Resultado de aplicar la heuristica Bottom-Left a una permutacion de objetos.
 *  - altura    : altura total alcanzada (fitness a minimizar).
 *  - colocados : lista de objetos en el orden en que fueron colocados,
 *                ya con sus coordenadas (x, y) asignadas.
 */
public class ResultadoBL {

    private final double altura;
    private final List<Objeto> colocados;

    public ResultadoBL(double altura, List<Objeto> colocados) {
        this.altura = altura;
        this.colocados = colocados;
    }

    public double getAltura()           { return altura; }
    public List<Objeto> getColocados()  { return colocados; }
}
