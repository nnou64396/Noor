$targetFiles = @(
    ".github/workflows/release.yml",
    "RELEASE.md",
    "NOOR_AUDIT.md",
    "tools/quran/importer/src/main/kotlin/com/noor/quran/importer/Rows.kt",
    "tools/quran/importer/src/test/kotlin/com/noor/quran/importer/QuranDatabaseBuilderTest.kt"
)

foreach ($relativePath in $targetFiles) {
    $fullPath = Join-Path "C:/Users/nesyo/Documents/Noor" $relativePath
    if (Test-Path $fullPath) {
        $content = Get-Content $fullPath -Raw -Encoding UTF8
        if ($content -match 'com\.noor\.islamic') {
            $newContent = $content -replace 'com\.noor\.islamic', 'dz.techvision.noor'
            Set-Content $fullPath -Value $newContent -Encoding UTF8
            Write-Host "Updated config/doc: $relativePath"
        }
    }
}

# Clean up old com.noor.islamic schema directories
$oldSchemas = Get-ChildItem -Path "app/schemas" -Directory -Filter "com.noor.islamic*"
foreach ($dir in $oldSchemas) {
    Remove-Item -Recurse -Force $dir.FullName
    Write-Host "Removed old schema dir: $($dir.Name)"
}

Write-Host "Config and schema cleanup completed."
