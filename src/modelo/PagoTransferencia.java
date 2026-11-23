package modelo;

import java.io.Serializable;

/**
 * Implementación de MetodoPago para transferencia.
 */
public class PagoTransferencia implements MetodoPago, Serializable {
    private static final long serialVersionUID = 1L;

    @Override public String getNombre() { return "Transferencia"; }

    @Override public String getInstruccionCobro() {
        return "Verificar el comprobante de transferencia en la cuenta del restaurante antes de entregar el pedido.";
    }
}
