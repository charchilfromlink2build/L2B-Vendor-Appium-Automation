#!/usr/bin/env bash
set -euo pipefail
RESULTS=target/allure-results
REPORT=target/allure-report
# carry forward history from the previous report
if [ -d "$REPORT/history" ]; then
  cp -r "$REPORT/history" "$RESULTS/history"
fi
allure generate "$RESULTS" --clean -o "$REPORT"
echo "Report at $REPORT — open with: allure open $REPORT"
