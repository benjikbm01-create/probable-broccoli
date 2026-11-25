package dev.voidoutlaws.game;

import dev.voidoutlaws.reg.ModItems;
import dev.voidoutlaws.state.CrewState;
import dev.voidoutlaws.state.Job;
import dev.voidoutlaws.world.Loc;
import dev.voidoutlaws.world.Structures;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;

import java.util.*;

/** Job offers, accepting, delivering and payouts. */
public final class Jobs {
    private Jobs() {}

    private static final Map<Loc, List<Job>> OFFERS = new EnumMap<>(Loc.class);
    private static final Map<Loc, Long> OFFERS_AT = new EnumMap<>(Loc.class);

    private static final Item[] SMUGGLE_CARGO = {null, null, null, null};

    private static Item[] cargoPool() {
        if (SMUGGLE_CARGO[0] == null) {
            SMUGGLE_CARGO[0] = ModItems.VOID_DUST;
            SMUGGLE_CARGO[1] = ModItems.GLOWMOSS;
            SMUGGLE_CARGO[2] = ModItems.STOLEN_TECH;
            SMUGGLE_CARGO[3] = ModItems.HUM_CRYSTAL;
        }
        return SMUGGLE_CARGO;
    }

    static void reset() {
        OFFERS.clear();
        OFFERS_AT.clear();
    }

    public static String factionName(String f) {
        return switch (f) {
            case "syndicate" -> "Brightcoil Syndicate";
            case "freefolk" -> "Dustreach Free Folk";
            case "concordat" -> "The Concordat";
            case "rattlejaw" -> "Rattlejaw Pirates";
            default -> f;
        };
    }

    // ------------------------------------------------------------------ offers
    public static List<Job> offers(Loc board, ServerPlayerEntity viewer) {
        Long at = OFFERS_AT.get(board);
        if (at == null || Game.tick - at > 6000 || OFFERS.getOrDefault(board, List.of()).isEmpty()) {
            OFFERS.put(board, generate(board));
            OFFERS_AT.put(board, Game.tick);
        }
        List<Job> list = new ArrayList<>(OFFERS.get(board));
        if (board == Loc.DUSTREACH && Game.S().job == null && viewer != null && Tutorial.step(viewer) == Tutorial.TAKE_JOB) {
            list.add(0, tutorialJob());
        }
        return list;
    }

    private static List<Job> generate(Loc board) {
        CrewState s = Game.S();
        List<Job> out = new ArrayList<>();
        for (int i = 0; i < 3; i++) out.add(smuggle(board));
        if (board == Loc.GILDED_MAW && s.story >= 4) out.add(boarding(false));
        if (board == Loc.DUSTREACH && s.story >= 6 && Game.server.getOverworld().getTime() >= s.heistCooldownUntil) out.add(heist(false));
        return out;
    }

    private static Job smuggle(Loc board) {
        Job j = new Job();
        j.type = Job.Type.SMUGGLE;
        Item[] pool = cargoPool();
        j.cargo = pool[Game.R.nextInt(pool.length)];
        j.qty = 3 + Game.R.nextInt(6);
        List<Loc> dests = new ArrayList<>(List.of(Loc.MERIDIAN, Loc.DUSTREACH, Loc.GILDED_MAW));
        dests.remove(board);
        j.dest = dests.get(Game.R.nextInt(dests.size()));
        j.origin = board;
        boolean lawful = j.dest.lawful;
        j.payout = j.qty * (lawful ? 34 : 22) + 40 + Game.R.nextInt(30);
        j.heat = lawful ? 10 : 6;
        j.faction = board == Loc.GILDED_MAW ? "syndicate" : "freefolk";
        String[] titles = {"Quiet Delivery", "No Questions Asked", "Midnight Run", "Totally Legal Groceries", "Express Shipping (Illegal)", "A Favour for a Friend"};
        j.title = titles[Game.R.nextInt(titles.length)];
        return j;
    }

    public static Job tutorialJob() {
        Job j = new Job();
        j.type = Job.Type.SMUGGLE;
        j.title = "Tutorial: First Run";
        j.cargo = ModItems.GLOWMOSS;
        j.qty = 4;
        j.origin = Loc.DUSTREACH;
        j.dest = Loc.GILDED_MAW;
        j.payout = 120;
        j.heat = 5;
        j.faction = "freefolk";
        j.tutorial = true;
        return j;
    }

