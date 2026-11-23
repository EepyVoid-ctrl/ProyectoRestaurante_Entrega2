package negocio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import modelo.Ingrediente;
import modelo.Insumo;
import modelo.Plato;
import modelo.ValidacionException;
import modelo.Validar;
import modelo.Venta;

/**
 * RF06–RF10: registrar, consultar, actualizar y eliminar (o inactivar) platos y recetas.
 */
public class MenuServicio {

    private final BaseDatos bd;

    public MenuServicio(BaseDatos bd) {
        this.bd = bd;
    }

    // ---------------- registrar ----------------

    /**
     * Registra un plato con su receta.
     *
     * @param receta codigoInsumo -> cantidad por porción (al menos un ingrediente)
     */
    public Plato registrarPlato(String nombre, double pvp, Map<String, Double> receta) {
        String limpio = Validar.texto(nombre, "Nombre del plato");
        validarNombreUnico(limpio, null);
        if (receta == null || receta.isEmpty()) {
            throw new ValidacionException("La receta debe tener al menos un ingrediente.");
        }
        // Se valida todo antes de consumir el código del plato.
        Plato temporal = new Plato("TMP", limpio, pvp);
        for (Map.Entry<String, Double> e : receta.entrySet()) {
            temporal.agregarIngrediente(buscarInsumo(e.getKey()), e.getValue());
        }
        Plato plato = new Plato(bd.siguienteCodigoPlato(), limpio, pvp);
        for (Ingrediente ingrediente : temporal.getReceta()) {
            plato.agregarIngrediente(ingrediente.getInsumo(), ingrediente.getCantidadPorPorcion());
        }
        bd.getPlatos().add(plato);
        bd.guardar();
        return plato;
    }

    private void validarNombreUnico(String nombre, Plato excluido) {
        for (Plato plato : bd.getPlatos()) {
            if (plato != excluido && plato.getNombre().equalsIgnoreCase(nombre.trim())) {
                throw new ValidacionException("Ya existe un plato llamado «" + nombre.trim() + "» (" + plato.getCodigo() + ").");
            }
        }
    }

    private Insumo buscarInsumo(String codigo) {
        Insumo insumo = bd.buscarInsumo(Validar.texto(codigo, "Código del insumo"));
        if (insumo == null) {
            throw new ValidacionException("No existe un insumo con el código «" + codigo.trim() + "».");
        }
        return insumo;
    }

    // ---------------- consultar ----------------

    public List<Plato> listar(boolean incluirInactivos) {
        List<Plato> resultado = new ArrayList<>();
        for (Plato plato : bd.getPlatos()) {
            if (incluirInactivos || plato.isActivo()) {
                resultado.add(plato);
            }
        }
        return resultado;
    }

    public Plato buscar(String codigo) {
        Plato plato = bd.buscarPlato(Validar.texto(codigo, "Código del plato"));
        if (plato == null) {
            throw new ValidacionException("No existe un plato con el código «" + codigo.trim() + "».");
        }
        return plato;
    }

    /** RF07: búsqueda por nombre (o parte del nombre), sin distinguir mayúsculas. */
    public List<Plato> buscarPorNombre(String texto, boolean incluirInactivos) {
        String buscado = Validar.texto(texto, "Nombre del plato").toLowerCase();
        List<Plato> resultado = new ArrayList<>();
        for (Plato plato : listar(incluirInactivos)) {
            if (plato.getNombre().toLowerCase().contains(buscado)) {
                resultado.add(plato);
            }
        }
        return resultado;
    }

    public List<Plato> platosConPerdida() {
        List<Plato> resultado = new ArrayList<>();
        for (Plato plato : bd.getPlatos()) {
            if (plato.isActivo() && !plato.getAdvertenciaPrecio().isEmpty()) {
                resultado.add(plato);
            }
        }
        return resultado;
    }

