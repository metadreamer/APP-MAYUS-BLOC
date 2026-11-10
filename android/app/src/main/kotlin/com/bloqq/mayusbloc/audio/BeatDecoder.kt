package com.bloqq.mayusbloc.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/** Un loop ya decodificado en memoria: estéreo intercalado, 16 bits, 44,1 kHz. */
class Beat(val id: String, val bpm: Double, val bars: Int, val pcm: ShortArray, val decodeDiffFrames: Int) {
    val frames: Int get() = pcm.size / 2
}

object BeatDecoder {
    const val SAMPLE_RATE = 44100

    /**
     * Decodifica un OGG a memoria y lo ajusta al largo exacto que dicen el BPM y los compases.
     * Ese ajuste es lo que garantiza que el loop empalme justo en el compás.
     */
    fun decode(id: String, file: File, bpm: Double, bars: Int): Beat {
        val ex = MediaExtractor()
        ex.setDataSource(file.absolutePath)
        var track = -1
        for (i in 0 until ex.trackCount) {
            if (ex.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) { track = i; break }
        }
        check(track >= 0) { "El archivo del beat no tiene audio" }
        ex.selectTrack(track)
        val inFmt = ex.getTrackFormat(track)
        val codec = MediaCodec.createDecoderByType(inFmt.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(inFmt, null, null, 0)
        codec.start()

        val out = ByteArrayOutputStream(1 shl 22)
        val info = MediaCodec.BufferInfo()
        var channels = inFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var rate = inFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var inputDone = false
        var outputDone = false
        val tmp = ByteArray(1 shl 16)
        while (!outputDone) {
            if (!inputDone) {
                val ix = codec.dequeueInputBuffer(10_000)
                if (ix >= 0) {
                    val n = ex.readSampleData(codec.getInputBuffer(ix)!!, 0)
                    if (n < 0) {
                        codec.queueInputBuffer(ix, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(ix, 0, n, ex.sampleTime, 0)
                        ex.advance()
                    }
                }
            }
            val ox = codec.dequeueOutputBuffer(info, 10_000)
            if (ox >= 0) {
                val buf = codec.getOutputBuffer(ox)!!
                buf.position(info.offset)
                var left = info.size
                while (left > 0) {
                    val n = minOf(left, tmp.size)
                    buf.get(tmp, 0, n)
                    out.write(tmp, 0, n)
                    left -= n
                }
                codec.releaseOutputBuffer(ox, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
            } else if (ox == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                channels = codec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                rate = codec.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            }
        }
        codec.stop(); codec.release(); ex.release()
        check(rate == SAMPLE_RATE) { "El beat debe estar a 44,1 kHz (está a $rate)" }

        val raw = out.toByteArray()
        val decoded = ShortArray(raw.size / 2)
        ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(decoded)
        val gotFrames = decoded.size / channels

        val wantFrames = (bars * 4 * 60.0 / bpm * SAMPLE_RATE).roundToInt()
        val pcm = ShortArray(wantFrames * 2)
        for (f in 0 until minOf(gotFrames, wantFrames)) {
            if (channels == 1) {
                pcm[2 * f] = decoded[f]; pcm[2 * f + 1] = decoded[f]
            } else {
                pcm[2 * f] = decoded[f * channels]; pcm[2 * f + 1] = decoded[f * channels + 1]
            }
        }
        return Beat(id, bpm, bars, pcm, gotFrames - wantFrames)
    }
}
