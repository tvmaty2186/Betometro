# Betómetro — guía para Claude

Leé esto al empezar cualquier sesión en este repositorio. El detalle está en `docs/`:

| Archivo | Qué tiene |
|---|---|
| [docs/arquitectura.md](docs/arquitectura.md) | Cómo está hecha la app: código, motor, voz, GPS, widgets, informe, parte nativa |
| [docs/diseno.md](docs/diseno.md) | Línea estética: temas, tema Avispón Verde, colores, tipografías, reglas de layout |
| [docs/hardware.md](docs/hardware.md) | Equipo dedicado: Raspberry Pi (la caja con celular está cancelada), GPS, piezas compradas |
| [docs/historial.md](docs/historial.md) | Qué se hizo en cada versión y por qué (decisiones de Matías) |
| [PENDIENTES.md](PENDIENTES.md) | Lo que falta hacer |

## Qué es

Betómetro es una app Android para el **navegante de carreras de regularidad**. Las carreras se corren en vueltas y cada vuelta tiene un tiempo ideal. Hay vueltas **medidas** (cuentan para el resultado) y vueltas de **práctica**. El navegante marca con un pulsador o con el botón MARCAR cuando el auto pasa por la línea.

La app hace la cuenta regresiva en voz y calcula la velocidad sugerida con GPS. También muestra si vas adelantado o atrasado y guarda el historial, con un informe por carrera.

El dueño es **Matías Russo** (tvmaty2186@gmail.com). Corre con un **Peugeot 404 verde, número 101, apodado «el Avispón Verde»**. La app nació de un cronómetro con Arduino que él había hecho antes y que ya decía en voz la cuenta regresiva.

## Cómo trabajar con Matías

- **Idioma:** siempre en español rioplatense (vos), corto y claro. Tiene dislexia: van frases simples, sin paredes de texto, y lo importante primero.
- **Links:** cuando lo mandás a hacer algo, pasale el link directo para tocar, por ejemplo el APK, una página de GitHub o una descarga.
- **Antes de construir:** para cosas grandes, charlá primero. Si es visual, mostrale una imagen de muestra antes de programar; él confirma.
- **Tokens:** quiere ahorrarlos. No releas archivos enteros sin necesidad. `www/index.html` es grande, así que usá grep y leé por rangos.
- **Fotos de referencia:** cuando pasa fotos, prestá mucha atención a las posiciones y medidas.
- **El león es suyo:** el león corriendo (melena azul, cuerpo amarillo) lo dibujó él. **No es el logo de Peugeot.** No uses la marca PEUGEOT ni logos de concesionarias.
- **Play Protect:** a veces Google marca la app como «dañina» por los permisos que pide. Él no quiere apelar ni sacar permisos. La solución es apagar el análisis de Play Protect un momento, instalar y volver a prenderlo.

## Publicar una versión (receta)

1. Hacé los cambios. Toda la interfaz está en `www/index.html` y lo nativo en `android/app/src/main/java/ar/com/betometro/app/`.
2. Subí `version` en `package.json`: el primer número para un cambio enorme, el segundo para una función nueva, el tercero para un arreglo.
3. Hacé el commit con autor `Matías Russo <tvmaty2186@gmail.com>`. El mensaje va en español y empieza con la versión, por ejemplo `2.0.1: …`.
4. Hacé `git push origin HEAD:main`. GitHub Actions (`.github/workflows/compilar.yml`) compila y firma el APK. Si el tag `vX.Y.Z` no existe, publica la versión.
5. Seguí la compilación con `gh api repos/tvmaty2186/Betometro/actions/runs?per_page=1`. Tarda unos 4 a 6 minutos.
6. Link para Matías: https://github.com/tvmaty2186/Betometro/releases/latest/download/betometro.apk. La app también avisa sola («· Actualizar» en amarillo al lado de la versión y una notificación diaria).

**Secretos:** `BETO_KEYSTORE_B64` y `BETO_PASS` los carga Matías. **Nunca** escribas ni toques secretos ni la llave de firma.

## Probar sin celular

`www/index.html` abre directo en Chromium (Playwright está instalado: `require('playwright')`). Sin Capacitor funciona en «versión web de prueba», con simulador de velocidad.

- **Viewport:** usá 390×844 para vertical y 844×390 para horizontal.
- **Navegación en las pruebas:** las funciones están dentro de un cierre, así que no hay un `volver()` global. Navegá haciendo clic en `.pantalla:not([hidden]) .cab .btn.sec`.
- **Botones del inicio:** `#mCarreras`, `#mPractica`, `#mChequeo`, `#mPantalla`, `#mAjustes`.
- **Editor de carrera:** los campos son `#e-nombre`, `#e-vueltas`, `#e-distancia`, `#e-min` y `#e-seg`; se guarda con `#e-guardar`.
- **Largar:** `#p-detalle .btn.largar`.
