package negocio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import modelo.Cliente;
import modelo.Insumo;
import modelo.InsumoPerecedero;
import modelo.MetodoPago;
import modelo.PagoEfectivo;
import modelo.PagoTarjeta;
import modelo.PagoTransferencia;
import modelo.Plato;

/**
 * Datos de ejemplo del restaurante para pruebas.
 * Escenario: restaurante «Sabor Andino». Las fechas son relativas al día de la primera ejecución,
 * así las alertas de caducidad y los reportes de los últimos días siempre tienen datos.
 */
public class DatosDemo {

    private DatosDemo() { }

    public static void cargar(BaseDatos bd) {
        InventarioServicio inventario = new InventarioServicio(bd);
        MenuServicio menu = new MenuServicio(bd);
        VentaServicio ventas = new VentaServicio(bd);
        LocalDate hoy = LocalDate.now();

        // ---------------- Insumos (RF01) ----------------
        // nombre, categoría, unidad, cantidad comprada, costo total de compra, stock mínimo, [caducidad]
        // costo unitario = costo total ÷ cantidad comprada
        Insumo pollo    = inventario.registrarPerecedero("Pollo", "Carnes", "kg", 15, 67.50, 3, hoy.plusDays(5));        // $4.50/kg
        Insumo carne    = inventario.registrarPerecedero("Carne de res", "Carnes", "kg", 10, 80.00, 2, hoy.plusDays(4));  // $8.00/kg
        Insumo arroz    = inventario.registrarNoPerecedero("Arroz", "Granos", "kg", 25, 27.50, 5);                        // $1.10/kg
        Insumo papa     = inventario.registrarPerecedero("Papa", "Tubérculos", "kg", 20, 16.00, 5, hoy.plusDays(15));    // $0.80/kg
        Insumo huevos   = inventario.registrarPerecedero("Huevos", "Lácteos", "unidad", 60, 9.00, 12, hoy.plusDays(12)); // $0.15/u
        Insumo cebolla  = inventario.registrarPerecedero("Cebolla colorada", "Vegetales", "kg", 5, 6.00, 1, hoy.plusDays(10));
        Insumo tomate   = inventario.registrarPerecedero("Tomate riñón", "Vegetales", "kg", 4, 5.20, 1, hoy.plusDays(2));
        Insumo aceite   = inventario.registrarNoPerecedero("Aceite vegetal", "Despensa", "litro", 8, 22.40, 2);
        Insumo sal      = inventario.registrarNoPerecedero("Sal", "Despensa", "kg", 3, 1.80, 0.5);
        Insumo chorizo  = inventario.registrarPerecedero("Chorizo", "Carnes", "kg", 3, 19.50, 1, hoy.plusDays(7));
        Insumo aguacate = inventario.registrarPerecedero("Aguacate", "Vegetales", "unidad", 8, 4.00, 5, hoy.plusDays(3));

        // Insumo ya vencido: se agrega directo porque el registro normal no permite fechas pasadas.
        // Sirve para probar RT7: el «Locro de papa» no se puede vender mientras la leche esté vencida.
        Insumo leche = new InsumoPerecedero(bd.siguienteCodigoInsumo(), "Leche entera", "Lácteos", "litro",
                4, 1, 1.00, hoy.minusDays(1));
        bd.getInsumos().add(leche);

        // Insumo sin uso en recetas: se puede eliminar para probar RF04 (caso permitido).
        inventario.registrarNoPerecedero("Comino molido", "Despensa", "kg", 1, 9.00, 0.2);

        // ---------------- Platos y recetas (RF06) ----------------
        Map<String, Double> r1 = new LinkedHashMap<>();
        r1.put(pollo.getCodigo(), 0.25);
        r1.put(arroz.getCodigo(), 0.15);
        r1.put(cebolla.getCodigo(), 0.03);
        r1.put(aceite.getCodigo(), 0.02);
        r1.put(sal.getCodigo(), 0.005);
        Plato arrozConPollo = menu.registrarPlato("Arroz con pollo", 6.50, r1);

        Map<String, Double> r2 = new LinkedHashMap<>();
        r2.put(carne.getCodigo(), 0.25);
        r2.put(papa.getCodigo(), 0.20);
        r2.put(cebolla.getCodigo(), 0.05);
        r2.put(tomate.getCodigo(), 0.08);
        r2.put(arroz.getCodigo(), 0.12);
        r2.put(aceite.getCodigo(), 0.03);
        Plato lomoSaltado = menu.registrarPlato("Lomo saltado", 8.50, r2);

        Map<String, Double> r3 = new LinkedHashMap<>();
        r3.put(papa.getCodigo(), 0.35);
        r3.put(huevos.getCodigo(), 2.0);
        r3.put(chorizo.getCodigo(), 0.10);
        r3.put(aguacate.getCodigo(), 0.5);
        r3.put(aceite.getCodigo(), 0.02);
        r3.put(sal.getCodigo(), 0.005);
        Plato llapingachos = menu.registrarPlato("Llapingachos", 7.00, r3);

        Map<String, Double> r4 = new LinkedHashMap<>();
        r4.put(papa.getCodigo(), 0.30);
        r4.put(leche.getCodigo(), 0.15);
        r4.put(aguacate.getCodigo(), 0.25);
        r4.put(sal.getCodigo(), 0.005);
        menu.registrarPlato("Locro de papa", 4.50, r4);

        // ---------------- Ventas previas (RF11–RF13) ----------------
        Cliente maria = new Cliente("María López", "1712345678", "maria.lopez@correo.com");
        Cliente carlos = new Cliente("Carlos Andrade", "0102030405", "0991234567");
        Cliente final_ = Cliente.consumidorFinal();

        vender(ventas, hace(9, 13, 10), new PagoEfectivo(), final_, arrozConPollo, 2);
        vender(ventas, hace(7, 12, 45), new PagoTarjeta(), maria, lomoSaltado, 1, llapingachos, 1);
        vender(ventas, hace(5, 19, 20), new PagoTransferencia(), carlos, lomoSaltado, 2, arrozConPollo, 1);
        vender(ventas, hace(3, 13, 30), new PagoEfectivo(), final_, llapingachos, 2);
        vender(ventas, hace(2, 13, 15), new PagoTarjeta(), maria, arrozConPollo, 2, lomoSaltado, 1);
        vender(ventas, hace(1, 12, 40), new PagoEfectivo(), final_, llapingachos, 3, arrozConPollo, 1);
        vender(ventas, hace(0, 8, 30), new PagoEfectivo(), final_, arrozConPollo, 1);
        // Resultado: Aguacate queda en 5 unidades = stock mínimo → estado Crítico (RF05).
    }

    private static LocalDateTime hace(int dias, int hora, int minuto) {
        return LocalDateTime.now().minusDays(dias).withHour(hora).withMinute(minuto).withSecond(0).withNano(0);
    }

    /** Registra una venta demo: pares (plato, cantidad). Usa el mismo flujo que la consola (RT5, RT2). */
    private static void vender(VentaServicio ventas, LocalDateTime fecha, MetodoPago pago, Cliente cliente, Object... platoCantidad) {
        Map<String, Integer> pedido = new LinkedHashMap<>();
        for (int i = 0; i < platoCantidad.length; i += 2) {
            pedido.put(((Plato) platoCantidad[i]).getCodigo(), (Integer) platoCantidad[i + 1]);
        }
        ventas.registrarVenta(pedido, pago, cliente, fecha);
    }
}