    public static Job boarding(boolean story) {
        Job j = new Job();
        j.type = Job.Type.BOARDING;
        j.title = story ? "Story: The Egg Job" : "Salvage Rights (Disputed)";
        j.cargo = ModItems.GRUMBLEBACK_EGG;
        j.qty = 3;
        j.origin = Loc.GILDED_MAW;
        j.dest = Loc.GILDED_MAW;
        j.payout = story ? 300 : 260;
        j.heat = 12;
        j.faction = "syndicate";
        j.story = story;
        return j;
    }

    public static Job heist(boolean story) {
        Job j = new Job();
        j.type = Job.Type.HEIST;
        j.title = story ? "Story: The Lumen Vault" : "The Lumen Vault (Again)";
        j.origin = Loc.DUSTREACH;
        j.dest = Loc.MERIDIAN;
        j.payout = story ? 450 : 400;
        j.heat = 25;
        j.faction = "syndicate";
        j.story = story;
        return j;
    }

    public static List<String> describe(Job j) {
        List<String> l = new ArrayList<>();
        switch (j.type) {
            case SMUGGLE -> {
                l.add("Smuggle " + j.qty + "x " + j.cargoName());
                l.add("Deliver to: " + j.dest.display);
                l.add("Pay: " + j.payout + " credits (to the Crew Pot)");
                l.add("Heat when delivered: +" + j.heat);
                if (j.dest.lawful) l.add("§cRisky: lawful world, lots of scans!");
            }
            case BOARDING -> {
                l.add("Jump to the Derelict Freighter (new on the star map).");
                l.add("Reroute power to open the bulkhead, grab " + j.qty + " " + j.cargoName() + ".");
                l.add("Bring them back to " + j.dest.display + ".");
                l.add("Pay: " + j.payout + " + salvage. Heat: +" + j.heat);
                l.add("§eTeamwork: one fixes the panel, others fight scavengers.");
            }
            case HEIST -> {
                l.add("Jump to " + Loc.MERIDIAN.display + " and enter the bank.");
                l.add("Hack the Vault Terminal while your crew holds off troopers.");
                l.add("Loot the vault, get back to the ship, and JUMP away.");
                l.add("Pay: " + j.payout + " + loot. Heat: +" + j.heat);
                l.add("§eTeamwork: one hacks, everyone else defends!");
            }
        }
        l.add("Client: " + factionName(j.faction));
        return l;
    }

