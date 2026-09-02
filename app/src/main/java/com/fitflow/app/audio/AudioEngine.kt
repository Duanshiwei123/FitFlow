package com.fitflow.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

class AudioEngine(context: Context) {
    private val app: Context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val SR = 44100

    var voiceOn = true
    var volume = 0.7

    private var t0 = 0L
    private var offset = 0L
    private var pauseAt: Long? = null

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        tts = TextToSpeech(app) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale.SIMPLIFIED_CHINESE) ?: TextToSpeech.LANG_NOT_SUPPORTED
                ttsReady = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
                tts?.setSpeechRate(1.05f)
            }
        }
    }

    private fun raw(): Double = SystemClock.elapsedRealtime() / 1000.0

    fun now(): Double {
        val p = pauseAt
        return if (p != null) p / 1000.0 - t0 / 1000.0 - offset / 1000.0
        else raw() - t0 / 1000.0 - offset / 1000.0
    }

    fun reset() { t0 = SystemClock.elapsedRealtime(); offset = 0; pauseAt = null; stopAll() }
    fun pause() { if (pauseAt == null) pauseAt = SystemClock.elapsedRealtime() }
    fun resume() { val p = pauseAt ?: return; offset += SystemClock.elapsedRealtime() - p; pauseAt = null }
    fun isPaused(): Boolean = pauseAt != null

    private fun freqOf(sound: String): List<Triple<Double, Double, Double>> = when (sound) {
        "beat" -> listOf(Triple(760.0, 0.05, 0.28))
        "beatAccent" -> listOf(Triple(1180.0, 0.09, 0.42))
        "tick" -> listOf(Triple(1046.0, 0.09, 0.5))
        "start" -> listOf(Triple(660.0, 0.10, 0.42), Triple(990.0, 0.16, 0.40))
        "end" -> listOf(Triple(880.0, 0.12, 0.40), Triple(587.0, 0.18, 0.36))
        "rest" -> listOf(Triple(392.0, 0.30, 0.30))
        "finish" -> listOf(Triple(523.0, 0.14, 0.42), Triple(659.0, 0.14, 0.42), Triple(784.0, 0.34, 0.46))
        else -> listOf(Triple(760.0, 0.05, 0.28))
    }

    fun cue(sound: String) {
        freqOf(sound).forEachIndexed { i, (f, dur, amp) -> playTone(f, dur, amp, (i * 110).toLong()) }
    }

    fun playTone(freq: Double, durSec: Double, amp: Double, delayMs: Long) {
        if (volume <= 0.01) return
        val gain = (amp * volume).toFloat().coerceIn(0f, 1f)
        val n = (SR * durSec).toInt()
        if (n <= 0) return
        val samples = ShortArray(n)
        val step = 2.0 * PI * freq / SR
        for (i in 0 until n) {
            val env = 1.0 - i.toDouble() / n
            samples[i] = (sin(step * i) * env * gain * Short.MAX_VALUE).toInt().toShort()
        }
        val delayClamped = max(0, delayMs - 20)
        main.postDelayed({
            val minBuf = AudioTrack.getMinBufferSize(SR, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
                )
                .setAudioFormat(
                    AudioFormat.Builder().setSampleRate(SR)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(max(minBuf, n * 2))
                .build()
            runCatching {
                track.write(samples, 0, samples.size)
                track.play()
                main.postDelayed({
                    runCatching { track.stop() }
                    runCatching { track.release() }
                }, ((durSec * 1000) + 160).toLong())
            }
        }, delayClamped)
    }

    fun speak(text: String) {
        if (!voiceOn || !ttsReady) return
        runCatching { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ff_" + System.currentTimeMillis()) }
    }

    fun stopAll() { runCatching { tts?.stop() } }

    fun shutdown() {
        stopAll()
        runCatching { tts?.shutdown() }
        tts = null
    }
}