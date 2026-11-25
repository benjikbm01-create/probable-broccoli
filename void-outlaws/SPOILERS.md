# ⚠️ SPOILERS — DEVELOPERS ONLY ⚠️

> **PLAYERS: STOP READING.** This file describes the whole story, every twist, all three endings,
> every mission and every secret number. It exists so developers can maintain the mod.
> If you want to enjoy the game, close this file now and read `README.md` instead.

---

## 1. Architecture overview

Everything is **server-side logic + vanilla client UI**, so nothing needs custom client code:

| Concern | How it's done |
|---|---|
| Menus (star map, stations, shop, cards, mini-games) | `menu/Menu` = a `GenericContainerScreenHandler` whose slots are item "buttons". Clicks are intercepted in `onSlotClick`; items can never be taken. Views (`menu/*View`) re-render every 2 ticks, so animated mini-games work. |
| Locations | 6 data-pack dimensions in `data/voidoutlaws/dimension` (+ 3 dimension types). |
| Buildings | Built in code by `world/Structures` with the `world/Build` helper — no NBT structure files. Built lazily the first time a location is needed; tracked by `CrewState.built` (`<loc>:v1`). |
| Persistent data | `state/CrewState` (a `PersistentState` on the overworld, file `data/voidoutlaws_crew.dat`). |
| HUD | Vanilla scoreboard sidebar (`game/Sidebar`), boss bars (jump progress, bounty hunters), titles and action bar. |
| NPCs | Vanilla villagers with NoAI + command tag `vo_npc_<id>`, kept in place by `game/Npcs`. Right-click handled by `UseEntityCallback`. |
| Enemies | Vanilla mobs in costume with tags `vo_mob` + type tag (`game/Mobs`). |
| Companion | A renamed, invulnerable allay (`game/Companion`). |
| Books | Custom items carrying a `WRITTEN_BOOK_CONTENT` component, opened with `ServerPlayerEntity.useBook`. |

Main tick: `Game.tick` → scheduler, jumps (every tick), menus, then once a second: sidebar, heat decay,
missions (enemy waves), mobs (hunter bar, disguise), tutorial, gear (disguise timer); NPC upkeep every 5 s.

Drop-in/drop-out: roles are transient (`game/Roles`) and freed when a screen closes / player leaves.
All shared progress is in `CrewState`. Per-player data: wallet, tutorial step, grudge, disguise timer, companion id.

## 2. Locations (dimensions)

| Loc | Dimension | Generator | Notes |
|---|---|---|---|
| Ship interior "Lucky Larceny" | `voidoutlaws:ship` | void (flat, air) | type `space`: fixed night (stars), no mob spawns. Built at server start at (0,100,0). |
| Meridian Prime (lawful core world) | `voidoutlaws:meridian_prime` | flat plains | fixed noon. Pad, Customs Office (Officer Pell + Fine Kiosk), Lumen Trust Bank (heist), Dead Drop alley. |
| Dustreach (lawless frontier) | `voidoutlaws:dustreach` | overworld noise, fixed badlands biome | fixed dusk, land is flattened around the pad (pad height clamped 64–140). Cantina "The Thirsty Comet" (Old Mags, Job Board, Card Table, Black Market), shacks. |
| The Gilded Maw (crime-lord station) | `voidoutlaws:gilded_maw` | void | Floating disc. Hall with the Baroness's golden throne, Job Board, Black Market, Card Table. |
| The Burrow (asteroid hideout) | `voidoutlaws:burrow` | void | Hollow asteroid; Heat decays fast here, free hull repair on arrival. Two vanilla chests, crafting table, furnace. |
| Derelict Freighter | `voidoutlaws:derelict` | void | Only on the star map while a BOARDING job is active. Rebuilt (door + crates reset) when a boarding job is accepted. |

Ship layout (local to 0,100,0; front = −Z): bridge (Ship Console at 0,0,−12; Gunnery Station at −5,0,−9),
common room (Loot Locker 5,0,−3; spawn/med bay 0.5,0,−4.5), cargo bay (2× Cargo Hold at x=5;
Hidden Compartment = floor block at −3,−1,6; Hatch in the left wall at −6,0..1,5), engine room (Engineering Panel 0,0,13).

Block protection: inside each location's protected box, survival players can't break any block type used by the builders
(`Build.PALETTE`, filled by a dry run at mod init). All mod blocks are unbreakable anyway.

## 3. Jumps and jump events (`game/Jumps`, `game/JumpEvent`)

- Ship Console while docked = star map. Clicking a destination starts a jump: ~20 s (400 ticks; 460 to the Derelict).
  Players not aboard are left behind (message); a Docking Beacon shuttles them aboard after 3 s once the ship is docked.