    // ------------------------------------------------------------------ accept / abandon
    public static boolean accept(ServerPlayerEntity p, Job j, Loc board) {
        CrewState s = Game.S();
        if (s.job != null) {
            Game.msg(p, "Your crew already has a job: " + s.job.shortLine() + ". Finish or abandon it first.", Formatting.YELLOW);
            return false;
        }
        if (j.type == Job.Type.HEIST && !j.story && Game.server.getOverworld().getTime() < s.heistCooldownUntil) {
            Game.msg(p, "The bank is still on high alert. Try again later.", Formatting.YELLOW);
            return false;
        }
        s.job = j;
        s.openedCrates.clear();
        s.markDirty();
        List<Job> list = OFFERS.get(board);
        if (list != null) list.remove(j);
        switch (j.type) {
            case SMUGGLE -> {
                p.getInventory().offerOrDrop(new ItemStack(j.cargo, j.qty));
                Game.crew(Game.name(p) + " took a job: " + j.title + " - " + j.shortLine(), Formatting.GREEN);
                Game.msg(p, "You got " + j.qty + "x " + j.cargoName() + ". Hide it in the ship's Hidden Compartment before a patrol scans you!", Formatting.YELLOW);
            }
            case BOARDING -> {
                Structures.resetDerelict(Game.server, s);
                Missions.resetBoarding();
                Game.crew(Game.name(p) + " took a job: " + j.title + ". The Derelict Freighter is now on the star map!", Formatting.GREEN);
            }
            case HEIST -> {
                Structures.resetVault(Game.server, s);
                Missions.resetHeist();
                Game.crew(Game.name(p) + " took a job: " + j.title + ". Jump to Meridian Prime and find the bank!", Formatting.GREEN);
            }
        }
        Game.sound(Game.players(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        Tutorial.onJobTaken();
        return true;
    }

    public static void abandon(ServerPlayerEntity p) {
        CrewState s = Game.S();
        if (s.job == null) return;
        if (s.job.story) {
            Game.msg(p, "This is a story job - it can't be abandoned. (It doesn't expire, take your time!)", Formatting.YELLOW);
            return;
        }
        s.addRep(s.job.faction, -3);
        Game.crew(Game.name(p) + " abandoned the job \"" + s.job.title + "\". The client is disappointed in you.", Formatting.GRAY);
        s.job = null;
        s.markDirty();
    }

    // ------------------------------------------------------------------ delivery
    public static int count(ServerPlayerEntity p, Item item) {
        CrewState s = Game.S();
        return count(p.getInventory(), item) + count(s.cargo, item) + count(s.hidden, item);
    }

    private static int count(Inventory inv, Item item) {
        int n = 0;
        for (int i = 0; i < inv.size(); i++) if (inv.getStack(i).isOf(item)) n += inv.getStack(i).getCount();
        return n;
    }

    private static int take(Inventory inv, Item item, int n) {
        for (int i = 0; i < inv.size() && n > 0; i++) {
            ItemStack st = inv.getStack(i);
            if (!st.isOf(item)) continue;
            int k = Math.min(n, st.getCount());
            st.decrement(k);
            if (st.isEmpty()) inv.setStack(i, ItemStack.EMPTY);
            n -= k;
        }
        inv.markDirty();
        return n;
    }

    public static void take(ServerPlayerEntity p, Item item, int n) {
        CrewState s = Game.S();
        n = take(s.hidden, item, n);
        n = take(s.cargo, item, n);
        take(p.getInventory(), item, n);
    }

    public static boolean canDeliverHere(Loc here) {
        Job j = Game.S().job;
        return j != null && j.type != Job.Type.HEIST && j.dest == here;
    }

    public static void deliver(ServerPlayerEntity p, Loc here) {
        CrewState s = Game.S();
        Job j = s.job;
        if (j == null || !canDeliverHere(here)) {
            Game.msg(p, "Nothing to deliver here.", Formatting.GRAY);
            return;
        }
        int have = count(p, j.cargo);
        if (have < j.qty) {
            Game.msg(p, "You need " + j.qty + "x " + j.cargoName() + " but the crew only has " + have
                    + " (counting your pockets, the Cargo Hold and the Hidden Compartment).", Formatting.RED);
            if (j.type == Job.Type.SMUGGLE) Game.msg(p, "Lost some in a scan? Buy more at a Black Market, or abandon the job.", Formatting.GRAY);
            return;
        }
        take(p, j.cargo, j.qty);
        complete(j, Game.name(p) + " delivered the goods");
    }

    public static void complete(Job j, String how) {
        CrewState s = Game.S();
        int total = j.payout + j.bonus;
        s.pot += total;
        s.job = null;
        s.totalJobs++;
        s.markDirty();
        Game.title(Game.players(), Game.t("JOB COMPLETE!", Formatting.GOLD), Game.t("+" + total + " credits in the Crew Pot", Formatting.YELLOW));
        Game.sound(Game.players(), SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        Game.crew(how + "! +" + total + " credits went into the Crew Pot. Split it at the LOOT LOCKER on the ship.", Formatting.GOLD);
        String reason = switch (j.type) {
            case SMUGGLE -> "smuggling";
            case BOARDING -> "piracy";
            case HEIST -> "bank heist";
        };
        Heat.add(j.heat, reason);
        switch (j.type) {
            case SMUGGLE -> s.addRep(j.faction, 5);
            case BOARDING -> { s.addRep("syndicate", 6); s.addRep("rattlejaw", -8); }
            case HEIST -> {
                s.addRep("syndicate", 8);
                s.addRep("concordat", -12);
                s.heistCooldownUntil = Game.server.getOverworld().getTime() + 24000;
            }
        }
        Story.onJobComplete(j);
        Tutorial.onJobComplete();
    }

    static void onArrive(Loc dest) {
        CrewState s = Game.S();
        Job j = s.job;
        if (j != null && j.type == Job.Type.HEIST && j.phase >= 3 && dest != Loc.MERIDIAN) {
            complete(j, "The crew got away with the heist");
        }
    }
}
