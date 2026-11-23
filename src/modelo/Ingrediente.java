package modelo;

import java.io.Serializable;

/**
 * Línea de receta: un insumo y su cantidad por porción; calcula su costo.
 */
public class Ingrediente implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Insumo insumo;
    private double cantidadPorPorcion;

    public Ingrediente(Insumo insumo, double cantidadPorPorcion) {
        if (insumo == null) {
            throw new ValidacionException("El ingrediente debe estar asociado a un insumo existente.");
        }
        this.insumo = insumo;
        setCantidadPorPorcion(cantidadPorPorcion);
    }

    /** Costo del ingrediente en una porción, según el costo unitario actual del insumo. */
    public double getCosto() {
        return insumo.getCostoUnitario() * cantidadPorPorcion;
    }

    public Insumo getInsumo() { return insumo; }

    public double getCantidadPorPorcion() { return cantidadPorPorcion; }

    public void setCantidadPorPorcion(double cantidadPorPorcion) {
        this.cantidadPorPorcion = Validar.redondearCantidad(Validar.positivo(cantidadPorPorcion, "Cantidad por porción"));
    }

    @Override
    public String toString() {
        return insumo.getNombre() + ": " + Insumo.formatoCantidad(cantidadPorPorcion) + " " + insumo.getUnidadMedida();
    }
}
