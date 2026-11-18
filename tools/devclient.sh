#!/usr/bin/env bash
# Dev client helper (software-rendered under Xvfb). Usage:
#   tools/devclient.sh start [server|world <name>]   launch and join the dev server (default) or a singleplayer world
#   tools/devclient.sh do "<autopilot command>"      append a command (see Autopilot.java)
#   tools/devclient.sh stop
DIR="$(cd "$(dirname "$0")/../mod" && pwd)"; RUN="$DIR/run-client"; AP="$RUN/autopilot.txt"
case "$1" in
  start)
    mkdir -p "$RUN"; : > "$AP"; rm -f "$RUN/autopilot.log"
    pgrep -f "Xvfb :99" >/dev/null || (Xvfb :99 -screen 0 1280x720x24 >/dev/null 2>&1 &)
    sleep 1
    # skip first-run screens, sensible test settings
    [ -f "$RUN/options.txt" ] || printf 'onboardAccessibility:false\nnarrator:0\npauseOnLostFocus:false\nrenderDistance:12\nguiScale:2\nsoundCategory_master:0.0\ntutorialStep:none\njoinedFirstServer:true\nskipMultiplayerWarning:true\nfov:0.0\n' > "$RUN/options.txt"
    if [ "$2" = "world" ]; then QP="--quickPlaySingleplayer $3"; else QP="--quickPlayMultiplayer localhost:25565"; fi
    (cd "$DIR" && DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 FI_MIRROR=1 nohup gradle runClient --no-daemon --offline -q -Dfi.autopilot="$AP" --args="--username Dev $QP" > "$RUN/.stdout" 2>&1 &)
    echo "client launching" ;;
  do) echo "$2" >> "$AP" ;;
  stop) echo "quit" >> "$AP"; sleep 8; pkill -f "fabric.dli.env=client"; pkill -f "gradle-cli-main.*runClient"; echo stopped ;;
esac
