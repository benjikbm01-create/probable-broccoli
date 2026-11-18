#!/usr/bin/env bash
# Builds every deliverable into dist/: the mod jar, Fabric API, and a freshly built, pristine world save.
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; T="$ROOT/tools"; DIST="$ROOT/dist"
python3 "$T/skins/make_skins.py" "$ROOT/mod/src/main/resources/assets/forgotten_island/textures/entity/npc" > /dev/null
python3 "$T/content/build.py"
"$T/gradle-retry.sh" build -q
mkdir -p "$DIST/mods"
rm -f "$DIST"/mods/*.jar
cp "$ROOT/mod/build/libs/forgotten-island-1.0.0.jar" "$DIST/mods/"
FAPI=$(find ~/.gradle/caches/modules-2 -name "fabric-api-0.116.7+1.21.1.jar" | head -1)
cp "$FAPI" "$DIST/mods/"
# fresh island on a clean server
"$T/devclient.sh" stop >/dev/null 2>&1 || true
"$T/devserver.sh" stop >/dev/null || true
"$T/devserver.sh" start fresh >/dev/null
"$T/devserver.sh" cmd "fi build"
"$T/devserver.sh" wait "Island complete" 1200 >/dev/null
"$T/devserver.sh" cmd "save-all flush"; sleep 5
"$T/devserver.sh" stop >/dev/null
W="$DIST/The Forgotten Island"
rm -rf "$W"; cp -r "$ROOT/mod/run-server/world" "$W"
rm -rf "$W/session.lock" "$W/playerdata" "$W/stats" "$W/advancements"
python3 - "$W/level.dat" <<'PY'
import sys, nbtlib
f = nbtlib.load(sys.argv[1]); d = f["Data"]
d["LevelName"] = nbtlib.String("The Forgotten Island")
d["allowCommands"] = nbtlib.Byte(0)
d["GameType"] = nbtlib.Int(2)          # adventure
d["Difficulty"] = nbtlib.Byte(2)       # normal
d["DifficultyLocked"] = nbtlib.Byte(0)
d["hardcore"] = nbtlib.Byte(0)
if "Player" in d: del d["Player"]
f.save()
print("level.dat:", d["LevelName"], "commands", d["allowCommands"], "mode", d["GameType"])
PY
python3 -c "from PIL import Image; Image.open('$ROOT/mod/src/main/resources/assets/forgotten_island/icon.png').resize((64,64)).save('$W/icon.png')"
(cd "$DIST" && rm -f TheForgottenIsland-world.zip && zip -qr TheForgottenIsland-world.zip "The Forgotten Island")
ls -la "$DIST" "$DIST/mods"; du -sh "$W" "$DIST/TheForgottenIsland-world.zip"
