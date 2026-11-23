package interfaz;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Scanner;

import modelo.ValidacionException;
import modelo.Validar;

/**
 * Lectura y validación de entradas por teclado y formato de salida.
 * Usa un único Scanner y siempre lee líneas completas (nextLine) para evitar el
 * problema clásico de nextInt()/nextDouble() que dejan el salto de línea en el búfer.
 * Cada lectura repite la pregunta hasta recibir un dato válido (RT7).
 */
public final class Consola {

    private static final Scanner SCANNER = new Scanner(System.in);
    private static Runnable accionAlCerrarEntrada = () -> { };

    private Consola() { }

    /** Acción a ejecutar si la entrada estándar se cierra (Ctrl+D / Ctrl+Z): por ejemplo, guardar. */
    public static void setAccionAlCerrarEntrada(Runnable accion) {
        accionAlCerrarEntrada = (accion == null) ? () -> { } : accion;
    }

    // ---------------- lectura básica ----------------

    /** Lee una línea completa; si la entrada se cierra, guarda y termina de forma ordenada. */
    public static String leerLinea(String mensaje) {
        System.out.print(mensaje);
        try {
            return SCANNER.nextLine().trim();
        } catch (NoSuchElementException | IllegalStateException e) {
            System.out.println();
            System.out.println("Entrada finalizada. Guardando y saliendo del sistema...");
            accionAlCerrarEntrada.run();
            System.exit(0);
            return "";
        }
    }

    /** Texto obligatorio. */
    public static String leerTexto(String etiqueta) {
        while (true) {
            try {
                return Validar.texto(leerLinea(etiqueta + ": "), etiqueta);
            } catch (ValidacionException e) {
                error(e.getMessage());
            }
        }
    }

    /** Texto que puede quedar vacío (devuelve ""). */
    public static String leerTextoOpcional(String etiqueta) {
        return leerLinea(etiqueta + " (Enter para omitir): ");
    }

    public static int leerEntero(String etiqueta, int min, int max) {
        while (true) {
            try {
                int valor = Validar.entero(leerLinea(etiqueta + ": "), etiqueta);
                return Validar.rango(valor, min, max, etiqueta);
            } catch (ValidacionException e) {
                error(e.getMessage());
            }
        }
    }

    /** Número decimal mayor que cero. */
    public static double leerDecimalPositivo(String etiqueta) {
        while (true) {
            try {
                return Validar.positivo(Validar.decimal(leerLinea(etiqueta + ": "), etiqueta), etiqueta);
            } catch (ValidacionException e) {
                error(e.getMessage());
            }
        }
    }

    /** Número decimal mayor o igual que cero. */
    public static double leerDecimalNoNegativo(String etiqueta) {
        while (true) {
            try {
                return Validar.noNegativo(Validar.decimal(leerLinea(etiqueta + ": "), etiqueta), etiqueta);
            } catch (ValidacionException e) {
                error(e.getMessage());
            }
        }
    }

    /** Fecha AAAA-MM-DD. */
    public static LocalDate leerFecha(String etiqueta) {
        while (true) {
            try {
                return Validar.fecha(leerLinea(etiqueta + " (AAAA-MM-DD): "), etiqueta);
            } catch (ValidacionException e) {
                error(e.getMessage());
            }
        }
    }

    public static boolean confirmar(String pregunta) {
        while (true) {
            String r = leerLinea(pregunta + " (s/n): ").toLowerCase();
            if (r.equals("s") || r.equals("si") || r.equals("sí")) {
                return true;
            }
            if (r.equals("n") || r.equals("no")) {
                return false;
            }
            error("Responda «s» (sí) o «n» (no).");
        }
    }

    public static int leerOpcion(int min, int max) {
        return leerEntero("Seleccione una opción", min, max);
    }

    public static void pausa() {
        leerLinea("\nPresione Enter para continuar...");
    }

    // ---------------- salida ----------------

    public static void titulo(String texto) {
        String borde = "=".repeat(Math.max(60, texto.length() + 4));
        System.out.println();
        System.out.println(borde);
        System.out.println("  " + texto);
        System.out.println(borde);
    }

    public static void subtitulo(String texto) {
        System.out.println();
        System.out.println("--- " + texto + " ---");
    }

    public static void linea(int ancho) {
        System.out.println("-".repeat(ancho));
    }

    public static void exito(String mensaje) {
        System.out.println("[OK] " + mensaje);
    }

    public static void error(String mensaje) {
        System.out.println("[ERROR] " + mensaje);
    }

    public static void aviso(String mensaje) {
        System.out.println("[!] " + mensaje);
    }

    public static String dinero(double valor) {
        return String.format("$%.2f", Validar.redondear(valor));
    }

    public static String recortar(String texto, int max) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
    }
}
