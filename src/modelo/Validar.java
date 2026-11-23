package modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Validación de datos de entrada (vacíos, negativos, no numéricos).
 * Todos los métodos lanzan ValidacionException con un mensaje claro (RT7).
 */
public class Validar {

    private Validar() { }

    /** Texto obligatorio: no nulo ni vacío. Devuelve el texto sin espacios extremos. */
    public static String texto(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new ValidacionException("El campo «" + campo + "» no puede estar vacío.");
        }
        return valor.trim();
    }

    /** Convierte un texto a número decimal (acepta coma o punto). */
    public static double decimal(String valor, String campo) {
        String limpio = texto(valor, campo).replace(',', '.');
        try {
            double numero = Double.parseDouble(limpio);
            if (Double.isNaN(numero) || Double.isInfinite(numero)) {
                throw new NumberFormatException();
            }
            return numero;
        } catch (NumberFormatException e) {
            throw new ValidacionException("El campo «" + campo + "» debe ser un número válido (ejemplo: 12.50). Ingresó: «" + valor.trim() + "».");
        }
    }

    /** Convierte un texto a número entero. */
    public static int entero(String valor, String campo) {
        String limpio = texto(valor, campo);
        try {
            return Integer.parseInt(limpio);
        } catch (NumberFormatException e) {
            throw new ValidacionException("El campo «" + campo + "» debe ser un número entero. Ingresó: «" + limpio + "».");
        }
    }

    /** El valor debe ser estrictamente mayor que cero. */
    public static double positivo(double valor, String campo) {
        if (Double.isNaN(valor) || valor <= 0) {
            throw new ValidacionException("El campo «" + campo + "» debe ser mayor que cero.");
        }
        return valor;
    }

    /** El valor puede ser cero pero nunca negativo. */
    public static double noNegativo(double valor, String campo) {
        if (Double.isNaN(valor) || valor < 0) {
            throw new ValidacionException("El campo «" + campo + "» no puede ser negativo.");
        }
        return valor;
    }

    /** Entero dentro de un rango cerrado. */
    public static int rango(int valor, int min, int max, String campo) {
        if (valor < min || valor > max) {
            throw new ValidacionException("El campo «" + campo + "» debe estar entre " + min + " y " + max + ".");
        }
        return valor;
    }

    /** Fecha en formato AAAA-MM-DD. */
    public static LocalDate fecha(String valor, String campo) {
        String limpio = texto(valor, campo);
        try {
            return LocalDate.parse(limpio);
        } catch (DateTimeParseException e) {
            throw new ValidacionException("El campo «" + campo + "» debe tener el formato AAAA-MM-DD (ejemplo: 2026-12-31).");
        }
    }

    /** Cédula (10 dígitos) o RUC (13 dígitos), solo números. */
    public static String identificacion(String valor) {
        String limpio = texto(valor, "Identificación");
        if (!limpio.matches("\\d{10}|\\d{13}")) {
            throw new ValidacionException("La identificación debe tener 10 dígitos (cédula) o 13 dígitos (RUC), solo números.");
        }
        return limpio;
    }

    /**
     * Redondeo monetario a 2 decimales con HALF_UP (RT8).
     * Primero se limpia el error de coma flotante (p. ej. 15.50 × 0.15 = 2.3249999999999997 en double)
     * redondeando a 9 decimales, y luego se aplica HALF_UP a 2 decimales: 2.325 → 2.33.
     */
    public static double redondear(double valor) {
        return BigDecimal.valueOf(valor)
                .setScale(9, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /** Redondeo de cantidades físicas (stock) a 4 decimales para evitar errores de coma flotante. */
    public static double redondearCantidad(double valor) {
        return BigDecimal.valueOf(valor).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
