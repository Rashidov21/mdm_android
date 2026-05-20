# GMDM (Geeks MDM)

Murabaha / bo'lib to'lash telefonlarini masofadan nazorat qilish uchun Android MDM ilovasi.

## Modul A — `core` (tayyor)

- Device Admin (`CustomDeviceAdminReceiver`)
- Device Owner siyosatlari (`DevicePolicyController`)
- Shifrlangan holat (`MdmStateStore`)
- Layer 1 qulflash (`MdmLockCoordinator` + `lockNow`)
- Bo'sh API tayyorligi (`ApiClient` — URL bo'lmaguncha ishlamaydi)

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

## Keyingi modul

**Modul B — `services`**: Foreground Service va doimiy notification (Xiaomi/Samsung fon hayoti).
