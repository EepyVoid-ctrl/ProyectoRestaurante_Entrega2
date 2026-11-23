package negocio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import modelo.Factura;
import modelo.Insumo;
import modelo.Plato;
import modelo.Venta;

/**
 * Almacén de insumos, platos, ventas y facturas, con guardado y carga en archivo.
 * Persistencia por serialización en datos/restaurante.dat; si el archivo no existe
 * se trabaja en memoria con los datos de demostración y se crea el archivo.
 */
public class BaseDatos implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String CARPETA = "datos";
    public static final String RUTA = CARPETA + File.separator + "restaurante.dat";

    private final List<Insumo> insumos = new ArrayList<>();
    private final List<Plato> platos = new ArrayList<>();
    private final List<Venta> ventas = new ArrayList<>();
    private final List<Factura> facturas = new ArrayList<>();

    private int contadorInsumos = 0;
    private int contadorPlatos = 0;
    private int contadorVentas = 0;
    private int contadorFacturas = 0;

    /** Permite desactivar el guardado automático (por ejemplo, mientras se precargan datos). */
    private transient boolean guardadoAutomatico = true;

    // ---------------- persistencia ----------------

    /** Carga la base desde el archivo; si no existe o está dañado, usa los datos demo en memoria. */
    public static BaseDatos cargar() {
        File archivo = new File(RUTA);
        if (archivo.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(archivo))) {
                BaseDatos bd = (BaseDatos) in.readObject();
                bd.guardadoAutomatico = true;
                System.out.println("Datos cargados desde " + RUTA);
                return bd;
            } catch (IOException | ClassNotFoundException | ClassCastException e) {
                System.out.println("AVISO: no se pudo leer " + RUTA + " (" + e.getMessage() + "). Se cargan los datos de demostración.");
            }
        } else {
            System.out.println("No existe " + RUTA + ". Se cargan los datos de demostración en memoria.");
        }
        BaseDatos bd = new BaseDatos();
        bd.guardadoAutomatico = false;
        DatosDemo.cargar(bd);
        bd.guardadoAutomatico = true;
        bd.guardar();
        return bd;
    }

    /** Guarda toda la base en el archivo. Si falla, avisa y los datos siguen en memoria. */
    public boolean guardar() {
        if (!guardadoAutomatico) {
            return false;
        }
        File carpeta = new File(CARPETA);
        if (!carpeta.exists() && !carpeta.mkdirs()) {
            System.out.println("AVISO: no se pudo crear la carpeta " + CARPETA + ". Los datos siguen en memoria.");
            return false;
        }
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(RUTA))) {
            out.writeObject(this);
            return true;
        } catch (IOException e) {
            System.out.println("AVISO: no se pudo guardar en " + RUTA + " (" + e.getMessage() + "). Los datos siguen en memoria.");
            return false;
        }
    }

    private Object readResolve() {
        this.guardadoAutomatico = true;
        return this;
    }

    // ---------------- búsquedas ----------------

    public Insumo buscarInsumo(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (Insumo insumo : insumos) {
            if (insumo.getCodigo().equalsIgnoreCase(codigo.trim())) {
                return insumo;
            }
        }
        return null;
    }

    public Plato buscarPlato(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (Plato plato : platos) {
            if (plato.getCodigo().equalsIgnoreCase(codigo.trim())) {
                return plato;
            }
        }
        return null;
    }

    public Factura buscarFactura(String numero) {
        if (numero == null) {
            return null;
        }
        for (Factura factura : facturas) {
            if (factura.getNumero().equalsIgnoreCase(numero.trim())) {
                return factura;
            }
        }
        return null;
    }

    // ---------------- numeración ----------------

    public String siguienteCodigoInsumo() {
        contadorInsumos++;
        return String.format("INS-%03d", contadorInsumos);
    }

    public String siguienteCodigoPlato() {
        contadorPlatos++;
        return String.format("PLA-%03d", contadorPlatos);
    }

    public String siguienteNumeroVenta() {
        contadorVentas++;
        return String.format("V-%04d", contadorVentas);
    }

    /** Formato SRI: establecimiento-punto de emisión-secuencial. */
    public String siguienteNumeroFactura() {
        contadorFacturas++;
        return String.format("001-001-%09d", contadorFacturas);
    }

    // ---------------- colecciones ----------------

    public List<Insumo> getInsumos() { return insumos; }

    public List<Plato> getPlatos() { return platos; }

    public List<Venta> getVentas() { return ventas; }

    public List<Factura> getFacturas() { return facturas; }
}
