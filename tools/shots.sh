#!/usr/bin/env bash
# Teleport the dev client through a list of viewpoints and screenshot each.
# Usage: tools/shots.sh <time> "name x y z yaw pitch" ...
T="$(dirname "$0")"; TIME=$1; shift
"$T/devserver.sh" cmd "time set $TIME"
"$T/devserver.sh" cmd "gamemode spectator Dev"
"$T/devclient.sh" do "hud off"
for s in "$@"; do
  set -- $s
  "$T/devserver.sh" cmd "tp Dev $2 $3 $4 $5 $6"
  "$T/devclient.sh" do "wait 90"
  "$T/devclient.sh" do "shot $1"
  sleep 16
done
