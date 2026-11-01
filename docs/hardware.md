# Hardware: equipo dedicado de Betómetro

Hubo dos caminos. **La caja con el A37 se canceló el 01/11/2026.** Sigue la Raspberry Pi, en su propio repo.

## A) Caja con celular dedicado (Samsung Galaxy A37) — CANCELADA

Matías la canceló el 01/11/2026. Queda lo que sigue solo como referencia.

**Celular:** uno dedicado, no el S23 Ultra personal de Matías. El Ulefone RugKing se descartó.

**Caja:**
- gris tipo carbono, con rejillas y un cooler de 4×4 cm a 12 V;
- la pantalla queda recortada para usar el táctil;
- el soporte gira la caja **solo en sentido horario**, de vertical a horizontal.

**Fichas GX12 roscadas**, del lado derecho con la caja vertical:
- la de abajo es 12 V;
- la de arriba es el pulsador.

**Pulsador:**
- cable espiralado hasta la mano del copiloto;
- entra por un **hub USB-C con carga «PD pass-through»**, fijado adentro de la caja;
- **antes de cerrar la caja:** probar que el A37 cargue y lea el pulsador al mismo tiempo con ese hub.

**Audio:** auriculares Bluetooth. No requiere cambios en la app.

**Alimentación:** 12 V del auto con un convertidor a 5 V.

**En la app (hecho en 1.9.0):**
- abrir Betómetro al prender el teléfono (apagado de fábrica);
- clic mecánico al marcar;
- pitido del cero apagado de fábrica.

**Modelo 3D:** `docs/diseno/caja/caja-betometro.html` ([versión en línea](https://claude.ai/artifact/7CS6LmiuA6sTKc4nm5AFmt)).

## B) Raspberry Pi

Se mudó a su propio repo: https://github.com/tvmaty2186/Betometro-Raspberry (piezas, cableado, sistema y pantallas).

## Descartado o evaluado

- **Placas ESP32 Waveshare** (7" y 4,3"B): son peores para la voz. Por ahora no se hace nada con ESP32.
- **Android Auto:** se explicó la factibilidad. No se avanzó.
- **SIM868 HAT y Pico SIM7080G:** se evaluaron como GPS. Se eligió el NEO-M8N.
