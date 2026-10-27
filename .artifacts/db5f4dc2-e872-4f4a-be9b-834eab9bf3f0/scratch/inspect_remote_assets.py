import urllib.request
import os
import hashlib
import json
import subprocess
import glob

os.makedirs(".artifacts/scratch/downloaded", exist_ok=True)
apk_url = "https://github.com/nnou64396/Noor/releases/download/v1.1.4/app-release.apk"
json_url = "https://github.com/nnou64396/Noor/releases/download/v1.1.4/noor-update.json"

apk_path = ".artifacts/scratch/downloaded/app-release.apk"
json_path = ".artifacts/scratch/downloaded/noor-update.json"

print("Downloading published manifest...")
urllib.request.urlretrieve(json_url, json_path)
with open(json_path, 'r', encoding='utf-8') as f:
    manifest_content = f.read()
    print("Manifest Content:")
    print(manifest_content)
    manifest = json.loads(manifest_content)

print("\nDownloading published APK (5.4MB)...")
urllib.request.urlretrieve(apk_url, apk_path)

# Calculate SHA256 of downloaded APK
sha256_hash = hashlib.sha256()
with open(apk_path, "rb") as f:
    for byte_block in iter(lambda: f.read(65536), b""):
        sha256_hash.update(byte_block)
downloaded_apk_sha256 = sha256_hash.hexdigest()
print(f"Downloaded APK SHA-256: {downloaded_apk_sha256}")
print(f"Manifest SHA-256: {manifest.get('sha256')}")
print(f"Checksum match: {downloaded_apk_sha256.lower() == manifest.get('sha256', '').lower()}")

# Inspect APK with aapt
sdk_root = os.environ.get("ANDROID_SDK_ROOT", os.path.expanduser("~\\AppData\\Local\\Android\\Sdk"))
build_tools = glob.glob(os.path.join(sdk_root, "build-tools", "*"))
build_tools.sort(reverse=True)
aapt_path = None
apksigner_path = None
for bt in build_tools:
    candidate_aapt = os.path.join(bt, "aapt.exe")
    candidate_signer = os.path.join(bt, "apksigner.bat")
    if os.path.exists(candidate_aapt) and not aapt_path:
        aapt_path = candidate_aapt
    if os.path.exists(candidate_signer) and not apksigner_path:
        apksigner_path = candidate_signer

if aapt_path:
    print(f"\nUsing aapt: {aapt_path}")
    res = subprocess.run([aapt_path, "dump", "badging", apk_path], capture_output=True, text=True)
    for line in res.stdout.splitlines():
        if "package:" in line or "launchable-activity:" in line:
            print(line)

if apksigner_path:
    print(f"\nUsing apksigner: {apksigner_path}")
    res = subprocess.run([apksigner_path, "verify", "--print-certs", apk_path], capture_output=True, text=True)
    print(res.stdout)
