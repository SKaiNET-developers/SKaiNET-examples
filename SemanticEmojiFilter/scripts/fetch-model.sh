#!/usr/bin/env bash
# Downloads the MongoDB LEAF retrieval encoder (mdbr-leaf-ir, Apache-2.0) at a pinned revision
# into a directory laid out exactly as on the Hugging Face Hub. Default target: the wasm
# Compose resources, so the web build bundles the checkpoint (~92 MB). Desktop and Android
# download the same files at runtime into their own cache instead.
#
#   scripts/fetch-model.sh [target-dir]
set -euo pipefail

REPO="MongoDB/mdbr-leaf-ir"
REVISION="4262131b32c3182bd06e67e92ae69d7bd66e0c5c"
FILES=(
  "model.safetensors"
  "config.json"
  "tokenizer.json"
  "tokenizer_config.json"
  "2_Dense/model.safetensors"
  "2_Dense/config.json"
  "1_Pooling/config.json"
)
MIN_MODEL_BYTES=$((80 * 1024 * 1024))

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET="${1:-$SCRIPT_DIR/../composeApp/src/wasmJsMain/composeResources/files/leaf}"
mkdir -p "$TARGET"

# A local copy (the desktop/CLI cache, or $LEAF_MODEL_DIR) is reused instead of downloading twice.
LOCAL="${LEAF_MODEL_DIR:-$HOME/.skainet-examples/semantic-emoji-filter/leaf}"

for f in "${FILES[@]}"; do
  out="$TARGET/$f"
  if [[ ! -s "$out" && -s "$LOCAL/$f" && "$LOCAL" != "$TARGET" ]]; then
    mkdir -p "$(dirname "$out")"
    cp "$LOCAL/$f" "$out"
    echo "fetch-model: $f copied from $LOCAL"
    continue
  fi
  if [[ -s "$out" ]]; then
    if [[ "$f" != "model.safetensors" || $(stat -c %s "$out" 2>/dev/null || stat -f %z "$out") -ge $MIN_MODEL_BYTES ]]; then
      echo "fetch-model: $f present, skipping"
      continue
    fi
  fi
  mkdir -p "$(dirname "$out")"
  echo "fetch-model: downloading $f"
  curl -fL --retry 3 "https://huggingface.co/$REPO/resolve/$REVISION/$f" -o "$out.part"
  mv "$out.part" "$out"
done

size=$(stat -c %s "$TARGET/model.safetensors" 2>/dev/null || stat -f %z "$TARGET/model.safetensors")
if (( size < MIN_MODEL_BYTES )); then
  echo "fetch-model: model.safetensors is only $size bytes — download incomplete" >&2
  exit 1
fi
echo "fetch-model: LEAF checkpoint ready in $TARGET"
