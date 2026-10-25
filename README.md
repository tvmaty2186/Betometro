# Betómetro

Cronómetro de regularidad para el navegante: vueltas medidas y de práctica, cuenta regresiva en voz, velocidad necesaria con GPS, pulsador externo e historial.

## Instalar

Descargá `betometro.apk` desde la última [versión publicada](https://github.com/tvmaty2186/betometro/releases/latest) e instalalo en el teléfono Android. Las siguientes actualizaciones se hacen desde la app: **Ajustes → Aplicación → Buscar actualización**.

## Cómo está armado

- `www/index.html`: toda la app (pantalla, motor de cálculo, historial). El motor no depende de la plataforma.
- `android/`: proyecto Android generado con Capacitor. El código propio está en `android/app/src/main/java/ar/com/betometro/app/BetoPlugin.java` (voz sin demora, teclas del pulsador, actualizador).
- `.github/workflows/compilar.yml`: compila el APK. Cada etiqueta `vX.Y.Z` publica una versión nueva firmada.

## Publicar una versión

```
git tag v1.0.1 && git push origin v1.0.1
```

Requiere los secretos `BETO_KEYSTORE_B64` y `BETO_PASS` en el repositorio (la llave de firma; siempre la misma).
