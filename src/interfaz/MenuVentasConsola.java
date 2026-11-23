package interfaz;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import modelo.Cliente;
import modelo.Factura;
import modelo.Insumo;
import modelo.MetodoPago;
import modelo.PagoEfectivo;
import modelo.PagoTarjeta;
import modelo.PagoTransferencia;
import modelo.Plato;
import modelo.ValidacionException;
import modelo.Validar;
import negocio.MenuServicio;
import negocio.VentaServicio;

/**
 * Opciones del módulo 3 (RF11–RF14).
 */
public class MenuVentasConsola {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final VentaServicio servicio;
    private final MenuServicio menu;

    public MenuVentasConsola(VentaServicio servicio, MenuServicio menu) {
        this.servicio = servicio;
        this.menu = menu;
    }

    public void mostrar() {
        int opcion;
        do {
            Consola.titulo("MÓDULO 3: VENTAS Y FACTURACIÓN");
            System.out.println("1. Registrar venta y emitir factura");
            System.out.println("2. Historial de ventas");
            System.out.println("3. Consultar factura por número");
            System.out.println("4. Ventas por cliente (identificación)");
            System.out.println("5. Ventas por rango de fechas");
            System.out.println("0. Volver al menú principal");
            opcion = Consola.leerOpcion(0, 5);
            try {
                switch (opcion) {
                    case 1: registrarVenta(); break;
                    case 2: historial(servicio.listarFacturas()); break;
                    case 3: consultarFactura(); break;
                    case 4: porCliente(); break;
                    case 5: porFechas(); break;
                    default: break;
                }
            } catch (ValidacionException e) {
                Consola.error(e.getMessage());
            }
            if (opcion != 0) {
                Consola.pausa();
            }
        } while (opcion != 0);
    }

    // ---------------- registrar venta ----------------

