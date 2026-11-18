# Developer Guide (spoiler-free)

Everything here can be used without reading the story. **Don't open `story/` or
`mod/src/main/resources/data/forgotten_island/fi/`** if you want to keep the plot a surprise.

## Developer testing mode
In a singleplayer world, enable commands for the session with **Esc → Open to LAN → Allow Cheats: ON → Start LAN World**
(or use a world copy with cheats enabled). Then:

| Command | What it does |
|---|---|
| `/fi dev on` / `/fi dev off` | **Spoiler-free mode.** Dialogue shows only node ids and option targets; the journal and tracker show objective ids; hints are redacted. Mechanics work normally. |
| `/fi status` | Progress summary: flag count, checkpoint, secrets, relationships, where each resident should be right now. |
| `/fi stage <village\|forest\|keep\|undercroft\|vault\|boss\|lantern>` | Jump to the start of a stage (sets the required progress, opens gates, moves you to the checkpoint). |
| `/fi warp <place>` | Teleport to a named place (tab-completes). |
| `/fi npc <id>` | Teleport to where a resident is scheduled to be (`hester tobias wendel corin wren ione sol`). |
| `/fi time <morning\|noon\|evening\|night\|midnight>` | Change the time of day. |
| `/fi puzzle <stones\|mirrors\|slide> <solve\|reset>` | Solve or reset a puzzle. |
| `/fi boss <start\|abort\|weak>` | Start or abort the final fight; `weak` multiplies damage to the boss ×10 for testing. |
| `/fi flag set\|clear <name>`, `/fi var <name> <n>`, `/fi flags` | Inspect and edit raw progression state. |
| `/fi talk <id>`, `/fi choose <n>` | Drive a conversation from the command line. |
| `/fi hint` | Show the next hint. |
| `/fi reset` | **Reset the adventure** to the very beginning (state, gates, puzzles, encounters, lantern) and replay the intro. |
| `/fi selftest [suite]` | Run the in-world automated tests (see below). |
| `/fi gamemode` | Toggle creative/adventure. |

## Automated tests
| Layer | How to run | What it covers |
|---|---|---|
| Unit tests (JUnit, with Minecraft and the mod bootstrapped via `fabric-loader-junit`) | `tools/gradle-retry.sh test` | condition language, state, relationship clamps, save/load NBT round trip, puzzle solvability (unique mirror solution; 11-push sliding solution), content validation (every reference, every node reachable, every condition flag settable, skins present and layered), **scripted story routes through the real dialogue files** (no romance, one, two, three partners, friend-only, side quests) |
| In-world self-test | `/fi selftest` (or `tools/devserver.sh cmd "fi selftest"`) | real puzzle blocks, gates, encounters, boss phases and death reset, the Ember and lantern, key-item recovery, live dialogue for every resident, quest chain, resident entities, schedule spots physically usable, walking paths between schedule spots, full reset. Writes `selftest_report.txt`. |
| Client end-to-end | `tools/e2e/run_main.sh`, `tools/e2e/dev_and_persist.sh` | a scripted player using the real client UI: intro, dialogue choices, journal, hints, all puzzles via block interaction, a romance with a night-time date, the boss, the ending and credits; developer mode, stage jump, reset; persistence across a full server and client restart |

The client tests run Minecraft under Xvfb with software OpenGL; screenshots go to `mod/run-client/screenshots/`.

## Building
- Mod: `tools/gradle-retry.sh build` → `mod/build/libs/forgotten-island-1.0.0.jar`.
  (`gradle-retry.sh` starts a small local retrying Maven mirror; the sandbox this was built in had a flaky CDN.)
- Content: `python3 tools/content/build.py` regenerates the data JSON from the (spoiler) story sources.
- Skins: `python3 tools/skins/make_skins.py <out_dir> [preview_dir]`.
- World: `tools/package.sh` builds everything into `dist/` from scratch, including a freshly generated island
  (dedicated server + `/fi build`, about 20 seconds of build time).

## Project layout
```
mod/                      Fabric mod (Java 21, Yarn mappings)
  src/main/java/.../core      state, condition language
  src/main/java/.../content   data model + validator (pure Java)
  src/main/java/.../dialogue  server-side dialogue sessions
  src/main/java/.../npc       resident entity, schedules, director
  src/main/java/.../game      quests/journal, puzzles, gates, encounters, boss, story events, commands
  src/main/java/.../build     the island builder (terrain + every structure)
  src/main/java/.../test      in-world self-test
  src/client/java/...         renderers, screens, HUD, fog, autopilot test driver
tools/                    build, packaging, test and content scripts
docs/                     player and developer documentation (spoiler-free)
story/                    ⚠ the private story bible and dialogue sources (spoilers)
dist/                     deliverables (mods + world)
```
