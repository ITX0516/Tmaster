#!/bin/bash
# ── KataGo 源码获取脚本 ───────────────────────────────────
# Clone KataGo 源码到 app/src/main/cpp/katago/，
# 然后 CMakeLists.txt 会检测到源码并与 JNI 桥一起编译为单个 .so。
#
# 用法:
#   chmod +x build_katago_android.sh
#   ./build_katago_android.sh
#
# Android APK 编译时会自动把 KataGo + bridge 编译到一起。

set -e

KATAGO_VERSION="v1.16.5"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BUILD_DIR="$REPO_ROOT/build/katago-source"
KATAGO_DEST="$REPO_ROOT/app/src/main/cpp/katago"

echo "=== Tmaster: Fetching KataGo Sources ==="
echo "Version: $KATAGO_VERSION"
echo "Dest:    $KATAGO_DEST"

# ── 1. Clone KataGo ───────────────────────────────────────
mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

if [ ! -d "KataGo" ]; then
    echo "Cloning KataGo $KATAGO_VERSION..."
    git clone --branch "$KATAGO_VERSION" --depth 1 \
        https://github.com/lightvector/KataGo.git
fi

# ── 2. 复制源码到项目 ─────────────────────────────────────
rm -rf "$KATAGO_DEST/cpp"
cp -r "$BUILD_DIR/KataGo/cpp" "$KATAGO_DEST/cpp"

echo "=== SUCCESS ==="
echo "KataGo sources at: $KATAGO_DEST/cpp"
echo ""
echo "Next: Build APK with Android Studio or ./gradlew assembleDebug"
echo "CMakeLists.txt will detect KataGo sources and compile everything into katago-bridge.so"
