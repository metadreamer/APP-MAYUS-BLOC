# Informe del hito 1: prototipo técnico

## Decisión de audio

**Motor propio en Kotlin, con las piezas básicas de Android (AudioTrack y AudioRecord). Sin Oboe por ahora.**

- Los paquetes de Flutter reproducen y graban por separado, sin un reloj en común. Sin eso no hay forma
  confiable de saber dónde cae el beat dentro de la grabación.
- Android informa en qué instante exacto sonó y se captó cada muestra, con el mismo reloj. Con esos dos
  datos se calcula el desfase y se guarda con cada voz.
- Como BLOQQ no se escucha la voz en vivo por los auriculares, no hace falta latencia mínima: hace falta
  latencia conocida. Por eso alcanza con Kotlin y no hace falta C++.
- El loop se decodifica una vez a memoria y se ajusta al largo exacto que dicen el BPM y los compases.
  Se reproduce escribiendo el final y el principio seguidos, sin hueco.
- El MP3 se hace con LAME, que suma 230 KB al APK.

Oboe queda como plan B si las pruebas en el celular muestran cortes o una sincronía inestable.

## Qué está verificado y qué no

Verificado en la computadora de desarrollo:

- El APK compila, pesa 17 MB y está firmado con la firma definitiva.
- Los loops en OGG se decodifican con el largo exacto (ni una muestra de más o de menos).
- LAME genera MP3 de 192 kbps válidos, mono y estéreo.
- Cuatro pruebas automáticas pasan: leer y escribir WAV, recuperar una grabación interrumpida,
  alinear el beat en la mezcla y que la mezcla nunca supere -1 dBFS.

**No verificado: nada de lo que necesita un celular real.** El prototipo no se pudo ejecutar acá.
Que suene, que grabe y que la sincronía sea buena lo decide la prueba de BLOQQ.

## Versión 0.1.1: el cierre de la 0.1.0

La 0.1.0 se cerraba al tocar play, parar o borrar. Las acciones que no devuelven ningún dato
le pasaban a Flutter un valor que no sabe enviar, y eso tiraba abajo la app. Se reprodujo en un
emulador de Android 14 y se corrigió. En ese mismo emulador se probó la 0.1.1: abrir, play,
parar, grabar sobre el beat, parar y exportar la mezcla en MP3, sin cierres.

Además, la 0.1.1:

- Muestra en un cartel rojo el detalle de cualquier cierre o error, con un botón para copiarlo.
- Muestra el modelo del celular y la versión de la app.
- Usa un solo motor de audio aunque Android recree la pantalla.
- Consulta la salida de audio solo cuando cambia, en vez de varias veces por segundo.
- Dibuja con OpenGL en vez de Vulkan, que da problemas en algunos chips.

El emulador no sirve para juzgar el sonido ni la sincronía: eso sigue dependiendo de la prueba en el celular.

## Lista de prueba para BLOQQ

Instalar `mayus-bloc-hito1-v0.1.1.apk` y anotar qué pasa en cada punto.

1. **Arranque.** La app abre y el cartel de arriba dice "Listo."
2. **Loop.** Tocar play en cada beat y escuchar dos minutos con auriculares. Cada compás empieza con
   un clic agudo. ¿Se nota un corte, un salto o un tiempo que se atrasa cuando el loop vuelve a empezar?
3. **Voz sola.** Apagar "Sobre el beat elegido", grabar diez segundos, parar y escuchar la toma.
4. **Sobre el beat, con cable.** Con auriculares de cable, grabar palmas justo sobre el clic durante
   cuatro compases. Abrir la toma y tocar "Escuchar mezcla". ¿Las palmas caen sobre el clic?
5. **Sobre el beat, con Bluetooth.** Lo mismo. Si las palmas quedan corridas, tocar "Calibrar latencia"
   acercando un auricular al micrófono, grabar otra toma y comparar. El "Ajuste fino" corrige lo que falte.
6. **Sin auriculares.** Grabar sobre el beat por el parlante: tiene que avisar antes de dejar seguir.
7. **Exportar.** Con una toma sobre beat, tocar los cuatro botones (voz WAV, voz MP3, mezcla WAV,
   mezcla MP3) y mandarse cada archivo por WhatsApp o guardarlo en Drive. ¿Se reproducen bien?
8. **Rescate.** Empezar a grabar y cerrar la app deslizándola desde las apps recientes. Al reabrir,
   la toma tiene que aparecer marcada como RECUPERADA.
9. **Desconexión.** Con un beat sonando, desenchufar o apagar los auriculares: el beat se tiene que parar.
10. **Micrófono.** Grabar la misma frase en modo "normal" y en modo "crudo" y decir cuál suena mejor.

Mandar una captura del cartel de arriba después de los puntos 4 y 5: muestra la salida, el desfase
medido, la calibración y los cortes detectados.

**El hito pasa si:** el loop no tiene corte audible, las palmas quedan sobre el clic con cable sin tocar
nada, los cuatro archivos se reproducen bien y la toma interrumpida se recupera.

## Lo que el prototipo todavía no hace

- Grabar con la pantalla apagada o desde otra app (hito 5). Mientras graba, la pantalla queda encendida.
- Cortar la grabación al entrar una llamada y avisar por poco espacio (hito 5).
- Base de datos, pestañas y diseño (hitos 2 y 3).
- Los beats finales (hito 6). Los dos loops incluidos son de prueba.
