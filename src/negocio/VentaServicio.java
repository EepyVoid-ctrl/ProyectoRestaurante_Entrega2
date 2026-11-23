package negocio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import modelo.Cliente;
import modelo.DetalleVenta;
import modelo.Factura;
import modelo.Ingrediente;
import modelo.Insumo;
import modelo.InsumoPerecedero;
import modelo.MetodoPago;
import modelo.Plato;
import modelo.ValidacionException;
import modelo.Validar;
import modelo.Venta;

/**
 * RF11–RF14: valida existencias, descuenta inventario, crea la venta y la factura, consulta el historial.
 */
public class VentaServicio {

    private final BaseDatos bd;

    public VentaServicio(BaseDatos bd) {
        this.bd = bd;
    }

    // ---------------- validar existencias ----------------

    /** Convierte codigoPlato -> cantidad en Plato -> cantidad, validando cada línea. */
    public Map<Plato, Integer> resolverPedido(Map<String, Integer> pedido) {
        if (pedido == null || pedido.isEmpty()) {
            throw new ValidacionException("El pedido está vacío: agregue al menos un plato.");
        }
        Map<Plato, Integer> resultado = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : pedido.entrySet()) {
            Plato plato = bd.buscarPlato(Validar.texto(e.getKey(), "Código del plato"));
            if (plato == null) {
                throw new ValidacionException("No existe un plato con el código «" + e.getKey().trim() + "».");
            }
            if (!plato.isActivo()) {
                throw new ValidacionException("El plato «" + plato.getNombre() + "» está Inactivo y no se puede vender.");
            }
            if (plato.getReceta().isEmpty()) {
                throw new ValidacionException("El plato «" + plato.getNombre() + "» no tiene receta registrada.");
            }
            Integer cantidad = e.getValue();
            if (cantidad == null || cantidad <= 0) {
                throw new ValidacionException("La cantidad de «" + plato.getNombre() + "» debe ser mayor que cero.");
            }
            resultado.merge(plato, cantidad, Integer::sum);
        }
        return resultado;
    }

    /** Cantidad total requerida de cada insumo para todo el pedido. */
    private Map<Insumo, Double> calcularRequerimientos(Map<Plato, Integer> pedido) {
        Map<Insumo, Double> requeridos = new LinkedHashMap<>();
        for (Map.Entry<Plato, Integer> e : pedido.entrySet()) {
            for (Ingrediente ingrediente : e.getKey().getReceta()) {
                requeridos.merge(ingrediente.getInsumo(), ingrediente.getCantidadPorPorcion() * e.getValue(), Double::sum);
            }
        }
        requeridos.replaceAll((k, v) -> Validar.redondearCantidad(v));
        return requeridos;
    }

    /**
     * RT4, RT5 y RT7: revisa todo el pedido y devuelve la lista de problemas (vacía si se puede vender).
     * No modifica el inventario.
     */
    public List<String> verificarDisponibilidad(Map<Plato, Integer> pedido) {
        List<String> problemas = new ArrayList<>();
        for (Map.Entry<Insumo, Double> e : calcularRequerimientos(pedido).entrySet()) {
            Insumo insumo = e.getKey();
            double requerido = e.getValue();
            if (insumo instanceof InsumoPerecedero && ((InsumoPerecedero) insumo).estaVencido()) {
                problemas.add(insumo.getNombre() + " (" + insumo.getCodigo() + "): insumo perecedero VENCIDO (caducó el "
                        + ((InsumoPerecedero) insumo).getFechaCaducidad() + ").");
            }
            if (requerido > insumo.getStock() + 1e-9) {
                double falta = Validar.redondearCantidad(requerido - insumo.getStock());
                problemas.add(insumo.getNombre() + " (" + insumo.getCodigo() + "): requerido "
                        + Insumo.formatoCantidad(requerido) + " " + insumo.getUnidadMedida()
                        + ", disponible " + Insumo.formatoCantidad(insumo.getStock()) + " " + insumo.getUnidadMedida()
                        + ", faltan " + Insumo.formatoCantidad(falta) + " " + insumo.getUnidadMedida() + ".");
            }
        }
        return problemas;
    }

    // ---------------- registrar venta ----------------

    public Factura registrarVenta(Map<String, Integer> pedido, MetodoPago metodoPago, Cliente cliente) {
        return registrarVenta(pedido, metodoPago, cliente, LocalDateTime.now());
    }

    /**
     * Venta atómica (RT5): primero se valida TODO el pedido; si hay un faltante o un insumo vencido
     * se rechaza la venta completa sin descontar nada. Solo si todo está bien se descuenta el
     * inventario, se crea la venta con la instantánea de precios/costos (RT2, RT3) y su factura.
     */
    public Factura registrarVenta(Map<String, Integer> pedido, MetodoPago metodoPago, Cliente cliente, LocalDateTime fecha) {
        if (metodoPago == null) {
            throw new ValidacionException("Debe seleccionar un método de pago.");
        }
        Map<Plato, Integer> items = resolverPedido(pedido);
        List<String> problemas = verificarDisponibilidad(items);
        if (!problemas.isEmpty()) {
            throw new ValidacionException("VENTA RECHAZADA. No se descontó ningún insumo. Detalle:\n   - "
                    + String.join("\n   - ", problemas));
        }

        // 1) Instantánea de precio, costo y consumo ANTES de tocar el inventario.
        List<DetalleVenta> detalles = new ArrayList<>();
        for (Map.Entry<Plato, Integer> e : items.entrySet()) {
            detalles.add(new DetalleVenta(e.getKey(), e.getValue()));
        }

        // 2) Descuento de inventario (ya validado, no puede fallar por stock).
        for (Map.Entry<Insumo, Double> e : calcularRequerimientos(items).entrySet()) {
            e.getKey().descontar(e.getValue());
        }

        // 3) Venta y factura.
        Venta venta = new Venta(bd.siguienteNumeroVenta(), fecha, detalles, metodoPago);
        Factura factura = new Factura(bd.siguienteNumeroFactura(), venta, cliente == null ? Cliente.consumidorFinal() : cliente);
        bd.getVentas().add(venta);
        bd.getFacturas().add(factura);
        bd.guardar();
        return factura;
    }

    /** RF12 → RF05: tras el descuento, insumos de la venta que quedaron en estado Crítico. */
    public List<Insumo> insumosCriticosTrasVenta(Factura factura) {
        List<Insumo> criticos = new ArrayList<>();
        for (DetalleVenta detalle : factura.getVenta().getDetalles()) {
            for (Insumo insumo : detalle.getConsumoInsumos().keySet()) {
                if (insumo.esCritico() && !criticos.contains(insumo)) {
                    criticos.add(insumo);
                }
            }
        }
        return criticos;
    }

    // ---------------- historial ----------------

    public List<Factura> listarFacturas() {
        return new ArrayList<>(bd.getFacturas());
    }

    public Factura buscarFactura(String numero) {
        String limpio = Validar.texto(numero, "Número de factura");
        Factura factura = bd.buscarFactura(limpio);
        if (factura == null && limpio.matches("\\d+")) {
            // Permite buscar solo con el secuencial: 3 -> 001-001-000000003
            factura = bd.buscarFactura(String.format("001-001-%09d", Long.parseLong(limpio)));
        }
        if (factura == null) {
            throw new ValidacionException("No existe la factura «" + limpio + "».");
        }
        return factura;
    }

    public List<Factura> buscarPorCliente(String identificacion) {
        String id = Validar.texto(identificacion, "Identificación");
        List<Factura> resultado = new ArrayList<>();
        for (Factura factura : bd.getFacturas()) {
            if (factura.getCliente().getIdentificacion().equals(id)) {
                resultado.add(factura);
            }
        }
        return resultado;
    }

    public List<Factura> buscarPorFecha(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ValidacionException("Debe indicar las dos fechas del rango.");
        }
        if (hasta.isBefore(desde)) {
            throw new ValidacionException("La fecha final no puede ser anterior a la fecha inicial.");
        }
        List<Factura> resultado = new ArrayList<>();
        for (Factura factura : bd.getFacturas()) {
            LocalDate dia = factura.getVenta().getFecha().toLocalDate();
            if (!dia.isBefore(desde) && !dia.isAfter(hasta)) {
                resultado.add(factura);
            }
        }
        return resultado;
    }

    /** Busca un cliente ya facturado por su identificación (para no volver a digitar sus datos). */
    public Cliente buscarClienteRegistrado(String identificacion) {
        for (int i = bd.getFacturas().size() - 1; i >= 0; i--) {
            Cliente c = bd.getFacturas().get(i).getCliente();
            if (c.getIdentificacion().equals(identificacion)) {
                return c;
            }
        }
        return null;
    }
}
