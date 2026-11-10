# MÁYUS BLOC: app de ideas de rap — brief v2

Autor: BLOQQ MÁYUS · versión 2 del 10 de noviembre de 2026 (reemplaza a la del 9 de noviembre)

## Qué cambió respecto de la v1

- **Sin celulares viejos.** Se diseña y se prueba para celulares actuales, con el Motorola G56 de BLOQQ como referencia. Se van los objetivos de 2 GB de RAM, el emulador de Android 8 y los niveles de calidad por equipo.
- **Sin PC.** La app se piensa solo para el celular: se captura, se trabaja, se mezcla y se comparte desde ahí. Nada del flujo depende de una computadora, ni para usarla ni para instalarla.
- **Entra la maqueta.** La idea no solo se captura: puede evolucionar dentro de la app hasta una maqueta básica de la canción.
- **Sin número fijo de volumen para los beats.** Se va el objetivo de -9 LUFS. Fable decide el máster para que beat, voz y mezcla suenen lo mejor posible con lo que da un celular.
- Se suman al brief los supuestos acordados en la revisión de la v1.

## Resumen

MÁYUS BLOC es una app para celular (Android primero, iOS a futuro) nacida de una situación concreta: a un rapero se le ocurre una idea en cualquier momento y necesita plasmarla ya, de forma eficiente y copada. La app le permite escribirla, rapearla sola o rapearla sobre un beat en un momento breve. Y si tiene tiempo, lo deja seguir trabajando para que la idea evolucione hasta una maqueta básica de la canción.

La promesa central es convertir una idea minúscula en una idea MAYÚSCULA. Esa transformación se siente en la interfaz, las animaciones y los colores, y ahora también en el recorrido: de un apunte suelto a una maqueta.

El universo BLOQQ MÁYUS es intuitivo, salvaje, práctico y eficiente: salvaje en la personalidad, nunca en la complejidad.

Se hace primero para BLOQQ MÁYUS (rapero y filmmaker argentino), después para compartir como .apk de persona a persona y publicar en Google Play.

Reparto de trabajo: Claude (Sonnet) planea. Fable 5.2 programa y diseña la app y también produce los beats.

## Cómo trabajar con este brief

**Prioridades cuando dos requisitos chocan**, de mayor a menor:

1. No perder nunca una idea.
2. Velocidad para capturarla.
3. Que suene bien: grabación, beats y mezcla.
4. Identidad visual y animaciones.
5. Funciones extra.

**Reglas de trabajo**

- Entregar por hitos chicos. Cada uno termina en una versión que BLOQQ instala y prueba en su celular.
- No preguntar lo que ya está decidido acá. Si algo falta, elegir la opción más simple que respete las prioridades, anotarla en la lista de supuestos y seguir.
- Frenar y preguntar solo si una decisión es costosa de rehacer, o si este brief la deja a elección de BLOQQ.
- No asumir que BLOQQ conoce Flutter ni Android. Explicar en español simple y corto.
- **Instalación sin PC:** GitHub compila el APK solo en cada entrega y BLOQQ lo baja e instala desde el celular. No hace falta instalar ninguna herramienta.
- Código simple, ordenado y comentado, con un README corto: estructura de carpetas, cómo se compila y supuestos tomados.

## Decisiones cerradas

1. **Pestañas separadas:** Notas, Voces y Beats.
2. **La voz siempre se guarda sola**, con opción de mezclarla con el beat. Se exporta en MP3 a 192 kbps (para compartir) o en WAV (calidad completa).
3. **Nombre final: MÁYUS BLOC, dentro del universo BLOQQ MÁYUS.** El concepto visual es la idea minúscula que se vuelve MAYÚSCULA.
4. **Flutter**, para poder sumar iOS más adelante.
5. **Cinco beats iniciales**, uno por estilo: boom bap agresivo, boom bap lofi, trap, Memphis y G-Funk. Los produce Fable 5.2.
6. **Solo celular.** Ninguna función depende de una PC.
7. **Celulares actuales.** No se optimiza ni se prueba para equipos viejos.

