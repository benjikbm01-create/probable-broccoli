"""Generates the autopilot script for the end-to-end client playthrough.
Usage: python3 gen.py <layout_dump.json> <out_script> [part]   part = main (default)"""
import json, sys, os, math
sys.path.insert(0, "/tmp/claude-0/work")
L = json.load(open(sys.argv[1])); out = open(sys.argv[2], "w")
P = L["places"]
def w(*lines):
    for l in lines: out.write(l + "\n")
def wait(t): w(f"wait {t}")
def tp(x, y, z, yaw=0, pitch=0): w(f"chat /tp Dev {x} {y} {z} {yaw} {pitch}"); wait(30)
def warp(place): w(f"chat /fi warp {place}"); wait(35)
def talk(npc, locate=True):
    if locate: w(f"chat /fi npc {npc}"); wait(45)
    w(f"talk {npc}", "screen DialogueScreen 80"); wait(10)
def say(*choices):
    for c in choices:
        wait(12)
        if isinstance(c, int): w(f"choose {c}")
        else: w(f"choosetext {c}")
    wait(15); w("screen none 60")
def opt(c):
    wait(12); w(f"choose {c}" if isinstance(c, int) else f"choosetext {c}")
def kill_mobs(): w("chat /kill @e[tag=fi_enc]", "chat /kill @e[tag=fi_boss_add]"); wait(10)

w("# ---- The Forgotten Island: end-to-end playthrough ----")
w("screen IntroScreen 600"); wait(80); w("shot e2e_01_intro"); wait(20)
w("intro"); wait(80)
w("expecthud Letter from Nowhere")
w("expectitem journal", "expectitem letter")
w("chat /effect give Dev minecraft:resistance infinite 4 true", "chat /effect give Dev minecraft:saturation infinite 1 true")
w("shot e2e_02_arrival")
# --- act 1 ---
w("chat /time set 1000"); warp("cp_dock")
talk("tobias", locate=False)
w("shot e2e_03_dialogue_tobias")
say("Gullhaven", "letter", 1, 1, 1)
w("journal", "screen JournalScreen 60"); w("shot e2e_04_journal"); w("close"); wait(10)
w("hint"); wait(20); w("expectui hint"); w("shot e2e_05_hint")
talk("hester"); say(1, 1, 1, 1, 1, "be off")
w("expectitem bread")
talk("ione"); say("read a seal", 1, 1, 1, 1)
wait(30); w("expecthud Old Road")
talk("hester"); say("old song", 1, 1, 1, 1, "be off")
w("expectitem lullaby")
# --- Wren: meeting, guidance, spark, quest (romance path) ---
w("chat /time set 2000")
talk("wren"); say(1, 1, "Kestrel Keep", 1)
talk("wren"); say("Singing Stones", "Hester taught", 1)
talk("wren"); say("nicer than", "Maybe you should", 1)
talk("wren"); say("far away", 1, "get it", 1)
# --- the Singing Stones ---
warp("stones_center"); w("shot e2e_06_stones")
for animal in ["heron", "stag", "owl", "hare"]:
    b = L["stone_buttons"][animal]
    w(f"lookat {b[0]} {b[1]} {b[2]}", f"use {b[0]} {b[1]} {b[2]}"); wait(15)
wait(60)
g = L["gates"]["thornwall"]; w(f"expectblock {g[0]} {g[1]} {g[2]} air")
w("expecthud Silent Keep")
# --- Wren's bow from the Hollow Oak, then the firefly date ---
hx, hy, hz = math.floor(P["hollow_oak"][0]), math.floor(P["hollow_oak"][1]), math.floor(P["hollow_oak"][2])
tp(int(hx), int(hy), int(hz) - 7, 180, 30); wait(40); kill_mobs()
w(f"use {int(hx)} {int(hy)} {int(hz) - 10}", "screen GenericContainerScreen 60", "loot"); wait(20)
w("expectitem mother_bow"); kill_mobs()
w("chat /time set 2000")
talk("wren"); say("Hollow Oak", 1, 1, "more than friends", "Wait for nightfall")
wait(80)
gp = P["glade_player"]; tp(gp[0], gp[1], gp[2], 45, 10); wait(60)
w("chat /fi effect event:snap:wren"); wait(20)
talk("wren", locate=False); w("shot e2e_07_glade_date")
say(1, 1, 1, "will you be mine", 1)
w("clearchat", "chat /fi status"); wait(20); w("expectchat dating=1")
w("chat /time set 3000")
# --- Kestrel Keep ---
warp("cp_keep_gate"); w("shot e2e_08_keep_gate"); warp("keep_court"); wait(40); kill_mobs()
warp("sun_court"); w("shot e2e_09_sun_court")
for m in L["mirrors"]:
    tp(m[0] + 0.5, m[1], m[2] + 1.5, 180, 30)
    w(f"lookat {m[0]} {m[1]} {m[2]}", f"use {m[0]} {m[1]} {m[2]}"); wait(10)
