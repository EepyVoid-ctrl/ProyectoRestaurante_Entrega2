package negocio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import modelo.Insumo;
import modelo.InsumoNoPerecedero;
import modelo.InsumoPerecedero;
import modelo.Plato;
import modelo.ValidacionException;
import modelo.Validar;

/**
 * RF01–RF05: registrar, consultar y filtrar, reabastecer y eliminar insumos; genera alertas.
 */
public class InventarioServicio {

    private final BaseDatos bd;

    public InventarioServicio(BaseDatos bd) {
        this.bd = bd;
    }

    // ---------------- registrar ----------------

    /**
     * RF01: registra un insumo perecedero. El costo unitario se calcula como
     * costo total de compra ÷ cantidad comprada; la cantidad comprada es el stock inicial.
     */
    public Insumo registrarPerecedero(String nombre, String categoria, String unidadMedida, double cantidadComprada,
                                      double costoTotalCompra, double stockMinimo, LocalDate fechaCaducidad) {
        validarNombreUnico(nombre, null);
        validarCompra(cantidadComprada, costoTotalCompra, stockMinimo);
        if (fechaCaducidad == null) {
            throw new ValidacionException("La fecha de caducidad es obligatoria para un insumo perecedero.");
        }
        if (fechaCaducidad.isBefore(LocalDate.now())) {
            throw new ValidacionException("No se puede registrar un insumo ya vencido (fecha de caducidad anterior a hoy).");
        }
        double costoUnitario = costoTotalCompra / cantidadComprada;
        // Se construye primero (valida todo) y solo entonces se consume el código.
        new InsumoPerecedero("TMP", nombre, categoria, unidadMedida, cantidadComprada, stockMinimo, costoUnitario, fechaCaducidad);
        InsumoPerecedero insumo = new InsumoPerecedero(bd.siguienteCodigoInsumo(), nombre, categoria, unidadMedida,
                cantidadComprada, stockMinimo, costoUnitario, fechaCaducidad);
        bd.getInsumos().add(insumo);
        bd.guardar();
        return insumo;
    }

    /** RF01: registra un insumo no perecedero (costo unitario = costo total ÷ cantidad). */
    public Insumo registrarNoPerecedero(String nombre, String categoria, String unidadMedida, double cantidadComprada,
                                        double costoTotalCompra, double stockMinimo) {
        validarNombreUnico(nombre, null);
        validarCompra(cantidadComprada, costoTotalCompra, stockMinimo);
        double costoUnitario = costoTotalCompra / cantidadComprada;
        new InsumoNoPerecedero("TMP", nombre, categoria, unidadMedida, cantidadComprada, stockMinimo, costoUnitario);
        InsumoNoPerecedero insumo = new InsumoNoPerecedero(bd.siguienteCodigoInsumo(), nombre, categoria, unidadMedida,
                cantidadComprada, stockMinimo, costoUnitario);
        bd.getInsumos().add(insumo);
        bd.guardar();
        return insumo;
    }

    /** RT7: cantidades y costos menores o iguales a 0 se rechazan. */
    private void validarCompra(double cantidadComprada, double costoTotalCompra, double stockMinimo) {
        Validar.positivo(cantidadComprada, "Cantidad comprada");
        Validar.positivo(costoTotalCompra, "Costo total de compra");
        Validar.positivo(stockMinimo, "Stock mínimo");
    }

    private void validarNombreUnico(String nombre, Insumo excluido) {
        String limpio = Validar.texto(nombre, "Nombre");
        for (Insumo insumo : bd.getInsumos()) {
            if (insumo != excluido && insumo.getNombre().equalsIgnoreCase(limpio)) {
                throw new ValidacionException("Ya existe un insumo con el nombre «" + limpio + "» (" + insumo.getCodigo() + ").");
            }
        }
    }

    // ---------------- consultar y filtrar ----------------

    public List<Insumo> listar() {
        return new ArrayList<>(bd.getInsumos());
    }

    public Insumo buscar(String codigo) {
        Insumo insumo = bd.buscarInsumo(Validar.texto(codigo, "Código del insumo"));
        if (insumo == null) {
            throw new ValidacionException("No existe un insumo con el código «" + codigo.trim() + "».");
        }
        return insumo;
    }

    public List<Insumo> filtrarPorNombre(String texto) {
        String buscado = Validar.texto(texto, "Texto a buscar").toLowerCase();
        List<Insumo> resultado = new ArrayList<>();
        for (Insumo insumo : bd.getInsumos()) {
            if (insumo.getNombre().toLowerCase().contains(buscado)) {
                resultado.add(insumo);
            }
        }
        return resultado;
    }

    public List<Insumo> filtrarPorCategoria(String categoria) {
        String buscada = Validar.texto(categoria, "Categoría").toLowerCase();
        List<Insumo> resultado = new ArrayList<>();
        for (Insumo insumo : bd.getInsumos()) {
            if (insumo.getCategoria().toLowerCase().contains(buscada)) {
                resultado.add(insumo);
            }
        }
        return resultado;
    }

    /** @param perecederos true = perecederos, false = no perecederos */
    public List<Insumo> filtrarPorTipo(boolean perecederos) {
        List<Insumo> resultado = new ArrayList<>();
        for (Insumo insumo : bd.getInsumos()) {
            if ((insumo instanceof InsumoPerecedero) == perecederos) {
                resultado.add(insumo);
            }
        }
        return resultado;
    }

