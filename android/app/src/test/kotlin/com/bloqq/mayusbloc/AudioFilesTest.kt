package com.bloqq.mayusbloc

import com.bloqq.mayusbloc.audio.Beat
import com.bloqq.mayusbloc.audio.Mixer
import com.bloqq.mayusbloc.audio.Wav
import com.bloqq.mayusbloc.audio.WavWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.abs

/** Pruebas automáticas de archivos de audio: WAV, rescate de grabaciones y mezcla. */
class AudioFilesTest {
    private fun tmp(name: String) = File.createTempFile(name, ".wav").apply { deleteOnExit() }

    private fun writeMono(f: File, samples: ShortArray) {
        WavWriter(f, 44100, 1).apply { write(samples, samples.size); close() }
    }

    @Test fun wavSeEscribeYSeLeeIgual() {
        val f = tmp("ida_vuelta")
        val s = ShortArray(10_000) { (it * 3 - 15_000).toShort() }
        writeMono(f, s)
        val info = Wav.info(f)
        assertEquals(44100, info.sampleRate); assertEquals(1, info.channels); assertEquals(10_000, info.frames)
        assertTrue(s.contentEquals(Wav.readAll(f)))
    }

    @Test fun grabacionInterrumpidaSeRecupera() {
        // Simula que la app murió: hay audio en el disco pero la cabecera quedó en cero.
        val f = tmp("interrumpida")
        val w = WavWriter(f, 44100, 1)
        w.write(ShortArray(5000) { 1000 }, 5000)
        // (no se llama a close ni a updateHeader)
        RandomAccessFile(f, "r").use { it.seek(40); assertEquals(0, it.readInt()) }
        assertTrue(Wav.repair(f))
        assertEquals(5000, Wav.info(f).frames)
        assertEquals(5000, Wav.readAll(f).size)
        assertTrue(!Wav.repair(f)) // la segunda vez ya está bien
    }

    @Test fun mezclaAlineaElBeatConElDesfase() {
        // Beat de 100 muestras con un golpe en la muestra 0. Voz en silencio.
        val beat = Beat("t", 120.0, 1, ShortArray(200).also { it[0] = 10_000; it[1] = 10_000 }, 0)
        val voice = tmp("voz"); writeMono(voice, ShortArray(350))
        val out = tmp("mezcla")
        Mixer.mix(voice, beat, 30, 1.0, 1.0, out)
        val m = Wav.readAll(out)
        // La mezcla arranca en el uno del beat: dura 350 - 30 y el golpe cae en 0, 100, 200, 300.
        assertEquals((350 - 30) * 2, m.size)
        for (frame in 0 until 320) assertEquals(if (frame % 100 == 0) 10_000 else 0, m[frame * 2].toInt())
    }

    @Test fun mezclaNoSaturaNunca() {
        val beat = Beat("t", 120.0, 1, ShortArray(200) { 30_000 }, 0)
        val voice = tmp("voz_fuerte"); writeMono(voice, ShortArray(500) { 30_000 })
        val out = tmp("mezcla_fuerte")
        Mixer.mix(voice, beat, 0, 2.0, 1.0, out)
        val peak = Wav.readAll(out).maxOf { abs(it.toInt()) }
        assertTrue("pico $peak", peak <= (0.891 * 32767).toInt() + 1) // -1 dBFS
        assertTrue(peak > 29_000) // y tampoco quedó bajísima
    }
}