    /** Cuántas porciones se pueden preparar con el stock actual (limitado por el insumo más escaso). */
    public int porcionesDisponibles(Plato plato) {
        if (plato.getReceta().isEmpty()) {
            return 0;
        }
        int minimo = Integer.MAX_VALUE;
        for (Ingrediente ingrediente : plato.getReceta()) {
            int posibles = (int) Math.floor((ingrediente.getInsumo().getStock() + 1e-9) / ingrediente.getCantidadPorPorcion());
            minimo = Math.min(minimo, posibles);
        }
        return minimo;
    }

    // ---------------- actualizar ----------------

    public Plato actualizarNombre(String codigo, String nuevoNombre) {
        Plato plato = buscar(codigo);
        String limpio = Validar.texto(nuevoNombre, "Nombre del plato");
        validarNombreUnico(limpio, plato);
        plato.setNombre(limpio);
        bd.guardar();
        return plato;
    }

    /** RT3: el nuevo PVP solo afecta a ventas futuras. */
    public Plato actualizarPvp(String codigo, double nuevoPvp) {
        Plato plato = buscar(codigo);
        plato.setPvp(nuevoPvp);
        bd.guardar();
        return plato;
    }

    /** Agrega un ingrediente o modifica su cantidad si ya estaba en la receta. */
    public Plato agregarOModificarIngrediente(String codigoPlato, String codigoInsumo, double cantidadPorPorcion) {
        Plato plato = buscar(codigoPlato);
        Insumo insumo = buscarInsumo(codigoInsumo);
        plato.agregarIngrediente(insumo, cantidadPorPorcion);
        bd.guardar();
        return plato;
    }

    public Plato quitarIngrediente(String codigoPlato, String codigoInsumo) {
        Plato plato = buscar(codigoPlato);
        Validar.texto(codigoInsumo, "Código del insumo");
        if (plato.getReceta().size() == 1 && plato.usaInsumo(bd.buscarInsumo(codigoInsumo))) {
            throw new ValidacionException("No se puede quitar el único ingrediente: la receta debe tener al menos uno.");
        }
        plato.quitarIngrediente(codigoInsumo.trim());
        bd.guardar();
        return plato;
    }

    public Plato reactivar(String codigo) {
        Plato plato = buscar(codigo);
        if (plato.isActivo()) {
            throw new ValidacionException("El plato «" + plato.getNombre() + "» ya está activo.");
        }
        plato.setActivo(true);
        bd.guardar();
        return plato;
    }

    // ---------------- eliminar ----------------

    public boolean tieneVentas(Plato plato) {
        for (Venta venta : bd.getVentas()) {
            if (venta.incluyePlato(plato)) {
                return true;
            }
        }
        return false;
    }

    /**
     * RT6: si el plato tiene ventas pasa a Inactivo (activo = false); si no, se elimina.
     *
     * @return mensaje con el resultado de la operación
     */
    public String eliminar(String codigo) {
        Plato plato = buscar(codigo);
        if (tieneVentas(plato)) {
            if (!plato.isActivo()) {
                throw new ValidacionException("El plato «" + plato.getNombre() + "» ya está inactivo y tiene ventas registradas; no puede eliminarse.");
            }
            plato.setActivo(false);
            bd.guardar();
            return "El plato «" + plato.getNombre() + "» tiene ventas registradas: se cambió a estado Inactivo (no aparecerá en nuevas ventas).";
        }
        bd.getPlatos().remove(plato);
        bd.guardar();
        return "El plato «" + plato.getNombre() + "» se eliminó definitivamente (no tenía ventas).";
    }

    /** Resumen de costeo de la receta: insumo -> costo de la línea. */
    public Map<String, Double> desgloseCostos(Plato plato) {
        Map<String, Double> desglose = new LinkedHashMap<>();
        for (Ingrediente ingrediente : plato.getReceta()) {
            desglose.put(ingrediente.getInsumo().getNombre(), Validar.redondear(ingrediente.getCosto()));
        }
        return desglose;
    }
}
