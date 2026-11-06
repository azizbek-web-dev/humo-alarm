package uz.humo.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Telefon qayta yonganda kuzatuvni avtomatik davom ettiradi (bir marta yoqilgan bo'lsa) */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Prefs.startedOnce(context)) {
            TelegramWatchService.start(context)
        }
    }
}
