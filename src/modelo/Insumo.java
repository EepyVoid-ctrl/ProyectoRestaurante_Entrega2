package modelo;

import java.io.Serializable;

/**
 * Materia prima. Controla su stock y su costo unitario (promedio ponderado). Clase base de la jerarquía de insumos.
 */
public abstract class Insumo implements Serializable {
    private static final long serialVersionUID = 1L;
    /** Tolerancia para comparar cantidades decimales. */
    protected static final double EPSILON = 1e-9;

    private final String codigo;
    private String nombre;
    private String categoria;
    private String unidadMedida;
    private double stock;
    private double stockMinimo;
    private double costoUnitario;

    protected Insumo(String codigo, String nombre, String categoria, String unidadMedida,
                     double stock, double stockMinimo, double costoUnitario) {
        this.codigo = Validar.texto(codigo, "Código");
        setNombre(nombre);
        setCategoria(categoria);
        this.unidadMedida = Validar.texto(unidadMedida, "Unidad de medida");
        this.stock = Validar.redondearCantidad(Validar.noNegativo(stock, "Stock inicial"));
        setStockMinimo(stockMinimo);
        this.costoUnitario = Validar.positivo(costoUnitario, "Costo unitario");
    }

    /**
     * RT1: costo promedio ponderado.
     * nuevoCosto = (stockAnterior * costoAnterior + costoCompra) / (stockAnterior + cantidadComprada)
     * Si el stock anterior es 0, la fórmula se reduce a costoCompra / cantidadComprada.
     *
     * @param cantidad   cantidad comprada (en la unidad de medida del insumo)
     * @param costoTotal costo total pagado por la compra
     */
    public void reabastecer(double cantidad, double costoTotal) {
        Validar.positivo(cantidad, "Cantidad comprada");
        Validar.positivo(costoTotal, "Costo total de la compra");
        double stockAnterior = stock;
        double costoAnterior = costoUnitario;
        double nuevoStock = stockAnterior + cantidad;
        this.costoUnitario = (stockAnterior * costoAnterior + costoTotal) / nuevoStock;
        this.stock = Validar.redondearCantidad(nuevoStock);
    }

    /** RT4: descuenta stock; nunca permite que quede negativo. */
    public void descontar(double cantidad) {
        Validar.positivo(cantidad, "Cantidad a descontar");
        if (cantidad > stock + EPSILON) {
            throw new ValidacionException("Stock insuficiente de " + nombre + ": disponible "
                    + formatoCantidad(stock) + " " + unidadMedida + ", requerido "
                    + formatoCantidad(cantidad) + " " + unidadMedida + ".");
        }
        double resultado = Validar.redondearCantidad(stock - cantidad);
        this.stock = Math.max(0, resultado);
    }

    /** RT8: estado crítico cuando stock <= stockMinimo. */
    public boolean esCritico() {
        return stock <= stockMinimo + EPSILON;
    }

    public String getEstado() {
        return esCritico() ? "Crítico" : "Normal";
    }

    public abstract String getTipo();

    public abstract String getAlertaConservacion();

    // ---------------- getters / setters ----------------

    public String getCodigo() { return codigo; }

    public String getNombre() { return nombre; }

    public void setNombre(String nombre) { this.nombre = Validar.texto(nombre, "Nombre"); }

    public String getCategoria() { return categoria; }

    public void setCategoria(String categoria) { this.categoria = Validar.texto(categoria, "Categoría"); }

    public String getUnidadMedida() { return unidadMedida; }

    public double getStock() { return stock; }

    public double getStockMinimo() { return stockMinimo; }

    public void setStockMinimo(double stockMinimo) {
        this.stockMinimo = Validar.redondearCantidad(Validar.noNegativo(stockMinimo, "Stock mínimo"));
    }

    public double getCostoUnitario() { return costoUnitario; }

    /** Valor del inventario de este insumo (stock × costo unitario), redondeado a 2 decimales. */
    public double getValorInventario() {
        return Validar.redondear(stock * costoUnitario);
    }

    public static String formatoCantidad(double cantidad) {
        if (Math.abs(cantidad - Math.rint(cantidad)) < EPSILON) {
            return String.format("%.0f", cantidad);
        }
        return String.format("%.3f", cantidad).replaceAll("0+$", "");
    }

    @Override
    public String toString() {
        return codigo + " - " + nombre + " (" + formatoCantidad(stock) + " " + unidadMedida + ")";
    }
}
