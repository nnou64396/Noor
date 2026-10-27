$apkFiles = Get-ChildItem -Path "app/build/outputs/apk/release" -Filter "*.apk" -Recurse
foreach ($f in $apkFiles) {
    Write-Host "APK Path: $($f.FullName)"
    Write-Host "APK Length: $($f.Length)"
}
