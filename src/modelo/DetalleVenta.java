package modelo;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Línea de venta con la «foto» del precio, costo y consumo de insumos al momento de vender.
 * RT2 y RT3: precioUnitario, costoUnitario y consumoInsumos se copian al crear el detalle;
 * los cambios posteriores de precio, receta o costo no modifican las ventas ya registradas.
 * Asociación UML: * detalles referencian 1 Plato.
 */
public class DetalleVenta implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Plato plato;
    private final int cantidad;
    private final double precioUnitario;
    private final double costoUnitario;
    /** Insumo -> cantidad total consumida en esta línea (porción × cantidad). */
    private final Map<Insumo, Double> consumoInsumos;

    /** Crea el detalle tomando la instantánea del plato en este momento. */
    public DetalleVenta(Plato plato, int cantidad) {
        if (plato == null) {
            throw new ValidacionException("El detalle de venta requiere un plato.");
        }
        if (cantidad <= 0) {
            throw new ValidacionException("La cantidad de porciones debe ser mayor que cero.");
        }
        this.plato = plato;
        this.cantidad = cantidad;
        this.precioUnitario = plato.getPvp();
        this.costoUnitario = plato.getCostoProduccion();
        Map<Insumo, Double> consumo = new LinkedHashMap<>();
        for (Ingrediente ingrediente : plato.getReceta()) {
            consumo.merge(ingrediente.getInsumo(),
                    Validar.redondearCantidad(ingrediente.getCantidadPorPorcion() * cantidad), Double::sum);
        }
        this.consumoInsumos = consumo;
    }

    /** Precio guardado × cantidad. */
    public double getSubtotal() {
        return Validar.redondear(precioUnitario * cantidad);
    }

    /** Costo guardado × cantidad. */
    public double getCostoTotal() {
        return Validar.redondear(costoUnitario * cantidad);
    }

    /** (precio − costo) × cantidad, con los valores guardados al vender. */
    public double getUtilidad() {
        return Validar.redondear(getSubtotal() - getCostoTotal());
    }

    public Plato getPlato() { return plato; }

    public int getCantidad() { return cantidad; }

    public double getPrecioUnitario() { return precioUnitario; }

    public double getCostoUnitario() { return costoUnitario; }

    public Map<Insumo, Double> getConsumoInsumos() { return Collections.unmodifiableMap(consumoInsumos); }
}
