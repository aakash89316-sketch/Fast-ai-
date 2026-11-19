package com.example.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class FightSound {
    PUNCH,
    KICK,
    CLAW,
    BLOCK,
    HIT_TAKEN,
    KO,
    UPGRADE,
}

class SoundEffectsManager {
    private val executor = Executors.newSingleThreadExecutor()
    private val sampleRate = 22050
    private val buffers: Map<FightSound, ShortArray> by lazy {
        mapOf(
            FightSound.PUNCH to buildPercussiveBuffer(freqStart = 180.0, freqEnd = 55.0, durationSec = 0.11, noiseMix = 0.45),
            FightSound.KICK to buildPercussiveBuffer(freqStart = 240.0, freqEnd = 70.0, durationSec = 0.14, noiseMix = 0.55),
            FightSound.CLAW to buildClawBuffer(),
            FightSound.BLOCK to buildMetallicBuffer(freq = 920.0, durationSec = 0.12),
            FightSound.HIT_TAKEN to buildPercussiveBuffer(freqStart = 130.0, freqEnd = 42.0, durationSec = 0.13, noiseMix = 0.5),
            FightSound.KO to buildKoGongBuffer(),
            FightSound.UPGRADE to buildChimeBuffer(),
        )
    }

    fun play(sound: FightSound, enabled: Boolean) {
        if (!enabled) return
        executor.execute {
            runCatching {
                val pcm = buffers[sound] ?: return@runCatching
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                )
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(maxOf(minBuf, pcm.size * 2))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep((pcm.size * 1000L / sampleRate) + 40L)
                track.stop()
                track.release()
            }
        }
    }

    fun release() {
        executor.shutdownNow()
    }

    private fun buildPercussiveBuffer(
        freqStart: Double,
        freqEnd: Double,
        durationSec: Double,
        noiseMix: Double,
    ): ShortArray {
        val count = (sampleRate * durationSec).toInt()
        val rng = Random(42)
        return ShortArray(count) { i ->
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / count
            val freq = freqStart + (freqEnd - freqStart) * progress
            val env = exp(-progress * 6.5)
            val tone = sin(2.0 * PI * freq * t)
            val noise = rng.nextDouble(-1.0, 1.0)
            val mixed = ((1.0 - noiseMix) * tone + noiseMix * noise) * env
            (mixed * Short.MAX_VALUE * 0.7).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun buildClawBuffer(): ShortArray {
        val durationSec = 0.24
        val count = (sampleRate * durationSec).toInt()
        val rng = Random(99)
        return ShortArray(count) { i ->
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / count
            val env = exp(-progress * 4.2)
            val sweep = sin(2.0 * PI * (1200.0 - 820.0 * progress) * t)
            val sub = sin(2.0 * PI * (160.0 - 90.0 * progress) * t)
            val hiss = rng.nextDouble(-1.0, 1.0)
            val sample = (0.45 * sweep + 0.35 * sub + 0.25 * hiss) * env
            (sample * Short.MAX_VALUE * 0.75).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun buildMetallicBuffer(freq: Double, durationSec: Double): ShortArray {
        val count = (sampleRate * durationSec).toInt()
        return ShortArray(count) { i ->
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / count
            val env = exp(-progress * 8.0)
            val sample = (sin(2.0 * PI * freq * t) + 0.6 * sin(2.0 * PI * freq * 1.47 * t)) * 0.5 * env
            (sample * Short.MAX_VALUE * 0.65).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun buildKoGongBuffer(): ShortArray {
        val durationSec = 0.45
        val count = (sampleRate * durationSec).toInt()
        return ShortArray(count) { i ->
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / count
            val env = exp(-progress * 3.5)
            val sample = (
                0.6 * sin(2.0 * PI * 110.0 * t) +
                    0.3 * sin(2.0 * PI * 165.0 * t) +
                    0.2 * sin(2.0 * PI * 221.0 * t)
                ) * env
            (sample * Short.MAX_VALUE * 0.8).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun buildChimeBuffer(): ShortArray {
        val durationSec = 0.22
        val count = (sampleRate * durationSec).toInt()
        return ShortArray(count) { i ->
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / count
            val freq = if (progress < 0.5) 523.25 else 783.99
            val env = exp(-progress * 4.0)
            val sample = sin(2.0 * PI * freq * t) * env
            (sample * Short.MAX_VALUE * 0.55).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }
}
