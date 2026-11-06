package uz.humo.alarm

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

/** Login + sozlamalar ekrani */
class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var accountText: TextView
    private lateinit var phoneInput: EditText
    private lateinit var codeInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var sendCodeButton: Button
    private lateinit var confirmCodeButton: Button
    private lateinit var confirmPasswordButton: Button
    private lateinit var suffixInput: EditText
    private lateinit var fsiText: TextView

    private val tgListener: () -> Unit = { refreshUi() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        accountText = findViewById(R.id.accountText)
        phoneInput = findViewById(R.id.phoneInput)
        codeInput = findViewById(R.id.codeInput)
        passwordInput = findViewById(R.id.passwordInput)
        sendCodeButton = findViewById(R.id.sendCodeButton)
        confirmCodeButton = findViewById(R.id.confirmCodeButton)
        confirmPasswordButton = findViewById(R.id.confirmPasswordButton)
        suffixInput = findViewById(R.id.suffixInput)
        fsiText = findViewById(R.id.fsiText)

        phoneInput.setText(Prefs.phone(this))
        suffixInput.setText(Prefs.suffix(this))

        sendCodeButton.setOnClickListener {
            val phone = phoneInput.text.toString().trim()
            if (phone.length < 9) {
                Toast.makeText(this, "Raqamni to'liq kiriting", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Prefs.savePhone(this, phone)
            Prefs.setStartedOnce(this, true)
            TelegramWatchService.start(this)
            Tg.start(this)
            Tg.setPhone(phone)
        }

        confirmCodeButton.setOnClickListener {
            val code = codeInput.text.toString().trim()
            if (code.isEmpty()) {
                Toast.makeText(this, "Kodni kiriting", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Tg.setCode(code)
        }

        confirmPasswordButton.setOnClickListener {
            val pw = passwordInput.text.toString()
            if (pw.isEmpty()) {
                Toast.makeText(this, "Parolni kiriting", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Tg.setPassword(pw)
        }

        findViewById<Button>(R.id.startWatchButton).setOnClickListener {
            Prefs.setStartedOnce(this, true)
            TelegramWatchService.start(this)
            Tg.start(this)
            Toast.makeText(this, "Kuzatuv boshlandi", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            Tg.logOut()
            Toast.makeText(this, "Telegram sessiyasi yopilmoqda…", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.saveSuffixButton).setOnClickListener {
            Prefs.saveSuffix(this, suffixInput.text.toString())
            Toast.makeText(this, "Saqlandi ✅", Toast.LENGTH_SHORT).show()
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
            val sfx = suffixInput.text.toString().trim().ifEmpty { Prefs.DEFAULT_SUFFIX }
            AlarmStarter.fire(
                this,
                "🎉 To'ldirish | ➕ 3.400.000,00 UZS | 📍 BLINKO>Toshkent | 💳 HUMOCARD *$sfx | 🕓 00:10 03.11.2026 | 💰 3.400.000,00 UZS"
            )
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    override fun onStart() {
        super.onStart()
        Tg.addListener(tgListener)
        refreshUi()
    }

    override fun onStop() {
        Tg.removeListener(tgListener)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        statusText.text = Tg.statusText
        val acc = Tg.accountLabel
        accountText.text = if (acc != null) "Akkaunt: $acc" else ""
        accountText.visibility = if (acc != null) View.VISIBLE else View.GONE

        val state = Tg.authState
        codeInput.visibility = if (state == Tg.AuthState.WAIT_CODE) View.VISIBLE else View.GONE
        confirmCodeButton.visibility = codeInput.visibility
        passwordInput.visibility = if (state == Tg.AuthState.WAIT_PASSWORD) View.VISIBLE else View.GONE
        confirmPasswordButton.visibility = passwordInput.visibility

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
                "⚠️ To'liq ekran ruxsati (Android 14+): berilmagan — budilnik ekranni o'zi ochishi uchun pastdagi tugmadan yoqing"
            }
        } else {
            fsiText.text = "ℹ️ To'liq ekran ruxsati faqat Android 14+ da alohida so'raladi"
        }
    }
}
