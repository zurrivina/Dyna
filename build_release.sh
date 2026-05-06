#!/bin/bash
set -e  # Exit on any error

# Default to release build
BUILD_TYPE="release"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --dev)
            BUILD_TYPE="debug"
            shift
            ;;
        --release)
            BUILD_TYPE="release"
            shift
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--dev|--release]"
            exit 1
            ;;
    esac
done

if [ "$BUILD_TYPE" = "release" ]; then
    APP_NAME="Dyna"
    echo "=== Starting $BUILD_TYPE build process for $APP_NAME ==="

    # Step 1: Clean and assemble release (unsigned APK)
    echo "Step 1: Building unsigned release APK for $APP_NAME..."
    ./gradlew clean assembleRelease

    # Step 2: Define paths
    UNSIGNED_APK="app/build/outputs/apk/release/app-release-unsigned.apk"
    ALIGNED_APK="app/build/outputs/apk/release/app-release-unsigned-aligned.apk"
    DATE=$(date +%m-%d-%y)
    FINAL_APK="app/builds/$APP_NAME-release-${DATE}.apk"

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

    echo "=== $APP_NAME release build completed successfully ==="
    echo "Final APK: $FINAL_APK"
    echo "Application: $APP_NAME (applicationId: com.flying_kiwi.dyna)"
else
    APP_NAME="Dyna-dev"
    echo "=== Starting $BUILD_TYPE build process for $APP_NAME ==="

    # Step 1: Clean and assemble debug APK
    echo "Step 1: Building debug APK for $APP_NAME..."
    ./gradlew clean assembleDebug

    DEBUG_APK="app/build/outputs/apk/debug/app-debug.apk"
    DATE=$(date +%m-%d-%y)
    FINAL_APK="app/builds/$APP_NAME-${DATE}.apk"

    # Copy to builds directory with clear naming
    mkdir -p app/builds
    cp "$DEBUG_APK" "$FINAL_APK"

    echo "Step 2: Installing debug APK on device..."
    adb install -r "$DEBUG_APK"

    echo "=== $APP_NAME debug build completed successfully ==="
    echo "APK: $FINAL_APK"
    echo "Application: $APP_NAME (applicationId: com.flying_kiwi.dyna.dev)"
fi
