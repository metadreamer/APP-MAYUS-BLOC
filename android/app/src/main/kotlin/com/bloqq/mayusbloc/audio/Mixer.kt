package com.bloqq.mayusbloc.audio

import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

/** Mezcla fuera de tiempo real: voz mono + beat estéreo en loop, a un WAV estéreo. */
object Mixer {
    private const val CEILING = 0.891 // -1 dBFS

    /**
     * shiftFrames: en qué muestra de la voz cae el inicio del beat.
     * La mezcla arranca justo en el uno del beat, así que lo grabado antes se descarta.
     * Si la suma supera -1 dBFS se baja todo parejo; nunca se recorta la onda.
     */
    fun mix(voiceWav: File, beat: Beat, shiftFrames: Int, voiceGain: Double, beatGain: Double, out: File) {
        val voice = Wav.readAll(voiceWav)
        val start = maxOf(0, shiftFrames)
        val frames = voice.size - start
        check(frames > 0) { "La toma es demasiado corta para mezclar" }
        val bp = beat.pcm
        val loopFrames = beat.frames

        fun beatIndex(n: Int): Int = Math.floorMod(n - shiftFrames, loopFrames)

        // Primera pasada: buscar el pico.
        var peak = 0.0
        for (i in 0 until frames) {
            val n = start + i
            val v = voice[n] * voiceGain
            val bi = beatIndex(n) * 2
            val l = abs(v + bp[bi] * beatGain)
            val r = abs(v + bp[bi + 1] * beatGain)
            if (l > peak) peak = l
            if (r > peak) peak = r
        }
        val limit = CEILING * 32767.0
        val scale = if (peak > limit) limit / peak else 1.0

        // Segunda pasada: escribir.
        val w = WavWriter(out, BeatDecoder.SAMPLE_RATE, 2)
        try {
            val chunk = ShortArray(8192)
            var c = 0
            for (i in 0 until frames) {
                val n = start + i
                val v = voice[n] * voiceGain
                val bi = beatIndex(n) * 2
                chunk[c++] = ((v + bp[bi] * beatGain) * scale).roundToInt().toShort()
                chunk[c++] = ((v + bp[bi + 1] * beatGain) * scale).roundToInt().toShort()
                if (c == chunk.size) { w.write(chunk, c); c = 0 }
            }
            if (c > 0) w.write(chunk, c)
        } finally {
            w.close()
        }
    }
}
