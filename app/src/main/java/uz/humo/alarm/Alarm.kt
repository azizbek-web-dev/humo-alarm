package uz.humo.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

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
            // POST_NOTIFICATIONS berilmagan bo'lsa ham startActivity urinishi qoladi
        }

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
