@echo off
setlocal EnableExtensions
chcp 65001 >nul

cd /d "%~dp0"
title GMDM One-Click Installer

if not exist "app_id.txt" (
  echo [XATO] app_id.txt topilmadi.
  pause
  exit /b 1
)

if not exist "apk_name.txt" (
  echo [XATO] apk_name.txt topilmadi.
  pause
  exit /b 1
)

set /p APP_ID=<app_id.txt
set /p APK_NAME=<apk_name.txt
set ADMIN_COMPONENT=%APP_ID%/.core.CustomDeviceAdminReceiver

if exist "adb\adb.exe" (
  set ADB_EXE=adb\adb.exe
) else (
  set ADB_EXE=adb
)

echo ================================================
echo GMDM O'RNATISH USTASI
echo ================================================
echo 1^) Telefonda Developer options yoqing
echo 2^) USB debugging yoqing
echo 3^) Telefonni USB orqali ulang
echo ================================================
echo.
pause

%ADB_EXE% start-server >nul 2>&1
if errorlevel 1 (
  echo [XATO] ADB ishga tushmadi. platform-tools ni tekshiring.
  pause
  exit /b 1
)

echo.
echo Qurilma tekshirilmoqda...
%ADB_EXE% devices
echo.
echo Agar "unauthorized" bo'lsa, telefonda USB debugging tasdig'ini bosing.
pause

if not exist "%APK_NAME%" (
  echo [XATO] APK topilmadi: %APK_NAME%
  pause
  exit /b 1
)

echo.
echo APK o'rnatilmoqda...
%ADB_EXE% install -r "%APK_NAME%"
if errorlevel 1 (
  echo [XATO] APK o'rnatilmadi.
  echo Ehtimol: eski versiya, imzo mos emas yoki ulanish muammosi.
  pause
  exit /b 1
)

echo.
echo Device Owner yoqilmoqda...
%ADB_EXE% shell dpm set-device-owner "%ADMIN_COMPONENT%"
if errorlevel 1 (
  echo [OGOHLANTIRISH] Device Owner yoqilmadi.
  echo Sabab: telefon factory reset holatda emas bo'lishi mumkin.
  echo Yechim: Factory reset qilib qayta urining.
  pause
  exit /b 2
)

echo.
echo [OK] O'rnatish yakunlandi.
echo Ilovani ochib Accessibility, Overlay va Battery ruxsatlarini bering.
pause
exit /b 0
