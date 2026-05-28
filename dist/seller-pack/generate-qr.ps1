param(
    [string]$InputJson = ".\qr-provisioning.json",
    [string]$OutputPng = ".\qr-provisioning.png"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $InputJson)) {
    throw "JSON topilmadi: $InputJson"
}

$json = Get-Content $InputJson -Raw
$pythonReady = $false
try {
    $null = & python -c "print('ok')" 2>$null
    if ($LASTEXITCODE -eq 0) {
        $pythonReady = $true
    }
} catch {
    $pythonReady = $false
}

if ($pythonReady) {
    $script = @"
import json
import pathlib
import sys

inp = pathlib.Path(sys.argv[1])
outp = pathlib.Path(sys.argv[2])
payload = inp.read_text(encoding="utf-8")

try:
    import qrcode
except Exception:
    raise SystemExit("MISSING_QRCODE")

img = qrcode.make(payload)
img.save(outp)
print(str(outp))
"@
    $tmpPy = Join-Path $env:TEMP "gmdm_make_qr.py"
    Set-Content -Path $tmpPy -Value $script -Encoding utf8

    $result = & python $tmpPy $InputJson $OutputPng 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "QR tayyor: $OutputPng" -ForegroundColor Green
        exit 0
    }
    if ($result -match "MISSING_QRCODE") {
        Write-Host "python qrcode kutubxonasi yo'q. O'rnatish: pip install qrcode[pil]" -ForegroundColor Yellow
    } else {
        Write-Host $result
    }
}

$escaped = [System.Uri]::EscapeDataString($json)
$url = "https://api.qrserver.com/v1/create-qr-code/?size=700x700&data=$escaped"
Write-Host "Local QR yaratilmadi. Quyidagi URL orqali PNG yuklab oling:" -ForegroundColor Yellow
Write-Host $url
