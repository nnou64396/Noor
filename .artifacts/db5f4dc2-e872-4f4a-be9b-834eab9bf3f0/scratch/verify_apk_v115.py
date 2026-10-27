import os
import glob
import subprocess
import hashlib
import re

apk_path = "app/build/outputs/apk/release/app-release.apk"
if not os.path.exists(apk_path):
    print("Error: APK not found at", apk_path)
    exit(1)

# Calculate SHA256
sha256_hash = hashlib.sha256()
with open(apk_path, "rb") as f:
    for byte_block in iter(lambda: f.read(65536), b""):
        sha256_hash.update(byte_block)
apk_sha256 = sha256_hash.hexdigest()
print(f"APK SHA-256: {apk_sha256}")

# Locate Android SDK tools
sdk_root = os.environ.get("ANDROID_SDK_ROOT", os.path.expanduser("~\\AppData\\Local\\Android\\Sdk"))
build_tools = glob.glob(os.path.join(sdk_root, "build-tools", "*"))
build_tools.sort(reverse=True)
aapt_path = None
apksigner_path = None
for bt in build_tools:
    ca = os.path.join(bt, "aapt.exe")
    cs = os.path.join(bt, "apksigner.bat")
    if os.path.exists(ca) and not aapt_path:
        aapt_path = ca
    if os.path.exists(cs) and not apksigner_path:
        apksigner_path = cs

if aapt_path:
    print(f"\nUsing aapt: {aapt_path}")
    res = subprocess.run([aapt_path, "dump", "badging", apk_path], capture_output=True, text=True)
    for line in res.stdout.splitlines():
        if "package:" in line or "launchable-activity:" in line:
            print(line)

expected_fp = "6c4e68e4f8a1bc982a09611886f0eda25893642e24f14fa77642ea1a6b3ef83d"
if apksigner_path:
    print(f"\nUsing apksigner: {apksigner_path}")
    res = subprocess.run([apksigner_path, "verify", "--print-certs", apk_path], capture_output=True, text=True)
    print(res.stdout)
    if expected_fp.lower() in res.stdout.lower():
        print("SUCCESS: Certificate SHA-256 fingerprint matches expected trusted identity.")
    else:
        print("ERROR: Fingerprint mismatch!")
        exit(1)
