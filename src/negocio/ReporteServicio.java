package negocio;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import modelo.DetalleVenta;
import modelo.Insumo;
import modelo.Plato;
import modelo.ValidacionException;
import modelo.Validar;
import modelo.Venta;

/**
 * RF15–RF16: rentabilidad por plato y consumo y rotación de insumos.
 * RT2: los reportes usan los valores guardados en DetalleVenta (precio, costo y consumo al vender),
 * no los valores actuales del plato o del insumo.
 */
public class ReporteServicio {

    private final BaseDatos bd;

    public ReporteServicio(BaseDatos bd) {
        this.bd = bd;
    }

    /** Fila del reporte de rentabilidad (RF15). */
    public static class FilaRentabilidad implements Serializable {
        private static final long serialVersionUID = 1L;
        public final Plato plato;
        public final int unidades;
        public final double ingresos;
        public final double costoTotal;

        FilaRentabilidad(Plato plato, int unidades, double ingresos, double costoTotal) {
            this.plato = plato;
            this.unidades = unidades;
            this.ingresos = Validar.redondear(ingresos);
            this.costoTotal = Validar.redondear(costoTotal);
        }

        /** Costo de producción promedio por unidad vendida (según lo guardado en cada venta). */
        public double getCostoUnitario() { return Validar.redondear(costoTotal / unidades); }

        /** PVP promedio por unidad vendida (según lo guardado en cada venta). */
        public double getPvp() { return Validar.redondear(ingresos / unidades); }

        public double getMargenUnitario() { return Validar.redondear(getPvp() - getCostoUnitario()); }

        /** RT7: PVP = 0 → margen % = 0. */
        public double getMargenPorcentual() {
            return ingresos == 0 ? 0 : Validar.redondear((ingresos - costoTotal) / ingresos * 100);
        }

        public double getUtilidadTotal() { return Validar.redondear(ingresos - costoTotal); }
    }

    /** Fila del reporte de consumo y rotación (RF16). */
    public static class FilaConsumo implements Serializable {
        private static final long serialVersionUID = 1L;
        public final Insumo insumo;
        public final double consumoTotal;
        public final long diasPeriodo;

        FilaConsumo(Insumo insumo, double consumoTotal, long diasPeriodo) {
            this.insumo = insumo;
            this.consumoTotal = Validar.redondearCantidad(consumoTotal);
            this.diasPeriodo = diasPeriodo;
        }

        public double getConsumoDiario() {
            return Validar.redondear(consumoTotal / diasPeriodo);
        }

        /** RT8: consumo diario = 0 → «sin consumo» (no se divide entre cero). */
        public String getDiasCobertura() {
            double diario = consumoTotal / diasPeriodo;
            if (diario <= 0) {
                return "sin consumo";
            }
            return String.format("%.2f", Validar.redondear(insumo.getStock() / diario));
        }
    }

    /** RT8: rechaza rangos con «desde» posterior a «hasta». */
    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ValidacionException("Debe indicar las fechas «desde» y «hasta».");
        }
        if (desde.isAfter(hasta)) {
            throw new ValidacionException("Rango de fechas inválido: «desde» (" + desde + ") es posterior a «hasta» (" + hasta + ").");
        }
    }

    private List<Venta> ventasDelPeriodo(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        List<Venta> resultado = new ArrayList<>();
        for (Venta venta : bd.getVentas()) {
            LocalDate dia = venta.getFecha().toLocalDate();
            if (!dia.isBefore(desde) && !dia.isAfter(hasta)) {
                resultado.add(venta);
            }
        }
        return resultado;
    }

    /** RF15: rentabilidad por plato vendido en el período, ordenada por utilidad total (mayor a menor). */
    public List<FilaRentabilidad> rentabilidadPorPlato(LocalDate desde, LocalDate hasta) {
        Map<Plato, double[]> acumulado = new LinkedHashMap<>(); // unidades, ingresos, costo
        for (Venta venta : ventasDelPeriodo(desde, hasta)) {
            for (DetalleVenta d : venta.getDetalles()) {
                double[] a = acumulado.computeIfAbsent(d.getPlato(), k -> new double[3]);
                a[0] += d.getCantidad();
                a[1] += d.getSubtotal();
                a[2] += d.getCostoTotal();
            }
        }
        List<FilaRentabilidad> filas = new ArrayList<>();
        for (Map.Entry<Plato, double[]> e : acumulado.entrySet()) {
            double[] a = e.getValue();
            filas.add(new FilaRentabilidad(e.getKey(), (int) a[0], a[1], a[2]));
        }
        filas.sort((x, y) -> Double.compare(y.getUtilidadTotal(), x.getUtilidadTotal()));
        return filas;
    }

    /**
     * RF16: consumo de cada insumo en el período (según lo guardado en las ventas), consumo diario
     * promedio y días de cobertura. Período sin ventas → lista vacía (la interfaz muestra el aviso).
     *
     * @param categoria opcional: null o vacío = todas las categorías
     */
    public List<FilaConsumo> consumoInsumos(LocalDate desde, LocalDate hasta, String categoria) {
        List<Venta> ventas = ventasDelPeriodo(desde, hasta);
        List<FilaConsumo> filas = new ArrayList<>();
        if (ventas.isEmpty()) {
            return filas;
        }
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        Map<Insumo, Double> consumo = new LinkedHashMap<>();
        // Todos los insumos actuales (para mostrar también los «sin consumo»).
        for (Insumo insumo : bd.getInsumos()) {
            consumo.put(insumo, 0.0);
        }
        for (Venta venta : ventas) {
            for (DetalleVenta d : venta.getDetalles()) {
                for (Map.Entry<Insumo, Double> e : d.getConsumoInsumos().entrySet()) {
                    consumo.merge(e.getKey(), e.getValue(), Double::sum);
                }
            }
        }
        boolean filtrar = categoria != null && !categoria.trim().isEmpty();
        for (Map.Entry<Insumo, Double> e : consumo.entrySet()) {
            if (filtrar && !e.getKey().getCategoria().equalsIgnoreCase(categoria.trim())) {
                continue;
            }
            filas.add(new FilaConsumo(e.getKey(), e.getValue(), dias));
        }
        filas.sort((x, y) -> Double.compare(y.consumoTotal, x.consumoTotal));
        return filas;
    }
}
