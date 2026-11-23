package modelo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Insumo con fecha de caducidad; alerta «Vence en N día(s)» o «VENCIDO».
 */
public class InsumoPerecedero extends Insumo {
    private static final long serialVersionUID = 1L;

    private LocalDate fechaCaducidad;

    public InsumoPerecedero(String codigo, String nombre, String categoria, String unidadMedida,
                            double stock, double stockMinimo, double costoUnitario, LocalDate fechaCaducidad) {
        super(codigo, nombre, categoria, unidadMedida, stock, stockMinimo, costoUnitario);
        setFechaCaducidad(fechaCaducidad);
    }

    public LocalDate getFechaCaducidad() { return fechaCaducidad; }

    public void setFechaCaducidad(LocalDate fechaCaducidad) {
        if (fechaCaducidad == null) {
            throw new ValidacionException("La fecha de caducidad es obligatoria para un insumo perecedero.");
        }
        this.fechaCaducidad = fechaCaducidad;
    }

    /** Días que faltan para vencer (negativo si ya venció). */
    public long getDiasParaVencer() {
        return ChronoUnit.DAYS.between(LocalDate.now(), fechaCaducidad);
    }

    /** RT7: un insumo vencido no puede usarse en ventas. Vence al terminar el día de caducidad. */
    public boolean estaVencido() {
        return getDiasParaVencer() < 0;
    }

    @Override public String getTipo() { return "Perecedero"; }

    @Override public String getAlertaConservacion() {
        long dias = getDiasParaVencer();
        if (dias < 0) {
            return "VENCIDO";
        }
        return "Vence en " + dias + " día(s)";
    }
}
