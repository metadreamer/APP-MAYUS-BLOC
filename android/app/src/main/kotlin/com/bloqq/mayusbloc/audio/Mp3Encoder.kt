package com.bloqq.mayusbloc.audio

import java.io.File
import java.io.FileOutputStream

/** Convierte un WAV de 16 bits a MP3 de tasa fija usando LAME (código nativo en cpp/). */
object Mp3Encoder {
    init { System.loadLibrary("mayusmp3") }

    @JvmStatic private external fun nativeInit(sampleRate: Int, channels: Int, kbps: Int): Long
    @JvmStatic private external fun nativeEncode(h: Long, pcm: ShortArray, frames: Int, channels: Int, out: ByteArray): Int
    @JvmStatic private external fun nativeFlush(h: Long, out: ByteArray): Int
    @JvmStatic private external fun nativeClose(h: Long)

    fun wavToMp3(wav: File, mp3: File, kbps: Int = 192) {
        val info = Wav.info(wav)
        val h = nativeInit(info.sampleRate, info.channels, kbps)
        check(h != 0L) { "No se pudo iniciar el codificador MP3" }
        try {
            val chunkFrames = 1152 * 16
            val out = ByteArray((chunkFrames * 1.25).toInt() + 7200)
            FileOutputStream(mp3).use { fos ->
                Wav.readChunks(wav, chunkFrames) { pcm, frames ->
                    val n = nativeEncode(h, pcm, frames, info.channels, out)
                    check(n >= 0) { "Error al codificar MP3 ($n)" }
                    fos.write(out, 0, n)
                }
                val n = nativeFlush(h, out)
                if (n > 0) fos.write(out, 0, n)
            }
        } finally {
            nativeClose(h)
        }
    }
}
