package modelo;

import java.io.Serializable;
import java.time.format.DateTimeFormatter;

/**
 * Comprobante con número único, IVA 15 % y total; genera su texto.
 */
public class Factura implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final double IVA = 0.15;
    public static final String NOMBRE_RESTAURANTE = "RESTAURANTE SABOR ANDINO";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String numero;
    private final Venta venta;
    private final Cliente cliente;

    public Factura(String numero, Venta venta, Cliente cliente) {
        this.numero = Validar.texto(numero, "Número de factura");
        if (venta == null) {
            throw new ValidacionException("La factura debe estar asociada a una venta.");
        }
        this.venta = venta;
        this.cliente = (cliente == null) ? Cliente.consumidorFinal() : cliente;
    }

    public double getSubtotal() {
        return Validar.redondear(venta.getSubtotal());
    }

    /** RT8: IVA del 15 % sobre el subtotal, redondeado HALF_UP a 2 decimales. */
    public double getIva() {
        return Validar.redondear(getSubtotal() * IVA);
    }

    public double getTotal() {
        return Validar.redondear(getSubtotal() + getIva());
    }

    public String generarTexto() {
        String linea = "-".repeat(52) + "\n";
        String doble = "=".repeat(52) + "\n";
        StringBuilder sb = new StringBuilder();
        sb.append(doble);
        sb.append(centrar(NOMBRE_RESTAURANTE, 52)).append("\n");
        sb.append(centrar("FACTURA", 52)).append("\n");
        sb.append(doble);
        sb.append(String.format("Factura N°     : %s%n", numero));
        sb.append(String.format("Venta N°       : %s%n", venta.getNumero()));
        sb.append(String.format("Fecha          : %s%n", venta.getFecha().format(FORMATO_FECHA)));
        sb.append(String.format("Cliente        : %s%n", cliente.getNombre()));
        sb.append(String.format("Identificación : %s%n", cliente.getIdentificacion()));
        sb.append(String.format("Contacto       : %s%n", cliente.getContacto()));
        sb.append(linea);
        sb.append(String.format("%-5s %-24s %9s %10s%n", "Cant", "Descripción", "P.Unit", "Subtotal"));
        sb.append(linea);
        for (DetalleVenta d : venta.getDetalles()) {
            sb.append(String.format("%-5d %-24s %9.2f %10.2f%n", d.getCantidad(), recortar(d.getPlato().getNombre(), 24),
                    d.getPrecioUnitario(), d.getSubtotal()));
        }
        sb.append(linea);
        sb.append(String.format("%40s %10.2f%n", "Subtotal:", getSubtotal()));
        sb.append(String.format("%40s %10.2f%n", "IVA " + Math.round(IVA * 100) + " %:", getIva()));
        sb.append(String.format("%40s %10.2f%n", "TOTAL A PAGAR:", getTotal()));
        sb.append(linea);
        sb.append(String.format("Forma de pago  : %s%n", venta.getMetodoPago().getNombre()));
        sb.append(String.format("Instrucción    : %s%n", venta.getMetodoPago().getInstruccionCobro()));
        sb.append(doble);
        sb.append(centrar("¡Gracias por su visita!", 52)).append("\n");
        sb.append(doble);
        return sb.toString();
    }

    private static String centrar(String texto, int ancho) {
        int espacios = Math.max(0, (ancho - texto.length()) / 2);
        return " ".repeat(espacios) + texto;
    }

    private static String recortar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
    }

    public String getNumero() { return numero; }

    public Venta getVenta() { return venta; }

    public Cliente getCliente() { return cliente; }
}
