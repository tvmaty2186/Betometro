# Betómetro — Pendientes

Anotado el 26/10/2026. Nada de esto está programado todavía.

## Equipo dedicado (la caja)

- **Celular:** uno dedicado, no el teléfono personal.
- **Caja:** gris tipo carbono, con rejillas y un cooler de 4 × 4 cm a 12 V. La pantalla queda recortada para poder usar el táctil.
- **Pulsador:**
  - Va cableado, con cable espiralado hasta la mano del copiloto.
  - Entra por un hub USB dentro de la caja, bien fijado con tornillos o silicona.
  - El hub tiene una ficha para cargar y otra para el pulsador. Para mantenimiento se usa la misma ficha de carga.
  - Antes de cerrar la caja hay que probar que ese celular cargue y lea el pulsador al mismo tiempo con ese hub. El hub tiene que ser de los que dejan pasar la carga (USB-C con "PD pass-through"). No todos los celulares lo aceptan bien.
- **Fichas:** GX12 roscadas, tanto para la alimentación de 12 V como para el pulsador.
- **Auriculares:** Bluetooth. Por ahora no se cambia nada en la app.
- **Placa ESP32:** por ahora no se hace nada.

## Cambios en la app — hechos en la 1.9.0

1. Ajustes → Modo carrera → «Abrir Betómetro al prender el teléfono» (apagado de fábrica).
2. Clic al marcar con el pulsador o MARCAR (Ajustes → Cuenta regresiva → «Clic al marcar»).
3. Pitido del cero apagado de fábrica; se prende en Ajustes → «Pitido largo al llegar a cero».
