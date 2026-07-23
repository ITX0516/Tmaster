#!/bin/bash
# ── KataGo Android ARM64 交叉编译脚本 ─────────────────────
# 在 Linux 环境运行 (或 WSL / GitHub Actions)
# 输出: build/libkatago.a (Eigen 后端, CPU only)
#
# 用法:
#   chmod +x build_katago_android.sh
#   ./build_katago_android.sh
#
# 依赖:
#   - Android NDK (r26+)
#   - cmake, make, git

set -e

KATAGO_VERSION="v1.16.5"
NDK_VERSION="26.3.11579264"
API_LEVEL=26
ABI="arm64-v8a"
BUILD_DIR="$(pwd)/build/katago-android"

echo "=== KataGo Android Build ==="
echo "Version: $KATAGO_VERSION"
echo "ABI:     $ABI"
echo "API:     $API_LEVEL"

# ── 1. 检查/安装 NDK ─────────────────────────────────────
if [ -z "$ANDROID_NDK_HOME" ]; then
    # 尝试常见路径
    if [ -d "$ANDROID_HOME/ndk/$NDK_VERSION" ]; then
        export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/$NDK_VERSION"
    elif [ -d "$HOME/Android/Sdk/ndk/$NDK_VERSION" ]; then
        export ANDROID_NDK_HOME="$HOME/Android/Sdk/ndk/$NDK_VERSION"
    else
        echo "ERROR: ANDROID_NDK_HOME not set and NDK not found."
        echo "Install NDK $NDK_VERSION via Android Studio SDK Manager or:"
        echo "  sdkmanager 'ndk;$NDK_VERSION'"
        exit 1
    fi
fi
echo "NDK: $ANDROID_NDK_HOME"

# ── 2. Clone KataGo ───────────────────────────────────────
mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

if [ ! -d "KataGo" ]; then
    echo "Cloning KataGo..."
    git clone --branch "$KATAGO_VERSION" --depth 1 \
        https://github.com/lightvector/KataGo.git
fi

cd KataGo/cpp

# ── 3. CMake 配置 ─────────────────────────────────────────
TOOLCHAIN="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake"

cmake . -B build-android \
    -DCMAKE_TOOLCHAIN_FILE="$TOOLCHAIN" \
    -DANDROID_ABI="$ABI" \
    -DANDROID_PLATFORM="android-$API_LEVEL" \
    -DUSE_BACKEND=EIGEN \
    -DNO_GIT_REVISION=1 \
    -DBUILD_TESTING=OFF \
    -DBUILD_DISTRIBUTED=OFF \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_CXX_FLAGS="-O3 -DNDEBUG -fopenmp -static-openmp"

# ── 4. 编译 ───────────────────────────────────────────────
cmake --build build-android --target katago -j$(nproc)

# ── 5. 复制产物 ───────────────────────────────────────────
OUTPUT_DIR="$(pwd)/../../../../app/src/main/cpp/katago"
mkdir -p "$OUTPUT_DIR"

# 如果生成了静态库
if [ -f "build-android/libkatago.a" ]; then
    cp build-android/libkatago.a "$OUTPUT_DIR/"
    echo "=== SUCCESS: libkatago.a copied to app/src/main/cpp/katago/ ==="
elif [ -f "build-android/katago" ]; then
    # 如果生成了可执行文件，复制过来
    cp build-android/katago "$OUTPUT_DIR/"
    echo "=== SUCCESS: katago binary copied ==="
else
    echo "=== Build completed but no output found. Check build-android/ ==="
    ls build-android/
fi

echo "=== Next step: update CMakeLists.txt to link libkatago.a ==="
echo "=== Remove -DKATAGO_MOCK from CMakeLists.txt ==="
