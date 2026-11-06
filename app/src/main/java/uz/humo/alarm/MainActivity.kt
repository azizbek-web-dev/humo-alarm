package uz.humo.alarm

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

/** Sozlamalar ekrani */
class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var fsiText: TextView
    private lateinit var suffixInput: EditText
    private lateinit var incomeInput: EditText
    private lateinit var expenseInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        fsiText = findViewById(R.id.fsiText)
        suffixInput = findViewById(R.id.suffixInput)
        incomeInput = findViewById(R.id.incomeInput)
        expenseInput = findViewById(R.id.expenseInput)

        suffixInput.setText(Prefs.suffix(this))
        incomeInput.setText(Prefs.incomeWords(this).joinToString("\n"))
        expenseInput.setText(Prefs.expenseWords(this).joinToString("\n"))

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            Prefs.save(
                this,
                suffixInput.text.toString(),
                incomeInput.text.toString(),
                expenseInput.text.toString()
            )
            Toast.makeText(this, "Saqlandi ✅", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.listenerButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            Toast.makeText(
                this,
                "Ro'yxatdan «HUMO budilnik»ni topib, ruxsatni yoqing",
                Toast.LENGTH_LONG
            ).show()
        }

        findViewById<Button>(R.id.fsiButton).setOnClickListener {
            if (Build.VERSION.SDK_INT >= 34) {
                try {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            Uri.parse("package:$packageName")
                        )
                    )
                } catch (e: Exception) {
                    startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:$packageName")
                        )
                    )
                }
            }
        }

        findViewById<Button>(R.id.testButton).setOnClickListener {
            val demoSuffix = suffixInput.text.toString().trim()
                .ifEmpty { Prefs.DEFAULT_SUFFIX }
            AlarmStarter.fire(
                this,
                "SINOV 💳 Karta *$demoSuffix\nKirim: +5 000 000 so'm\nBalans: 5 250 000 so'm"
            )
        }

        // Android 13+ da bildirishnoma ruxsatini so'raymiz
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val granted = isListenerEnabled()
        statusText.text = if (granted) {
            "✅ Bildirishnoma ruxsati: BERILGAN — ilova Telegram xabarlarini kuzatyapti"
        } else {
            "❌ Bildirishnoma ruxsati: BERILMAGAN — pastdagi tugma bilan yoqing, bo'lmasa budilnik ishlamaydi"
        }

        if (Build.VERSION.SDK_INT >= 34) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val fsiOk = try {
                nm.canUseFullScreenIntent()
            } catch (e: Exception) {
                true
            }
            fsiText.text = if (fsiOk) {
                "✅ To'liq ekran ruxsati (Android 14+): berilgan"
            } else {
                "⚠️ To'liq ekran ruxsati (Android 14+): berilmagan — budilnik ekranni o'zi ochishi uchun «To'liq ekran ruxsati» tugmasidan yoqing"
            }
        } else {
            fsiText.text = "ℹ️ To'liq ekran ruxsati faqat Android 14+ da alohida so'raladi"
        }
    }

    private fun isListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
        ) ?: return false
        val mine = ComponentName(this, PaymentListenerService::class.java).flattenToString()
        return flat.split(":").any { it.equals(mine, ignoreCase = true) } ||
            flat.contains(packageName)
    }
}
