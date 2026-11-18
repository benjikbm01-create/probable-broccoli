#!/usr/bin/env bash
# Runs a Gradle command in mod/, retrying when the network flakes on dependency downloads.
cd "$(dirname "$0")/../mod" || exit 1
# Start the local retrying Maven mirror if it is not running yet.
if ! curl -s -o /dev/null http://127.0.0.1:8765/ 2>/dev/null; then
  nohup python3 ../tools/maven-mirror.py 8765 >/dev/null 2>&1 &
  sleep 1
fi
export FI_MIRROR=1
for i in 1 2 3 4 5 6 7 8; do
  out=$(gradle --no-daemon "$@" 2>&1); code=$?
  if [ $code -eq 0 ]; then echo "$out" | grep -v "Picked up" | tail -25; exit 0; fi
  if echo "$out" | grep -qE "Could not (find|resolve|download|GET|HEAD)|Resource missing|Connection reset|timed out|Premature"; then
    echo "[gradle-retry] network failure on attempt $i, retrying..." >&2; sleep 3; continue
  fi
  echo "$out" | grep -v "Picked up" | tail -60; exit $code
done
echo "$out" | grep -v "Picked up" | tail -40; exit 1
