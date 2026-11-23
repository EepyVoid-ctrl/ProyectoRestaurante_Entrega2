package modelo;

import java.io.Serializable;

/**
 * Implementación de MetodoPago para tarjeta.
 */
public class PagoTarjeta implements MetodoPago, Serializable {
    private static final long serialVersionUID = 1L;

    @Override public String getNombre() { return "Tarjeta"; }

    @Override public String getInstruccionCobro() {
        return "Pasar la tarjeta por el datáfono y entregar el voucher al cliente.";
    }
}
