package modelo;

import java.io.Serializable;

/**
 * Datos del cliente de la factura; «Consumidor Final» por defecto.
 */
public class Cliente implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String ID_CONSUMIDOR_FINAL = "9999999999999";

    private final String nombre;
    private final String identificacion;
    private final String contacto;

    public Cliente(String nombre, String identificacion, String contacto) {
        this.nombre = Validar.texto(nombre, "Nombre del cliente");
        this.identificacion = Validar.identificacion(identificacion);
        this.contacto = (contacto == null || contacto.trim().isEmpty()) ? "N/D" : contacto.trim();
    }

    public static Cliente consumidorFinal() {
        return new Cliente("Consumidor Final", ID_CONSUMIDOR_FINAL, "N/D");
    }

    public boolean esConsumidorFinal() {
        return ID_CONSUMIDOR_FINAL.equals(identificacion);
    }

    public String getNombre() { return nombre; }

    public String getIdentificacion() { return identificacion; }

    public String getContacto() { return contacto; }

    @Override
    public String toString() {
        return nombre + " (" + identificacion + ")";
    }
}
