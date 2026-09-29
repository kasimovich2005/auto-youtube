# Auto Browser 1.0.0

`uz.auto.browser` — Samsung Galaxy A70 (Android 11, One UI 3.1) uchun Kotlin’da yozilgan browser, Android Auto (Car App Library) bilan integratsiyalangan.

> **Eng muhim xulosa (rasmiy hujjatlar asosida):** Android Auto telefon orqali proyeksiya rejimida **browser yoki video ilovani avtomobil ekranida ko‘rsatishga ruxsat bermaydi**. Shuning uchun Auto Browser avtomobil ekranida faqat rasmiy shablon ekranlarini (Home ro‘yxati, Diagnostics, “mavjud emas” xabari) ko‘rsatadi, YouTube esa **telefonning o‘zida** to‘liq ishlaydi. Batafsil: [Platform limitation diagnostics](#platform-limitation-diagnostics).

---

## Mundarija

1. [Platform compatibility](#platform-compatibility)
2. [Build status va Network access talablari](#build-status-va-network-access-talablari)
3. [Keyingi bosqich: Variant A va Variant B](#keyingi-bosqich-variant-a-va-variant-b)
4. [Loyiha tuzilmasi](#loyiha-tuzilmasi)
5. [How to build](#how-to-build)
6. [How to install](#how-to-install)
7. [How to enable Android Auto developer settings](#how-to-enable-android-auto-developer-settings)
8. [How to enable Unknown Sources](#how-to-enable-unknown-sources)
9. [How to connect Galaxy A70 to Chevrolet Tracker 2](#how-to-connect-galaxy-a70-to-chevrolet-tracker-2)
10. [How to open Auto Browser](#how-to-open-auto-browser)
11. [How to open YouTube](#how-to-open-youtube)
12. [How to diagnose if Auto Browser does not appear](#how-to-diagnose-if-auto-browser-does-not-appear)
13. [Platform limitation diagnostics](#platform-limitation-diagnostics)
14. [Android Auto testing instructions](#android-auto-testing-instructions)
15. [Known limitations](#known-limitations)
16. [Debugging instructions](#debugging-instructions)

---

## Platform compatibility

Qisqa javob: **oddiy APK + WebView orqali YouTube’ni Android Auto (phone projection) ekraniga chiqarish rasmiy API bilan mumkin emas.**

| Kategoriya | Android Auto (phone projection) | Android Automotive OS (Google built-in) |
|---|---|---|
| **Browser** (`android.intent.category.APP_BROWSER`) | **Qo‘llab-quvvatlanmaydi** | Qo‘llab-quvvatlanadi (beta) |
| **Video** (`android:appCategory="video"`) | **Qo‘llab-quvvatlanmaydi** (faqat early-access beta e’lon qilingan) | Qo‘llab-quvvatlanadi |
| Games (parked) | Faqat Android 15+ telefonda | Qo‘llab-quvvatlanadi |
| Car App Library shablonlari (Navigation, POI, IoT, Weather, Media, Messaging, Calling) | Qo‘llab-quvvatlanadi, lekin faqat shablon UI: WebView/video yo‘q | Qo‘llab-quvvatlanadi |

- Android Auto phone projection’da **Browser category qo‘llab-quvvatlanmaydi**.
- Android Auto phone projection’da **Video category qo‘llab-quvvatlanmaydi**.
- **Browser va Video kategoriyalari Android Automotive OS uchun** (mashinaning o‘zida ishlaydigan Android). Chevrolet Tracker 2 esa Android Auto proyeksiyasini ishlatadi, AAOS emas.
- Android Auto’dagi yagona parked kategoriya (Games) ham Android 15+ talab qiladi; Galaxy A70 Android 11’da.
- Shu sababli Auto Browser’da Android Auto uchun browser/video metadata **yo‘q** va qo‘shilmaydi. Mashina qismi faqat rasmiy Car App Library shablonlaridan foydalanadi.

Manbalar (2026-09-29 da tekshirilgan): [Parked apps](https://developer.android.com/training/cars/parked), [Parked apps on Android Auto](https://developer.android.com/training/cars/parked/auto), [Browsers](https://developer.android.com/training/cars/parked/browser), [Video](https://developer.android.com/training/cars/parked/video), [Car App Library categories](https://developer.android.com/training/cars/apps/library/set-up-project), [Testing / Unknown sources](https://developer.android.com/training/cars/testing).

---

## Build status va Network access talablari

**Holat (2026-09-29):** kod va loyiha strukturasi tayyor, lekin Claude’ning bulut muhitida **build bajarilmadi**:

- Local cache’da faqat Gradle 8.14.3 distributivi bor. Android Gradle Plugin, AndroidX, Car App Library va Android SDK (platform 35, build-tools) cache’da **yo‘q**.
- `./gradlew assembleDebug --offline` natijasi:
  ```text
  Plugin [id: 'com.android.application', version: '8.7.3', apply: false] was not found
  ```
- `maven.google.com` barcha artefaktlarni `301` bilan `dl.google.com/dl/android/maven2/...` ga yo‘naltiradi, `dl.google.com` esa muhit tarmoq siyosatida bloklangan (`CONNECT 403`).
- Faqat statik tekshiruv o‘tkazildi: barcha XML resurslar va Manifest to‘g‘ri tuzilgan (parse xatosiz). Kotlin kompilyatsiyasi hali tasdiqlanmagan.

**Build uchun Project Network Access’da quyidagi domenlarga ruxsat kerak:**

| Domen | Nima uchun |
|---|---|
| `dl.google.com` | Android SDK (platform 35, build-tools), Google Maven artefaktlari |
| `maven.google.com` | Android Gradle Plugin, AndroidX, Material, Car App Library (`dl.google.com` ga redirect qiladi) |

Qo‘shimcha (hozir ochiq): `services.gradle.org` (Gradle wrapper), `repo.maven.apache.org` / `plugins.gradle.org` (Kotlin plugin va boshqa kutubxonalar).

O‘z kompyuteringizdagi Android Studio’da bu cheklov yo‘q: loyihani ochib, `./gradlew assembleDebug assembleRelease` ishlatsangiz bo‘ldi.

---

## Keyingi bosqich: Variant A va Variant B

### Variant A — Official Android Auto compatible application

Faqat ruxsat berilgan kategoriyalar, faqat rasmiy API. Joriy kod shu yo‘nalishda.

- **Hozirgi holat:** telefonda to‘liq browser (WebView, YouTube mobile web); Android Auto’da Car App Library shablon ilovasi (Home ro‘yxati, Diagnostics, “not available” xabari).
- **Kategoriya tanlovi:** browser/video Car App Library kategoriyasi emas. Hozirgi `IOT` faqat test/diagnostika uchun; Play’ga chiqarish uchun ilovaning mashinadagi vazifasi ruxsat berilgan kategoriyadan biriga haqiqatan mos kelishi kerak (masalan POI: yaqin atrofdagi joylar ro‘yxati; Weather; IoT: haqiqiy qurilma boshqaruvi). Audio (Media) kerak emas deb belgilangan.
- **Real avtomobilda ko‘rinishi:** Unknown sources Car App Library ilovalariga ta’sir qilmaydi, shuning uchun Google Play Console → **Internal testing** yoki **Internal App Sharing** orqali o‘rnatish kerak.
- **Kelajakdagi rasmiy yo‘l:** agar Google Android Auto uchun Video yoki Browsers kategoriyasini umumiy ochsa **va** telefon Android 15+ bo‘lsa, `MainActivity` ga `android.intent.category.CAR_LAUNCHER` va tegishli `appCategory` qo‘shish mumkin bo‘ladi. Galaxy A70 (Android 11) bunga mos kelmaydi; yangi telefon kerak bo‘ladi.
- **Boshqa rasmiy yo‘l:** Android Automotive OS (Google built-in) mashinalar uchun Browser kategoriyasi (`APP_BROWSER`) mavjud, lekin bu Tracker 2 ga taalluqli emas.

### Variant B — Phone screen mirroring / video solution (Android Auto API emas)

Bu **Android Auto API emas** va Auto Browser kodiga kirmaydi. Alohida texnik yo‘nalish sifatida qoldiriladi.

- **Nima:** telefon ekrani yoki videoni mashina ekraniga Android Auto’dan tashqari yo‘l bilan chiqarish.
- **Mumkin bo‘lgan yo‘nalishlar (tekshirilishi kerak):**
  - Mashina multimedia tizimining o‘z imkoniyatlari (HDMI, USB video, Miracast/screen mirroring). Tracker 2 head unit’i bularni qo‘llaydimi — ishlab chiqaruvchi hujjatidan tekshirish kerak.
  - Alohida apparat yechim: o‘zi Android’da ishlaydigan “Android Auto/CarPlay AI box” qurilmalari yoki aftermarket head unit. Ular telefon o‘rniga ishlaydi; bu Android Auto API emas va ishlab chiqaruvchi/huquqiy shartlari alohida tekshiriladi.
- **Qilinmaydigan narsalar:** Android Auto cheklovlarini chetlab o‘tuvchi mirroring ilovalari (root, yashirin API, Android Auto’ni patch qilish, xarita Surface’ini suiiste’mol qilish). Ular spec’ning 2-bo‘limiga zid va implement qilinmaydi.
- **Xavfsizlik talabi o‘zgarmaydi:** har qanday Variant B yechimida ham video faqat mashina to‘xtab turganda ko‘rsatilishi kerak.

---

## Loyiha tuzilmasi

```text
auto-browser/
├── settings.gradle.kts, build.gradle.kts, gradle.properties
├── gradlew, gradlew.bat, gradle/wrapper/        (Gradle 8.14.3)
└── app/
    ├── build.gradle.kts                          (AGP 8.7.3, Kotlin 2.0.21)
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/uz/auto/browser/
        │   ├── AutoBrowserApp.kt       Application, dark mode
        │   ├── MainActivity.kt         Telefon browser: WebView, toolbar, Home, fullscreen, xatolar
        │   ├── SettingsActivity.kt     Settings + Clear browsing data
        │   ├── DiagnosticsActivity.kt  About / Diagnostics ekrani
        │   ├── Diagnostics.kt          Diagnostika ma’lumotlari (faqat public API)
        │   ├── Prefs.kt                Sozlamalar, oxirgi sahifa, Recently visited
        │   ├── UrlUtils.kt             URL validatsiya (faqat http/https)
        │   ├── AbLog.kt                Logcat (faqat debug build’da)
        │   └── car/
        │       ├── AutoBrowserCarAppService.kt   Android Auto kirish nuqtasi
        │       ├── AutoBrowserSession.kt
        │       ├── CarHomeScreen.kt              Avtomobil ekranidagi Home (ListTemplate)
        │       ├── CarUnavailableScreen.kt       “not available” xabari (MessageTemplate)
        │       └── CarDiagnosticsScreen.kt       Avtomobil ekranidagi diagnostika
        └── res/  (layout, values, values-night, xml, drawable, mipmap-anydpi-v26)
```

| Parametr | Qiymat |
|---|---|
| Application name | Auto Browser |
| Package / applicationId | `uz.auto.browser` |
| Version | 1.0.0 (versionCode 1) |
| minSdk | 26 (Android 8.0) |
| targetSdk / compileSdk | 35 |
| Permissions | `INTERNET`, `ACCESS_NETWORK_STATE` (ikkalasi ham “normal”, runtime so‘ralmaydi) |
| Android Auto | `androidx.car.app:app:1.4.0`, `CarAppService`, kategoriya `androidx.car.app.category.IOT` |

---

## How to build

Talablar: JDK 17 yoki yangiroq, Android SDK (Platform 35, Build-Tools 35), internet (Gradle birinchi marta kutubxonalarni yuklaydi).

1. Android Studio’da **File → Open** → `auto-browser` papkasini tanlang. Studio `local.properties`’ni o‘zi yaratadi.
   Terminaldan build qilsangiz, `local.properties` yarating:
   ```properties
   sdk.dir=/path/to/Android/Sdk
   ```
2. Build:
   ```bash
   ./gradlew assembleDebug
   ./gradlew assembleRelease
   ```
3. Natija:
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   app/build/outputs/apk/release/app-release.apk
   ```

**Release imzosi.** `keystore.properties` bo‘lmasa, release APK debug kalit bilan imzolanadi (sideload test uchun yetarli). Google Play uchun o‘z kalitingizni yarating va loyiha ildiziga `keystore.properties` qo‘ying:

```properties
storeFile=release.jks
storePassword=...
keyAlias=autobrowser
keyPassword=...
```

```bash
keytool -genkeypair -v -keystore release.jks -alias autobrowser -keyalg RSA -keysize 2048 -validity 10000
```

---

## How to install

1. Telefonda: **Settings → About phone → Software information → Build number** ni 7 marta bosing → Developer options yoqiladi.
2. **Settings → Developer options → USB debugging** ni yoqing.
3. Telefonni kompyuterga ulang va “Allow USB debugging” ga rozilik bering.
4. O‘rnating:
   ```bash
   adb install -r app-debug.apk
   ```
   ADB’siz: APK’ni telefonga ko‘chiring, fayl menejerida oching va “Install unknown apps” ruxsatini bering (bu Android’ning “unknown sources” sozlamasi, Android Auto’niki emas).

Tekshirish: `adb shell pm list packages | grep uz.auto.browser`

---

## How to enable Android Auto developer settings

Android 11 (Galaxy A70) da:

1. **Settings → Apps → Android Auto → Advanced → Additional settings in the app** (yoki Android Auto ilovasini oching).
2. Pastdagi **About** bo‘limida **Version** qatorini **10 marta** bosing.
3. “Allow development settings?” oynasida **OK** ni bosing.
4. Endi yuqori o‘ng burchakdagi **⋮** menyusida **Developer settings** paydo bo‘ladi.

Manba: https://developer.android.com/training/cars/testing

---

## How to enable Unknown Sources

1. Android Auto → **⋮ → Developer settings**.
2. **Unknown sources** ni yoqing.
3. Telefonni avtomobildan uzib, qayta ulang.

> **Muhim:** rasmiy hujjat bo‘yicha Android Auto’ning *Unknown sources* sozlamasi **media, messaging notification va parked app**’larga ta’sir qiladi, **lekin Android for Cars App Library bilan yozilgan ilovalarga ta’sir qilmaydi**. Auto Browser’ning avtomobil qismi Car App Library ilovasi. Ya’ni sideload qilingan APK real avtomobil ekranida paydo bo‘lishi kafolatlanmaydi. Real avtomobilda sinash uchun rasmiy yo‘l — ilovani Google Play **Internal testing** treki yoki **Internal App Sharing** orqali o‘rnatish. Desktop Head Unit (DHU) da esa sideload qilingan debug APK ko‘rinadi.
>
> Manba: https://developer.android.com/training/cars/testing#allow-unknown-sources

---

## How to connect Galaxy A70 to Chevrolet Tracker 2

1. Avtomobilni yoqing, multimedia ekranida Android Auto (Phone projection) yoqilganiga ishonch hosil qiling.
2. Telefonda Android Auto ilovasi o‘rnatilgan va yangilangan bo‘lsin (sizda 17.6.663484).
3. Sifatli, ma’lumot uzatadigan USB kabel bilan telefonni avtomobilning Android Auto/CarPlay belgili USB portiga ulang.
4. Telefondagi so‘rovlarga rozilik bering. Ekranda Android Auto launcher ochiladi.

---

## How to open Auto Browser

**Telefonda:** ilovalar ro‘yxatidan **Auto Browser** ni oching.

**Avtomobil ekranida:** Android Auto launcher’da **Auto Browser** ikonkasini bosing. Ochiladigan shablon ekran:

```text
Auto Browser
  YouTube          https://www.youtube.com
  Google           https://www.google.com
  Enter website
  (oxirgi saytlar)
  Diagnostics
```

Istalgan saytni tanlasangiz, quyidagi xabar chiqadi (sababi pastda):

```text
This feature is not available in the current Android Auto environment.
```

---

## How to open YouTube

**Telefonda:** Home ekranidagi **YouTube** tugmasi yoki URL maydoniga `youtube.com` yozing. Default holatda (“Open YouTube on startup” = ON) ilova ochilganda YouTube avtomatik ochiladi.

YouTube ichida: qidiruv, thumbnail, video ochish, play/pause, seek, volume va fullscreen (video ichidagi fullscreen tugmasi → landscape) oddiy YouTube mobile web orqali ishlaydi.

**Android Auto ulangan paytda** telefon parked/driving holatini bila olmaydi (sabab pastda), shuning uchun ilova xavfsiz tarafni tanlaydi: qizil banner chiqadi, sahifadagi video/audio pauza qilinadi va fullscreen o‘chiriladi:

```text
Video playback unavailable while driving.

Please stop the vehicle to watch video.
```

**Avtomobil ekranida YouTube ochilmaydi** — bu platforma cheklovi.

---

## How to diagnose if Auto Browser does not appear

APK’ni o‘zgartirishdan oldin quyidagilarni ketma-ket tekshiring:

| # | Tekshiruv | Qanday |
|---|---|---|
| 1 | Manifest | `CarAppService` `androidx.car.app.CarAppService` action va kategoriya bilan e’lon qilingan; `com.google.android.gms.car.application` meta-data → `@xml/automotive_app_desc` (`<uses name="template"/>`). Tekshirish: `aapt dump xmltree app-debug.apk AndroidManifest.xml` |
| 2 | Android Auto category | `androidx.car.app.category.IOT`. Browser/video Car App Library kategoriyasi emas |
| 3 | App declaration | `adb shell dumpsys package uz.auto.browser` → `AutoBrowserCarAppService` ro‘yxatda bo‘lishi kerak |
| 4 | Supported API | `androidx.car.app.minCarApiLevel` = 1; host Car API darajasi Diagnostics’da emas, Logcat’da (`AndroidAuto` tegi) ko‘rinadi |
| 5 | Developer mode | Android Auto → About → Version 10 marta |
| 6 | Unknown sources | Yoqilgan bo‘lsa ham **Car App Library ilovalariga ta’sir qilmaydi** (rasmiy). Real avtomobil uchun Play Internal testing kerak |
| 7 | Android Auto version | Telefondagi About / Diagnostics → “Android Auto: installed, 17.6…” |
| 8 | Package visibility | Manifest’dagi `<queries>` Android Auto paketini ko‘rish imkonini beradi; Diagnostics’da “not installed” chiqsa, AA o‘rnatilmagan |
| 9 | Installation state | `adb shell pm list packages -i uz.auto.browser` → `installer=` qiymati. Sideload uchun `null`/`com.android.shell`; Play’dan o‘rnatilganda `com.android.vending` |

Tez sinov uchun kompyuterdagi **Desktop Head Unit**: Android Auto → Developer settings → **Start head unit server**, keyin:

```bash
adb forward tcp:5277 tcp:5277
$ANDROID_SDK/extras/google/auto/desktop-head-unit
```

DHU’da Auto Browser ko‘rinsa, manifest va kod to‘g‘ri; real avtomobilda ko‘rinmasa, sabab — o‘rnatish manbasi (Unknown sources Car App Library’ga ta’sir qilmaydi).

---

## Platform limitation diagnostics

Quyidagilar developer.android.com’dagi rasmiy hujjatlardan olingan (2026-09-29 holatiga):

| Savol | Javob | Manba |
|---|---|---|
| Android Auto 17.6 bu app category’ni (browser) qo‘llaydimi? | **Yo‘q.** Parked kategoriyalar: Video — faqat Android Automotive OS; Games — Android Auto va AAOS; Browsers — faqat Android Automotive OS | https://developer.android.com/training/cars/parked |
| Android Auto qaysi parked kategoriyani qo‘llaydi? | “Games are the only parked app category supported by Android Auto at this time.” | https://developer.android.com/training/cars/parked/auto |
| Parked app’lar uchun telefon talabi | “On devices running Android 15 or higher, Android Auto supports running apps in supported parked app categories” — Galaxy A70 Android 11 da qolgan, demak parked rejim umuman mavjud emas | https://developer.android.com/training/cars/parked/auto, https://developer.android.com/training/cars/parked/games |
| Video kategoriyasi Android Auto’da? | “The video category is coming to Android Auto in beta” — faqat early access partnerlar uchun, `android:appCategory="video"` hozircha AAOS’da ishlaydi | https://developer.android.com/training/cars/parked/video |
| Browser `APP_BROWSER` intent kategoriyasi | Faqat Android Automotive OS uchun (beta, internal testing) | https://developer.android.com/training/cars/parked/browser |
| Car App Library qanday kategoriyalarni beradi? | NAVIGATION, POI, IOT, WEATHER, MEDIA, MESSAGING, CALLING. Browser yoki video yo‘q; UI faqat shablonlar | https://developer.android.com/training/cars/apps/library/set-up-project |
| Erkin chizish uchun Surface? | Faqat Navigation/POI/Weather’dagi xarita shablonlarida (`MapWithContentTemplate`, `NavigationTemplate`) va faqat xarita uchun. Bu yerga WebView/YouTube chizish kategoriya sifat qoidalarini buzadi — **implement qilinmadi** | https://developer.android.com/training/cars/apps/library/draw-maps |
| Unknown sources | Car App Library ilovalariga ta’sir qilmaydi | https://developer.android.com/training/cars/testing |

Kutilgan yakuniy natija (kod va hujjatlar bo‘yicha; real qurilmada sinalishi kerak):

```text
PHONE APP:               PASS (kutilgan; A70’da sinash kerak)
ANDROID AUTO DISCOVERY:  DHU’da PASS kutiladi; real avtomobilda sideload bilan FAIL bo‘lishi mumkin
                         (Unknown sources Car App Library’ga ta’sir qilmaydi) → Play Internal testing orqali PASS
ANDROID AUTO LAUNCHER:   yuqoridagi kabi
BROWSER (car screen):    FAIL — platforma cheklovi: Browsers kategoriyasi faqat Android Automotive OS
YOUTUBE (car screen):    FAIL — platforma cheklovi: WebView renderingga ruxsat beruvchi AA kategoriyasi yo‘q
VIDEO (car screen):      FAIL — platforma cheklovi: Video kategoriyasi faqat AAOS (AA uchun early-access beta),
                         parked app’lar esa Android 15+ talab qiladi (A70 = Android 11)
BROWSER/YOUTUBE/VIDEO (phone): PASS (kutilgan)
```

Root, exploit, yashirin API, xarita Surface’ini suiiste’mol qilish, AA’ni “patch” qiluvchi uchinchi tomon ilovalar — hech biri ishlatilmagan va tavsiya etilmaydi.

**Rasmiy yo‘l bilan kelajakda nima o‘zgarishi mumkin:** agar Google Android Auto uchun Video/Browsers kategoriyasini ochsa va telefon Android 15+ bo‘lsa, `MainActivity`’ga `android.intent.category.CAR_LAUNCHER` va `android:appCategory="video"` qo‘shish mumkin bo‘ladi. Hozir buni qo‘shish Play review’da rad etiladi va A70’da baribir ishlamaydi.

---

## Android Auto testing instructions

**Test 1 — telefon (Galaxy A70):**

```bash
adb install -r app-debug.apk
adb logcat -s AutoBrowser AndroidAuto WebView YouTube Navigation
```

1. App ochiladi → YouTube ochiladi (default)
2. YouTube’da qidiruv ishlaydi
3. Video ochiladi, play/pause/seek ishlaydi
4. Video fullscreen → landscape
5. Toolbar ← Back: web history bo‘lsa orqaga, bo‘lmasa Home
6. ⟳ Refresh qayta yuklaydi
7. ⋮ → About / Diagnostics — qiymatlarni tekshiring

**Test 2 — Desktop Head Unit (tavsiya etiladi, avtomobilgacha):** yuqoridagi DHU bo‘limi. Kutiladi: launcher’da Auto Browser, Home ro‘yxati, “not available” xabari, Diagnostics.

**Test 3 — Chevrolet Tracker 2 (USB):**

1. Android Auto ishga tushadi
2. Launcher’da Auto Browser bormi? (yo‘q bo‘lsa — “How to diagnose” jadvali; eng ehtimoliy sabab: o‘rnatish manbasi)
3. Auto Browser ochiladi → Home ro‘yxati, katta qatorlar, touch ishlaydi
4. YouTube bosilganda “This feature is not available in the current Android Auto environment.”
5. Telefonda: qizil “Video playback unavailable while driving” banneri, video pauza bo‘ladi, fullscreen ochilmaydi
6. Natijani “Platform limitation diagnostics” formatida yozing

---

## Known limitations

1. **Avtomobil ekranida web sahifa va video ko‘rsatilmaydi** — Android Auto (proyeksiya) browser/video kategoriyasini qo‘llamaydi; parked app’lar Android 15+ talab qiladi. Bu APK xatosi emas.
2. **Kategoriya IOT** — Car App Library xizmat kategoriyasisiz bog‘lanmaydi, browser kategoriyasi esa yo‘q. IOT — rasmiy ro‘yxatdagi eng neytral variant, lekin ilova IoT qurilmalarini boshqarmaydi, shuning uchun **bu holatda Google Play review’dan o‘tmaydi**. U faqat DHU/internal test va diagnostika uchun.
3. **Sideload + real avtomobil** — Unknown sources Car App Library ilovalariga ta’sir qilmaydi.
4. **Parked holatni telefon bilmaydi** — Android Auto proyeksiya rejimida telefon ilovalariga harakat/parked holatini beradigan public API yo‘q (Car App Library’ning `CarHardwareManager` tezlik ma’lumoti faqat ruxsat va host qo‘llasa, va faqat car session ichida). Shu sababli AA ulanganida telefondagi video bloklanadi.
5. **Video bloklash JavaScript orqali** — agar Settings’da JavaScript o‘chirilgan bo‘lsa yoki video cross-origin iframe ichida bo‘lsa, pauza qilish ishlamasligi mumkin; fullscreen baribir bloklanadi.
6. **One UI versiyasi** public API’da yo‘q — Android versiyasidan taxmin qilinadi (Android 11 → 3.x).
7. **YouTube** oddiy mobile web sifatida ochiladi. User agent’dan `; wv` belgisi olib tashlangan; YouTube o‘z siyosatini o‘zgartirsa, sahifa boshqacha ishlashi mumkin. Settings → Desktop user agent bilan desktop versiya.
8. Downloads v1’da yo‘q (“Downloads are not supported in Auto Browser v1.”). Popup’lar va boshqa ilovalarni ochish bloklangan (`intent://` faqat `browser_fallback_url` web manzili bo‘lsa ochiladi).
9. `http://` saytlar ruxsat etilgan (spec bo‘yicha), shuning uchun `usesCleartextTraffic="true"`. HTTPS sahifalardagi mixed content bloklangan.

---

## Debugging instructions

Logcat (faqat debug build; release’da hech narsa log qilinmaydi):

```bash
adb logcat -s AutoBrowser AndroidAuto WebView YouTube Navigation
```

Namuna:

```text
AutoBrowser: App started, version 1.0.0
WebView: WebView initialized: com.google.android.webview 1xx.x
YouTube: Loading https://www.youtube.com
YouTube: Page loaded
AndroidAuto: Car connection: connected (Android Auto projection)
AutoBrowser: Android Auto connection detected
AndroidAuto: Android Auto connection detected: car session created
YouTube: Video playback requested: fullscreen
```

Foydali buyruqlar:

```bash
adb shell dumpsys package uz.auto.browser | grep -A3 CarAppService   # xizmat e’lon qilinganmi
adb shell dumpsys package com.google.android.projection.gearhead | grep versionName
adb shell dumpsys webviewupdate                                      # WebView provayderi
chrome://inspect   # kompyuterdagi Chrome’da: debug build WebView’ini tekshirish
```

YouTube ochilmasa: (1) Logcat’dagi `YouTube:`/`WebView:` xato kodi; (2) `dumpsys webviewupdate` — WebView yangimi; (3) Settings → Desktop user agent bilan sinab ko‘ring; (4) Diagnostics → Internet.
