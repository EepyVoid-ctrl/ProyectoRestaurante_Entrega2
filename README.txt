Proyecto Restaurante "Sabor Andino" - Entrega 2 (Grupo 9) - Interfaz en consola
Paquetes: modelo, negocio, interfaz (según la Figura 1 y el árbol del Entregable 2).

Abrir en NetBeans / IntelliJ / Eclipse / VS Code: proyecto existente -> carpeta src.
Requiere JDK 17 o superior. Codificación de los archivos: UTF-8.
Ejecutar la clase: interfaz.Main

Desde la terminal (en la carpeta ProyectoRestaurante):
  javac -encoding UTF-8 -d bin src/modelo/*.java src/negocio/*.java src/interfaz/*.java
  java -Dstdout.encoding=UTF-8 -cp bin interfaz.Main

Persistencia: los datos se guardan en datos/restaurante.dat (se crea solo).
Para volver a los datos de demostración, borre la carpeta "datos" y ejecute de nuevo.

Datos de prueba precargados (fechas relativas al día de la primera ejecución):
  - 13 insumos: Pollo, Carne de res, Arroz, Papa, Huevos, Cebolla colorada, Tomate riñón,
    Aceite vegetal, Sal, Chorizo, Aguacate, Leche entera y Comino molido.
    * Aguacate queda en estado Crítico (stock = mínimo).
    * Leche entera está VENCIDA (RT7).
    * Comino molido no se usa en recetas (se puede eliminar, RF04).
  - 4 platos: Arroz con pollo ($6.50), Lomo saltado ($8.50), Llapingachos ($7.00) y
    Locro de papa ($4.50, usa la leche vencida: su venta se rechaza).
  - 7 ventas en los últimos 10 días (María López 1712345678, Carlos Andrade 0102030405,
    Consumidor Final), con números V-0001... y facturas 001-001-000000001...

Pruebas rápidas sugeridas:
  RF01  Módulo 1 > 1: Harina, Despensa, kg, cantidad 10, costo total 12 -> costo unitario $1.20
  RF03  Módulo 1 > 4: Arroz, 50 kg por $80 -> costo promedio ponderado (RT1)
  RF04  Módulo 1 > 6: eliminar INS-001 (Pollo) -> rechazado, indica los platos
  RF05  Módulo 1 > 1: Cilantro 2 kg, $3, mínimo 5 -> Crítico y alerta
  RF09  Módulo 2 > 5: eliminar Llapingachos -> pasa a Inactivo (tiene ventas)
  RF10  Módulo 2 > 4: cambiar PVP de Arroz con pollo a 1.00 -> advertencia de pérdida
  RF12  Módulo 3 > 1: 200 Arroz con pollo -> venta rechazada con el faltante exacto
  RT7   Módulo 3 > 1: Locro de papa -> rechazado por insumo VENCIDO
  RF14  Módulo 3 > 4: identificación 1712345678
  RF15  Módulo 4 > 1 / RF16 Módulo 4 > 2 (Enter = últimos 30 días, categoría Carnes)
