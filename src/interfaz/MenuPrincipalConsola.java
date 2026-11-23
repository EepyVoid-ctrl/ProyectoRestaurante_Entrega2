package interfaz;

import java.util.List;

import negocio.BaseDatos;
import negocio.InventarioServicio;
import negocio.MenuServicio;
import negocio.ReporteServicio;
import negocio.VentaServicio;

/**
 * Menú con los cuatro módulos y la opción de salir.
 */
public class MenuPrincipalConsola {

    private final BaseDatos bd;
    private final InventarioServicio inventarioServicio;
    private final MenuInventarioConsola menuInventario;
    private final MenuRecetasConsola menuRecetas;
    private final MenuVentasConsola menuVentas;
    private final MenuReportesConsola menuReportes;

    public MenuPrincipalConsola(BaseDatos bd) {
        this.bd = bd;
        this.inventarioServicio = new InventarioServicio(bd);
        MenuServicio menuServicio = new MenuServicio(bd);
        this.menuInventario = new MenuInventarioConsola(inventarioServicio);
        this.menuRecetas = new MenuRecetasConsola(menuServicio, inventarioServicio);
        this.menuVentas = new MenuVentasConsola(new VentaServicio(bd), menuServicio);
        this.menuReportes = new MenuReportesConsola(new ReporteServicio(bd));
    }

    public void mostrar() {
        int opcion;
        do {
            Consola.titulo("SISTEMA DE GESTIÓN - RESTAURANTE SABOR ANDINO");
            List<String> alertas = inventarioServicio.generarAlertas();
            if (!alertas.isEmpty()) {
                Consola.aviso("Hay " + alertas.size() + " alerta(s) de inventario. Revíselas en Módulo 1 > opción 7.");
            }
            System.out.println("1. Gestión de inventario y materia prima");
            System.out.println("2. Ingeniería de menú y recetas");
            System.out.println("3. Ventas y facturación");
            System.out.println("4. Reportes");
            System.out.println("0. Guardar y salir");
            opcion = Consola.leerOpcion(0, 4);
            switch (opcion) {
                case 1: menuInventario.mostrar(); break;
                case 2: menuRecetas.mostrar(); break;
                case 3: menuVentas.mostrar(); break;
                case 4: menuReportes.mostrar(); break;
                default: break;
            }
        } while (opcion != 0);

        if (bd.guardar()) {
            Consola.exito("Datos guardados en " + BaseDatos.RUTA + ". ¡Hasta pronto!");
        } else {
            Consola.aviso("No se pudo guardar el archivo; los cambios de esta sesión se perderán.");
        }
    }
}
