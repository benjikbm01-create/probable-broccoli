#!/usr/bin/env bash
# Dev server helper. Usage:
#   tools/devserver.sh start [fresh]   start the build/test server in the background (fresh = delete world)
#   tools/devserver.sh cmd "<command>" send a console command
#   tools/devserver.sh wait "<regex>" [timeout]  wait for a log line
#   tools/devserver.sh stop
DIR="$(cd "$(dirname "$0")/../mod" && pwd)"; RUN="$DIR/run-server"; CMDS="$RUN/.console"
case "$1" in
  start)
    mkdir -p "$RUN"; echo "eula=true" > "$RUN/eula.txt"
    [ "$2" = "fresh" ] && rm -rf "$RUN/world"
    cat > "$RUN/server.properties" <<PROPS
online-mode=false
level-name=world
level-type=minecraft\:flat
generator-settings={"layers"\:[{"block"\:"minecraft\:bedrock","height"\:1},{"block"\:"minecraft\:deepslate","height"\:60},{"block"\:"minecraft\:stone","height"\:52},{"block"\:"minecraft\:sand","height"\:2},{"block"\:"minecraft\:water","height"\:12}],"biome"\:"minecraft\:ocean","features"\:false,"lakes"\:false,"structure_overrides"\:[]}
generate-structures=false
spawn-protection=0
view-distance=12
simulation-distance=8
gamemode=adventure
difficulty=normal
spawn-monsters=false
spawn-animals=false
max-tick-time=-1
enable-rcon=false
motd=Forgotten Island dev
PROPS
    : > "$CMDS"; rm -f "$RUN/logs/latest.log"
    (cd "$DIR" && FI_MIRROR=1 nohup bash -c "tail -n +1 -f '$CMDS' | gradle runServer --no-daemon --offline -q -Dorg.gradle.jvmargs=-Xmx3G" > "$RUN/.stdout" 2>&1 &)
    for i in $(seq 1 240); do grep -q "Done (" "$RUN/logs/latest.log" 2>/dev/null && { echo "server up"; exit 0; }; sleep 1; done
    echo "server did not start"; tail -30 "$RUN/.stdout"; exit 1 ;;
  cmd) echo "$2" >> "$CMDS" ;;
  wait)
    T=${3:-600}; for i in $(seq 1 $T); do grep -qE "$2" "$RUN/logs/latest.log" 2>/dev/null && { grep -E "$2" "$RUN/logs/latest.log" | tail -3; exit 0; }; sleep 1; done
    echo "timeout waiting for $2"; tail -20 "$RUN/logs/latest.log"; exit 1 ;;
  stop)
    echo "stop" >> "$CMDS"
    for i in $(seq 1 120); do pgrep -f "fabric.dli.env=server" >/dev/null || { pkill -f "tail -n +1 -f $CMDS"; echo stopped; exit 0; }; sleep 1; done
    pkill -f "fabric.dli.env=server"; pkill -f "tail -n +1 -f $CMDS"; echo "forced"; exit 0 ;;
esac
