# HUMO budilnik ⏰💳

Kichik Android ilova: Telegram bildirishnomasida karta tushumi (masalan, oxiri **5131**
bilan tugaydigan HUMO karta) haqida xabar kelishi bilan **baland budilnik** chalinadi —
foydalanuvchi TO'XTATISH tugmasini bosmaguncha to'xtamaydi. Maqsad: pul tushganini
darhol bilish, u kreditga avtomatik yechilib ketishidan oldin ogoh bo'lish.

## Qanday ishlaydi

1. Ilova `NotificationListenerService` orqali `org.telegram.messenger`
   bildirishnomalarini kuzatadi (internet ruxsati umuman yo'q, hammasi lokal).
2. Xabar matnida **karta oxiri** (standart: `5131`, sozlamada o'zgaradi) VA
   **tushum belgisi** bo'lsa budilnik ishga tushadi:
   - tushum kalit so'zlar: `Kirim`, `Tushum`, `Пополнение`, `Зачисление`,
     `Поступление` va boshqalar (sozlamada tahrirlanadi);
   - yoki summa oldida `+` belgisi (masalan `+5 000 000`). Chiqim kalit so'zi
     (`Chiqim`, `Списание`, ...) bo'lsa, `+` belgisi bilan ham chalinmaydi.
3. Budilnik `STREAM_ALARM` oqimida **maksimal ovozda**, loop rejimida chalinadi
   (ovozsiz / bezovta qilmaslik rejimida ham eshitiladi), vibratsiya beradi va
   qulflangan ekranda ham to'liq ekranli qizil oyna ochiladi. Katta
   **TO'XTATISH** tugmasi bosilgandagina to'xtaydi.

## O'rnatish va ruxsatlar

1. APK'ni o'rnating (Release APK debug kalit bilan imzolangan — odatiy o'rnatiladi,
   "noma'lum manba" ruxsati kerak bo'lishi mumkin).
2. Ilovani ochib, **"Bildirishnoma ruxsatini ochish"** tugmasi orqali
   «HUMO budilnik» uchun bildirishnomalarni o'qish ruxsatini yoqing — busiz
   ilova ishlamaydi.
3. Android 13+: bildirishnoma ko'rsatish ruxsatini bering (ilova o'zi so'raydi).
4. Android 14+: **to'liq ekran ruxsatini** yoqing (sozlamalar ekranidagi tugma) —
   budilnik ekranni o'zi ochishi uchun kerak.
5. "Sinov" tugmasi bilan budilnikni tekshirib ko'ring.

Qo'shimcha maslahat: batareyani tejash optimizatsiyasidan ilovani istisno qiling
(«cheklovsiz»), shunda tizim xizmatni uyquga yubormaydi.

## Texnik

- Native Kotlin, AndroidX'siz — faqat framework API, APK ~kichik.
- minSdk 26, targetSdk/compileSdk 34.
- Build: GitHub Actions (`.github/workflows/build.yml`), JDK 17, Gradle wrapper.
