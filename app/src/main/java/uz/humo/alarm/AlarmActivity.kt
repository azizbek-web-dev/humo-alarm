package uz.humo.alarm

import android.app.Activity
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

/**
 * To'liq ekranli budilnik: STREAM_ALARM oqimida maksimal ovozda,
 * foydalanuvchi TO'XTATISH bosmaguncha loop chaladi + vibratsiya beradi.
 */
class AlarmActivity : Activity() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var oldAlarmVolume: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        setContentView(R.layout.activity_alarm)

        val text = intent?.getStringExtra(EXTRA_TEXT).orEmpty()
        findViewById<TextView>(R.id.alarmText).text =
            if (text.isBlank()) "Karta tushumi aniqlandi!" else text

        findViewById<Button>(R.id.stopButton).setOnClickListener { stopAndFinish() }

        startAlarmSound()
        startVibration()
    }

    private fun startAlarmSound() {
        try {
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val am = audioManager!!
            oldAlarmVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
            // Budilnik oqimini maksimalga ko'taramiz (DND da ham STREAM_ALARM eshitiladi)
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
                setDataSource(this@AlarmActivity, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // Ovoz chiqmasa ham ekran+vibratsiya qoladi
        }
    }

    private fun startVibration() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val pattern = longArrayOf(0, 900, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            // vibratsiya ixtiyoriy
        }
    }

    private fun stopAndFinish() {
        AlarmStarter.cancelNotification(this)
        finish()
    }

    override fun onDestroy() {
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
        // Eski ovoz balandligini qaytaramiz
        if (oldAlarmVolume >= 0) {
            try {
                audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, oldAlarmVolume, 0)
            } catch (e: Exception) {
            }
        }
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Orqaga tugmasi bilan tasodifan o'chib ketmasin - faqat TO'XTATISH tugmasi
    }

    companion object {
        const val EXTRA_TEXT = "matched_text"
    }
}
