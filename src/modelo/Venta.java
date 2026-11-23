package modelo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Transacción con número, fecha, detalles y método de pago; calcula el subtotal.
 * Composición UML: 1 venta tiene 1..* DetalleVenta. Asociación: * ventas usan 1 MetodoPago.
 */
public class Venta implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String numero;
    private final LocalDateTime fecha;
    private final List<DetalleVenta> detalles;
    private final MetodoPago metodoPago;

    public Venta(String numero, LocalDateTime fecha, List<DetalleVenta> detalles, MetodoPago metodoPago) {
        this.numero = Validar.texto(numero, "Número de venta");
        if (detalles == null || detalles.isEmpty()) {
            throw new ValidacionException("La venta debe tener al menos un plato.");
        }
        if (metodoPago == null) {
            throw new ValidacionException("Debe seleccionar un método de pago.");
        }
        this.fecha = (fecha == null) ? LocalDateTime.now() : fecha;
        this.detalles = new ArrayList<>(detalles);
        this.metodoPago = metodoPago;
    }

    /** Suma de los subtotales de los detalles (sin IVA). */
    public double getSubtotal() {
        double total = 0;
        for (DetalleVenta detalle : detalles) {
            total += detalle.getSubtotal();
        }
        return Validar.redondear(total);
    }

    /** RT6: indica si la venta contiene el plato. */
    public boolean incluyePlato(Plato plato) {
        if (plato == null) {
            return false;
        }
        for (DetalleVenta detalle : detalles) {
            if (detalle.getPlato().getCodigo().equals(plato.getCodigo())) {
                return true;
            }
        }
        return false;
    }

    public String getNumero() { return numero; }

    public LocalDateTime getFecha() { return fecha; }

    public List<DetalleVenta> getDetalles() { return Collections.unmodifiableList(detalles); }

    public MetodoPago getMetodoPago() { return metodoPago; }
}