wait(70)
g = L["gates"]["tower_door"]; w(f"expectblock {g[0]} {g[1]} {g[2]} air")
warp("lantern_room"); wait(40)
w("expecthud Beneath the Keep"); w("shot e2e_10_empty_lantern")
lx, ly, lz = L["lantern"]
w(f"use {lx + 2} {ly} {lz + 2}", "screen GenericContainerScreen 60", "loot"); wait(20)
w("expectitem warden_key")
# --- the Undercroft ---
warp("keep_court"); kill_mobs()
w("hold warden_key"); g = L["gates"]["cistern"]
tp(g[0] + 2, g[1] + 1, g[2], 90, 50); w(f"lookat 20 {g[1]} -160", f"use 20 {g[1]} -160"); wait(20)
w(f"expectblock 20 {g[1]} -160 air")
warp("cp_undercroft"); wait(40); kill_mobs(); w("shot e2e_11_cistern")
for i in (1, 2, 3):
    warp(f"echo_pool_{i}"); wait(20); kill_mobs()
    w("screen DialogueScreen 100")
    if i == 3: w("shot e2e_12_echo")
    say(1, 1, 1)
g = L["gates"]["echo_door"]; w(f"expectblock {g[0]} {g[1]} {g[2]} air")
# gliding stones, pushed through the real block interaction
from slide import solve
grid = ["#########", "#......##", "#...S#..#", "#.S.....#", "#....#..#", "#.......#", "##.S..o##", "#..#o.o##", "#########"]
d, path, _ = solve(grid)
sx, sy, sz = L["slide_origin"]
stones = [(x, z) for z, row in enumerate(grid) for x, c in enumerate(row) if c == "S"]
stones = sorted(stones)
w("shot e2e_13_slide_before")
for (i, (dx, dz)) in path:
    stones.sort()
    x, z = stones[i]
    bx, bz = x - dx, z - dz
    standY = sy + (1 if (bx, bz) in stones else 0)
    tp(sx + bx + 0.5, standY, sz + bz + 0.5, 0, 30)
    w(f"lookat {sx + x} {sy} {sz + z}", f"use {sx + x} {sy} {sz + z}"); wait(10)
    # update the model
    S = set(stones); nx, nz = x, z
    while True:
        tx, tz = nx + dx, nz + dz
        if grid[tz][tx] == "#" or (tx, tz) in S: break
        nx, nz = tx, tz
    stones[i] = (nx, nz)
wait(70); w("shot e2e_14_slide_after")
g = L["gates"]["vault_door"]; w(f"expectblock {g[0]} {g[1]} {g[2]} air")
w("expecthud Wellspring")
# --- the Drowned Warden (dev 'weak' mode only to keep the automated fight short) ---
w("chat /fi boss weak"); wait(10)
w("chat /fi effect give:kestrel_blade"); wait(10); w("hold kestrel_blade")
warp("cp_wellspring"); wait(30)
bc = P["boss_center"]; tp(bc[0], bc[1], bc[2] - 9, 0, 0); wait(60)
w("shot e2e_15_boss")
for k in range(45):
    w("attack"); wait(22)
wait(20)
w("screen DialogueScreen 400"); w("shot e2e_16_warden")
say(1, 1, "daughter", 1, 1, 1)
w("expectitem ember"); kill_mobs()
# --- the last light ---
warp("lantern_room"); wait(30)
w("hold ember", f"lookat {lx} {ly} {lz}", f"use {lx} {ly} {lz}"); wait(60)
w(f"expectblock {lx} {ly} {lz} beacon"); w("expecthud Homecoming")
w("shot e2e_17_lantern_lit")
wait(400)
w("shot e2e_18_festival")
talk("wren"); say(1, 1)
talk("hester")
for c in (1, "I told him", 1, "pier now"): opt(c)
w("screen CreditsScreen 100"); wait(140); w("shot e2e_19_credits"); wait(200); w("close"); wait(60)
w("expectui toast")
w("clearchat", "chat /fi status"); wait(20); w("expectchat dating=1")
w("summary")
out.close()
