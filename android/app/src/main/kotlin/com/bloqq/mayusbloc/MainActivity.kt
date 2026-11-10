package com.bloqq.mayusbloc

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bloqq.mayusbloc.audio.AudioEngine
import com.bloqq.mayusbloc.audio.Mp3Encoder
import io.flutter.FlutterInjector
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.io.File
import java.util.concurrent.Executors

/**
 * Puente entre la interfaz (Flutter) y el motor de audio (Kotlin).
 * Flutter llama por el canal "mayusbloc/audio"; el trabajo pesado corre en un hilo aparte.
 */
private val sharedWorker = Executors.newSingleThreadExecutor()

class MainActivity : FlutterActivity() {
    private lateinit var engine: AudioEngine
    private val worker get() = sharedWorker
    private val main = Handler(Looper.getMainLooper())
    private var pendingPermission: MethodChannel.Result? = null

    private fun crashFile() = File(filesDir, "ultimo_error.txt")

    /**
     * Junta todo lo que se sabe del último cierre: el error que guardó la app y, en Android 11
     * o más nuevo, el motivo que anotó el propio sistema (sirve también para cierres del
     * código nativo o por falta de memoria, que la app no puede atrapar).
     */
    private fun lastCrash(): String? {
        val sb = StringBuilder()
        crashFile().takeIf { it.exists() }?.let { sb.append(it.readText()).append("\n") }
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            try {
                val am = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
                val seen = getPreferences(MODE_PRIVATE).getLong("exitSeen", 0)
                var newest = seen
                for (e in am.getHistoricalProcessExitReasons(packageName, 0, 5)) {
                    if (e.timestamp <= seen) continue
                    newest = maxOf(newest, e.timestamp)
                    val bad = e.reason in setOf(
                        android.app.ApplicationExitInfo.REASON_CRASH,
                        android.app.ApplicationExitInfo.REASON_CRASH_NATIVE,
                        android.app.ApplicationExitInfo.REASON_ANR,
                        android.app.ApplicationExitInfo.REASON_LOW_MEMORY,
                        android.app.ApplicationExitInfo.REASON_SIGNALED,
                        android.app.ApplicationExitInfo.REASON_INITIALIZATION_FAILURE,
                        android.app.ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE)
                    if (!bad) continue
                    sb.append("Sistema: motivo ${e.reason}, estado ${e.status}. ${e.description ?: ""}\n")
                    try {
                        e.traceInputStream?.use { s ->
                            val raw = s.readBytes()
                            // En cierres nativos el informe es binario: se rescatan los textos legibles.
                            val text = String(raw, Charsets.ISO_8859_1).replace(Regex("[^\\x20-\\x7E\\n]+"), " ")
                            sb.append(text.take(1500)).append("\n")
                        }
                    } catch (_: Throwable) {}
                }
                pendingExitSeen = newest
            } catch (_: Throwable) {}
        }
        return sb.toString().trim().ifEmpty { null }
    }
    private var pendingExitSeen = 0L

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        // Si la app se cierra por un error, se guarda el detalle para mostrarlo al reabrir.
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                crashFile().writeText("Hilo ${t.name}\n" + android.util.Log.getStackTraceString(e).take(6000))
            } catch (_: Throwable) {}
            previous?.uncaughtException(t, e)
        }
        super.onCreate(savedInstanceState)
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        // Un solo motor para toda la app: si Android recrea la pantalla, el audio y los beats ya cargados siguen.
        engine = AudioEngine.get(applicationContext)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "mayusbloc/audio")
            .setMethodCallHandler { call, result -> handle(call, result) }
    }

    private fun handle(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            // Rápidas: se responden en el momento.
            "status" -> result.success(engine.status())
            "lastCrash" -> result.success(lastCrash())
            "clearCrash" -> {
                crashFile().delete()
                getPreferences(MODE_PRIVATE).edit().putLong("exitSeen", pendingExitSeen).apply()
                result.success(null)
            }
            "deviceInfo" -> result.success(
                "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} · Android ${android.os.Build.VERSION.RELEASE} · ${android.os.Build.HARDWARE}")
            "requestMic" -> requestMic(result)
            "openSettings" -> {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                result.success(null)
            }
            // El resto va al hilo de trabajo.
            else -> worker.execute {
                try {
                    // Las acciones que no devuelven nada dan "Unit" en Kotlin, y Flutter no sabe
                    // enviar ese valor: hay que cambiarlo por null (este era el cierre de la 0.1.0).
                    val r = heavy(call).let { if (it is Unit) null else it }
                    main.post {
                        try { result.success(r) } catch (e: Throwable) { result.error("audio", e.toString(), null) }
                    }
                } catch (e: NotImplementedError) {
                    main.post { result.notImplemented() }
                } catch (e: Throwable) {
                    main.post { result.error("audio", e.message ?: e.toString(), null) }
                }
            }
        }
    }

    private fun heavy(call: MethodCall): Any? = when (call.method) {
        "loadBeat" -> {
            val id = call.argument<String>("id")!!
            engine.loadBeat(id, assetToFile(call.argument<String>("asset")!!, id),
                call.argument<Number>("bpm")!!.toDouble(), call.argument<Int>("bars")!!)
        }
        "playBeat" -> engine.playBeat(call.argument<String>("id")!!)
        "playFile" -> engine.playFile(call.argument<String>("path")!!)
        "stopPlayback" -> engine.stopPlayback()
        "startRecording" -> {
            val name = engine.startRecording(call.argument<String>("beatId"))
            main.post { window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            name
        }
        "stopRecording" -> {
            main.post { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            engine.stopRecording()
        }
        "listTakes" -> engine.listTakes()
        "storage" -> engine.storage()
        "deleteTake" -> engine.deleteTake(call.argument<String>("name")!!)
        "calibrate" -> engine.calibrate()
        "setRawMic" -> engine.setRawMic(call.argument<Boolean>("raw")!!)
        "mixPreview" -> buildMix(call).absolutePath
        "export" -> export(call)
        else -> throw NotImplementedError()
    }

    private fun exportDir() = File(cacheDir, "export").apply { mkdirs() }

    private fun buildMix(call: MethodCall): File {
        val name = call.argument<String>("name")!!
        return engine.mix(name, call.argument<Double>("voiceGain")!!, call.argument<Double>("beatGain")!!,
            call.argument<Double>("nudgeMs")!!, File(exportDir(), "${name}_mezcla.wav"))
    }

    /** Arma el archivo pedido (voz o mezcla, WAV o MP3) y abre el menú de compartir de Android. */
    private fun export(call: MethodCall): Map<String, Any> {
        val name = call.argument<String>("name")!!
        val kind = call.argument<String>("kind")!!   // "voz" o "mezcla"
        val format = call.argument<String>("format")!! // "wav" o "mp3"
        val wav = if (kind == "mezcla") buildMix(call)
                  else File(engine.takesDir, "$name.wav").copyTo(File(exportDir(), "$name.wav"), overwrite = true)
        val out = if (format == "mp3") {
            File(exportDir(), wav.nameWithoutExtension + ".mp3").also { Mp3Encoder.wavToMp3(wav, it, 192) }
        } else wav
        val uri = FileProvider.getUriForFile(this, "$packageName.files", out)
        val send = Intent(Intent.ACTION_SEND)
            .setType(if (format == "mp3") "audio/mpeg" else "audio/wav")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        main.post { startActivity(Intent.createChooser(send, "Compartir ${out.name}")) }
        return mapOf("file" to out.name, "bytes" to out.length())
    }

    /** Copia un beat empaquetado en la app a un archivo normal, que es lo que el decodificador sabe leer. */
    private fun assetToFile(assetKey: String, id: String): File {
        val dir = File(cacheDir, "beats").apply { mkdirs() }
        val out = File(dir, "$id.ogg")
        val lookup = FlutterInjector.instance().flutterLoader().getLookupKeyForAsset(assetKey)
        assets.open(lookup).use { i -> out.outputStream().use { o -> i.copyTo(o) } }
        return out
    }

    // El permiso de micrófono se pide recién cuando hace falta.
    private fun requestMic(result: MethodChannel.Result) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            result.success("ok"); return
        }
        pendingPermission = result
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != 1) return
        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        // "bloqueado" = Android ya no va a mostrar el cartel; hay que ir a los ajustes.
        val blocked = !granted && !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)
        pendingPermission?.success(if (granted) "ok" else if (blocked) "bloqueado" else "denegado")
        pendingPermission = null
    }
}
