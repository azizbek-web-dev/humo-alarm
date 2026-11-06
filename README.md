# HUMO budilnik ⏰💳

Kichik Android ilova: **Telegramga bevosita (TDLib/MTProto) ulanib**, @HUMOcardbot
chatidagi yangi xabarlarni real vaqtda kuzatadi. Karta tushumi kelishi bilan **baland
budilnik** chalinadi — foydalanuvchi TO'XTATISH tugmasini bosmaguncha to'xtamaydi.
Maqsad: pul tushganini darhol bilish, u kreditga avtomatik yechilib ketishidan oldin
ogoh bo'lish. (Bildirishnoma o'qish usuli ataylab ishlatilmadi — bildirishnoma
kechikishi mumkin, ilova esa xabarni to'g'ridan-to'g'ri Telegramdan oladi.)

## Qanday ishlaydi

1. Ilovada Telegram raqami bilan **bir marta kirish** qilinadi (raqam + kod + agar
   bo'lsa 2FA parol). Sessiya telefonda (TDLib bazasida) saqlanadi.
   Login ekranida raqam oldindan to'ldirilgan: `+998935493969` (o'zgartirish mumkin).
2. Foreground xizmat TDLib ulanishini doimiy tirik tutadi va `@HUMOcardbot` bilan
   chatni ochib, yangi xabarlarni `UpdateNewMessage` orqali oladi.
   Telefon qayta yonganda xizmat avtomatik davom etadi (BootReceiver).
3. **Filtr (botning aniq formati):** xabarda bir vaqtda
   - `To'ldirish` (yoki 🎉), **va**
   - `➕`, **va**
   - `*<karta oxiri>` (standart `*5131`, sozlamada o'zgaradi)
   bo'lsa — **budilnik**. `To'lov` / `➖` / `💸` — hech qachon chalinmaydi.
   Misol (kirim): `🎉 To'ldirish | ➕ 3.400.000,00 UZS | 📍 … | 💳 HUMOCARD *5131 | 🕓 … | 💰 …`
4. Budilnik `STREAM_ALARM` oqimida **maksimal ovozda**, loop rejimida chalinadi
   (ovozsiz / bezovta qilmaslik rejimida ham eshitiladi), vibratsiya beradi va
   qulflangan ekranda ham to'liq ekranli qizil oyna ochiladi. Katta
   **TO'XTATISH** tugmasi bosilgandagina to'xtaydi.

## O'rnatish va ruxsatlar

1. APK'ni o'rnating (Release APK debug kalit bilan imzolangan — odatiy o'rnatiladi,
   "noma'lum manba" ruxsati kerak bo'lishi mumkin).
2. Ilovani ochib, raqam → «Kod yuborish» → Telegramga kelgan kod → (2FA bo'lsa) parol.
3. Android 13+: bildirishnoma ko'rsatish ruxsati (ilova o'zi so'raydi) — foreground
   xizmat va budilnik oynasi uchun kerak.
4. Android 14+: **to'liq ekran ruxsatini** yoqing (ilovadagi tugma) — budilnik
   ekranni o'zi ochishi uchun kerak.
5. Batareya tejash optimizatsiyasidan ilovani istisno qiling («cheklovsiz») —
   tizim xizmatni uyquga yubormasligi uchun.
6. «Sinov» tugmasi bilan budilnikni tekshirib ko'ring.

## Texnik

- Native Kotlin; TDLib 1.8.56 tayyor AAR: `com.github.tdlibx:td:1.8.56` (JitPack,
  manba: [tdlibx/td](https://github.com/tdlibx/td)). APK'ga faqat ARM ABI lar
  (arm64-v8a, armeabi-v7a) kiradi.
- minSdk 26, targetSdk/compileSdk 34.
- Telegram `api_id`/`api_hash` repozitoriyda **saqlanmaydi**: build paytida GitHub
  Actions secretlaridan (`TG_API_ID`, `TG_API_HASH`) BuildConfig'ga yoziladi.
  (TDLib klientlarida api_hash baribir APK ichida bo'ladi — bu Telegram ilovalarining
  odatiy holati; lekin repodagi manba kodda u yo'q.)
- Build: GitHub Actions (`.github/workflows/build.yml`), JDK 17, Gradle wrapper.
