# Move test directory if exists
if (Test-Path 'app/src/test/java/com/noor/islamic') {
    New-Item -ItemType Directory -Force 'app/src/test/java/dz/techvision/noor' | Out-Null
    Get-ChildItem 'app/src/test/java/com/noor/islamic' | Move-Item -Destination 'app/src/test/java/dz/techvision/noor/' -Force
    Remove-Item -Recurse -Force 'app/src/test/java/com/noor'
    Write-Host "Moved test directory from com/noor/islamic to dz/techvision/noor"
}
