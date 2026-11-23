package modelo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Plato del menú: PVP, receta, estado; calcula costo de producción, margen y advertencia de precio.
 */
public class Plato implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String codigo;
    private String nombre;
    private double pvp;
    private boolean activo;
    private final List<Ingrediente> receta;

    public Plato(String codigo, String nombre, double pvp) {
        this.codigo = Validar.texto(codigo, "Código");
        setNombre(nombre);
        setPvp(pvp);
        this.activo = true;
        this.receta = new ArrayList<>();
    }

    /** Suma del costo de todos los ingredientes de una porción (costo promedio actual). */
    public double getCostoProduccion() {
        double total = 0;
        for (Ingrediente ingrediente : receta) {
            total += ingrediente.getCosto();
        }
        return Validar.redondear(total);
    }

    public double getMargenUnitario() {
        return Validar.redondear(pvp - getCostoProduccion());
    }

    /** Margen sobre el PVP, en porcentaje. */
    public double getMargenPorcentual() {
        if (pvp <= 0) {
            return 0;
        }
        return Validar.redondear((pvp - getCostoProduccion()) / pvp * 100);
    }

    /** RT8: advertencia de pérdida si PVP <= costo de producción. Cadena vacía si no hay problema. */
    public String getAdvertenciaPrecio() {
        double costo = getCostoProduccion();
        if (pvp <= costo) {
            return "ADVERTENCIA: el PVP ($" + String.format("%.2f", pvp) + ") es menor o igual al costo de producción ($"
                    + String.format("%.2f", costo) + "). El plato genera pérdida.";
        }
        return "";
    }

    /** RT6: indica si el insumo forma parte de la receta. */
    public boolean usaInsumo(Insumo insumo) {
        if (insumo == null) {
            return false;
        }
        for (Ingrediente ingrediente : receta) {
            if (ingrediente.getInsumo().getCodigo().equals(insumo.getCodigo())) {
                return true;
            }
        }
        return false;
    }

    /** Agrega un ingrediente; si el insumo ya estaba en la receta, actualiza su cantidad. */
    public void agregarIngrediente(Insumo insumo, double cantidadPorPorcion) {
        for (Ingrediente ingrediente : receta) {
            if (ingrediente.getInsumo().getCodigo().equals(insumo.getCodigo())) {
                ingrediente.setCantidadPorPorcion(cantidadPorPorcion);
                return;
            }
        }
        receta.add(new Ingrediente(insumo, cantidadPorPorcion));
    }

    public void quitarIngrediente(String codigoInsumo) {
        boolean eliminado = receta.removeIf(i -> i.getInsumo().getCodigo().equalsIgnoreCase(codigoInsumo));
        if (!eliminado) {
            throw new ValidacionException("El insumo " + codigoInsumo + " no forma parte de la receta de " + nombre + ".");
        }
    }

    // ---------------- getters / setters ----------------

    public String getCodigo() { return codigo; }

    public String getNombre() { return nombre; }

    public void setNombre(String nombre) { this.nombre = Validar.texto(nombre, "Nombre del plato"); }

    public double getPvp() { return pvp; }

    public void setPvp(double pvp) { this.pvp = Validar.redondear(Validar.positivo(pvp, "PVP")); }

    public boolean isActivo() { return activo; }

    public void setActivo(boolean activo) { this.activo = activo; }

    public String getEstado() { return activo ? "Activo" : "Inactivo"; }

    public List<Ingrediente> getReceta() { return Collections.unmodifiableList(receta); }

    @Override
    public String toString() {
        return codigo + " - " + nombre + " ($" + String.format("%.2f", pvp) + ")";
    }
}