## Principios y alcance de la v1

- **Abre lista para capturar.** Una idea (nota o voz) se plasma en dos toques o menos desde que se abre la app.
- **Una pantalla, un propósito.** Pocas opciones a la vez.
- **Volver rápido a una idea.** Orden por fecha, buscador y favoritas con estrella.
- **De idea a maqueta.** Capturar es instantáneo; elaborar es opcional y nunca estorba a la captura.
- **Todo local.** Sin cuenta, sin internet, sin nube.

**Queda afuera de la v1, a propósito:** efectos de audio, cortar o editar audio, cuentas y nube, funciones sociales, inteligencia artificial y colaboración. Sí entra respaldar y exportar, para no perder rimas si se rompe o se pierde el celular.

## Pantallas y flujos

Barra inferior con tres pestañas: Notas, Voces y Beats. Al abrir se muestra la última pestaña usada, lista para capturar.

Si un beat está sonando, aparece un mini reproductor fijo con play, pausa y nombre, para que siga sonando mientras BLOQQ escribe o graba.

1. **Nota rápida:** pestaña Notas, botón de nota nueva, teclado ya abierto. Guardado automático mientras escribe. El título es opcional y por defecto es la primera línea.
2. **Voz:** pestaña Voces, botón grande de grabar, un toque para empezar y otro para parar. Se guarda sola con fecha y hora como nombre, y se puede renombrar.
3. **Sobre un beat:** en Beats se toca un loop y empieza a sonar. BLOQQ pasa a Voces para grabar o a Notas para escribir, con el beat sonando.
4. **Volver a una idea:** cada pestaña muestra su lista por fecha, con buscador y filtro de favoritas. Una voz se reproduce desde la lista, sin abrir otra pantalla.
5. **Hacer crecer la idea (maqueta):** ver la sección Maqueta.

**Atajos del sistema:** al mantener apretado el ícono aparecen "Nota rápida" y "Grabar".

## Maqueta

Es lo nuevo de esta versión. Propuesta de Fable, que BLOQQ confirma o corrige antes del hito 7:

- **Qué es:** una maqueta junta un beat, una letra (una nota) y varias tomas de voz, por ejemplo principal, doblaje y adlibs.
- **Cómo nace:** desde cualquier voz grabada sobre un beat, con una acción "Hacer maqueta". No hay que planearla de antemano: la idea rápida se convierte en maqueta cuando BLOQQ quiere.
- **Cómo crece:** se graba una toma nueva escuchando el beat y las tomas anteriores, con la letra a la vista. Máximo cuatro tomas.
- **Controles:** volumen y silencio por toma y para el beat. Nada más.
- **Salida:** la mezcla se exporta y se comparte desde el celular, en MP3 o WAV.
- **Dónde vive:** dentro de Voces, como una voz que creció, sin sumar una cuarta pestaña.
- **Lo que no es:** un estudio. No corta, no mueve, no agrega efectos.

El motor de audio se construye desde el hito 1 pensando en esto: un beat más varias voces sonando mientras se graba.

## Identidad visual y animación

La tecla Bloq Mayús es el concepto visual de toda la app.

- **Botones tipo tecla**, con relieve suave, que se hunden al tocarlos.
- **Luz de Caps Lock:** un LED pequeño que se enciende cuando la app graba o cuando suena un beat.
- **De minúscula a MAYÚSCULA:** al cerrar una idea, su título sube a mayúsculas con una animación de unos 300 ms. El título se muestra en mayúsculas pero se guarda tal cual se escribió. El cuerpo de la nota no se modifica nunca.
- **Modo oscuro por defecto.**
- **Un solo color de acento**, alto contraste, tipografía gruesa en mayúsculas para títulos y una muy legible para el cuerpo.
- **Tono:** agresivo pero simpático, como BLOQQ. Hermosa y única, sin recargar.

