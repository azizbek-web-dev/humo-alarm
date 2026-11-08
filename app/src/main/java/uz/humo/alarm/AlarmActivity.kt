package uz.humo.alarm

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

/**
 * To'liq ekranli budilnik oynasi.
 * Ovoz/vibratsiyani AlarmPlayer chaladi (xabar kelgan zahoti, shu oyna
 * ochilishini kutmasdan) — bu oyna faqat ko'rsatadi va to'xtatadi.
 */
class AlarmActivity : Activity() {

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

        // Oyna keyinroq ochilgan bo'lsa ham budilnik chalinib tursin
        AlarmPlayer.start(this)
    }

    private fun stopAndFinish() {
        AlarmPlayer.stop()
        AlarmStarter.cancelNotification(this)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Orqaga tugmasi bilan tasodifan o'chib ketmasin - faqat TO'XTATISH tugmasi
    }

    companion object {
        const val EXTRA_TEXT = "matched_text"
    }
}
