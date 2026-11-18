#!/usr/bin/env bash
# Part B of the automated client tests: developer mode, stage jumps, reset, and save/reload persistence.
set -e
T="$(cd "$(dirname "$0")/.." && pwd)"; AP="$T/../mod/run-client/autopilot.txt"; LOG="$T/../mod/run-client/autopilot.log"
"$T/devclient.sh" stop >/dev/null 2>&1 || true
"$T/devserver.sh" stop >/dev/null || true
"$T/devserver.sh" start fresh >/dev/null
"$T/devserver.sh" cmd "fi build"; "$T/devserver.sh" wait "Island complete" 900 >/dev/null
"$T/devserver.sh" cmd "fi dump"; sleep 2; "$T/devserver.sh" cmd "op Dev"
"$T/devclient.sh" start >/dev/null
cat >> "$AP" <<'S'
screen IntroScreen 600
wait 60
intro
wait 60
chat /fi dev on
wait 20
chat /fi warp cp_dock
wait 40
talk tobias
screen DialogueScreen 80
wait 10
expecttext [dev]
shot dev_01_dialogue_redacted
choose 1
wait 12
close
wait 10
journal
screen JournalScreen 60
wait 10
shot dev_02_journal_redacted
close
wait 10
expecthud m1
chat /fi stage keep
wait 60
expecthud m3
chat /fi warp sun_court
wait 40
chat /fi puzzle mirrors solve
wait 70
chat /fi puzzle stones reset
wait 10
chat /fi selftest gates
wait 60
expectchat SELFTEST SUMMARY: 1 passed, 0 failed
chat /fi reset
wait 40
screen IntroScreen 200
wait 20
intro
wait 60
expecthud m1
chat /fi dev off
wait 20
expecthud Letter from Nowhere
chat /fi flag set ione_dating
chat /fi flag set wren_dating
chat /fi var rel.ione 6
chat /fi stage undercroft
wait 60
expecthud Beneath the Keep
chat /save-all
wait 60
summary
S
timeout 900 bash -c "until grep -q 'SUMMARY passes=' '$LOG' 2>/dev/null; do sleep 5; done"
cp "$LOG" "$T/../docs/test-evidence/e2e_dev_part1.log"
# ---- restart everything, then verify persistence ----
"$T/devclient.sh" stop >/dev/null 2>&1 || true
"$T/devserver.sh" stop >/dev/null
"$T/devserver.sh" start >/dev/null
"$T/devclient.sh" start >/dev/null
cat >> "$AP" <<'S'
wait 200
expecthud Beneath the Keep
clearchat
chat /fi status
wait 20
expectchat dating=2
expectitem warden_key
summary
S
timeout 900 bash -c "until grep -q 'SUMMARY passes=' '$LOG' 2>/dev/null; do sleep 5; done"
cp "$LOG" "$T/../docs/test-evidence/e2e_dev_part2_after_restart.log"
grep -h "FAIL\|SUMMARY" "$T/../docs/test-evidence/e2e_dev_part1.log" "$T/../docs/test-evidence/e2e_dev_part2_after_restart.log"
