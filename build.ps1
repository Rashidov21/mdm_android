# GMDM debug APK build skripti
# Talab: JDK 17+ va Android SDK (Android Studio orqali)

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot

# JDK 17 (Android Studio JBR yoki temp JDK)
$JdkCandidates = @(
    "$env:LOCALAPPDATA\Programs\Android Studio\jbr",
    "$env:ProgramFiles\Android\Android Studio\jbr",
    "$env:TEMP\jdk17-extract2\jdk-17.0.13+11"
)
foreach ($jdk in $JdkCandidates) {
    if (Test-Path "$jdk\bin\java.exe") {
        $env:JAVA_HOME = $jdk
        break
    }
}
if (-not $env:JAVA_HOME) {
    Write-Host "JAVA_HOME topilmadi. Android Studio o'rnating yoki JDK 17 o'rnating." -ForegroundColor Red
    exit 1
}

# Android SDK
$SdkRoot = "$env:LOCALAPPDATA\Android\Sdk"
if (-not (Test-Path $SdkRoot)) {
    Write-Host "Android SDK topilmadi: $SdkRoot" -ForegroundColor Red
    Write-Host "Android Studio o'rnating va SDK Manager orqali API 34 o'rnating." -ForegroundColor Yellow
    exit 1
}
$env:ANDROID_HOME = $SdkRoot
$env:ANDROID_SDK_ROOT = $SdkRoot

$sdkDir = $SdkRoot -replace '\\', '\\'
"sdk.dir=$sdkDir" | Out-File "$ProjectRoot\local.properties" -Encoding ascii -NoNewline

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "ANDROID_SDK=$SdkRoot"
Write-Host "Build boshlandi..."

Set-Location $ProjectRoot
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

if (Test-Path ".\gradlew.bat") {
    & .\gradlew.bat assembleDebug --no-daemon
} elseif (Test-Path "$env:TEMP\gradle-8.7\bin\gradle.bat") {
    & "$env:TEMP\gradle-8.7\bin\gradle.bat" assembleDebug --no-daemon
} else {
    Write-Host "Gradle topilmadi. Android Studio orqali oching yoki gradlew.bat yarating." -ForegroundColor Red
    exit 1
}

if ($LASTEXITCODE -eq 0) {
    $apk = Get-ChildItem -Path "app\build\outputs\apk\debug" -Filter "*.apk" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($apk) {
        Write-Host "`nAPK tayyor:" -ForegroundColor Green
        Write-Host $apk.FullName
    }
}

exit $LASTEXITCODE
