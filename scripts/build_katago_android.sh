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
git clone --branch "$KATAGO_VERSION" --depth 1 \
    https://github.com/lightvector/KataGo.git \
    "$KATAGO_DEST"

# 单独下载 tclap (shallow clone + submodules 不兼容)
TCLAP_DIR="$KATAGO_DEST/cpp/command/tclap"
rm -rf "$TCLAP_DIR"
git clone --depth 1 https://github.com/lightvector/tclap.git "$TCLAP_DIR"
rm -rf "$TCLAP_DIR/.git"

# 验证关键文件
echo "=== Verifying ==="
ls "$KATAGO_DEST/cpp/command/tclap/CmdLine.h" && echo "tclap: OK"
ls "$KATAGO_DEST/cpp/core/main.cpp" && echo "KataGo core: OK"

echo "=== SUCCESS: KataGo sources ready ==="
