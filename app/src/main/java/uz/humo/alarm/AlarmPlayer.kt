package uz.humo.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Budilnik ovozi va vibratsiyasini MARKAZIY boshqaradi.
 *
 * Muammo (v2.0): ovoz faqat AlarmActivity ochilganda chalinardi. Telefon
 * qulflangan bo'lsa, Android orqa fondan Activity ochishni kechiktiradi —
 * avval bildirishnoma keladi, ovoz esa uni bosgandan keyingina chiqadi.
 *
 * Endi ovoz/vibratsiya xabar kelgan zahoti shu yerda, foreground xizmat
 * kontekstidan boshlanadi — Activity ochilishini kutmaydi.
 *
 * Rejim qoidasi:
 * - Telefon ovozsiz (SILENT) yoki vibratsiya (VIBRATE) rejimida bo'lsa:
 *   ovoz CHALINMAYDI, faqat kuchli vibratsiya beriladi.
 * - Oddiy (NORMAL) rejimda: maksimal ovozda budilnik + vibratsiya.
 */
object AlarmPlayer {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var oldAlarmVolume: Int = -1

    @Volatile
    var isRinging: Boolean = false
        private set

    @Synchronized
    fun start(ctx: Context) {
        if (isRinging) return
        val appCtx = ctx.applicationContext
        isRinging = true

        val am = appCtx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager = am
        val silentOrVibrate = am.ringerMode != AudioManager.RINGER_MODE_NORMAL

        if (!silentOrVibrate) {
            startSound(appCtx, am)
        }
        // Vibratsiya har ikki rejimda ham bor; ovozsiz rejimda — kuchliroq
        startVibration(appCtx, strong = silentOrVibrate)
    }

    private fun startSound(ctx: Context, am: AudioManager) {
        try {
            oldAlarmVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(
                AudioManager.STREAM_ALARM,
                am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0
            )

            val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(ctx, uri)
                isLooping = true
                // Ekran o'chgan bo'lsa ham ijro to'xtab qolmasin
                setWakeMode(ctx, PowerManager.PARTIAL_WAKE_LOCK)
                prepare()
                start()
            }
        } catch (e: Exception) {
            player = null
        }
    }

    private fun startVibration(ctx: Context, strong: Boolean) {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val v = vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (strong && v.hasAmplitudeControl()) {
                    // Kuchli: 1.2 soniya to'liq quvvatda + 0.25 soniya tanaffus, loop
                    val timings = longArrayOf(0, 1200, 250)
                    val amplitudes = intArrayOf(0, 255, 0)
                    v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, 0))
                } else {
                    val pattern = if (strong) {
                        longArrayOf(0, 1200, 250)
                    } else {
                        longArrayOf(0, 900, 400)
                    }
                    v.vibrate(VibrationEffect.createWaveform(pattern, 0))
                }
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(longArrayOf(0, 1200, 250), 0)
            }
        } catch (e: Exception) {
            // vibratsiya ixtiyoriy
        }
    }

    @Synchronized
    fun stop() {
        isRinging = false
        try {
            player?.stop()
            player?.release()
        } catch (e: Exception) {
        }
        player = null
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
        }
        if (oldAlarmVolume >= 0) {
            try {
                audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, oldAlarmVolume, 0)
            } catch (e: Exception) {
            }
            oldAlarmVolume = -1
        }
    }
}