**Inspiración rap y hip hop, sin clichés.** La cultura se siente en el oficio y no en los accesorios.

- **Texturas sutiles:** grano fino de papel o cartón y trazos de marker, a muy baja opacidad, como el cuaderno de un writer. Nunca ladrillo, concreto ni fondos recargados.
- **Tags a mano alzada:** detalles con marker de writer, con la irregularidad natural del trazo, en pocos lugares: el logo, un subrayado sobre la idea guardada, la estrella de favoritas y los estados vacíos. Máximo un detalle a mano por pantalla.
- **Evitar:** cadenas y joyas, gorras o micrófonos retro como ícono, goteos de pintura, llamas, boombox y tipografías de graffiti de stock.

**Fresco y al día.** Jerarquía clara, buen aire, microinteracciones pulidas y movimiento con intención. Cada animación se justifica: si no ayuda a entender o a disfrutar, se saca.

**Fluidez.** Meta de 60 cuadros por segundo en el Motorola G56. Se respeta la opción del sistema de reducir animaciones.

Fable propone dos direcciones completas, con paleta, textura y estilo de tag, para que BLOQQ elija.

## Audio: grabar, mezclar y exportar

Criterio general: que todo suene lo mejor posible con lo que da un celular. Fable elige los niveles y el procesamiento con ese fin, y BLOQQ juzga con sus oídos.

- **Calidad de grabación:** mono, 44,1 kHz, 16 bits, sin pérdida (WAV), unos 5 MB por minuto. Siempre con el micrófono del celular, nunca con el del auricular Bluetooth, que baja la calidad a nivel llamada. La app muestra el espacio usado.
- **Grabar sobre beat:** el beat suena por auriculares y el micrófono capta solo la voz. Aviso breve la primera vez. Si se graba sin auriculares, se permite igual y la voz queda marcada como "grabada sin auriculares".
- **Sincronía:** si hay un beat elegido, al tocar grabar el beat arranca desde el inicio del loop y la grabación empieza al mismo tiempo. Con cada voz se guardan el beat usado, su BPM, el momento exacto de inicio y la latencia.
- **Latencia en tres capas:** estimación automática del sistema, calibración opcional (acercar el auricular al micrófono) y ajuste fino manual en la mezcla. Necesario sobre todo con Bluetooth.
- **Mezclar:** combina voz y beat, con un control de volumen para cada uno. Dura lo mismo que la voz y el beat se repite en loop. El resultado no satura: el pico queda en -1 dBFS o menos.
- **Exportar:** voz sola o mezcla, en MP3 a 192 kbps o en WAV, por el menú de compartir de Android (WhatsApp, Drive, etc.). Codificador MP3 liviano (LAME), sin bibliotecas de video.
- **Beats:** loop sin corte audible, con estilo y BPM visibles.
- **Permisos:** el micrófono se pide recién la primera vez que se toca grabar.
- **Grabación estable:** si se apaga la pantalla o se cambia de app, la grabación sigue, con la notificación fija que exige Android.

## Datos, archivos y respaldo

Todo vive en el celular: una base SQLite y una carpeta privada con los audios.

| Entidad | Campos | Notas |
| --- | --- | --- |
| Nota | id, texto, título, creación, modificación, favorita | Título por defecto: la primera línea. |
| Voz | id, nombre, archivo, duración, fecha, favorita, beat usado, inicio sobre el beat, latencia, con o sin auriculares | Los datos del beat son opcionales. |
| Maqueta | id, nombre, beat, nota con la letra, tomas (voces) con su volumen y silencio, volumen del beat | Se define en detalle en el hito 7. |
| Beat | id, nombre, estilo, BPM, compases, duración, archivo | Se lee de un manifiesto JSON. |
| Ajustes | última pestaña usada, calibración de latencia | Solo lo necesario. |