    /** @param criticos true = estado Crítico, false = estado Normal */
    public List<Insumo> filtrarPorEstado(boolean criticos) {
        List<Insumo> resultado = new ArrayList<>();
        for (Insumo insumo : bd.getInsumos()) {
            if (insumo.esCritico() == criticos) {
                resultado.add(insumo);
            }
        }
        return resultado;
    }

    public List<Insumo> filtrarCriticos() {
        List<Insumo> resultado = new ArrayList<>();
        for (Insumo insumo : bd.getInsumos()) {
            if (insumo.esCritico()) {
                resultado.add(insumo);
            }
        }
        return resultado;
    }

    public List<String> listarCategorias() {
        List<String> categorias = new ArrayList<>();
        for (Insumo insumo : bd.getInsumos()) {
            boolean existe = categorias.stream().anyMatch(c -> c.equalsIgnoreCase(insumo.getCategoria()));
            if (!existe) {
                categorias.add(insumo.getCategoria());
            }
        }
        return categorias;
    }

    // ---------------- reabastecer ----------------

    /**
     * RT1: reabastece con costo promedio ponderado. Si el insumo es perecedero y se indica una
     * nueva fecha de caducidad (lote nuevo), se actualiza.
     */
    public Insumo reabastecer(String codigo, double cantidad, double costoTotal, LocalDate nuevaFechaCaducidad) {
        Insumo insumo = buscar(codigo);
        Validar.positivo(cantidad, "Cantidad comprada");
        Validar.positivo(costoTotal, "Costo total de la compra");
        if (nuevaFechaCaducidad != null) {
            if (!(insumo instanceof InsumoPerecedero)) {
                throw new ValidacionException("Solo los insumos perecederos tienen fecha de caducidad.");
            }
            if (nuevaFechaCaducidad.isBefore(LocalDate.now())) {
                throw new ValidacionException("La nueva fecha de caducidad no puede ser anterior a hoy.");
            }
        }
        insumo.reabastecer(cantidad, costoTotal);
        if (nuevaFechaCaducidad != null) {
            ((InsumoPerecedero) insumo).setFechaCaducidad(nuevaFechaCaducidad);
        }
        bd.guardar();
        return insumo;
    }

    /**
     * RF03: modifica nombre, categoría y stock mínimo. Se valida todo antes de cambiar nada.
     * El stock solo cambia por reabastecimiento o venta (RT4), por eso no se edita aquí.
     */
    public Insumo actualizarDatos(String codigo, String nombre, String categoria, double stockMinimo) {
        Insumo insumo = buscar(codigo);
        String nuevoNombre = Validar.texto(nombre, "Nombre");
        String nuevaCategoria = Validar.texto(categoria, "Categoría");
        Validar.positivo(stockMinimo, "Stock mínimo");
        validarNombreUnico(nuevoNombre, insumo);
        insumo.setNombre(nuevoNombre);
        insumo.setCategoria(nuevaCategoria);
        insumo.setStockMinimo(stockMinimo);
        bd.guardar();
        return insumo;
    }

    // ---------------- eliminar ----------------

    /** Platos cuya receta usa el insumo. */
    public List<Plato> platosQueUsan(Insumo insumo) {
        List<Plato> resultado = new ArrayList<>();
        for (Plato plato : bd.getPlatos()) {
            if (plato.usaInsumo(insumo)) {
                resultado.add(plato);
            }
        }
        return resultado;
    }

    /** RT6: no se elimina un insumo usado en alguna receta (activa o inactiva). */
    public Insumo eliminar(String codigo) {
        Insumo insumo = buscar(codigo);
        List<Plato> usados = platosQueUsan(insumo);
        if (!usados.isEmpty()) {
            StringBuilder nombres = new StringBuilder();
            for (Plato plato : usados) {
                if (nombres.length() > 0) {
                    nombres.append(", ");
                }
                nombres.append(plato.getNombre()).append(" (").append(plato.getCodigo()).append(")");
            }
            throw new ValidacionException("No se puede eliminar «" + insumo.getNombre()
                    + "» porque se usa en la receta de: " + nombres + ".");
        }
        bd.getInsumos().remove(insumo);
        bd.guardar();
        return insumo;
    }

    // ---------------- alertas ----------------

    /** RT7/RT8: alertas de stock crítico y de caducidad. */
    public List<String> generarAlertas() {
        List<String> alertas = new ArrayList<>();
        for (Insumo insumo : bd.getInsumos()) {
            if (insumo.esCritico()) {
                alertas.add("[STOCK CRÍTICO] " + insumo.getCodigo() + " " + insumo.getNombre() + ": "
                        + Insumo.formatoCantidad(insumo.getStock()) + " " + insumo.getUnidadMedida()
                        + " (mínimo " + Insumo.formatoCantidad(insumo.getStockMinimo()) + ")");
            }
            if (insumo instanceof InsumoPerecedero) {
                InsumoPerecedero p = (InsumoPerecedero) insumo;
                if (p.estaVencido()) {
                    alertas.add("[VENCIDO] " + p.getCodigo() + " " + p.getNombre() + ": caducó el "
                            + p.getFechaCaducidad() + ". No puede usarse en ventas.");
                } else if (p.getDiasParaVencer() <= 3) {
                    alertas.add("[POR VENCER] " + p.getCodigo() + " " + p.getNombre() + ": "
                            + p.getAlertaConservacion() + " (" + p.getFechaCaducidad() + ")");
                }
            }
        }
        return alertas;
    }

    public double getValorTotalInventario() {
        double total = 0;
        for (Insumo insumo : bd.getInsumos()) {
            total += insumo.getStock() * insumo.getCostoUnitario();
        }
        return Validar.redondear(total);
    }
}