- The timer pauses while an event runs. Boss bar shows ETA / event.
- Event planning at jump start:
  - First-jump newbies aboard (tutorial step ≤ JUMP): no events.
  - Tutorial player at step SCAN aboard: forced tutorial **scan**, then a forced easy tutorial **pirate attack** 100 ticks after.
  - Escaping Meridian with heist loot: forced **police chase**.
  - Otherwise: P(scan) = 0.12 + heat/160 (+0.35 if to/from Meridian; ×0.5 to the Burrow); at Heat ≥ 70, 18% chance of a chase instead.
    P(pirates) = 0.22 + max(0, −Rattlejaw rep)/250 (+0.3 to the Derelict). 15% salvage if fewer than 2 events. Max 2 events.
- **Scan** (pilot at console, 20 s to answer, tutorial 45 s): COMPLY / BRIBE / RUN.
  - Scan searches Cargo Hold + pockets of everyone aboard (disguised players skipped; companion 30% chance to "swallow" one stack).
    Hidden Compartment is found only with chance max(0,(heat−50)/200) (doubled on strict scans; never in the tutorial).
  - Found: confiscated, pot fined up to 50, Concordat −5, **Heat +15**. Clean: Heat −2.
  - Bribe: cost 40 + 3×heat, ±Concordat rep (min 20), from the pilot's wallet. 85% success (50% at Heat ≥ 80). Fail = strict scan + Heat +5. Disabled in tutorial.
  - Run: Heat +6 and a police chase. Disabled in tutorial.
  - No answer: auto-comply.
- **Pirate attack** (30 s, tutorial 60 s): gunner grid (9×5) spawns red fighters; click before they expire.
  Squadron HP = 5 + 2×(crew−1) (tutorial 4). Expired fighter = shot: consumes a pilot dodge or deals 5 hull (tutorial 2) and may add a spark.
  Pilot timing bar: success = +2 dodges (max 4). Mechanic "Boost Shields" = +1 dodge (8 s cooldown).
  Win: 25–64 cr salvage to pot, Rattlejaw −2. Timeout: −8 hull. Hull 0: "disabled" — pirates take ~1/3 of Cargo Hold contraband stacks and 20% of the pot, hull set to 25.
  Hull < 50 once per attack (non-tutorial): pirate **boarders** spawn in the cargo bay (removed on arrival).
- **Police chase** (26 s): need 5 escape points. Pilot success +1, drone hit +0.5, expired drone −0.5, mechanic overcharge +1 (cooldown).
  Escape: Heat +4, Concordat −2. Caught: Heat +8 + strict scan.
- **Salvage** (15 s): one pilot success = 2–4 random contraband into the Cargo Hold.
- **Sparks** (engineering): each spark drains 1 hull per 5 s during the jump; mechanic click = remove, +2 hull. Cleared on arrival.

Roles are simply "whoever has that station's screen open": PILOT (console), GUNNER (gunnery), MECHANIC (engineering). One player per station.

## 4. Heat (`game/Heat`)

0–100, shared. Levels: Clean <20, Noticed 20+, Wanted 40+, Hunted 60+, MOST WANTED 80+.

| Raises | Amount |
|---|---|
| Smuggling delivery | +6 (+10 if delivered to Meridian) |
| Tutorial delivery | +5 |
| Contraband found in scan | +15 |
| Ran from scan | +6 |
| Failed bribe | +5 |
| Escaped chase / caught | +4 / +8 |
| Bank alarm (heist start) | +5 |
| Heist complete | +25 |
| Boarding job delivered | +12 |
| Trooper knocked out | +1 (1/3 chance) |

| Lowers | Amount |
|---|---|
| Docked at the Burrow | −1 per 15 s (with someone online) |
| Docked elsewhere (not Meridian) | −1 per 120 s |
| Clean scan / successful bribe | −2 / −1 |
| Fine Kiosk or Officer Pell (Meridian) | −15 for 30+4×heat cr, or −40 for 2.5× that |
| Transponder Chip swap (Engineering Panel) | −35 |
| Defeating a bounty hunter | −10 |
| Ending "Clean Slate" | set to 0 |

Effects: scan chance scales with Heat; at Heat ≥ 40, Concordat Troopers patrol the Meridian pad on arrival
(2 + (heat−40)/20); at Heat ≥ 60, on arrival anywhere except the Burrow, after ≥ 2 jumps since the last one,
a bounty hunter appears 10 s after docking with 40% chance (75% at ≥ 80); high Heat also makes the Hidden Compartment findable.

## 5. Jobs (`game/Jobs`)

