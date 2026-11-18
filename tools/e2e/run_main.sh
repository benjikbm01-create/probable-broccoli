#!/usr/bin/env bash
# Full end-to-end client playthrough on a freshly built island (about 10 minutes).
T="$(cd "$(dirname "$0")/.." && pwd)"; ROOT="$T/.."
"$T/devclient.sh" stop >/dev/null 2>&1; "$T/devserver.sh" stop >/dev/null
"$T/devserver.sh" start fresh >/dev/null
"$T/devserver.sh" cmd "fi build"; "$T/devserver.sh" wait "Island complete" 900 >/dev/null
"$T/devserver.sh" cmd "fi dump"; sleep 3; "$T/devserver.sh" cmd "op Dev"
python3 "$T/e2e/gen.py" "$ROOT/mod/run-server/layout_dump.json" /tmp/claude-0/work/e2e_main.txt
rm -f "$ROOT"/mod/run-client/screenshots/e2e_*
"$T/devclient.sh" start >/dev/null
cat /tmp/claude-0/work/e2e_main.txt >> "$ROOT/mod/run-client/autopilot.txt"
timeout 1500 bash -c "until grep -q 'SUMMARY passes=' '$ROOT/mod/run-client/autopilot.log' 2>/dev/null; do sleep 10; done"
cp "$ROOT/mod/run-client/autopilot.log" "$ROOT/docs/test-evidence/e2e_autopilot.log"
cp /tmp/claude-0/work/e2e_main.txt "$ROOT/docs/test-evidence/e2e_script.txt"
grep -E "FAIL|SUMMARY passes" "$ROOT/mod/run-client/autopilot.log"
