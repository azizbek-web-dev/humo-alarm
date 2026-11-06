package uz.humo.alarm

import android.content.Context

/** Ilova sozlamalari (SharedPreferences) */
object Prefs {
    private const val FILE = "settings"
    const val DEFAULT_SUFFIX = "5131"
    const val DEFAULT_PHONE = "+998935493969"

    fun suffix(ctx: Context): String =
        prefs(ctx).getString("suffix", DEFAULT_SUFFIX)?.trim()
            ?.takeIf { it.isNotEmpty() } ?: DEFAULT_SUFFIX

    fun saveSuffix(ctx: Context, value: String) {
        prefs(ctx).edit().putString("suffix", value.trim()).apply()
    }

    fun phone(ctx: Context): String =
        prefs(ctx).getString("phone", DEFAULT_PHONE)?.trim()
            ?.takeIf { it.isNotEmpty() } ?: DEFAULT_PHONE

    fun savePhone(ctx: Context, value: String) {
        prefs(ctx).edit().putString("phone", value.trim()).apply()
    }

    fun startedOnce(ctx: Context): Boolean = prefs(ctx).getBoolean("started_once", false)

    fun setStartedOnce(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean("started_once", value).apply()
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
