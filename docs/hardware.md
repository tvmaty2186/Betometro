# Hardware: equipo dedicado de Betómetro

Hubo dos caminos. **La caja con el A37 se canceló el 01/11/2026.** Sigue la Raspberry Pi, con las piezas compradas en noviembre de 2026.

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

## B) Raspberry Pi (piezas compradas)

**Compradas:**
- Raspberry Pi 3 B+ (Element14);
- Raspberry Pi Touch Display 2 de 7" (DSI);
- microSD SanDisk Ultra de 16 GB;
- módulo GPS **NEO-M8N** con SMA y micro USB;
- antena GPS activa de 1 m, SMA macho.

**Falta comprar (recomendado):**
- fuente de 5 V 2,5 A micro-USB y cable USB-A a micro-USB;
- amplificador **MAX98357A** (I2S) con un parlante de 3 W 4 Ω, para la voz;
- cables dupont y un pulsador;
- más adelante: convertidor de 12 V a 5 V 3 A, fichas GX12 y cable espiralado.

**Software:** está ofrecido, todavía no empezado. Espera el OK de Matías.
- guía de instalación;
- un servicio que lea el GPS (NMEA por USB o UART), el pulsador (GPIO) y la voz;
- la misma interfaz web en Chromium en modo kiosco;
- sistema de archivos de solo lectura, para que no se rompa al cortar la corriente.

**La idea:** reutilizar `Motor` y la interfaz de `www/index.html` tal cual. Solo cambia la fuente de velocidad y la de marcas.

## Descartado o evaluado

- **Placas ESP32 Waveshare** (7" y 4,3"B): son peores para la voz. Por ahora no se hace nada con ESP32.
- **Android Auto:** se explicó la factibilidad. No se avanzó.
- **SIM868 HAT y Pico SIM7080G:** se evaluaron como GPS. Se eligió el NEO-M8N.
