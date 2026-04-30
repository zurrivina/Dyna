#!/bin/bash
set -e  # Exit on any error

echo "=== Starting release build process ==="

# Step 1: Clean and assemble release (unsigned APK)
echo "Step 1: Building unsigned release APK..."
./gradlew clean assembleRelease

# Step 2: Define paths
UNSIGNED_APK="app/build/outputs/apk/release/app-release-unsigned.apk"
ALIGNED_APK="app/build/outputs/apk/release/app-release-unsigned-aligned.apk"
DATE=$(date +%m-%d-%y)
FINAL_APK="app/builds/app-release${DATE}.apk"

echo "Step 2: Zipaligning the unsigned APK..."
/home/zurrivina/Android/Sdk/build-tools/36.1.0/zipalign -v -p 4 "$UNSIGNED_APK" "$ALIGNED_APK"

echo "Step 3: Signing the aligned APK with apksigner..."
/home/zurrivina/Android/Sdk/build-tools/36.1.0/apksigner sign \
  --ks ~/.android/debug.keystore \
  --ks-key-alias androiddebugkey \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out "$FINAL_APK" \
  "$ALIGNED_APK"

echo "Step 4: Verifying the signed APK..."
/home/zurrivina/Android/Sdk/build-tools/36.1.0/apksigner verify -v "$FINAL_APK"

echo "Step 5: Installing on device..."
adb install -r "$FINAL_APK"

echo "=== Release build completed successfully ==="
echo "Final APK: $FINAL_APK"