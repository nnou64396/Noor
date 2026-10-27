$sdkRoot = $env:ANDROID_SDK_ROOT
if (-not $sdkRoot) {
    $sdkRoot = "$env:LOCALAPPDATA\Android\Sdk"
}
$buildToolsDirs = Get-ChildItem -Path "$sdkRoot\build-tools" -Directory
$aaptPath = $null
foreach ($dir in $buildToolsDirs | Sort-Object Name -Descending) {
    $candidate = Join-Path $dir.FullName "aapt.exe"
    if (Test-Path $candidate) {
        $aaptPath = $candidate
        break
    }
}

if ($aaptPath) {
    Write-Host "Using aapt: $aaptPath"
    $apkPath = "app/build/outputs/apk/release/app-release.apk"
    & $aaptPath dump badging $apkPath
} else {
    Write-Host "aapt.exe not found."
}
