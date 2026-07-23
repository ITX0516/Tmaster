#!/bin/bash
# ── 下载 KataGo 模型权重并打包到 assets ─────────────────
# 使用 katagotraining.org 的最新 b18 模型 (~45MB)
# 安装后无需联网即可对弈

set -e

MODEL_URL="https://media.katagotraining.org/uploaded/networks/models/kata1/kata1-b18c384nbt-s7890181632-d3762437685.bin.gz"
MODEL_NAME="kata1-b18c384nbt.bin.gz"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ASSETS_DIR="$REPO_ROOT/app/src/main/assets/models"

mkdir -p "$ASSETS_DIR"

if [ -f "$ASSETS_DIR/$MODEL_NAME" ]; then
    echo "Model already downloaded: $MODEL_NAME"
    ls -lh "$ASSETS_DIR/$MODEL_NAME"
    exit 0
fi

echo "Downloading KataGo model ($MODEL_NAME)..."
echo "URL: $MODEL_URL"
curl -L -o "$ASSETS_DIR/$MODEL_NAME" "$MODEL_URL"

echo "=== SUCCESS ==="
ls -lh "$ASSETS_DIR/$MODEL_NAME"
echo "Model bundled at: $ASSETS_DIR/$MODEL_NAME"
