package dev.voidoutlaws.game;

import dev.voidoutlaws.menu.HackView;
import dev.voidoutlaws.menu.Menu;
import dev.voidoutlaws.menu.PowerView;
import dev.voidoutlaws.reg.ModItems;
import dev.voidoutlaws.state.CrewState;
import dev.voidoutlaws.state.Job;
import dev.voidoutlaws.world.Loc;
import dev.voidoutlaws.world.Structures;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Heists (bank vault) and boarding (derelict freighter). */
public final class Missions {
    private Missions() {}

    public static int hackRound = 0;   // 0..3
    public static int powerRound = 0;  // 0..2
    private static boolean heistAlarm, boardingAlarm;
    private static int waveTimer;

    static void reset() {
        heistAlarm = false;
        boardingAlarm = false;
        waveTimer = 0;
    }

    public static void resetHeist() {
        hackRound = 0;
        heistAlarm = false;
    }

    public static void resetBoarding() {
        powerRound = 0;
        boardingAlarm = false;
    }

    private static Job job(Job.Type t) {
        Job j = Game.S().job;
        return j != null && j.type == t ? j : null;
    }

    // ------------------------------------------------------------------ heist
    public static void vaultTerminal(ServerPlayerEntity p) {
        Job j = job(Job.Type.HEIST);
        if (j == null) {
            Game.msg(p, "The terminal chirps: \"Welcome to Lumen Trust! Have a lawful day.\" (You need a heist job to crack this.)", Formatting.GRAY);
            return;
        }
        if (j.phase >= 2) {
            Game.msg(p, "The vault is already open. Grab the loot from the crates inside!", Formatting.YELLOW);
            return;
        }
        if (!heistAlarm) {
            heistAlarm = true;
            waveTimer = 0;
            if (j.phase == 0) {
                j.phase = 1;
                Game.S().markDirty();
                Heat.add(5, "bank alarm");
            }
            List<ServerPlayerEntity> here = Game.in(Loc.MERIDIAN);
            Game.title(here, Game.t("ALARM!", Formatting.RED), Game.t("Hacker: crack the terminal. Everyone else: hold off the troopers!", Formatting.WHITE));
            Game.sound(here, SoundEvents.BLOCK_BELL_USE, 1f, 0.5f);
        }
        Menu.open(p, new HackView());
    }

    public static void hackSolved(ServerPlayerEntity p) {
        Job j = job(Job.Type.HEIST);
        if (j == null) return;
        hackRound++;
        List<ServerPlayerEntity> here = Game.in(Loc.MERIDIAN);
        if (hackRound >= 3) {
            j.phase = 2;
            heistAlarm = false;
            Game.S().markDirty();
            Structures.openVault(Game.server);
            Game.title(here, Game.t("VAULT OPEN!", Formatting.GOLD), Game.t("Grab the loot from the crates inside!", Formatting.WHITE));
            Game.sound(here, SoundEvents.BLOCK_IRON_DOOR_OPEN, 1f, 0.5f);
            p.closeHandledScreen();
        } else {
            Game.to(here, "Hack progress: " + hackRound + "/3 locks cracked by " + Game.name(p) + "!", Formatting.YELLOW);
        }
    }

    // ------------------------------------------------------------------ boarding
    public static void bulkhead(ServerPlayerEntity p) {
        Job j = job(Job.Type.BOARDING);
        if (j == null) {
            Game.msg(p, "The panel is dead. (You need a boarding job.)", Formatting.GRAY);
            return;
        }
        if (j.phase >= 1) {
            Game.msg(p, "The bulkhead is already open.", Formatting.YELLOW);
            return;
        }
        if (!boardingAlarm) {
            boardingAlarm = true;
            waveTimer = 0;
            List<ServerPlayerEntity> here = Game.in(Loc.DERELICT);
            Game.title(here, Game.t("SCAVENGERS!", Formatting.RED), Game.t("Someone reroute the power - everyone else, defend!", Formatting.WHITE));
            Game.sound(here, SoundEvents.EVENT_RAID_HORN.value(), 0.6f, 1.4f);
        }
        Menu.open(p, new PowerView());
    }

    public static void powerSolved(ServerPlayerEntity p) {
        Job j = job(Job.Type.BOARDING);
        if (j == null) return;
        powerRound++;
        List<ServerPlayerEntity> here = Game.in(Loc.DERELICT);
        if (powerRound >= 2) {
            j.phase = 1;
            boardingAlarm = false;
            Game.S().markDirty();
            Structures.openBulkhead(Game.server);
            Game.title(here, Game.t("BULKHEAD OPEN!", Formatting.GREEN), Game.t("Search the crates in the cargo deck", Formatting.WHITE));
            Game.sound(here, SoundEvents.BLOCK_IRON_DOOR_OPEN, 1f, 0.7f);
            p.closeHandledScreen();
        } else {
            Game.to(here, "Power relay " + powerRound + "/2 rerouted by " + Game.name(p) + "!", Formatting.YELLOW);
        }
    }

