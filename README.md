# GMDM (Geeks MDM)

Murabaha / bo'lib to'lash telefonlarini masofadan nazorat qilish uchun Android MDM ilovasi.

## Modul A — `core` (tayyor)

- Device Admin (`CustomDeviceAdminReceiver`)
- Device Owner siyosatlari (`DevicePolicyController`)
- Shifrlangan holat (`MdmStateStore`)
- Layer 1 qulflash (`MdmLockCoordinator` + `lockNow`)
- Bo'sh API tayyorligi (`ApiClient` — URL bo'lmaguncha ishlamaydi)

## Modul B — `services` (tayyor)

- `MdmForegroundService` — doimiy foreground xizmat (`START_STICKY`)
- `MdmNotificationFactory` — yopilmaydigan notification
- `MdmServiceLauncher` — xavfsiz ishga tushirish
- `BatteryOptimizationHelper` — Xiaomi/Samsung batareya va autostart sozlamalari
- Ilova ochilganda va `onTaskRemoved` da xizmat qayta tiklanadi

## Sozlash

`gradle.properties`:

```properties
APPLICATION_ID=com.geeks.mdm
API_BASE_URL=
```

API ulash:

```properties
API_BASE_URL=https://your-api.example.com/
```

## Build

Android Studio orqali oching yoki:

```bash
./gradlew assembleDebug
```

## Device Owner (test)

Fabrika resetdan keyin ADB (USB debugging yoqilgan):

```bash
adb shell dpm set-device-owner com.geeks.mdm/.core.CustomDeviceAdminReceiver
```

> `APPLICATION_ID` o'zgarganda buyruqdagi paket nomini ham yangilang.

## Xiaomi / Samsung sozlash

Ilova ochilganda **Batareya / Autostart sozlamalari** tugmasini bosing:

1. Batareya optimizatsiyasidan chiqarish (cheklanmagan)
2. Autostart — yoqilgan
3. Fon faoliyat — cheklanmagan

## Modul C — `receivers` (tayyor)

- `BootReceiver` — rebootdan keyin xizmat va himoya tiklash
- `SimChangeReceiver` — SIM almashtirilsa avtomatik qulflash
- `AdminProtectionReceiver` — `USER_PRESENT`, paket yangilanganda tiklash
- `AdminProtectionHandler` — Device Admin o'chirilsa tamper + qulflash

## Modul D — `workers` (tayyor)

- `HeartbeatWorker` — har **15 daqiqa**: xizmat tirikmi, offline qulf, API heartbeat
- `SyncWorker` — serverdan lock/unlock sinxronlash (API yoqilganda)
- `AntiTamperWorker` — xavfsizlik skaneri + darhol qulflash
- `OfflineLockEvaluator` — 24 soat sync yo'q → avtonom qulflash (debug: 5 daqiqa)
- `utils/SecurityVerifier` — Root, ADB, Mock GPS, Frida/Xposed

## Modul E — `ui` (tayyor)

- **Layer 1**: `DevicePolicyManager.lockNow()` (`MdmLockCoordinator`)
- **Layer 2**: `LockScreenActivity` — fullscreen Kiosk + `startLockTask` (Device Owner)
- **Layer 3**: `OverlayLockManager` — `TYPE_APPLICATION_OVERLAY`
- **Layer 4**: `MdmAccessibilityService` — Settings bloklash

### Qulflashdan oldin (tavsiya)

1. Device Admin yoqing
2. **Overlay ruxsatini bering**
3. **Accessibility** da GMDM xizmatini yoqing
4. Device Owner: `adb shell dpm set-device-owner com.geeks.mdm/.core.CustomDeviceAdminReceiver`

### Debug ochish

Qulf ekranida sarlavhaga **7 marta** bosing (faqat debug APK).

## Loyiha modullari — barchasi tayyor

`core` → `services` → `receivers` → `workers` → `ui`
