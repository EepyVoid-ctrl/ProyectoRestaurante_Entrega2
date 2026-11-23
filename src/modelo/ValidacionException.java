package modelo;

/**
 * Excepción con mensajes claros para el usuario.
 */
public class ValidacionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ValidacionException(String mensaje) { super(mensaje); }
}
