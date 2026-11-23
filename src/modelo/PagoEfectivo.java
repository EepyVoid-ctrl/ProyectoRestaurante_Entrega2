package modelo;

import java.io.Serializable;

/**
 * Implementación de MetodoPago para efectivo.
 */
public class PagoEfectivo implements MetodoPago, Serializable {
    private static final long serialVersionUID = 1L;

    @Override public String getNombre() { return "Efectivo"; }

    @Override public String getInstruccionCobro() {
        return "Cobrar en caja, verificar billetes y entregar el cambio exacto.";
    }
}