- **Beats intercambiables:** el manifiesto JSON permite usar loops de prueba y cambiarlos por los finales sin tocar código.
- **Respaldo:** un solo ZIP con las notas en texto, los audios originales y un archivo JSON con todo lo demás (favoritas, fechas, datos de beat, maquetas). Se comparte por el menú de Android. Al importar se suma a lo que hay: nunca pisa ni borra.
- **Borrar con red de seguridad:** se puede deshacer durante unos segundos.

## Casos borde

- **Se corta la grabación** (se cierra la app, se apaga el celular): el audio se escribe mientras se graba y al reabrir se recupera.
- **Entra una llamada:** la grabación se corta y se guarda lo grabado.
- **Se desconectan los auriculares:** el beat se pausa solo. Si se estaba grabando, la voz se sigue grabando.
- **Poco espacio:** avisar antes de grabar. Si se llena, cortar y guardar lo que haya.
- **Micrófono denegado u ocupado:** explicarlo en una línea y ofrecer ir a los ajustes. Notas y beats siguen funcionando.
- **Pantallas chicas o grandes:** nada queda cortado ni fuera del alcance de una mano.

## Rendimiento y compatibilidad

- **Versión mínima:** Android 8.0. No cuesta nada mantenerla y deja instalar la app a más gente, pero solo se prueba en celulares actuales.
- **Arranque:** se siente instantáneo en el Motorola G56.
- **Tamaño:** liviano, con meta orientativa de 30 MB. Un paquete por arquitectura de procesador.
- **Listas fluidas** con cientos de ideas.
- **Guardado automático** continuo en las notas.
- **Accesibilidad:** tamaño de texto del sistema, buen contraste y botones de al menos 48 dp.
- **Pruebas:** en el Motorola G56 y, cuando se pueda, en el celular de otra persona.

## Stack y recomendaciones técnicas

Flutter con Dart, pocas dependencias y manejo de estado simple.

- **Primero el riesgo técnico:** el hito 1 prueba juntos loop sin corte, grabación simultánea y exportación.
- **Audio:** motor propio en código nativo de Android (Kotlin), porque los paquetes de Flutter reproducen y graban sin un reloj común y así no se puede alinear. Como BLOQQ no se escucha la voz en vivo, importa conocer la latencia, no que sea mínima. Oboe queda como plan B.
- **Preparada para iOS:** el audio va detrás de una capa por plataforma.
- **Compilación:** GitHub Actions genera el APK en cada entrega.
- **Firma:** una firma definitiva desde el hito 1, para que actualizar la app nunca obligue a desinstalar y perder ideas. BLOQQ guarda una copia de la clave.
- **Identificadores y arte:** nombre visible MÁYUS BLOC, con tilde. ID del paquete `com.bloqq.mayusbloc`. Ícono adaptable, pantalla de arranque simple y capturas para Google Play.
- **Entrega:** APK firmado para compartir y AAB para Google Play.
- **Google Play:** hace falta política de privacidad, por el micrófono. Las cuentas personales nuevas exigen una prueba cerrada con 12 personas durante 14 días; conviene abrir la cuenta temprano.
- **Pruebas:** automáticas para la base de datos y la exportación. El audio se prueba a mano, con lista de control, en el celular.

## Beats iniciales

Cinco loops de 20 a 30 segundos, uno por estilo, cortados justo al final de un compás. Los BPM son una propuesta de partida.

| Estilo | BPM | Compases y duración | Carácter del sonido |
| --- | --- | --- | --- |
| Boom bap agresivo | 92 | 8 compases, 20,9 s | Bombo y caja secos y fuertes, bajo grave, hi-hat con swing |
| Boom bap lofi | 82 | 8 compases, 23,4 s | Teclas suaves, ruido de vinilo, batería relajada |
| Trap | 140 | 16 compases, 27,4 s | 808 largos, ráfagas de hi-hat, caja en el tercer tiempo |
| Memphis | 132 | 16 compases, 29,1 s | Cowbell, 808 sucio, melodía oscura y mínima |
| G-Funk | 94 | 8 compases, 20,4 s | Bajo sintético, lead tipo silbido, teclas con portamento |

