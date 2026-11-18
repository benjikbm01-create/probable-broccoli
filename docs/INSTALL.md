# Installing The Forgotten Island

You need **Minecraft Java Edition 1.21.1** and the **Fabric** mod loader. Setup takes about five minutes.

## 1. Install Fabric for 1.21.1
1. Download the Fabric installer from <https://fabricmc.net/use/installer/>.
2. Run it, choose **Client**, Minecraft version **1.21.1**, Loader **0.16.14** (or any newer 0.16.x), and click **Install**.
3. Open the Minecraft Launcher once with the new **fabric-loader-1.21.1** profile and close the game again (this creates the folders).

## 2. Add the two mod files
Copy both files from `dist/mods/` into your `mods` folder:

| File | What it is |
|---|---|
| `forgotten-island-1.0.0.jar` | the adventure itself |
| `fabric-api-0.116.7+1.21.1.jar` | Fabric API (required library) |

Where the `mods` folder is:
- **Windows:** `%APPDATA%\.minecraft\mods`
- **macOS:** `~/Library/Application Support/minecraft/mods`
- **Linux:** `~/.minecraft/mods`

(Create the `mods` folder if it doesn't exist.)

## 3. Add the world
Unzip `dist/TheForgottenIsland-world.zip` into your `saves` folder (next to `mods`), so that you have
`saves/The Forgotten Island/level.dat`.

## 4. Play
Start Minecraft with the **fabric-loader-1.21.1** profile → **Singleplayer** → **The Forgotten Island** → **Play Selected World**.

The adventure starts automatically with a title card. Recommended settings: render distance 12+, brightness "Moody" to 50 %, sound on.

## Starting over
Keep an untouched copy of the zip. To replay from the beginning, delete the world from your `saves` folder and unzip it again.
(Developers can also reset in-game; see `DEVELOPER.md`.)

## Troubleshooting
- **"Incompatible mods" or the game won't start:** make sure both jars are in `mods`, you're using the Fabric 1.21.1 profile, and there is no other version of Fabric API in the folder.
- **The world isn't listed:** the folder inside `saves` must directly contain `level.dat` (not another nested folder).
- **Performance mods:** the adventure was tested without other mods. Sodium/Lithium should work, but they weren't tested (see `KNOWN_ISSUES.md`).
