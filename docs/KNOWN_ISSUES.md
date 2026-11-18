# Known issues and limitations

Honest list of what is unresolved, approximate or untested.

## Gameplay / behaviour
1. **Residents sometimes teleport.** Characters walk between nearby schedule spots, but long trips (e.g. forest ↔ village, 50–190 blocks) happen by teleporting when you're not near either end. A few spots can't be walked to at all — two ladder-only lofts (the Archive roof and the boathouse loft) and the harbourmaster's upstairs bedroom. In those cases the resident tries for a moment and then appears there, which can be visible if you're watching. The self-test measures 24 of 29 short routes as walkable.
2. **Seated pose is approximate.** Seated residents use the player "riding" pose, offset by hand. On some chairs they sit slightly high or low.
3. **Boss balance is untested by humans.** The final fight was verified by automation (spawning, phases, adds, spear, surge, death reset, victory), but the scripted player used a ×10 damage test aid. Difficulty for real players hasn't been tuned with playtesters.
4. **Playtime estimate (2–3 h) is a design estimate.** The automated playthrough takes about 10 minutes because it teleports between locations. No human playtest has been done.
5. **Multiplayer isn't supported.** Story state is per-world, not per-player. The adventure is designed for singleplayer.
6. **Creative mode bypasses protection.** If you enable cheats and switch to creative, you can break the island. Adventure mode (the default) protects everything.
7. **Some dates need a time of day.** You're always offered a "wait until evening/nightfall" option, but if you sleep through the night in a bed first, you'll need to wait or rest again.

## Visuals
8. **Rendering was only checked with software OpenGL** (Mesa llvmpipe, 854×480) in a headless environment. It hasn't been seen on a real GPU, at high resolutions, or with shader packs. The sea-fog effect is a custom client fog and may look different with Sodium/Iris or shaders.
9. **Visual polish is uneven.** The village, Keep and forest sites are handcrafted with furnished interiors. Some secondary interiors (barracks, upper floors, cottages) are simpler. The underground halls are intentionally dark.
10. **Reference images weren't available.** None were supplied, so the build standard is my own interpretation (see `ATTRIBUTION.md`).

## Untested
11. **Sound.** The test environment had no audio device. Sound events are vanilla, but nothing was heard.
12. **Official launcher.** The shipped world and jars were tested with the Fabric *production server* launcher and the Fabric *development* client (Loom), not the official Minecraft launcher on Windows or macOS. Installation follows the standard Fabric procedure.
13. **Other mods.** Nothing was tested alongside performance or other content mods.
14. **Very small windows.** At very small window sizes, the longest dialogue option lists and journal pages may need scrolling or a larger GUI scale.
