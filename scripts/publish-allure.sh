#!/usr/bin/env bash
# Build a shareable Allure pack for the company (does not live in target/).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
export PATH="$JAVA_HOME/bin:$PATH"

RESULTS="$ROOT/target/allure-results"
STAMP="$(date +%Y-%m-%d_%H%M)"
OUT_DIR="$ROOT/reports/company-allure"
LATEST="$OUT_DIR/latest"
ZIP="$OUT_DIR/L2B-Vendor-Allure-${STAMP}.zip"

ALLURE="$ROOT/.allure/allure-2.32.2/bin/allure"
if [ ! -x "$ALLURE" ]; then
  echo "Allure CLI missing. Run: mvn -q allure:report  (downloads it once)"
  mvn -q allure:report
fi

if [ ! -d "$RESULTS" ] || [ -z "$(ls -A "$RESULTS" 2>/dev/null)" ]; then
  echo "FAIL: no test results in $RESULTS"
  echo "Run tests first: ./run-tests.sh"
  exit 1
fi

mkdir -p "$LATEST"
rm -rf "$LATEST"/*

echo "--- Single-file Allure (easy to email / Drive) ---"
"$ALLURE" generate "$RESULTS" --clean --single-file \
  --name "L2B Vendor" \
  -o "$LATEST"

# Keep a dated copy so a new publish does not erase what you already sent.
DATED="$OUT_DIR/$STAMP"
rm -rf "$DATED"
cp -R "$LATEST" "$DATED"

echo "--- Zip ---"
rm -f "$ZIP"
(
  cd "$LATEST"
  zip -q -r "$ZIP" .
)

cat > "$LATEST/HOW_TO_OPEN.txt" <<'EOF'
L2B Vendor Allure report
========================

This folder is a snapshot. It will not refresh unless someone re-runs tests.

Option A — one file (simplest for Slack / email / Drive)
  Open index.html in Chrome.
  If the page is blank, the browser blocked local files. Use Option B.

Option B — tiny local server (always works)
  python3 -m http.server 8080
  Then open http://127.0.0.1:8080

Option C — company can open the .zip, unzip, then A or B.

Do not open this report via Cursor Live Preview (port 5500) — it auto-reloads.
EOF

cp "$LATEST/HOW_TO_OPEN.txt" "$DATED/HOW_TO_OPEN.txt"

echo
echo "Share these (they stay put; mvn clean will not delete them):"
echo "  Single HTML: $LATEST/index.html"
echo "  Zip:         $ZIP"
echo "  Dated copy:  $DATED"
echo
echo "Open locally:  open \"$LATEST/index.html\""
echo "Serve (no auto-refresh): python3 -m http.server 8080 --directory \"$LATEST\""

if [ "${DEPLOY_LIVE:-1}" = "1" ]; then
  echo "--- GitHub Pages ---"
  bash "$ROOT/scripts/deploy-allure-live.sh" \
    || echo "WARN: live deploy failed. Local pack is still in $LATEST"
fi
