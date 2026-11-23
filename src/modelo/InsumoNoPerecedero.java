package modelo;

/**
 * Insumo de larga duración, sin alerta de caducidad.
 */
public class InsumoNoPerecedero extends Insumo {
    private static final long serialVersionUID = 1L;

    public InsumoNoPerecedero(String codigo, String nombre, String categoria, String unidadMedida,
                              double stock, double stockMinimo, double costoUnitario) {
        super(codigo, nombre, categoria, unidadMedida, stock, stockMinimo, costoUnitario);
    }

    @Override public String getTipo() { return "No perecedero"; }

    @Override public String getAlertaConservacion() { return ""; }
}
