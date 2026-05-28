GMDM SOTUVCHI PACK - TEZKOR YO'RIQNOMA
======================================

Bu papka kam tajribali sotuvchi uchun tayyorlangan.

FAYLLAR:
- install.bat            -> USB orqali 1-click o'rnatish
- app-release.apk        -> o'rnatiladigan APK (nomi apk_name.txt da)
- qr-provisioning.json   -> QR payload
- generate-qr.ps1        -> QR PNG chiqarish
- app_id.txt             -> package nomi
- apk_name.txt           -> APK fayl nomi

SOTUVCHI UCHUN (USB usul):
1) Telefonni USB bilan ulang
2) Telefonda Developer options + USB debugging yoqing
3) install.bat ni oching
4) Ekrandagi ko'rsatmaga amal qiling

MUHIM:
- Device Owner o'rnatilishi uchun telefon factory reset holatga yaqin bo'lishi kerak.
- dpm set-device-owner xato bersa, telefonni reset qilib qayta urining.

QR USUL:
1) qr-provisioning.json tayyor bo'lishi kerak (APK URL va checksum to'g'ri)
2) generate-qr.ps1 ni ishga tushiring
3) Chiqan qr-provisioning.png ni setup paytida skaner qiling

QAYSI HOLATDA QAYSI USUL:
- Asosiy usul: QR provisioning
- Zaxira usul: install.bat (QR ishlamasa)
