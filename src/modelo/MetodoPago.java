package modelo;

/**
 * Forma de pago: cada implementación define su nombre y su instrucción de cobro.
 */
public interface MetodoPago {
    String getNombre();
    String getInstruccionCobro();
}
