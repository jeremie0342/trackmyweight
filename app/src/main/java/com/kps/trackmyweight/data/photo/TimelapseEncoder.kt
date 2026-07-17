package com.kps.trackmyweight.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.GLES20
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encodage timelapse H.264 pur Android via MediaCodec + MediaMuxer.
 *
 * Approche : on décode chaque JPEG en Bitmap, on l'écrit dans un input surface d'encodeur
 * H.264, on drain les paquets vers MediaMuxer.
 *
 * Cette implémentation est SIMPLIFIÉE pour v1 : elle utilise le mode buffer (pas surface) avec
 * conversion Bitmap → YUV. Suffisant pour des vidéos de progression (rare, offline).
 */
@Singleton
class TimelapseEncoder @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    data class EncodeParams(
        val width: Int = 720,
        val height: Int = 1280,
        val fps: Int = 2,
        val bitrate: Int = 3_000_000,
    )

    /**
     * Encode une liste de fichiers image en un MP4.
     * Retourne le fichier MP4 créé, ou null en cas d'erreur.
     */
    fun encode(
        imagePaths: List<String>,
        params: EncodeParams = EncodeParams(),
    ): File? {
        if (imagePaths.isEmpty()) return null
        val outDir = File(context.filesDir, "videos").apply { if (!exists()) mkdirs() }
        val outFile = File(outDir, "timelapse_${System.currentTimeMillis()}.mp4")

        val format = MediaFormat.createVideoFormat("video/avc", params.width, params.height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, params.bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, params.fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val encoder = MediaCodec.createEncoderByType("video/avc")
        return runCatching {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var trackIndex = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()

            val frameDurationUs = 1_000_000L / params.fps

            imagePaths.forEachIndexed { i, path ->
                val bmp = BitmapFactory.decodeFile(path) ?: return@forEachIndexed
                val scaled = Bitmap.createScaledBitmap(bmp, params.width, params.height, true)
                if (scaled != bmp) bmp.recycle()

                val yuv = ByteArray(params.width * params.height * 3 / 2)
                bitmapToNV12(scaled, yuv)
                scaled.recycle()

                val inputIndex = encoder.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val inputBuf = encoder.getInputBuffer(inputIndex) ?: return@forEachIndexed
                    inputBuf.clear()
                    inputBuf.put(yuv)
                    val ptsUs = i * frameDurationUs
                    encoder.queueInputBuffer(inputIndex, 0, yuv.size, ptsUs, 0)
                }

                drain(encoder, muxer, bufferInfo, endOfStream = false) { fmt ->
                    if (!muxerStarted) {
                        trackIndex = muxer.addTrack(fmt)
                        muxer.start()
                        muxerStarted = true
                    }
                }.let { newTrack ->
                    if (newTrack >= 0) trackIndex = newTrack
                }
            }

            // Signal EOS
            val inputIndex = encoder.dequeueInputBuffer(10_000)
            if (inputIndex >= 0) {
                encoder.queueInputBuffer(inputIndex, 0, 0, imagePaths.size * frameDurationUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }
            drainUntilEos(encoder, muxer, bufferInfo, trackIndex, muxerStarted)

            encoder.stop()
            encoder.release()
            if (muxerStarted) { muxer.stop(); muxer.release() }
            outFile
        }.onFailure {
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            it.printStackTrace()
        }.getOrNull()
    }

    private fun drain(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        info: MediaCodec.BufferInfo,
        endOfStream: Boolean,
        onFormatChanged: (MediaFormat) -> Unit,
    ): Int {
        var newTrack = -1
        while (true) {
            val outIndex = encoder.dequeueOutputBuffer(info, if (endOfStream) 10_000 else 0)
            if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) break
            if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                onFormatChanged(encoder.outputFormat)
                continue
            }
            if (outIndex >= 0) {
                val out = encoder.getOutputBuffer(outIndex) ?: continue
                if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                    runCatching { muxer.writeSampleData(0, out, info) }
                }
                encoder.releaseOutputBuffer(outIndex, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
        }
        return newTrack
    }

    private fun drainUntilEos(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        info: MediaCodec.BufferInfo,
        trackIndex: Int,
        muxerStarted: Boolean,
    ) {
        var eos = false
        while (!eos) {
            val outIndex = encoder.dequeueOutputBuffer(info, 10_000)
            if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) continue
            if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) continue
            if (outIndex >= 0) {
                val out = encoder.getOutputBuffer(outIndex) ?: continue
                if (info.size > 0 && muxerStarted && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                    runCatching { muxer.writeSampleData(trackIndex.coerceAtLeast(0), out, info) }
                }
                encoder.releaseOutputBuffer(outIndex, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) eos = true
            }
        }
    }

    /**
     * Conversion basique Bitmap → NV12 (YUV planar). Approximation acceptable pour timelapse.
     */
    private fun bitmapToNV12(bitmap: Bitmap, out: ByteArray) {
        val w = bitmap.width
        val h = bitmap.height
        val argb = IntArray(w * h)
        bitmap.getPixels(argb, 0, w, 0, 0, w, h)
        val frameSize = w * h
        var uvIndex = frameSize
        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = argb[y * w + x]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val yVal = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                out[y * w + x] = yVal.coerceIn(0, 255).toByte()
                if (y % 2 == 0 && x % 2 == 0) {
                    val uVal = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val vVal = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    if (uvIndex + 1 < out.size) {
                        out[uvIndex] = uVal.coerceIn(0, 255).toByte()
                        out[uvIndex + 1] = vVal.coerceIn(0, 255).toByte()
                        uvIndex += 2
                    }
                }
            }
        }
    }
}