One active crew job. Boards: Dustreach (Free Folk client) and Gilded Maw (Syndicate client), 3 smuggling offers each, refreshed every 5 min.
- **Smuggle**: random contraband (Void Dust, Glowmoss, Stolen Tech, Hum Crystal) ×3–8 to another location; pay = qty×22 (×34 to Meridian) + 40–69. Cargo goes to the accepting player.
  Deliver at the destination's Job Board or Meridian's Dead Drop; delivery counts pockets + Cargo Hold + Hidden Compartment. +5 rep with the client.
- **Tutorial job**: shown first on the Dustreach board to players at tutorial step TAKE_JOB: 4 Glowmoss → Gilded Maw, 120 cr.
- **Boarding** (Gilded Maw board once story ≥ 4, or story job): Derelict appears on the star map. Use the Bulkhead Panel → "reroute power"
  (lights-out puzzle, 2 relays: 5 then 7 cells) while Rattlejaw scavengers spawn in waves every 14 s (cap 6). Bulkhead opens → 3 crates give
  1 Grumbleback Egg each (+40 cr bonus, 50% +2 Void Dust). Deliver 3 eggs at the Gilded Maw board. Pay 260 (story 300) + bonus, Heat +12, Syndicate +6, Rattlejaw −8.
- **Heist** (Dustreach board once story ≥ 6 and cooldown over, or story job): Lumen Trust Bank on Meridian. Vault Terminal → "memory pattern" hack
  (3 locks, 3/4/5 lights). Starting triggers the alarm (Heat +5) and trooper waves every 12 s (cap 8). Taking damage while hacking closes the hacker's screen
  (progress per lock kept). Vault door (gold) opens → 3 strongboxes: each 2 Stolen Tech + 90 cr bonus. Then jump away → forced police chase →
  on arrival anywhere else the heist completes: 400 (story 450) + bonus, Heat +25, Syndicate +8, Concordat −12, 20-minute (1 in-game day) cooldown.
- Abandon (right-click): −3 client rep; story jobs can't be abandoned.

## 6. Loot split and betrayal (`game/Loot`)

Job pay goes to the **Crew Pot**. Loot Locker (ship):
- **Split evenly** among online players.
- **Skim**: take 20% first, split the rest; 50% chance everyone is told ("pockets jingling suspiciously") and the skimmer glows 60 s.
- **Grab it all and run** (right-click): take 60%, others share 40%. Title "BACKSTABBER!", glowing 3 min, a **grudge** (next 2 shares taxed 25%,
  given to honest crewmates), and every other online player gets 4 **Space Tomatoes** (thrown snowballs — harmless knockback).
- Forgiveness: "Buy the crew a round" (30 cr) at any Card Table clears your grudge and gives everyone Nebula Noodles.

## 7. Factions / reputation

`concordat` (law), `syndicate` (Brightcoil Syndicate, Gilded Maw), `freefolk` (Dustreach), `rattlejaw` (pirates). Range −100..100.
Effects: Concordat rep changes bribe cost; Free Folk / Syndicate rep give discounts (price × (1 − rep/400)) at the Dustreach / Maw black markets;
negative Rattlejaw rep raises pirate-attack chance. Shown in `/crew`.

## 8. Bounty hunters

Strays in netherite helmets, 45 HP + 15 per extra player (story hunter 70+), Resistance I + Speed I, boss bar.
Random names: Sable Quint, Mother Gorse, The Tally Man, Dex Marrowind, Ivo "Nine Lives" Krane, Jessamy Coil.
Defeat: +100 cr pot, Heat −10. Jumping away removes them. Troopers/pirates/hunters in a location are removed when the ship leaves it.

## 9. The story campaign (`game/Story`) — BIG SPOILERS

Characters (all original):
- **Old Mags** — bartender of the Thirsty Comet cantina on Dustreach. Tips and nudges.
- **Baroness Kessa Brightcoil** — crime lady of the Gilded Maw, head of the Brightcoil Syndicate.
- **Customs Officer Pell** — Concordat customs on Meridian Prime; also opens the fine kiosk.
- **Rook Venn** — the Baroness's personal bounty hunter.

