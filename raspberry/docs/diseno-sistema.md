# Betómetro para Raspberry Pi — diseño del sistema

Estado: **diseño acordado con Matías (01/11/2026). Todavía no se programó nada.**

**Regla:** la app Android (`www/`, `android/`) **no se toca**. Todo lo de la Raspberry vive en `raspberry/`.

## Qué es

- Equipo **autónomo**: no necesita el celular para nada.
- **Betómetro es el sistema operativo:** se prende y arranca directo en Betómetro. No hay escritorio, ni usuario, ni navegador.
- Programa **nativo, hecho de cero**. No es un emulador de la app del celular.

## Equipo

- Raspberry Pi 3 B+
- Pantalla Raspberry Pi Touch Display 2 de 7" (DSI)
- GPS GY-NEO-M8N por UART (cableado en [cableado-gps.png](cableado-gps.png))
- Amplificador MAX98357A (I2S) con parlante de 3 W 4 Ω, para la voz (falta comprar)
- Pulsador en GPIO

### Pines

| Pin | Uso |
|---|---|
| 2 y 6 | Alimentación de la pantalla |
| 4 (5 V), 14 (GND) | GPS |
| 8 (TX) → RXD del GPS | GPS |
| 10 (RX) ← TXD del GPS | GPS |
| 7 (GPIO4) | PPS del GPS (opcional) |
| 12, 35, 40 | Amplificador I2S |
| 11 (GPIO17) y 9 (GND) | Pulsador (propuesto) |

- El Bluetooth se apaga (`dtoverlay=disable-bt`) para usar el UART bueno con el GPS.
- El GPS se configura al arrancar: solo GPS, **10 Hz**, 115200 baudios.
- La hora del sistema se toma del GPS (la Raspberry no tiene reloj propio).

## Sistema operativo

**Buildroot**: un Linux armado a medida, solo con lo necesario.

Por qué (para que no se tilde ni se trabe):
- Pocos programas corriendo: arranca rápido (unos 10–15 s) y no hay nada que compita con Betómetro.
- **Sistema de solo lectura:** cortar el contacto no lo rompe.
- Las carreras van en una partición aparte, con escritura segura (archivo nuevo y después reemplazo).
- **Vigilante (watchdog) del procesador:** si el programa se colgara, la Raspberry se reinicia sola y vuelve a Betómetro.
- **Dos copias del sistema (A y B):** la actualización se escribe en la copia que no se está usando. Si la nueva no arranca bien, vuelve sola a la anterior.

GitHub Actions arma la imagen completa (`.img`). La primera vez se graba en la tarjeta con Raspberry Pi Imager.

## Programa

- **Qt 6 con QML**, dibujando directo en la pantalla (EGLFS), con aceleración de la placa de video. Es lo que se usa en tableros de autos.
- **Motor:** se reescribe en C++ con la misma lógica que `Motor` de la app. Se prueba con los mismos datos que el motor de la app: tienen que dar igual.
- **Vertical u horizontal:** lo elige el usuario desde el inicio o desde Ajustes. Cada orientación tiene su armado de pantalla.
- **Voz:** Piper (voz natural, español argentino). Los números y frases se graban al armar la imagen; la Raspberry solo los reproduce, sin demora.
- **Diseño:** misma línea estética que la app (temas, Avispón Verde). Antes de programar, se muestran imágenes de muestra.

## Wifi

- Ajustes → Wifi: elegir red y escribir la clave con teclado en pantalla.
- En la pista lo normal es conectarse a la **zona wifi del celular**.

## Informes

- Se ven **en la pantalla de la Raspberry** (mapa, velocidad, aceleración, tabla), dibujados por el programa.
- **Compartir por wifi:** la Raspberry muestra un **código QR**. Con el celular en la misma red, se escanea y se descarga el informe en HTML, igual al de la app.

## Actualizaciones

- Solo cuando el usuario quiere: Ajustes → Actualizar, o tocando la versión con el texto amarillo «· Actualizar» en el inicio.
- Busca la última *release* de GitHub, descarga el paquete del sistema y lo instala en la otra copia (A/B). Reinicia en la versión nueva.

## Pendiente de definir

- Imágenes de muestra de las pantallas (inicio, carrera vertical y horizontal, ajustes, wifi, informe).
