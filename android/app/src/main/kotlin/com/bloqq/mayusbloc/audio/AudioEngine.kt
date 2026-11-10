package com.bloqq.mayusbloc.audio

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTimestamp
import android.media.AudioTrack
import android.media.MediaRecorder
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Motor de audio de MÁYUS BLOC.
 *
 * Idea central: reproducir y grabar con las APIs básicas de Android (AudioTrack y AudioRecord),
 * que informan en qué instante exacto sonó o se captó cada muestra, las dos con el mismo reloj.
 * Con esos dos datos se calcula dónde cae el inicio del beat dentro de la grabación.
 */
class AudioEngine(private val ctx: Context) {
    companion object {
        const val SR = BeatDecoder.SAMPLE_RATE
    }

    private val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val prefs = ctx.getSharedPreferences("audio", Context.MODE_PRIVATE)
    private val beats = HashMap<String, Beat>()
    val takesDir: File = File(ctx.filesDir, "voces").apply { mkdirs() }

    private var player: Player? = null
    private var playingBeatId: String? = null
    private var playingFile: String? = null
    private var recorder: Recorder? = null
    private var lastSync = "sin datos"

    // Si se desconectan los auriculares, el beat se pausa solo. La grabación sigue.
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) { stopPlayback() }
    }

    init {
        androidx.core.content.ContextCompat.registerReceiver(
            ctx, noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    // ---------------------------------------------------------------- beats

    fun loadBeat(id: String, file: File, bpm: Double, bars: Int): Map<String, Any> {
        val b = beats.getOrPut(id) { BeatDecoder.decode(id, file, bpm, bars) }
        return mapOf("frames" to b.frames, "decodeDiffFrames" to b.decodeDiffFrames)
    }

    @Synchronized fun playBeat(id: String) {
        val b = beats[id] ?: error("Beat no cargado: $id")
        stopPlayback()
        player = Player(loop = b.pcm, oneShot = null).also { it.start() }
        playingBeatId = id
    }

    /** Reproduce un WAV (una voz o una mezcla) una sola vez. */
    @Synchronized fun playFile(path: String) {
        stopPlayback()
        val f = File(path)
        val info = Wav.info(f)
        val pcm = Wav.readAll(f)
        val stereo = if (info.channels == 2) pcm else ShortArray(pcm.size * 2) { pcm[it / 2] }
        player = Player(loop = null, oneShot = stereo).also { it.start() }
        playingFile = path
    }

    @Synchronized fun stopPlayback() {
        player?.stop()
        player = null
        playingBeatId = null
        playingFile = null
    }

    // ------------------------------------------------------------- grabación

    /**
     * Empieza a grabar. Si se pasa un beat, primero arranca el micrófono y enseguida el beat
     * desde el inicio del loop; el desfase real entre ambos se mide al parar.
     */
    @Synchronized fun startRecording(beatId: String?): String {
        check(recorder == null) { "Ya se está grabando" }
        stopPlayback()
        val name = "voz_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val wav = File(takesDir, "$name.wav")
        val beat = beatId?.let { beats[it] ?: error("Beat no cargado: $it") }
        val meta = JSONObject()
        meta.put("headphones", hasHeadphones())
        meta.put("route", routeName())
        if (beat != null) {
            meta.put("beatId", beat.id)
            meta.put("bpm", beat.bpm)
            meta.put("calMs", calibrationMs())
        }
        // Se escribe ya, para que la toma sea recuperable aunque la app se cierre.
        File(takesDir, "$name.json").writeText(meta.toString())

        val rec = Recorder(wav)
        rec.start()
        recorder = rec
        if (beat != null) {
            player = Player(loop = beat.pcm, oneShot = null).also { it.start() }
            playingBeatId = beat.id
        }
        return name
    }

    @Synchronized fun stopRecording(): Map<String, Any?> {
        val rec = recorder ?: error("No se está grabando")
        val p = player
        // Las marcas de tiempo se leen antes de frenar, que es cuando son más confiables.
        val sync = if (p != null && playingBeatId != null) measureOffset(p, rec) else null
        rec.stop()
        recorder = null
        stopPlayback()

        val name = rec.file.nameWithoutExtension
        val metaFile = File(takesDir, "$name.json")
        val meta = JSONObject(metaFile.readText())
        if (meta.has("beatId")) {
            meta.put("offsetFrames", sync?.first ?: 0)
            meta.put("syncMeasured", sync?.second ?: false)
            lastSync = if (sync?.second == true) "medida: ${(sync.first * 1000.0 / SR).roundToInt()} ms"
                       else "estimada (el equipo no dio marcas de tiempo)"
        }
        metaFile.writeText(meta.toString())
        return takeInfo(rec.file)
    }

    /**
     * Devuelve en qué muestra de la grabación empezó a sonar el beat, y si se pudo medir.
     * T_beat0 = instante en que la muestra 0 del beat salió por el auricular.
     * T_rec0  = instante en que el micrófono captó la muestra 0 de la grabación.
     */
    private fun measureOffset(p: Player, r: Recorder): Pair<Int, Boolean> {
        val pt = p.timestamp()
        val rt = r.timestamp()
        if (pt != null && rt != null) {
            val beat0 = pt.nanoTime - pt.framePosition * 1_000_000_000L / SR
            val rec0 = rt.nanoTime - rt.framePosition * 1_000_000_000L / SR
            val frames = ((beat0 - rec0) * SR / 1_000_000_000L).toInt()
            return Pair(frames, true)
        }
        // Plan B: diferencia entre los momentos en que se dio la orden de arrancar.
        val frames = ((p.startNanos - r.startNanos) * SR / 1_000_000_000L).toInt()
        return Pair(frames, false)
    }

    // ------------------------------------------------------------ calibración

    /**
     * Reproduce una serie de clics y los graba a la vez. Compara dónde deberían caer según las
     * marcas de tiempo con dónde cayeron de verdad, y guarda la diferencia para esta salida
     * (parlante, cable o Bluetooth). Con auriculares hay que acercar uno al micrófono.
     */
    fun calibrate(): Map<String, Any> {
        synchronized(this) { check(recorder == null) { "No se puede calibrar mientras se graba" }; stopPlayback() }
        val total = SR * 4
        val clickLen = SR / 200 // 5 ms
        val clickPos = (1..7).map { it * SR / 2 }
        val pcm = ShortArray(total * 2)
        for (cp in clickPos) for (i in 0 until clickLen) {
            val v = (Math.sin(2 * Math.PI * 2000 * i / SR) * 26000 * (1.0 - i.toDouble() / clickLen)).toInt().toShort()
            pcm[2 * (cp + i)] = v; pcm[2 * (cp + i) + 1] = v
        }
        val tmp = File(ctx.cacheDir, "calibracion.wav")
        val rec = Recorder(tmp)
        rec.start()
        val p = Player(loop = null, oneShot = pcm)
        p.start()
        Thread.sleep(4300)
        val sync = measureOffset(p, rec)
        rec.stop()
        p.stop()

        val voice = Wav.readAll(tmp)
        tmp.delete()
        // Suma acumulada del volumen, para medir rápido la energía en cualquier ventana.
        val acc = LongArray(voice.size + 1)
        for (i in voice.indices) acc[i + 1] = acc[i] + abs(voice[i].toInt())
        fun score(lag: Int): Long {
            var s = 0L
            for (cp in clickPos) {
                val a = sync.first + cp + lag
                if (a < 0 || a + clickLen > voice.size) return -1
                s += acc[a + clickLen] - acc[a]
            }
            return s
        }
        var best = 0; var bestScore = -1L; var sum = 0.0; var n = 0
        for (lag in -SR / 10..SR / 2) {
            val s = score(lag)
            if (s < 0) continue
            sum += s; n++
            if (s > bestScore) { bestScore = s; best = lag }
        }
        val ok = n > 0 && bestScore > 4 * (sum / n)
        val ms = best * 1000.0 / SR
        if (ok) prefs.edit().putFloat("cal_" + routeClass(), ms.toFloat()).apply()
        return mapOf("ok" to ok, "ms" to ms, "route" to routeName(), "syncMeasured" to sync.second)
    }

    fun calibrationMs(): Double = prefs.getFloat("cal_" + routeClass(), 0f).toDouble()

    // ---------------------------------------------------------------- tomas

    fun listTakes(): List<Map<String, Any?>> {
        val files = takesDir.listFiles { f -> f.extension == "wav" } ?: emptyArray()
        val active = recorder?.file
        return files.filter { it != active }.sortedByDescending { it.name }.map { takeInfo(it) }
    }

    fun takeInfo(wav: File): Map<String, Any?> {
        val recovered = Wav.repair(wav)
        val info = Wav.info(wav)
        val metaFile = File(takesDir, wav.nameWithoutExtension + ".json")
        val meta = if (metaFile.exists()) JSONObject(metaFile.readText()) else JSONObject()
        return mapOf(
            "name" to wav.nameWithoutExtension,
            "path" to wav.absolutePath,
            "seconds" to info.frames.toDouble() / info.sampleRate,
            "bytes" to wav.length(),
            "beatId" to meta.optString("beatId", "").ifEmpty { null },
            "offsetMs" to meta.optInt("offsetFrames", 0) * 1000.0 / SR,
            "calMs" to meta.optDouble("calMs", 0.0),
            "syncMeasured" to meta.optBoolean("syncMeasured", false),
            "headphones" to meta.optBoolean("headphones", false),
            "route" to meta.optString("route", ""),
            "recovered" to recovered,
        )
    }

    fun deleteTake(name: String) {
        File(takesDir, "$name.wav").delete()
        File(takesDir, "$name.json").delete()
    }

    /** Mezcla una toma con su beat. Devuelve el WAV resultante. */
    fun mix(name: String, voiceGain: Double, beatGain: Double, nudgeMs: Double, out: File): File {
        val meta = JSONObject(File(takesDir, "$name.json").readText())
        val beat = beats[meta.getString("beatId")] ?: error("El beat de esta toma no está cargado")
        val shiftMs = meta.optInt("offsetFrames", 0) * 1000.0 / SR + meta.optDouble("calMs", 0.0) + nudgeMs
        Mixer.mix(File(takesDir, "$name.wav"), beat, (shiftMs * SR / 1000.0).roundToInt(), voiceGain, beatGain, out)
        return out
    }

    // ---------------------------------------------------------------- estado

    @Synchronized fun status(): Map<String, Any?> = mapOf(
        "playingBeatId" to playingBeatId,
        "playingFile" to playingFile?.takeIf { player?.finished == false },
        "recording" to (recorder != null),
        "recSeconds" to (recorder?.frames ?: 0L).toDouble() / SR,
        "level" to (recorder?.level ?: 0.0),
        "underruns" to (player?.underruns() ?: 0),
        "headphones" to hasHeadphones(),
        "route" to routeName(),
        "calMs" to calibrationMs(),
        "lastSync" to lastSync,
        "micSource" to micSourceName(),
        "storageBytes" to (takesDir.listFiles()?.sumOf { it.length() } ?: 0L),
        "freeBytes" to takesDir.usableSpace,
    )

    fun setRawMic(raw: Boolean) { prefs.edit().putBoolean("rawMic", raw).apply() }
    private fun rawMic() = prefs.getBoolean("rawMic", false)
    private fun micSourceName() = if (rawMic()) "crudo" else "normal"

    private val headphoneTypes = setOf(
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, 26 /* TYPE_BLE_HEADSET */,
    )
    private fun outputs() = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    fun hasHeadphones() = outputs().any { it.type in headphoneTypes }
    private fun routeClass(): String {
        val o = outputs()
        return when {
            o.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == 26 } -> "bt"
            o.any { it.type in headphoneTypes } -> "cable"
            else -> "parlante"
        }
    }
    private fun routeName() = when (routeClass()) {
        "bt" -> "Auriculares Bluetooth"
        "cable" -> "Auriculares con cable"
        else -> "Parlante del celular"
    }

    // --------------------------------------------------------------- Player

    /** Un hilo que le va dando audio a la salida. Si hay loop, lo repite sin corte. */
    private inner class Player(private val loop: ShortArray?, private val oneShot: ShortArray?) {
        private val bufFrames = 1024
        private val track: AudioTrack
        @Volatile private var running = false
        @Volatile var finished = false
        @Volatile var startNanos = 0L
        private var thread: Thread? = null

        init {
            val min = AudioTrack.getMinBufferSize(SR, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
            track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder()
                    .setSampleRate(SR).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                .setBufferSizeInBytes(maxOf(min, bufFrames * 4 * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }

        fun start() {
            running = true
            thread = Thread({
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
                val src = loop ?: oneShot!!
                var pos = 0
                track.play()
                startNanos = System.nanoTime()
                while (running) {
                    if (loop != null) {
                        // Se escribe hasta el final del loop y se sigue desde el inicio: sin hueco.
                        val n = minOf(bufFrames * 2, src.size - pos)
                        val w = track.write(src, pos, n)
                        if (w < 0) break
                        pos = (pos + w) % src.size
                    } else {
                        if (pos >= src.size) break
                        val w = track.write(src, pos, minOf(bufFrames * 2, src.size - pos))
                        if (w < 0) break
                        pos += w
                    }
                }
                if (running && loop == null) {
                    // Deja que termine de sonar lo que quedó en el búfer.
                    val endFrames = src.size / 2
                    var guard = 0
                    while (running && track.playbackHeadPosition < endFrames && guard++ < 200) Thread.sleep(10)
                }
                finished = true
            }, "mayus-player").also { it.start() }
        }

        fun stop() {
            running = false
            try { track.pause(); track.flush() } catch (_: Exception) {}
            thread?.join(500)
            try { track.release() } catch (_: Exception) {}
        }

        fun timestamp(): AudioTimestamp? {
            val ts = AudioTimestamp()
            return try { if (track.getTimestamp(ts) && ts.framePosition > 0) ts else null } catch (_: Exception) { null }
        }

        fun underruns(): Int = try { track.underrunCount } catch (_: Exception) { 0 }
    }

    // ------------------------------------------------------------- Recorder

    /** Un hilo que lee el micrófono y lo escribe directo a un WAV mono. */
    @SuppressLint("MissingPermission")
    private inner class Recorder(val file: File) {
        private val rec: AudioRecord
        private val writer = WavWriter(file, SR, 1)
        @Volatile private var running = false
        @Volatile var frames = 0L
        @Volatile var level = 0.0
        @Volatile var startNanos = 0L
        private var thread: Thread? = null

        init {
            val min = AudioRecord.getMinBufferSize(SR, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            // "Normal" = sin los filtros de llamada. "Crudo" = sin ningún proceso (si el equipo lo permite).
            val source = if (rawMic() && am.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true")
                MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.VOICE_RECOGNITION
            rec = AudioRecord(source, SR, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(min, SR))
            check(rec.state == AudioRecord.STATE_INITIALIZED) { "El micrófono está ocupado o no disponible" }
            // Siempre el micrófono del celular, nunca el del auricular Bluetooth.
            am.getDevices(AudioManager.GET_DEVICES_INPUTS)
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
                ?.let { rec.setPreferredDevice(it) }
        }

        fun start() {
            rec.startRecording()
            startNanos = System.nanoTime()
            check(rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "No se pudo empezar a grabar" }
            running = true
            thread = Thread({
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
                val buf = ShortArray(2048)
                var sinceHeader = 0
                while (running) {
                    val n = rec.read(buf, 0, buf.size)
                    if (n <= 0) break
                    try {
                        writer.write(buf, n)
                    } catch (e: Exception) {
                        break // disco lleno: se corta y queda guardado lo anterior
                    }
                    frames += n
                    var peak = 0
                    for (i in 0 until n) { val a = abs(buf[i].toInt()); if (a > peak) peak = a }
                    level = peak / 32768.0
                    sinceHeader += n
                    if (sinceHeader >= SR) { writer.updateHeader(); sinceHeader = 0 }
                }
            }, "mayus-recorder").also { it.start() }
        }

        fun timestamp(): AudioTimestamp? {
            val ts = AudioTimestamp()
            return try {
                if (rec.getTimestamp(ts, AudioTimestamp.TIMEBASE_MONOTONIC) == AudioRecord.SUCCESS && ts.framePosition > 0) ts else null
            } catch (_: Exception) { null }
        }

        fun stop() {
            running = false
            try { rec.stop() } catch (_: Exception) {}
            thread?.join(1000)
            rec.release()
            writer.close()
        }
    }
}
