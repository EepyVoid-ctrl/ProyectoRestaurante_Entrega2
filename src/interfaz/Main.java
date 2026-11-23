package interfaz;

import negocio.BaseDatos;

/**
 * Punto de entrada: carga los datos y muestra el menú principal.
 */
public class Main {
    public static void main(String[] args) {
        BaseDatos bd = BaseDatos.cargar();
        Consola.setAccionAlCerrarEntrada(bd::guardar);
        new MenuPrincipalConsola(bd).mostrar();
    }
}
