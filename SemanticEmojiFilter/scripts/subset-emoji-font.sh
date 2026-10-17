#!/usr/bin/env bash
# Regenerates the emoji font the web build bundles:
#   composeApp/src/wasmJsMain/composeResources/files/font/noto-color-emoji-subset.ttf
# Compose for Web draws text through Skiko and has no system emoji font, so without a bundled one
# every catalog emoji renders as a replacement box. The file is Noto Color Emoji (COLRv1 glyphs;
# the SVG table is dropped) cut down to the catalog's emoji, which keeps it a few hundred KB instead
# of 25 MB. Source and license: google/fonts ofl/notocoloremoji at the commit below, SIL Open Font
# License 1.1 (THIRD_PARTY_LICENSES/OFL-1.1-NotoColorEmoji.txt). Needs fonttools (`pip install fonttools`).
# Run it again after scripts/generate-catalog.py changed the catalog.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
SRC_COMMIT=142c8963e7606b510c93a644c82a4c4cdeae6ef9   # google/fonts, 2026-09-09: Noto Color Emoji 2.055
SRC_URL="https://raw.githubusercontent.com/google/fonts/$SRC_COMMIT/ofl/notocoloremoji/NotoColorEmoji-Regular.ttf"
CATALOG="$ROOT/composeApp/src/commonMain/composeResources/files/catalog/catalog.json"
OUT="$ROOT/composeApp/src/wasmJsMain/composeResources/files/font/noto-color-emoji-subset.ttf"
WORK=${1:-$(mktemp -d)}

[ -f "$WORK/NotoColorEmoji-Regular.ttf" ] || curl -fsSL "$SRC_URL" -o "$WORK/NotoColorEmoji-Regular.ttf"
python3 - "$CATALOG" > "$WORK/emoji.txt" <<'PY'
import json, sys
items = json.load(open(sys.argv[1], encoding="utf-8"))["items"]
sys.stdout.write("".join(item["emoji"] for item in items))
PY
mkdir -p "$(dirname "$OUT")"
pyftsubset "$WORK/NotoColorEmoji-Regular.ttf" \
  --text-file="$WORK/emoji.txt" \
  --layout-features='*' \
  --drop-tables+=SVG \
  --no-hinting \
  --output-file="$OUT"
ls -l "$OUT"
