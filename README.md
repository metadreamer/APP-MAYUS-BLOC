# MÁYUS BLOC

App para celular que captura ideas de rap: escritas, rapeadas solas o sobre un beat.
El plan completo está en [BRIEF.md](BRIEF.md). Estado actual: **hito 1, prototipo técnico**
(ver [docs/INFORME_HITO1.md](docs/INFORME_HITO1.md)).

## Cómo se compila

No hace falta instalar nada: al subir cambios, GitHub compila el APK solo
(`.github/workflows/build.yml`) y lo deja para bajar en la pestaña Actions del repo.

Para compilar a mano hacen falta Flutter 3.47 y el kit de Android:

```
flutter build apk --release --split-per-abi
```

El APK para casi todos los celulares actuales es `build/app/outputs/flutter-apk/app-arm64-v8a-release.apk`.

Pruebas automáticas (archivos WAV, rescate de grabaciones y mezcla):

```
cd android
gradlew :app:testDebugUnitTest
```

## Firma

La app se firma con `android/mayusbloc-release.jks`, cuyos datos están en `android/key.properties`.
**Ninguno de los dos se sube al repo.** Si se pierden, las versiones nuevas no se pueden instalar
encima de las viejas sin desinstalar (y perder las ideas guardadas). BLOQQ guarda una copia.
Sin esos archivos el proyecto compila igual, firmado con una clave de pruebas.

## Carpetas

| Carpeta | Qué hay |
| --- | --- |
| `lib/` | Interfaz en Flutter. `lib/audio/audio_engine.dart` es la capa de audio por plataforma. |
| `android/app/src/main/kotlin/.../audio/` | Motor de audio para Android: reproducir, grabar, sincronizar, mezclar. |
| `android/app/src/main/cpp/` | LAME, el codificador de MP3 (licencia LGPL), y su puente con Kotlin. |
| `android/app/src/test/` | Pruebas automáticas. |
| `assets/beats/` | Loops y `manifest.json`. Para cambiar un beat se cambia el archivo y su línea en el manifiesto. |
| `tool/` | Script que genera los loops de prueba. |
| `docs/` | Informes de cada hito. |

## Supuestos tomados

- ID del paquete: `com.bloqq.mayusbloc`.
- Versión mínima: Android 8.0.
- Las tomas del prototipo se guardan como WAV más un archivo `.json` al lado con los datos del beat.
  La base de datos SQLite llega en el hito 3.
- El micrófono usa el modo de Android pensado para reconocer voz, que no aplica los filtros de llamada.
  Hay un modo "crudo" para comparar.
- Grabar con la pantalla apagada llega en el hito 5; en el prototipo la pantalla queda encendida mientras graba.
- LAME va como biblioteca separada (`libmayusmp3.so`), que es lo que pide su licencia.
