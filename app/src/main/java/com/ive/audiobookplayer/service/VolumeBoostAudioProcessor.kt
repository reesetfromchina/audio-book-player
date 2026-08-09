package com.ive.audiobookplayer.service

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

/**
 * A singleton audio processor that boosts the gain of PCM audio above unity.
 *
 * Kept in the audio pipeline at all times so gain changes apply on the very next
 * buffer, even mid-chapter. Formats we can't boost (e.g. 24-bit PCM) pass through
 * unchanged.
 */
@UnstableApi
object VolumeBoostAudioProcessor : BaseAudioProcessor() {

    const val MIN_GAIN = 1f
    const val MAX_GAIN = 4f

    @Volatile
    private var gain = MIN_GAIN

    private var canProcess = false

    fun setGain(gain: Float) {
        this.gain = gain.coerceIn(MIN_GAIN, MAX_GAIN)
    }

    fun getGain(): Float = gain

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val encoding = inputAudioFormat.encoding
        canProcess = encoding == C.ENCODING_PCM_16BIT || encoding == C.ENCODING_PCM_FLOAT
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return

        val buffer = replaceOutputBuffer(inputBuffer.remaining())

        if (!canProcess || gain == MIN_GAIN) {
            buffer.put(inputBuffer)
        } else {
            when (inputAudioFormat.encoding) {
                C.ENCODING_PCM_16BIT -> {
                    while (inputBuffer.hasRemaining()) {
                        val boosted = inputBuffer.short.toFloat() * gain
                        buffer.putShort(
                            boosted.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat())
                                .toInt().toShort()
                        )
                    }
                }

                C.ENCODING_PCM_FLOAT -> {
                    while (inputBuffer.hasRemaining()) {
                        buffer.putFloat(inputBuffer.float * gain)
                    }
                }
            }
        }

        buffer.flip()
    }
}
