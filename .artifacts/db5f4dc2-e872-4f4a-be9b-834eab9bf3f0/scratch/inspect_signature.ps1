$sdkRoot = $env:ANDROID_SDK_ROOT
if (-not $sdkRoot) {
    $sdkRoot = "$env:LOCALAPPDATA\Android\Sdk"
}
$buildToolsDirs = Get-ChildItem -Path "$sdkRoot\build-tools" -Directory
$apksignerPath = $null
foreach ($dir in $buildToolsDirs | Sort-Object Name -Descending) {
    $candidate = Join-Path $dir.FullName "apksigner.bat"
    if (Test-Path $candidate) {
        $apksignerPath = $candidate
        break
    }
}

if ($apksignerPath) {
    Write-Host "Using apksigner: $apksignerPath"
    $apkPath = "app/build/outputs/apk/release/app-release.apk"
    & $apksignerPath verify --print-certs $apkPath
} else {
    Write-Host "apksigner not found."
}
