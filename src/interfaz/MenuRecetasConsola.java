package interfaz;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import modelo.Ingrediente;
import modelo.Insumo;
import modelo.Plato;
import modelo.ValidacionException;
import negocio.InventarioServicio;
import negocio.MenuServicio;

/**
 * Opciones del módulo 2 (RF06–RF10).
 */
public class MenuRecetasConsola {

    private final MenuServicio servicio;
    private final InventarioServicio inventario;

    public MenuRecetasConsola(MenuServicio servicio, InventarioServicio inventario) {
        this.servicio = servicio;
        this.inventario = inventario;
    }

    public void mostrar() {
        int opcion;
        do {
            Consola.titulo("MÓDULO 2: INGENIERÍA DE MENÚ Y RECETAS");
            System.out.println("1. Registrar plato con su receta");
            System.out.println("2. Consultar catálogo de platos (buscar por nombre)");
            System.out.println("3. Ver ficha técnica de un plato (receta y costeo)");
            System.out.println("4. Actualizar plato o receta");
            System.out.println("5. Eliminar plato (o inactivar si tiene ventas)");
            System.out.println("6. Ver platos con advertencia de pérdida");
            System.out.println("0. Volver al menú principal");
            opcion = Consola.leerOpcion(0, 6);
            try {
                switch (opcion) {
                    case 1: registrar(); break;
                    case 2: consultar(); break;
                    case 3: fichaTecnica(); break;
                    case 4: actualizar(); break;
                    case 5: eliminar(); break;
                    case 6: perdidas(); break;
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
        Consola.subtitulo("Registrar plato");
        String nombre = Consola.leerTexto("Nombre del plato");
        double pvp = Consola.leerDecimalPositivo("PVP sin IVA ($)");
        Map<String, Double> receta = new LinkedHashMap<>();
        System.out.println("\nInsumos disponibles:");
        imprimirInsumosCompacto();
        System.out.println("Agregue los ingredientes (escriba «fin» en el código para terminar).");
        while (true) {
            String codigo = Consola.leerLinea("Código del insumo: ");
            if (codigo.equalsIgnoreCase("fin")) {
                if (receta.isEmpty()) {
                    Consola.error("La receta debe tener al menos un ingrediente.");
                    continue;
                }
                break;
            }
            try {
                Insumo insumo = inventario.buscar(codigo);
                double cantidad = Consola.leerDecimalPositivo("Cantidad por porción (" + insumo.getUnidadMedida() + ")");
                receta.put(insumo.getCodigo(), cantidad);
                Consola.exito("Agregado: " + insumo.getNombre() + " × " + Insumo.formatoCantidad(cantidad) + " " + insumo.getUnidadMedida());
            } catch (ValidacionException e) {
                Consola.error(e.getMessage());
            }
        }
        Plato plato = servicio.registrarPlato(nombre, pvp, receta);
        Consola.exito("Plato registrado con el código " + plato.getCodigo() + ".");
        imprimirFicha(plato);
    }

    private void consultar() {
        Consola.subtitulo("Catálogo de platos");
        boolean todos = Consola.confirmar("¿Incluir platos inactivos?");
        String texto = Consola.leerTextoOpcional("Buscar por nombre");
        List<Plato> lista = texto.isEmpty() ? servicio.listar(todos) : servicio.buscarPorNombre(texto, todos);
        if (!texto.isEmpty()) {
            System.out.println("Platos encontrados: " + lista.size());
        }
        imprimirTabla(lista);
        if (lista.size() == 1) {
            imprimirFicha(lista.get(0));
        } else if (!lista.isEmpty() && Consola.confirmar("¿Ver el detalle de un plato?")) {
            imprimirFicha(servicio.buscar(Consola.leerTexto("Código del plato")));
        }
    }

    private void fichaTecnica() {
        imprimirTabla(servicio.listar(true));
        Plato plato = servicio.buscar(Consola.leerTexto("Código del plato"));
        imprimirFicha(plato);
    }

    private void actualizar() {
        Consola.subtitulo("Actualizar plato");
        imprimirTabla(servicio.listar(true));
        Plato plato = servicio.buscar(Consola.leerTexto("Código del plato"));
        System.out.println("1. Cambiar nombre");
        System.out.println("2. Cambiar PVP");
        System.out.println("3. Agregar ingrediente o modificar su cantidad");
        System.out.println("4. Quitar ingrediente");
        System.out.println("5. Reactivar plato inactivo");
        int op = Consola.leerEntero("Opción", 1, 5);
        switch (op) {
            case 1:
                servicio.actualizarNombre(plato.getCodigo(), Consola.leerTexto("Nuevo nombre"));
                Consola.exito("Nombre actualizado.");
                break;
            case 2:
                System.out.println("PVP actual: " + Consola.dinero(plato.getPvp()) + " | Costo de producción: " + Consola.dinero(plato.getCostoProduccion()));
                servicio.actualizarPvp(plato.getCodigo(), Consola.leerDecimalPositivo("Nuevo PVP sin IVA ($)"));
                Consola.exito("PVP actualizado. Las ventas anteriores conservan su precio original.");
                break;
            case 3:
                imprimirInsumosCompacto();
                Insumo insumo = inventario.buscar(Consola.leerTexto("Código del insumo"));
                double cantidad = Consola.leerDecimalPositivo("Cantidad por porción (" + insumo.getUnidadMedida() + ")");
                servicio.agregarOModificarIngrediente(plato.getCodigo(), insumo.getCodigo(), cantidad);
                Consola.exito("Receta actualizada.");
                break;
            case 4:
                imprimirReceta(plato);
                servicio.quitarIngrediente(plato.getCodigo(), Consola.leerTexto("Código del insumo a quitar"));
                Consola.exito("Ingrediente quitado.");
                break;
            default:
                servicio.reactivar(plato.getCodigo());
                Consola.exito("Plato reactivado.");
                break;
        }
        imprimirFicha(plato);
    }

    private void eliminar() {
        Consola.subtitulo("Eliminar plato");
        imprimirTabla(servicio.listar(true));
        Plato plato = servicio.buscar(Consola.leerTexto("Código del plato"));
        if (servicio.tieneVentas(plato)) {
            Consola.aviso("«" + plato.getNombre() + "» tiene ventas registradas: no se borrará, pasará a Inactivo.");
        }
        if (Consola.confirmar("¿Confirma la operación sobre «" + plato.getNombre() + "»?")) {
            Consola.exito(servicio.eliminar(plato.getCodigo()));
        } else {
            System.out.println("Operación cancelada.");
        }
    }

    private void perdidas() {
        Consola.subtitulo("Platos con advertencia de pérdida (PVP <= costo)");
        List<Plato> lista = servicio.platosConPerdida();
        if (lista.isEmpty()) {
            Consola.exito("Todos los platos activos tienen margen positivo.");
            return;
        }
        for (Plato p : lista) {
            System.out.println(p.getCodigo() + " " + p.getNombre() + " -> " + p.getAdvertenciaPrecio());
        }
    }

    // ---------------- impresión ----------------

    void imprimirTabla(List<Plato> platos) {
        if (platos.isEmpty()) {
            Consola.aviso("No hay platos para mostrar.");
            return;
        }
        String formato = "%-8s %-22s %9s %9s %9s %9s %-9s %8s %s%n";
        int ancho = 110;
        Consola.linea(ancho);
        System.out.printf(formato, "Código", "Plato", "PVP", "Costo", "Margen", "Margen %", "Estado", "Porciones", "");
        Consola.linea(ancho);
        for (Plato p : platos) {
            String advertencia = p.getAdvertenciaPrecio().isEmpty() ? "" : "<< PÉRDIDA";
            System.out.printf(formato, p.getCodigo(), Consola.recortar(p.getNombre(), 22), Consola.dinero(p.getPvp()),
                    Consola.dinero(p.getCostoProduccion()), Consola.dinero(p.getMargenUnitario()),
                    String.format("%.2f%%", p.getMargenPorcentual()), p.getEstado(),
                    String.valueOf(servicio.porcionesDisponibles(p)), advertencia);
        }
        Consola.linea(ancho);
    }

    void imprimirFicha(Plato plato) {
        Consola.subtitulo("Ficha técnica: " + plato.getCodigo() + " " + plato.getNombre() + " [" + plato.getEstado() + "]");
        imprimirReceta(plato);
        System.out.println("Costo de producción : " + Consola.dinero(plato.getCostoProduccion()));
        System.out.println("PVP (sin IVA)       : " + Consola.dinero(plato.getPvp()));
        System.out.println("Margen unitario     : " + Consola.dinero(plato.getMargenUnitario())
                + " (" + String.format("%.2f", plato.getMargenPorcentual()) + " %)");
        System.out.println("Porciones posibles  : " + servicio.porcionesDisponibles(plato) + " (según stock actual)");
        if (!plato.getAdvertenciaPrecio().isEmpty()) {
            Consola.aviso(plato.getAdvertenciaPrecio());
        }
    }

    private void imprimirReceta(Plato plato) {
        String formato = "%-8s %-20s %10s %-7s %10s %10s%n";
        Consola.linea(70);
        System.out.printf(formato, "Código", "Insumo", "Cantidad", "Unidad", "Costo U.", "Costo");
        Consola.linea(70);
        for (Ingrediente ing : plato.getReceta()) {
            Insumo i = ing.getInsumo();
            System.out.printf(formato, i.getCodigo(), Consola.recortar(i.getNombre(), 20),
                    Insumo.formatoCantidad(ing.getCantidadPorPorcion()), Consola.recortar(i.getUnidadMedida(), 7),
                    String.format("%.4f", i.getCostoUnitario()), String.format("%.4f", ing.getCosto()));
        }
        Consola.linea(70);
    }

    private void imprimirInsumosCompacto() {
        for (Insumo i : inventario.listar()) {
            System.out.printf("  %-8s %-20s (%s, %s %s disponibles)%n", i.getCodigo(), i.getNombre(), i.getUnidadMedida(),
                    Insumo.formatoCantidad(i.getStock()), i.getUnidadMedida());
        }
    }
}