    private void registrarVenta() {
        Consola.subtitulo("Nueva venta");
        List<Plato> activos = menu.listar(false);
        if (activos.isEmpty()) {
            Consola.aviso("No hay platos activos en el menú.");
            return;
        }
        imprimirCarta(activos);

        Map<String, Integer> pedido = new LinkedHashMap<>();
        System.out.println("Agregue platos al pedido (escriba «fin» en el código para terminar, «x» para cancelar).");
        while (true) {
            String codigo = Consola.leerLinea("Código del plato: ");
            if (codigo.equalsIgnoreCase("x")) {
                System.out.println("Venta cancelada. No se modificó el inventario.");
                return;
            }
            if (codigo.equalsIgnoreCase("fin")) {
                if (pedido.isEmpty()) {
                    Consola.error("El pedido está vacío: agregue al menos un plato.");
                    continue;
                }
                break;
            }
            try {
                Plato plato = menu.buscar(codigo);
                if (!plato.isActivo()) {
                    throw new ValidacionException("El plato «" + plato.getNombre() + "» está Inactivo y no se puede vender.");
                }
                int cantidad = Consola.leerEntero("Cantidad", 1, 999);
                pedido.merge(plato.getCodigo(), cantidad, Integer::sum);
                Consola.exito(plato.getNombre() + " × " + cantidad + " agregado al pedido.");
            } catch (ValidacionException e) {
                Consola.error(e.getMessage());
            }
        }

        // Resumen y verificación previa (no descuenta nada).
        Map<Plato, Integer> items = servicio.resolverPedido(pedido);
        Consola.subtitulo("Resumen del pedido");
        double subtotal = 0;
        for (Map.Entry<Plato, Integer> e : items.entrySet()) {
            double linea = Validar.redondear(e.getKey().getPvp() * e.getValue());
            subtotal += linea;
            System.out.printf("  %3d × %-22s %8s = %9s%n", e.getValue(), e.getKey().getNombre(),
                    Consola.dinero(e.getKey().getPvp()), Consola.dinero(linea));
        }
        subtotal = Validar.redondear(subtotal);
        double iva = Validar.redondear(subtotal * Factura.IVA);
        System.out.printf("  Subtotal: %s | IVA 15 %%: %s | Total: %s%n", Consola.dinero(subtotal),
                Consola.dinero(iva), Consola.dinero(Validar.redondear(subtotal + iva)));

        List<String> problemas = servicio.verificarDisponibilidad(items);
        if (!problemas.isEmpty()) {
            Consola.error("VENTA RECHAZADA. No se descontó ningún insumo. Detalle:");
            for (String p : problemas) {
                System.out.println("   - " + p);
            }
            return;
        }
        Consola.exito("Existencias verificadas: hay insumos suficientes para todo el pedido.");

        Cliente cliente = leerCliente();
        MetodoPago metodo = leerMetodoPago();
        System.out.println("Instrucción de cobro: " + metodo.getInstruccionCobro());

        if (!Consola.confirmar("¿Confirmar la venta?")) {
            System.out.println("Venta cancelada. No se modificó el inventario.");
            return;
        }
        Factura factura = servicio.registrarVenta(pedido, metodo, cliente);
        Consola.exito("Venta " + factura.getVenta().getNumero() + " registrada. Inventario descontado automáticamente.");
        System.out.println();
        System.out.print(factura.generarTexto());
        // RF12 → RF05: tras el descuento se evalúa la alerta de reabastecimiento.
        List<Insumo> criticos = servicio.insumosCriticosTrasVenta(factura);
        if (!criticos.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Insumo i : criticos) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(i.getNombre()).append(" (").append(Insumo.formatoCantidad(i.getStock())).append(" ")
                        .append(i.getUnidadMedida()).append(", mínimo ").append(Insumo.formatoCantidad(i.getStockMinimo())).append(")");
            }
            Consola.aviso("ALERTA DE REABASTECIMIENTO tras la venta - Insumos en estado Crítico: " + sb);
        }
    }

    private Cliente leerCliente() {
        Consola.subtitulo("Datos de facturación");
        System.out.println("1. Consumidor Final");
        System.out.println("2. Con datos (cédula o RUC)");
        int op = Consola.leerEntero("Opción", 1, 2);
        if (op == 1) {
            return Cliente.consumidorFinal();
        }
        String identificacion;
        while (true) {
            try {
                identificacion = Validar.identificacion(Consola.leerLinea("Cédula (10 dígitos) o RUC (13 dígitos): "));
                break;
            } catch (ValidacionException e) {
                Consola.error(e.getMessage());
            }
        }
        Cliente registrado = servicio.buscarClienteRegistrado(identificacion);
        if (registrado != null && Consola.confirmar("Cliente encontrado: " + registrado.getNombre() + ". ¿Usar estos datos?")) {
            return registrado;
        }
        String nombre = Consola.leerTexto("Nombre y apellido");
        String contacto = Consola.leerTextoOpcional("Correo o teléfono");
        return new Cliente(nombre, identificacion, contacto);
    }

    private MetodoPago leerMetodoPago() {
        Consola.subtitulo("Método de pago");
        System.out.println("1. Efectivo");
        System.out.println("2. Tarjeta");
        System.out.println("3. Transferencia");
        int op = Consola.leerEntero("Opción", 1, 3);
        switch (op) {
            case 1: return new PagoEfectivo();
            case 2: return new PagoTarjeta();
            default: return new PagoTransferencia();
        }
    }

    // ---------------- consultas ----------------

    private void consultarFactura() {
        historial(servicio.listarFacturas());
        Factura factura = servicio.buscarFactura(Consola.leerTexto("Número de factura (completo o solo el secuencial)"));
        System.out.println();
        System.out.print(factura.generarTexto());
    }

    private void porCliente() {
        String id = Consola.leerTexto("Identificación del cliente (9999999999999 = Consumidor Final)");
        historial(servicio.buscarPorCliente(id));
    }

    private void porFechas() {
        LocalDate desde = Consola.leerFecha("Desde");
        LocalDate hasta = Consola.leerFecha("Hasta");
        historial(servicio.buscarPorFecha(desde, hasta));
    }

    private void historial(List<Factura> facturas) {
        Consola.subtitulo("Historial de ventas");
        if (facturas.isEmpty()) {
            Consola.aviso("No hay ventas registradas para este criterio.");
            return;
        }
        String formato = "%-18s %-6s %-17s %-20s %-14s %9s %8s %9s%n";
        int ancho = 108;
        Consola.linea(ancho);
        System.out.printf(formato, "Factura", "Venta", "Fecha", "Cliente", "Pago", "Subtotal", "IVA", "Total");
        Consola.linea(ancho);
        double totalSubtotal = 0;
        double totalIva = 0;
        double total = 0;
        for (Factura f : facturas) {
            System.out.printf(formato, f.getNumero(), f.getVenta().getNumero(),
                    f.getVenta().getFecha().format(FORMATO), Consola.recortar(f.getCliente().getNombre(), 20),
                    f.getVenta().getMetodoPago().getNombre(), String.format("%.2f", f.getSubtotal()),
                    String.format("%.2f", f.getIva()), String.format("%.2f", f.getTotal()));
            totalSubtotal += f.getSubtotal();
            totalIva += f.getIva();
            total += f.getTotal();
        }
        Consola.linea(ancho);
        System.out.printf(formato, "TOTALES", "", "", facturas.size() + " venta(s)", "",
                String.format("%.2f", Validar.redondear(totalSubtotal)), String.format("%.2f", Validar.redondear(totalIva)),
                String.format("%.2f", Validar.redondear(total)));
    }

    private void imprimirCarta(List<Plato> activos) {
        System.out.println("Carta disponible:");
        for (Plato p : activos) {
            System.out.printf("  %-8s %-22s %8s   (porciones posibles: %d)%n", p.getCodigo(), p.getNombre(),
                    Consola.dinero(p.getPvp()), menu.porcionesDisponibles(p));
        }
    }
}
