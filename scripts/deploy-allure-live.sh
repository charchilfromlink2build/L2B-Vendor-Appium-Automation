#!/usr/bin/env bash
# Publish Allure to GitHub Pages so the company URL always shows the latest run.
# Public URL: https://rajcharchil.github.io/l2b-vendor-allure/
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

REPO="${ALLURE_GH_REPO:-Rajcharchil/l2b-vendor-allure}"
LIVE="$ROOT/reports/company-allure/live-git"

if ! gh auth status >/dev/null 2>&1; then
  echo "FAIL: GitHub CLI not logged in. Run: gh auth login"
  exit 1
fi

if ! gh repo view "$REPO" >/dev/null 2>&1; then
  echo "Creating public repo $REPO ..."
  gh repo create "$REPO" --public \
    --description "Live Allure report for L2B Vendor Appium (updated after each test run)"
fi

# Landing page at site root; Allure report under ./allure/
# If there are no local results, reuse the last published Allure folder.
rm -rf "$LIVE"
mkdir -p "$LIVE/allure"
cp "$ROOT/docs/index.html" "$LIVE/index.html"
touch "$LIVE/.nojekyll"

ALLURE="$ROOT/.allure/allure-2.32.2/bin/allure"
RESULTS="$ROOT/target/allure-results"

HAS_RESULTS=0
if [ -d "$RESULTS" ] && ls "$RESULTS"/*-result.json >/dev/null 2>&1; then
  HAS_RESULTS=1
fi

PREV=$(mktemp -d)
if gh repo view "$REPO" >/dev/null 2>&1; then
  git clone --depth 1 --branch gh-pages "https://github.com/${REPO}.git" "$PREV" >/dev/null 2>&1 || true
fi

if [ "$HAS_RESULTS" = "1" ]; then
  if [ -d "$PREV/allure/history" ]; then
    mkdir -p "$RESULTS/history"
    cp -R "$PREV/allure/history/." "$RESULTS/history/" 2>/dev/null || true
  fi
  if [ -d "$ROOT/target/allure-report/history" ]; then
    mkdir -p "$RESULTS/history"
    cp -R "$ROOT/target/allure-report/history/." "$RESULTS/history/"
  fi
  if [ ! -x "$ALLURE" ]; then
    echo "Allure CLI missing. Run: mvn -q allure:report  (downloads it once)"
    mvn -q allure:report
  fi
  "$ALLURE" generate "$RESULTS" --clean -o "$LIVE/allure"
elif [ -d "$PREV/allure" ] && [ -f "$PREV/allure/index.html" ]; then
  echo "No local allure-results — keeping last published Allure under ./allure/"
  cp -R "$PREV/allure/." "$LIVE/allure/"
elif [ -d "$PREV" ] && [ -f "$PREV/index.html" ] && [ ! -f "$PREV/docs/index.html" ]; then
  echo "Previous site was a single-file Allure root — copying into ./allure/"
  mkdir -p "$LIVE/allure"
  cp -R "$PREV/." "$LIVE/allure/" 2>/dev/null || true
  rm -rf "$LIVE/allure/.git" "$LIVE/allure/.nojekyll"
else
  echo "WARN: no Allure results to publish; landing page only"
fi

export GIT_AUTHOR_NAME="${GIT_AUTHOR_NAME:-L2B Vendor Allure}"
export GIT_AUTHOR_EMAIL="${GIT_AUTHOR_EMAIL:-noreply@users.noreply.github.com}"
export GIT_COMMITTER_NAME="$GIT_AUTHOR_NAME"
export GIT_COMMITTER_EMAIL="$GIT_AUTHOR_EMAIL"

cd "$LIVE"
git init -b gh-pages
git add -A
git commit -m "Allure report $(date -u +%Y-%m-%dT%H:%M:%SZ)"
git remote add origin "https://github.com/${REPO}.git"
git push -f origin gh-pages

# Point Pages at gh-pages / (ignore error if already configured).
gh api -X PUT "repos/${REPO}/pages" \
  -H "Accept: application/vnd.github+json" \
  --raw-field "build_type=legacy" \
  --input - >/dev/null 2>&1 <<'EOF' || true
{"source":{"branch":"gh-pages","path":"/"}}
EOF

echo
echo "Live report (wait ~30s on first deploy):"
echo "  Landing:  https://rajcharchil.github.io/l2b-vendor-allure/"
echo "  Allure:   https://rajcharchil.github.io/l2b-vendor-allure/allure/"
echo
echo "This is a PUBLIC GitHub Pages site. Anyone with the link can open it."
echo "Re-run this script (or ./run-tests.sh) after tests; the same URL updates."
