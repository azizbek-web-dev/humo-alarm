package uz.humo.alarm

import android.content.Context

/** Ilova sozlamalari (SharedPreferences) */
object Prefs {
    private const val FILE = "settings"
    const val DEFAULT_SUFFIX = "5131"

    val DEFAULT_INCOME = listOf(
        "kirim", "tushum", "to'ldirildi", "to‘ldirildi",
        "пополнение", "зачисление", "поступление"
    )
    val DEFAULT_EXPENSE = listOf(
        "chiqim", "yechildi", "yechib", "xarajat", "komissiya",
        "списание", "расход", "комиссия", "оплата"
    )

    fun suffix(ctx: Context): String =
        prefs(ctx).getString("suffix", DEFAULT_SUFFIX)?.trim().ifNullOrBlank { DEFAULT_SUFFIX }

    fun incomeWords(ctx: Context): List<String> =
        prefs(ctx).getString("income_kws", null)?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: DEFAULT_INCOME

    fun expenseWords(ctx: Context): List<String> =
        prefs(ctx).getString("expense_kws", null)?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: DEFAULT_EXPENSE

    fun save(ctx: Context, suffix: String, income: String, expense: String) {
        prefs(ctx).edit()
            .putString("suffix", suffix.trim())
            .putString("income_kws", income)
            .putString("expense_kws", expense)
            .apply()
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private fun String?.ifNullOrBlank(fallback: () -> String): String =
        if (this.isNullOrBlank()) fallback() else this
}
