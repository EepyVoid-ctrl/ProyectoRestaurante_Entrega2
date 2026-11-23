package interfaz;

import java.time.LocalDate;
import java.util.List;

import modelo.Insumo;
import modelo.InsumoPerecedero;
import modelo.Plato;
import modelo.ValidacionException;
import negocio.InventarioServicio;

/**
 * Opciones del módulo 1 (RF01–RF05).
 */
public class MenuInventarioConsola {

    private final InventarioServicio servicio;

    public MenuInventarioConsola(InventarioServicio servicio) {
        this.servicio = servicio;
    }

    public void mostrar() {
        int opcion;
        do {
            Consola.titulo("MÓDULO 1: GESTIÓN DE INVENTARIO Y MATERIA PRIMA");
            System.out.println("1. Registrar insumo");
            System.out.println("2. Consultar inventario completo");
            System.out.println("3. Buscar y filtrar insumos");
            System.out.println("4. Reabastecer insumo (costo promedio ponderado)");
            System.out.println("5. Actualizar datos del insumo (nombre, categoría, stock mínimo)");
            System.out.println("6. Eliminar insumo");
            System.out.println("7. Ver alertas de reabastecimiento y caducidad");
            System.out.println("0. Volver al menú principal");
            opcion = Consola.leerOpcion(0, 7);
            try {
                switch (opcion) {
                    case 1: registrar(); break;
                    case 2: consultar(); break;
                    case 3: filtrar(); break;
                    case 4: reabastecer(); break;
                    case 5: actualizarDatos(); break;
                    case 6: eliminar(); break;
                    case 7: alertas(); break;
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

    // ---------------- opciones ----------------

    private void registrar() {
        Consola.subtitulo("Registrar insumo");
        System.out.println("Tipo de insumo: 1. Perecedero   2. No perecedero");
        int tipo = Consola.leerEntero("Tipo", 1, 2);
        String nombre = Consola.leerTexto("Nombre");
        String categoria = Consola.leerTexto("Categoría");
        String unidad = Consola.leerTexto("Unidad de medida (kg, litro, unidad...)");
        double cantidad = Consola.leerDecimalPositivo("Cantidad comprada (" + unidad + ")");
        double costoTotal = Consola.leerDecimalPositivo("Costo total de compra ($)");
        double stockMinimo = Consola.leerDecimalPositivo("Stock mínimo (" + unidad + ")");
        Insumo insumo;
        if (tipo == 1) {
            LocalDate fecha = Consola.leerFecha("Fecha de caducidad");
            insumo = servicio.registrarPerecedero(nombre, categoria, unidad, cantidad, costoTotal, stockMinimo, fecha);
        } else {
            insumo = servicio.registrarNoPerecedero(nombre, categoria, unidad, cantidad, costoTotal, stockMinimo);
        }
        Consola.exito("Insumo registrado con el código " + insumo.getCodigo() + ". Costo unitario: "
                + Consola.dinero(insumo.getCostoUnitario()) + " por " + insumo.getUnidadMedida()
                + " (" + Consola.dinero(costoTotal) + " ÷ " + Insumo.formatoCantidad(cantidad) + ").");
        imprimirTabla(List.of(insumo));
        mostrarAlertaReabastecimiento();
    }

    /** RF05: se evalúa después de cada movimiento de inventario. */
    private void mostrarAlertaReabastecimiento() {
        List<Insumo> criticos = servicio.filtrarCriticos();
        if (criticos.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Insumo i : criticos) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(i.getNombre()).append(" (").append(Insumo.formatoCantidad(i.getStock())).append(" ").append(i.getUnidadMedida()).append(")");
        }
        Consola.aviso("ALERTA DE REABASTECIMIENTO - Insumos en estado Crítico: " + sb);
    }

    private void consultar() {
        Consola.subtitulo("Inventario completo");
        imprimirTabla(servicio.listar());
        System.out.println("Valor total del inventario: " + Consola.dinero(servicio.getValorTotalInventario()));
    }

    private void filtrar() {
        Consola.subtitulo("Buscar y filtrar");
        System.out.println("1. Por código");
        System.out.println("2. Por nombre");
        System.out.println("3. Por categoría");
        System.out.println("4. Por estado (Normal / Crítico)");
        System.out.println("5. Por tipo (perecedero / no perecedero)");
        int op = Consola.leerEntero("Criterio", 1, 5);
        switch (op) {
            case 1:
                Insumo insumo = servicio.buscar(Consola.leerTexto("Código"));
                imprimirTabla(List.of(insumo));
                List<Plato> usados = servicio.platosQueUsan(insumo);
                System.out.print("Usado en recetas: ");
                if (usados.isEmpty()) {
                    System.out.println("ninguna");
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (Plato p : usados) {
                        if (sb.length() > 0) {
                            sb.append(", ");
                        }
                        sb.append(p.getNombre());
                    }
                    System.out.println(sb);
                }
                break;
            case 2:
                imprimirTabla(servicio.filtrarPorNombre(Consola.leerTexto("Nombre (o parte)")));
                break;
            case 3:
                System.out.println("Categorías existentes: " + String.join(", ", servicio.listarCategorias()));
                imprimirTabla(servicio.filtrarPorCategoria(Consola.leerTexto("Categoría")));
                break;
            case 4:
                System.out.println("1. Normal   2. Crítico");
                imprimirTabla(servicio.filtrarPorEstado(Consola.leerEntero("Estado", 1, 2) == 2));
                break;
            default:
                System.out.println("1. Perecedero   2. No perecedero");
                imprimirTabla(servicio.filtrarPorTipo(Consola.leerEntero("Tipo", 1, 2) == 1));
                break;
        }
    }

    private void reabastecer() {
        Consola.subtitulo("Reabastecer insumo");
        imprimirTabla(servicio.listar());
        Insumo insumo = servicio.buscar(Consola.leerTexto("Código del insumo"));
        double stockAnterior = insumo.getStock();
        double costoAnterior = insumo.getCostoUnitario();
        System.out.printf("Stock actual: %s %s | Costo unitario actual: %s%n",
                Insumo.formatoCantidad(stockAnterior), insumo.getUnidadMedida(), Consola.dinero(costoAnterior));
        double cantidad = Consola.leerDecimalPositivo("Cantidad comprada (" + insumo.getUnidadMedida() + ")");
        double costoTotal = Consola.leerDecimalPositivo("Costo TOTAL de la compra ($)");
        LocalDate nuevaFecha = null;
        if (insumo instanceof InsumoPerecedero
                && Consola.confirmar("¿El lote nuevo tiene otra fecha de caducidad?")) {
            nuevaFecha = Consola.leerFecha("Nueva fecha de caducidad");
        }
        servicio.reabastecer(insumo.getCodigo(), cantidad, costoTotal, nuevaFecha);
        Consola.exito("Reabastecimiento registrado.");
        System.out.printf("Cálculo RT1: (%s × %.4f + %.2f) / (%s + %s) = %.4f%n",
                Insumo.formatoCantidad(stockAnterior), costoAnterior, costoTotal,
                Insumo.formatoCantidad(stockAnterior), Insumo.formatoCantidad(cantidad), insumo.getCostoUnitario());
        imprimirTabla(List.of(insumo));
        List<Plato> afectados = servicio.platosQueUsan(insumo);
        if (!afectados.isEmpty()) {
            System.out.println("Platos con costo de producción recalculado (RF10):");
            for (Plato p : afectados) {
                System.out.printf("  %-8s %-22s costo %s | PVP %s%s%n", p.getCodigo(), p.getNombre(),
                        Consola.dinero(p.getCostoProduccion()), Consola.dinero(p.getPvp()),
                        p.getAdvertenciaPrecio().isEmpty() ? "" : "  << PÉRDIDA");
            }
        }
        mostrarAlertaReabastecimiento();
    }

    private void actualizarDatos() {
        Consola.subtitulo("Actualizar datos del insumo");
        imprimirTabla(servicio.listar());
        Insumo insumo = servicio.buscar(Consola.leerTexto("Código del insumo"));
        System.out.println("Deje vacío para conservar el valor actual.");
        String nombre = Consola.leerTextoOpcional("Nombre [" + insumo.getNombre() + "]");
        String categoria = Consola.leerTextoOpcional("Categoría [" + insumo.getCategoria() + "]");
        double stockMinimo = insumo.getStockMinimo();
        while (true) {
            String texto = Consola.leerTextoOpcional("Stock mínimo [" + Insumo.formatoCantidad(insumo.getStockMinimo()) + "]");
            if (texto.isEmpty()) {
                break;
            }
            try {
                stockMinimo = modelo.Validar.positivo(modelo.Validar.decimal(texto, "Stock mínimo"), "Stock mínimo");
                break;
            } catch (ValidacionException e) {
                Consola.error(e.getMessage());
            }
        }
        servicio.actualizarDatos(insumo.getCodigo(),
                nombre.isEmpty() ? insumo.getNombre() : nombre,
                categoria.isEmpty() ? insumo.getCategoria() : categoria,
                stockMinimo);
        Consola.exito("Insumo actualizado.");
        imprimirTabla(List.of(insumo));
        mostrarAlertaReabastecimiento();
    }

    private void eliminar() {
        Consola.subtitulo("Eliminar insumo");
        imprimirTabla(servicio.listar());
        Insumo insumo = servicio.buscar(Consola.leerTexto("Código del insumo a eliminar"));
        if (!servicio.platosQueUsan(insumo).isEmpty()) {
            servicio.eliminar(insumo.getCodigo()); // lanza la ValidacionException de RT6 con el detalle
            return;
        }
        if (Consola.confirmar("¿Seguro que desea eliminar «" + insumo.getNombre() + "»?")) {
            servicio.eliminar(insumo.getCodigo());
            Consola.exito("Insumo «" + insumo.getNombre() + "» eliminado.");
        } else {
            System.out.println("Operación cancelada.");
        }
    }

    private void alertas() {
        Consola.subtitulo("Alertas del inventario");
        List<String> alertas = servicio.generarAlertas();
        if (alertas.isEmpty()) {
            Consola.exito("No hay alertas: todo el inventario está en orden.");
            return;
        }
        for (String alerta : alertas) {
            System.out.println("  " + alerta);
        }
        System.out.println("Total de alertas: " + alertas.size());
    }

    // ---------------- tabla ----------------

    static void imprimirTabla(List<Insumo> insumos) {
        if (insumos.isEmpty()) {
            Consola.aviso("No se encontraron insumos.");
            return;
        }
        String formato = "%-8s %-18s %-14s %-14s %10s %8s %-7s %10s %-8s %-20s%n";
        int ancho = 126;
        Consola.linea(ancho);
        System.out.printf(formato, "Código", "Nombre", "Categoría", "Tipo", "Stock", "Mínimo", "Unidad", "Costo U.", "Estado", "Conservación");
        Consola.linea(ancho);
        for (Insumo i : insumos) {
            System.out.printf(formato, i.getCodigo(), Consola.recortar(i.getNombre(), 18), Consola.recortar(i.getCategoria(), 14),
                    i.getTipo(), Insumo.formatoCantidad(i.getStock()), Insumo.formatoCantidad(i.getStockMinimo()),
                    Consola.recortar(i.getUnidadMedida(), 7), String.format("%.4f", i.getCostoUnitario()),
                    i.getEstado(), i.getAlertaConservacion());
        }
        Consola.linea(ancho);
        System.out.println("Registros: " + insumos.size());
    }
}