Stages (`CrewState.story`):
0. **Tutorial** — finishing the tutorial (or any job) → 1.
1. **Meet the Baroness** — she reveals the Lucky Larceny was bought with *her* money; the crew owes her. → 2.
2. **Debt** — finish 2 jobs, report back. She sends the crew for Grumbleback eggs (story boarding job). → 3.
3. **The Egg Job** — board the derelict, deliver 3 eggs. Reward: every player gets a **Companion Whistle** ("she lets you keep a hatchling"). → 4.
4. **Talk to her** — she orders the **Lumen Vault heist** to steal a ledger, and tells you not to read it. → 5.
5. **The Lumen Vault** — the first strongbox looted gives the **Lumen Ledger** (readable book). → 6.
6. **THE TWIST** — the ledger shows the Baroness has a deal with the Concordat: she will hand over the crew (with Syndicate crimes planted on their ship)
   so the Concordat clears *her* record, and Rook Venn will "collect" them once they deliver the ledger. Choices:
   - **Hand it to the Baroness** → "Thank you, darlings... Rook, collect them." → stage 7 **Ambush**: defeat Rook Venn on the Gilded Maw
     (respawns on each arrival there if lost; auto-resolved on Peaceful) → **Ending "Loyal to a Fault"** (+500 cr; she grudgingly pays up).
   - **Keep it as insurance** (blackmail the Baroness) → **Ending "Insurance Policy"** (+800 cr hush money, Syndicate −20).
   - **Turn it in to Officer Pell** on Meridian Prime → **Ending "Clean Slate"** (Heat → 0, +300 cr reward, Concordat +40, Syndicate −50; the Baroness ends up under "house arrest" on her own station).
8. **Freeplay** — "THE END" title; boarding and heist jobs keep appearing on the boards. NPCs have ending-specific lines.

The ledger is not strictly required to make the choice (avoids soft-locks if it's lost); it is removed when the choice is made.

## 10. Tutorial (`game/Tutorial`)

Per player, lenient (advances on whatever the crew does, so late joiners don't get stuck):
Welcome → open Ship Console → jump anywhere → leave via Hatch → take a job → hide contraband in the Hidden Compartment →
jump & COMPLY with the forced scan → Heat explanation → forced easy pirate attack (pilot + gunner explained) → deliver → split the pot → done (+50 cr).
Reminders in the action bar every 30 s (toggle with `/tutorial hints off`). Auto-skips steps if the crew already did them
(e.g. a job already active, cargo already hidden, pot empty). `/tutorial restart|skip`.

## 11. Gear and economy

Wallets are per player (start 100 cr; tutorial +50). Death: respawn in the ship's med bay, bill = 10% of wallet (max 40). `keepInventory` is enabled once.

Black Market (both): Blaster 120, Jetpack 180, Grappling Hook 140, Disguise Kit 60, Transponder Chip 150, Hull Patch 40, Nebula Noodles ×2 8,
Space Tomato ×4 5, Companion Whistle 200. Dustreach also sells Void Dust 25 / Glowmoss 20; the Maw sells Stolen Tech 45 / Hum Crystal 35 / Grumbleback Egg 60.
Selling contraband (per item, Maw / Dustreach): Void Dust 38/15, Glowmoss 30/12, Stolen Tech 27/60, Hum Crystal 21/48, Egg 36/70 — so trading routes pay.

- **Blaster**: hitscan 40 blocks, 6 damage to mobs; on players it only stuns (Slowness III + Glowing 3 s). 0.6 s cooldown.
- **Jetpack**: upward + forward boost, short slow-fall, 1.5 s cooldown.
- **Grappling Hook**: raycast 32 blocks, yanks you toward the block.
- **Disguise Kit**: 5 min; troopers/pirates drop you as a target (hunters don't), pocket scans skip you.
- **Transponder Chip**: used at the Engineering Panel, new ship ID, Heat −35.
- **Hull Patch**: +30% hull (item use aboard or Engineering Panel).
- **Companion Whistle**: summons/creates your allay companion (random name: Zib, Pip, Blorbo, Nuggin, Squee, Mochi, Glimmer, Bean). Follows you on teleports. 30% chance per scan to hide one contraband stack in your pockets.
- **Comet Cards** (Card Table): higher/lower vs the house, bets 10/25/50/100, ×1.5 per correct guess, ties redraw, auto cash-out at 5, closing mid-hand cashes out (or refunds if no guess yet).

## 12. Files

```
src/main/java/dev/voidoutlaws/
  VoidOutlaws.java          mod entrypoint
  reg/                      blocks (OutlawBlock + Kind enum), items (GearItem), item group
  state/                    CrewState (saved data), Job
  world/                    Loc (locations), Build (builder helper), Structures (all buildings + positions)
  game/                     Game hub, Hooks (events), Jumps/JumpEvent, Scans, Heat, Jobs, Missions, Mobs,
                            Story, Npcs, Tutorial, Loot, Gear, Companion, Books, Sidebar, Roles, Tp, Sched, Interact
  menu/                     Menu (screen handler), View, Btn, Slots, and every *View
  cmd/Commands.java         /tutorial /guide /crew /pay /unstuck /outlaws
src/main/resources/
  data/voidoutlaws/dimension*/   dimensions
  assets/voidoutlaws/            models, blockstates, textures, lang, icon
tools/gen_textures.py        regenerates all textures
```

Testing status: the project compiles and builds with `./gradlew build`. It has not been play-tested.