- **Cómo se hacen:** Fable los sintetiza con código, sin samples ajenos. Son 100% originales y se pueden publicar sin problemas de derechos.
- **Máster:** a criterio de Fable, pensado para el uso real: fuertes y claros en auriculares y en el parlante del celular, y con lugar para que la voz entre encima sin pelear. Sin picos que saturen.
- **Graves:** el parlante de un celular casi no reproduce por debajo de unos 100 Hz, así que el bajo se refuerza con armónicos para que se escuche igual.
- **Loop:** las colas de reverb y delay cierran el empalme sin salto. En la app van en OGG, que no agrega silencio al inicio.
- **Revisión:** Fable mide volumen, picos y costura. No puede escuchar: el juicio final, en auriculares y en parlante, es de BLOQQ.

## Hitos

| Hito | Qué entrega Fable | Qué prueba BLOQQ en su celular |
| --- | --- | --- |
| 1. Prototipo técnico | App mínima con loop sin corte, grabación sobre beat, mezcla y exportación a MP3 y WAV, más un informe corto con la decisión de audio. | Escucha el loop, graba con auriculares de cable y Bluetooth, y reproduce y comparte los archivos exportados. |
| 2. Dirección visual | Dos propuestas con paleta, textura y estilo de tag, en tres pantallas: captura, lista de ideas y beats. | Elige una. |
| 3. Esqueleto | Tres pestañas, base de datos, guardado automático y loops de prueba, con la dirección elegida. | Navega y juzga la velocidad. |
| 4. Notas | Crear, editar, buscar, favoritas y borrar con deshacer. | Escribe, cierra la app a la fuerza y verifica que no se perdió nada. |
| 5. Voces | Grabar, reproducir, renombrar y exportar, con los casos borde resueltos. | Graba con la pantalla apagada. |
| 6. Beats y mezcla | Los cinco beats finales, mini reproductor, grabar sobre beat y mezcla. | Graba sobre cada estilo y juzga cómo suena. |
| 7. Maqueta | Varias tomas sobre un beat con la letra a la vista, volumen por toma y exportación. | Arma una maqueta de un tema real. |
| 8. Respaldo y pulido | Exportar e importar el ZIP, animaciones y tags. | Restaura un respaldo y juzga si se siente fresca y con carácter. |
| 9. Entrega | Optimización, APK firmado y requisitos de Google Play. | Lo instala y lo prueba con otra persona. |

**La v1 está lista cuando:**

- Una idea se captura en dos toques o menos desde que se abre la app.
- Ninguna nota ni grabación se pierde si la app se cierra o el celular se apaga.
- El beat suena en loop sin corte audible y la voz se graba sin él.
- La voz y el beat quedan alineados al mezclar.
- Los cinco beats suenan bien en auriculares y en el parlante del celular, a juicio de BLOQQ.
- Los MP3 y WAV exportados se reproducen bien en el celular y al compartirlos.
- Se puede armar una maqueta básica de un tema sin salir de la app.
- Un respaldo exportado se importa y restaura todo.
- Las animaciones se sienten parejas en el Motorola G56.

## Preguntas abiertas

Las decide BLOQQ antes o durante el desarrollo.

- **Dirección visual:** se elige en el hito 2.
- **Maqueta:** confirmar o corregir la propuesta de la sección Maqueta antes del hito 7.
- **Cuenta previa:** ¿cuatro tiempos antes de grabar sobre un beat? Sugerencia: opcional y apagada por defecto.
- **Modo ahorro de espacio:** ¿hace falta? Sugerencia: no en la v1.
- **Idioma:** español primero. ¿Inglés más adelante?
