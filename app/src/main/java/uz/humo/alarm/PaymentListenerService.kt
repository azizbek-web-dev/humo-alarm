package uz.humo.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Telegram (org.telegram.messenger) bildirishnomalarini kuzatadi.
 * Karta oxiri + tushum belgisi topilsa budilnikni ishga tushiradi.
 */
class PaymentListenerService : NotificationListenerService() {

    private val processed = HashMap<String, Long>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (sbn.packageName != TELEGRAM_PKG) return

        // Bir xil bildirishnoma yangilanishlarida qayta chalib yubormaslik uchun
        val stamp = sbn.key + "|" + sbn.postTime
        val now = System.currentTimeMillis()
        synchronized(processed) {
            if (processed.containsKey(stamp)) return
            processed[stamp] = now
            // Eski yozuvlarni tozalab turamiz
            val it = processed.entries.iterator()
            while (it.hasNext()) {
                if (now - it.next().value > 10 * 60 * 1000L) it.remove()
            }
        }

        val text = extractText(sbn.notification) ?: return
        if (Matcher.isIncome(this, text)) {
            AlarmStarter.fire(this, text)
        }
    }

    private fun extractText(n: Notification): String? {
        val ex = n.extras
        val parts = ArrayList<String>()
        ex.getCharSequence(Notification.EXTRA_TITLE)?.let { parts.add(it.toString()) }
        ex.getCharSequence(Notification.EXTRA_TEXT)?.let { parts.add(it.toString()) }
        ex.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let { parts.add(it.toString()) }
        ex.getCharSequence(Notification.EXTRA_SUB_TEXT)?.let { parts.add(it.toString()) }
        ex.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach { parts.add(it.toString()) }
        val joined = parts.joinToString("\n").trim()
        return joined.ifEmpty { null }
    }

    companion object {
        const val TELEGRAM_PKG = "org.telegram.messenger"
    }
}

/** Tushumni aniqlash mantig'i */
object Matcher {
    // "+5 000 000", "+500000", "+ 1 200.50" kabi yozuvlar
    private val PLUS_AMOUNT = Regex("""\+\s?\d[\d\s]*([.,]\d{1,2})?""")

    fun isIncome(ctx: Context, text: String): Boolean {
        val suffix = Prefs.suffix(ctx)
        if (suffix.isEmpty() || !text.contains(suffix)) return false

        val incomeHit = Prefs.incomeWords(ctx).any { text.contains(it, ignoreCase = true) }
        if (incomeHit) return true

        // Kalit so'z bo'lmasa: "+" bilan kelgan summa tushum belgisi,
        // lekin chiqim so'zi bo'lsa chalınmaydi
        val expenseHit = Prefs.expenseWords(ctx).any { text.contains(it, ignoreCase = true) }
        if (expenseHit) return false
        return PLUS_AMOUNT.containsMatchIn(text)
    }
}

/** Budilnikni ishga tushirish: to'liq ekranli bildirishnoma + Activity'ni ochishga urinish */
object AlarmStarter {
    private const val CHANNEL_ID = "humo_alarm_channel"
    private const val NOTIF_ID = 5131

    fun fire(ctx: Context, text: String) {
        val intent = Intent(ctx, AlarmActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(AlarmActivity.EXTRA_TEXT, text)
        }
        val pi = PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "HUMO budilnik", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Karta tushumida chalinadigan budilnik"
                setBypassDnd(true)
            }
            nm.createNotificationChannel(ch)
        }

        val notif = Notification.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("💰 KARTA TUSHUMI!")
            .setContentText(text.take(120))
            .setStyle(Notification.BigTextStyle().bigText(text.take(500)))
            .setPriority(Notification.PRIORITY_MAX)
            .setCategory(Notification.CATEGORY_ALARM)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()
        try {
            nm.notify(NOTIF_ID, notif)
        } catch (e: Exception) {
            // POST_NOTIFICATIONS berilmagan bo'lsa ham pastdagi startActivity urinishi qoladi
        }

        // Ko'p qurilmalarda listener xizmati Activity'ni to'g'ridan-to'g'ri ham ocha oladi
        try {
            ctx.startActivity(intent)
        } catch (e: Exception) {
            // Orqa fondan ochish cheklangan bo'lsa full-screen bildirishnoma o'zi ochadi
        }
    }

    fun cancelNotification(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_ID)
    }
}