    // ------------------------------------------------------------------ crates
    public static void crate(ServerPlayerEntity p, BlockPos pos) {
        CrewState s = Game.S();
        Loc l = Game.locOf(p);
        long key = pos.asLong();
        if (l == Loc.MERIDIAN) {
            Job j = job(Job.Type.HEIST);
            if (j == null || j.phase < 2) {
                Game.msg(p, "A locked strongbox. Locked very, very firmly.", Formatting.GRAY);
                return;
            }
            if (s.openedCrates.contains(key)) { Game.msg(p, "Empty. Someone already robbed this one. (Was it you?)", Formatting.GRAY); return; }
            s.openedCrates.add(key);
            p.getInventory().offerOrDrop(new ItemStack(ModItems.STOLEN_TECH, 2));
            j.bonus += 90;
            boolean first = j.phase < 3;
            j.phase = 3;
            s.markDirty();
            Game.sound(p, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.8f);
            Game.to(Game.in(Loc.MERIDIAN), Game.name(p) + " grabbed loot: 2x Stolen Tech and 90 credits for the pot!", Formatting.GOLD);
            if (first) {
                Story.onVaultLooted(p);
                Game.title(Game.in(Loc.MERIDIAN), Game.t("GET OUT!", Formatting.GOLD), Game.t("Back to the ship and JUMP AWAY to finish the heist", Formatting.WHITE));
            }
            return;
        }
        if (l == Loc.DERELICT) {
            Job j = job(Job.Type.BOARDING);
            if (j == null) { Game.msg(p, "Just rust and a very old sandwich.", Formatting.GRAY); return; }
            if (s.openedCrates.contains(key)) { Game.msg(p, "Already searched. Just packing foam now.", Formatting.GRAY); return; }
            s.openedCrates.add(key);
            p.getInventory().offerOrDrop(new ItemStack(ModItems.GRUMBLEBACK_EGG, 1));
            j.bonus += 40;
            if (Game.R.nextBoolean()) p.getInventory().offerOrDrop(new ItemStack(ModItems.VOID_DUST, 2));
            s.markDirty();
            Game.sound(p, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            int found = s.openedCrates.size();
            Game.to(Game.in(Loc.DERELICT), Game.name(p) + " found a Grumbleback Egg! (" + found + "/3) +40 salvage credits.", Formatting.GOLD);
            if (found >= 3) Game.to(Game.in(Loc.DERELICT), "That's all of them! Back to the ship and deliver the eggs to " + j.dest.display + ".", Formatting.GREEN);
            return;
        }
        Game.msg(p, "It's empty.", Formatting.GRAY);
    }

    // ------------------------------------------------------------------ ticking
    static void tickSecond(MinecraftServer server) {
        CrewState s = Game.S();
        if (heistAlarm && !Game.in(Loc.MERIDIAN).isEmpty()) {
            if (waveTimer++ % 12 == 0) {
                ServerWorld w = server.getWorld(Loc.MERIDIAN.key);
                int n = 1 + Game.in(Loc.MERIDIAN).size() / 2;
                List<BlockPos> pts = new ArrayList<>();
                for (int[] sp : Structures.BANK_SPAWNS) pts.add(Structures.abs(s, Loc.MERIDIAN, sp));
                if (w != null) Mobs.spawnTroopers(w, pts, n, 8);
            }
        }
        if (boardingAlarm && !Game.in(Loc.DERELICT).isEmpty()) {
            if (waveTimer++ % 14 == 0) {
                ServerWorld w = server.getWorld(Loc.DERELICT.key);
                int n = 1 + Game.in(Loc.DERELICT).size() / 2;
                List<BlockPos> pts = new ArrayList<>();
                for (int[] sp : Structures.DERELICT_SPAWNS) pts.add(Structures.abs(s, Loc.DERELICT, sp));
                if (w != null) {
                    Mobs.spawnPirates(w, pts, n, 6);
                    Game.to(Game.in(Loc.DERELICT), "Scavengers crawl out of the vents!", Formatting.RED);
                }
            }
        }
    }

    /** Called when the ship leaves a location. Returns true if a police chase must happen (heist escape). */
    static boolean onShipLeave(Loc from) {
        Job j = Game.S().job;
        if (from == Loc.MERIDIAN) heistAlarm = false;
        if (from == Loc.DERELICT) boardingAlarm = false;
        return from == Loc.MERIDIAN && j != null && j.type == Job.Type.HEIST && j.phase >= 3;
    }

    static void onArrive(MinecraftServer server, Loc dest) {
        Job j = Game.S().job;
        if (j == null) return;
        List<ServerPlayerEntity> aboard = Game.aboard();
        if (dest == Loc.MERIDIAN && j.type == Job.Type.HEIST && j.phase < 3) {
            Game.to(aboard, "Heist tip: the LUMEN TRUST BANK is just east of the landing pad. One crewmate uses the VAULT TERMINAL inside; everyone else guards them.", Formatting.YELLOW);
        }
        if (dest == Loc.DERELICT && j.type == Job.Type.BOARDING) {
            Game.to(aboard, "Boarding tip: go through the docking tube. One crewmate uses the BULKHEAD PANEL; everyone else keeps the scavengers busy.", Formatting.YELLOW);
        }
    }
}
