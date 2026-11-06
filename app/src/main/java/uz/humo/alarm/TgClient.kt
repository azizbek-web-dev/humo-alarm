package uz.humo.alarm

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * TDLib (MTProto) klienti - ilova Telegramga bevosita ulanadi.
 * @HUMOcardbot chatidagi yangi xabarlarni real vaqtda olib, filtrdan o'tkazadi.
 */
object Tg {

    enum class AuthState { NOT_STARTED, WAIT_PARAMETERS, WAIT_PHONE, WAIT_CODE, WAIT_PASSWORD, READY, CLOSED, ERROR }

    const val BOT_USERNAME = "HUMOcardbot"

    @Volatile
    var authState: AuthState = AuthState.NOT_STARTED
        private set

    @Volatile
    var statusText: String = "Telegram: ulanmagan"
        private set

    @Volatile
    var accountLabel: String? = null
        private set

    @Volatile
    var botReady: Boolean = false
        private set

    @Volatile
    private var botChatId: Long = 0L

    private var client: Client? = null
    private lateinit var appCtx: Context
    private var dbDirPath: String = ""
    private var filesDirPath: String = ""

    private val main = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private val seenMessages = HashSet<Long>()

    @Volatile
    private var pendingPhone: String? = null

    fun addListener(l: () -> Unit) = listeners.add(l)
    fun removeListener(l: () -> Unit) = listeners.remove(l)

    private fun emit() {
        main.post { listeners.forEach { it() } }
    }

    private fun setStatus(s: String) {
        statusText = s
        emit()
    }

    @Synchronized
    fun start(ctx: Context) {
        appCtx = ctx.applicationContext
        if (client != null) return
        val base = File(appCtx.filesDir, "tdlib")
        val dbDir = File(base, "db")
        val filesDir = File(base, "files")
        dbDir.mkdirs(); filesDir.mkdirs()
        dbDirPath = dbDir.absolutePath
        filesDirPath = filesDir.absolutePath
        try {
            Client.execute(TdApi.SetLogVerbosityLevel(1))
        } catch (e: Exception) {
        }
        authState = AuthState.WAIT_PARAMETERS
        setStatus("Telegram: ulanilmoqda…")
        client = Client.create({ obj -> onUpdate(obj) }, { setStatus("Telegram xato") }, null)
    }

    /** Raqamni navbatga qo'yadi: klient WAIT_PHONE holatiga kelishi bilan yuboriladi */
    fun setPhone(phone: String) {
        pendingPhone = phone
        if (authState == AuthState.WAIT_PHONE) {
            flushPhone()
        } else {
            setStatus("Raqam qabul qilindi, klient tayyor bo'lishi bilan kod yuboriladi…")
        }
    }

    private fun flushPhone() {
        val phone = pendingPhone ?: return
        pendingPhone = null
        send(TdApi.SetAuthenticationPhoneNumber(phone, TdApi.PhoneNumberAuthenticationSettings()), true)
    }

    fun setCode(code: String) {
        send(TdApi.CheckAuthenticationCode(code), true)
    }

    fun setPassword(password: String) {
        send(TdApi.CheckAuthenticationPassword(password), true)
    }

    fun logOut() {
        send(TdApi.LogOut(), true)
    }

    private fun send(query: TdApi.Function<*>, reportError: Boolean = false) {
        val c = client ?: return
        c.send(query, { obj ->
            if (reportError && obj is TdApi.Error) {
                setStatus("Telegram xato: ${obj.message}")
            }
        }, { })
    }

    private fun onUpdate(obj: TdApi.Object) {
        when (obj) {
            is TdApi.UpdateAuthorizationState -> onAuthState(obj.authorizationState)
            is TdApi.UpdateNewMessage -> onNewMessage(obj.message)
            is TdApi.Error -> setStatus("Telegram xato: ${obj.message}")
        }
    }

