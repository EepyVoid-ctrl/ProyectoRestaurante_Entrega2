package interfaz;

import java.time.LocalDate;
import java.util.List;

import modelo.Insumo;
import modelo.ValidacionException;
import negocio.ReporteServicio;
import negocio.ReporteServicio.FilaConsumo;
import negocio.ReporteServicio.FilaRentabilidad;

/**
 * Opciones del módulo 4 (RF15–RF16).
 */
public class MenuReportesConsola {

    private final ReporteServicio servicio;

    public MenuReportesConsola(ReporteServicio servicio) {
        this.servicio = servicio;
    }

    public void mostrar() {
        int opcion;
        do {
            Consola.titulo("MÓDULO 4: REPORTES E INTELIGENCIA DE NEGOCIO");
            System.out.println("1. Rentabilidad por plato");
            System.out.println("2. Consumo y rotación de insumos (días de cobertura)");
            System.out.println("0. Volver al menú principal");
            opcion = Consola.leerOpcion(0, 2);
            try {
                if (opcion == 1) {
                    rentabilidad();
                } else if (opcion == 2) {
                    consumo();
                }
            } catch (ValidacionException e) {
                Consola.error(e.getMessage());
            }
            if (opcion != 0) {
                Consola.pausa();
            }
        } while (opcion != 0);
    }

    /** Pide el rango; Enter en «desde» usa los últimos 30 días. */
    private LocalDate[] leerRango() {
        String texto = Consola.leerLinea("Fecha desde (AAAA-MM-DD, Enter = últimos 30 días): ");
        if (texto.isEmpty()) {
            LocalDate hasta = LocalDate.now();
            return new LocalDate[] {hasta.minusDays(29), hasta};
        }
        LocalDate desde = modelo.Validar.fecha(texto, "Fecha desde");
        LocalDate hasta = Consola.leerFecha("Fecha hasta");
        return new LocalDate[] {desde, hasta};
    }

    private void rentabilidad() {
        Consola.subtitulo("RF15 - Rentabilidad por plato");
        LocalDate[] rango = leerRango();
        List<FilaRentabilidad> filas = servicio.rentabilidadPorPlato(rango[0], rango[1]);
        System.out.println("Período: " + rango[0] + " a " + rango[1] + " (valores guardados al momento de cada venta)");
        if (filas.isEmpty()) {
            Consola.aviso("No hay ventas en el período seleccionado. Reporte vacío.");
            return;
        }
        String formato = "%-22s %9s %9s %10s %9s %9s %11s%n";
        Consola.linea(86);
        System.out.printf(formato, "Plato", "Costo", "PVP", "Margen U.", "Margen %", "Unidades", "Utilidad");
        Consola.linea(86);
        double utilidadTotal = 0;
        double maxUtilidad = 0;
        for (FilaRentabilidad f : filas) {
            System.out.printf(formato, Consola.recortar(f.plato.getNombre(), 22), Consola.dinero(f.getCostoUnitario()),
                    Consola.dinero(f.getPvp()), Consola.dinero(f.getMargenUnitario()),
                    String.format("%.2f%%", f.getMargenPorcentual()), String.valueOf(f.unidades),
                    Consola.dinero(f.getUtilidadTotal()));
            utilidadTotal += f.getUtilidadTotal();
            maxUtilidad = Math.max(maxUtilidad, f.getUtilidadTotal());
        }
        Consola.linea(86);
        System.out.printf("%-74s %11s%n", "UTILIDAD TOTAL DEL PERÍODO", Consola.dinero(utilidadTotal));

        System.out.println("\nGráfico de utilidad por plato (ordenado de mayor a menor):");
        for (FilaRentabilidad f : filas) {
            int largo = maxUtilidad <= 0 ? 0 : (int) Math.round(Math.max(0, f.getUtilidadTotal()) / maxUtilidad * 40);
            System.out.printf("  %-22s |%s %s%n", Consola.recortar(f.plato.getNombre(), 22), "#".repeat(largo),
                    Consola.dinero(f.getUtilidadTotal()));
        }
    }

    private void consumo() {
        Consola.subtitulo("RF16 - Consumo y rotación de insumos");
        LocalDate[] rango = leerRango();
        String categoria = Consola.leerTextoOpcional("Categoría");
        List<FilaConsumo> filas = servicio.consumoInsumos(rango[0], rango[1], categoria);
        System.out.println("Período: " + rango[0] + " a " + rango[1]
                + (categoria.isEmpty() ? " | Todas las categorías" : " | Categoría: " + categoria));
        if (filas.isEmpty()) {
            Consola.aviso("No hay ventas en el período (o insumos en esa categoría). Reporte vacío.");
            return;
        }
        String formato = "%-20s %-12s %12s %-7s %14s %13s %15s%n";
        Consola.linea(100);
        System.out.printf(formato, "Insumo", "Categoría", "Consumo", "Unidad", "Consumo diario", "Stock actual", "Días cobertura");
        Consola.linea(100);
        for (FilaConsumo f : filas) {
            System.out.printf(formato, Consola.recortar(f.insumo.getNombre(), 20), Consola.recortar(f.insumo.getCategoria(), 12),
                    Insumo.formatoCantidad(f.consumoTotal), Consola.recortar(f.insumo.getUnidadMedida(), 7),
                    String.format("%.2f", f.getConsumoDiario()), Insumo.formatoCantidad(f.insumo.getStock()),
                    f.getDiasCobertura());
        }
        Consola.linea(100);
        System.out.println("Días del período: " + filas.get(0).diasPeriodo
                + " | Consumo diario = consumo ÷ días | Cobertura = stock actual ÷ consumo diario");
    }
}
