#!/bin/bash
# ── 获取 KataGo 源码 (含 tclap 子模块) ────────────────────
# 直接克隆到 app/src/main/cpp/katago/
# CI 每次都是干净环境，不走缓存

set -e

KATAGO_VERSION="v1.16.5"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
KATAGO_DEST="$REPO_ROOT/app/src/main/cpp/katago"

echo "=== Fetching KataGo $KATAGO_VERSION ==="

# 清空旧目录
rm -rf "$KATAGO_DEST"

# 直接克隆到目标位置 (含 submodules)
git clone --branch "$KATAGO_VERSION" --depth 1 --recurse-submodules \
    https://github.com/lightvector/KataGo.git \
    "$KATAGO_DEST"

# 验证 tclap 是否下载成功
if [ ! -f "$KATAGO_DEST/cpp/command/tclap/CmdLine.h" ]; then
    echo "ERROR: tclap submodule not found!"
    echo "Trying manual init..."
    cd "$KATAGO_DEST"
    git submodule update --init --recursive --force
    cd "$REPO_ROOT"
fi

# 验证关键文件
echo "=== Verifying ==="
ls "$KATAGO_DEST/cpp/command/tclap/CmdLine.h" && echo "tclap: OK"
ls "$KATAGO_DEST/cpp/core/main.cpp" && echo "KataGo core: OK"

echo "=== SUCCESS: KataGo sources ready ==="