    private fun onAuthState(state: TdApi.AuthorizationState) {
        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                authState = AuthState.WAIT_PARAMETERS
                if (BuildConfig.TG_API_ID == 0 || BuildConfig.TG_API_HASH.isEmpty()) {
                    authState = AuthState.ERROR
                    setStatus("XATO: build'da API ID/HASH yo'q (CI secret sozlanmagan)")
                    return
                }
                send(
                    TdApi.SetTdlibParameters(
                        false, dbDirPath, filesDirPath, null,
                        true, true, true, false,
                        BuildConfig.TG_API_ID, BuildConfig.TG_API_HASH,
                        "uz", Build.MODEL ?: "Android",
                        Build.VERSION.RELEASE ?: "", "2.0"
                    )
                )
            }
            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                authState = AuthState.WAIT_PHONE
                setStatus("Telefon raqam kutilmoqda — «Kod yuborish»ni bosing")
                emit()
                flushPhone()
            }
            is TdApi.AuthorizationStateWaitCode -> {
                authState = AuthState.WAIT_CODE
                setStatus("Kod yuborildi — Telegram ilovasiga/SMS ga kelgan kodni kiriting")
            }
            is TdApi.AuthorizationStateWaitPassword -> {
                authState = AuthState.WAIT_PASSWORD
                setStatus("Ikki bosqichli (2FA) parol kutilmoqda")
            }
            is TdApi.AuthorizationStateReady -> {
                authState = AuthState.READY
                setStatus("Telegram: ulangan ✅ — bot qidirilmoqda…")
                onReady()
            }
            is TdApi.AuthorizationStateClosed -> {
                authState = AuthState.CLOSED
                client = null
                botReady = false
                accountLabel = null
                setStatus("Telegram: sessiya yopildi")
            }
            else -> {
                setStatus("Telegram holati: ${state.javaClass.simpleName}")
            }
        }
        emit()
    }

    private fun onReady() {
        val c = client ?: return
        c.send(TdApi.GetMe(), { obj ->
            if (obj is TdApi.User) {
                val name = listOf(obj.firstName, obj.lastName)
                    .filter { !it.isNullOrBlank() }.joinToString(" ")
                accountLabel = listOf(name, obj.phoneNumber)
                    .filter { !it.isNullOrBlank() }.joinToString(" • ")
                emit()
            }
        }, { })
        c.send(TdApi.LoadChats(TdApi.ChatListMain(), 30), { }, { })
        c.send(TdApi.SearchPublicChat(BOT_USERNAME), { obj ->
            if (obj is TdApi.Chat) {
                botChatId = obj.id
                botReady = true
                send(TdApi.OpenChat(obj.id))
                setStatus("Tayyor ✅ — @$BOT_USERNAME chatidagi tushumlar kuzatilmoqda")
            } else if (obj is TdApi.Error) {
                setStatus("Bot topilmadi (@$BOT_USERNAME): ${obj.message}")
            }
        }, { })
    }

    private fun onNewMessage(msg: TdApi.Message) {
        if (!botReady || msg.chatId != botChatId) return
        if (msg.isOutgoing) return
        synchronized(seenMessages) {
            if (!seenMessages.add(msg.id)) return
            if (seenMessages.size > 1000) seenMessages.clear()
        }
        val content = msg.content as? TdApi.MessageText ?: return
        val text = content.text?.text ?: return
        if (IncomeFilter.matches(text, Prefs.suffix(appCtx))) {
            setStatus("🔔 Tushum aniqlandi — budilnik chalindi")
            AlarmStarter.fire(appCtx, text)
        }
    }
}

/**
 * Aniq filtr (@HUMOcardbot formati):
 * xabarda BIR VAQTDA «To'ldirish» (yoki 🎉) + «➕» + «*<karta oxiri>» bo'lsa — tushum.
 * «To'lov» / «➖» / «💸» — hech qachon chalinmaydi.
 */
object IncomeFilter {
    private fun norm(s: String): String = s
        .replace('‘', '\'').replace('’', '\'')
        .replace('ʻ', '\'').replace('ʼ', '\'')

    fun matches(rawText: String, suffix: String): Boolean {
        val text = norm(rawText)
        val sfx = suffix.trim()
        if (sfx.isEmpty() || !text.contains("*$sfx")) return false
        // Chiqim veto: avval chiqim belgilarini tekshiramiz
        if (text.contains("➖") || text.contains("💸")) return false
        if (text.contains("to'lov", ignoreCase = true)) return false
        val fill = text.contains("To'ldirish") || text.contains("🎉")
        val plus = text.contains("➕")
        return fill && plus
    }
}
