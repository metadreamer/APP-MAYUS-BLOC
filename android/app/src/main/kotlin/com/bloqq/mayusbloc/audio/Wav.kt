package com.bloqq.mayusbloc.audio

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Lectura y escritura de WAV de 16 bits con la cabecera simple de 44 bytes que genera la app. */
object Wav {
    const val HEADER = 44

    data class Info(val sampleRate: Int, val channels: Int, val frames: Int)

    fun header(sampleRate: Int, channels: Int, dataBytes: Int): ByteArray {
        val b = ByteBuffer.allocate(HEADER).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36 + dataBytes).put("WAVE".toByteArray())
        b.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(channels.toShort())
        b.putInt(sampleRate).putInt(sampleRate * channels * 2)
        b.putShort((channels * 2).toShort()).putShort(16)
        b.put("data".toByteArray()).putInt(dataBytes)
        return b.array()
    }

    /** Los tamaños se calculan con el largo real del archivo, así sirve aunque la cabecera esté vieja. */
    fun info(f: File): Info {
        RandomAccessFile(f, "r").use { r ->
            val h = ByteArray(HEADER)
            r.readFully(h)
            val b = ByteBuffer.wrap(h).order(ByteOrder.LITTLE_ENDIAN)
            val channels = b.getShort(22).toInt()
            val sampleRate = b.getInt(24)
            val frames = ((f.length() - HEADER) / (2 * channels)).toInt()
            return Info(sampleRate, channels, frames)
        }
    }

    /**
     * Rescate: si la app se cerró en medio de una grabación, la cabecera dice un tamaño menor
     * que el audio que realmente quedó en el disco. Acá se corrige. Devuelve true si hizo falta.
     */
    fun repair(f: File): Boolean {
        if (f.length() < HEADER) return false
        RandomAccessFile(f, "rw").use { r ->
            val real = (f.length() - HEADER).toInt()
            r.seek(40)
            val b = ByteArray(4)
            r.readFully(b)
            val said = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).int
            if (said == real) return false
            r.seek(4); r.write(le(36 + real))
            r.seek(40); r.write(le(real))
            return true
        }
    }

    fun readAll(f: File): ShortArray {
        val i = info(f)
        val out = ShortArray(i.frames * i.channels)
        var pos = 0
        readChunks(f, 8192) { pcm, frames ->
            System.arraycopy(pcm, 0, out, pos, frames * i.channels)
            pos += frames * i.channels
        }
        return out
    }

    /** Recorre el archivo de a pedazos, para no cargar todo en memoria. */
    fun readChunks(f: File, chunkFrames: Int, onChunk: (ShortArray, Int) -> Unit) {
        val i = info(f)
        val bytes = ByteArray(chunkFrames * i.channels * 2)
        val pcm = ShortArray(chunkFrames * i.channels)
        f.inputStream().buffered(1 shl 16).use { s ->
            s.skip(HEADER.toLong())
            var left = i.frames
            while (left > 0) {
                val want = minOf(left, chunkFrames)
                var got = 0
                val need = want * i.channels * 2
                while (got < need) {
                    val n = s.read(bytes, got, need - got)
                    if (n < 0) break
                    got += n
                }
                val frames = got / (i.channels * 2)
                if (frames == 0) break
                ByteBuffer.wrap(bytes, 0, got).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(pcm, 0, frames * i.channels)
                onChunk(pcm, frames)
                left -= frames
            }
        }
    }

    fun le(v: Int): ByteArray = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array()
}

/**
 * Escribe un WAV mientras llega el audio. El audio va directo al disco y la cabecera se
 * actualiza cada tanto: si la app muere, lo grabado hasta ese momento queda a salvo.
 */
class WavWriter(private val file: File, private val sampleRate: Int, private val channels: Int) {
    private val raf = RandomAccessFile(file, "rw")
    private var dataBytes = 0
    private var bytes = ByteArray(0)

    init {
        raf.setLength(0)
        raf.write(Wav.header(sampleRate, channels, 0))
    }

    fun write(pcm: ShortArray, samples: Int) {
        if (bytes.size < samples * 2) bytes = ByteArray(samples * 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcm, 0, samples)
        raf.write(bytes, 0, samples * 2)
        dataBytes += samples * 2
    }

    fun updateHeader() {
        val end = raf.filePointer
        raf.seek(4); raf.write(Wav.le(36 + dataBytes))
        raf.seek(40); raf.write(Wav.le(dataBytes))
        raf.seek(end)
    }

    fun close() {
        try { updateHeader() } finally { raf.close() }
    }
}
