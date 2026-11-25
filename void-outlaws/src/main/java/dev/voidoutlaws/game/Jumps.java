package dev.voidoutlaws.game;

import dev.voidoutlaws.reg.ModItems;
import dev.voidoutlaws.state.CrewState;
import dev.voidoutlaws.state.Job;
import dev.voidoutlaws.world.Loc;
import dev.voidoutlaws.world.Structures;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** Travelling between locations, and the random events on the way. */
public final class Jumps {
    private Jumps() {}

    private record Planned(int at, JumpEvent.Type type, boolean tutorial) {}

    public static JumpEvent event;
    private static final List<Planned> PLANNED = new ArrayList<>();
    public static int jumpTicks, jumpTotal = 400;
    private static ServerBossBar bar;
    public static Loc from = Loc.BURROW;
    public static final boolean[] SPARKS = new boolean[9];
    private static boolean forceChase;

    static void reset() {
        event = null;
        PLANNED.clear();
        if (bar != null) bar.clearPlayers();
        bar = null;
        java.util.Arrays.fill(SPARKS, false);
    }

    public static boolean jumping() {
        return Game.S().jumping;
    }

    public static boolean destAvailable(Loc l) {
        if (l != Loc.DERELICT) return true;
        Job j = Game.S().job;
        return j != null && j.type == Job.Type.BOARDING;
    }

    // ------------------------------------------------------------------ start
    public static void start(ServerPlayerEntity pilot, Loc dest) {
        CrewState s = Game.S();
        if (s.jumping) { Game.msg(pilot, "We're already in hyperspace!", Formatting.YELLOW); return; }
        if (dest == s.shipLoc) { Game.msg(pilot, "We're already docked at " + dest.display + ".", Formatting.YELLOW); return; }
        if (!destAvailable(dest)) { Game.msg(pilot, "No coordinates for that. You need a boarding job first.", Formatting.RED); return; }
        if (s.hull <= 0) { s.hull = 10; }

        from = s.shipLoc;
        forceChase = Missions.onShipLeave(from);
        Mobs.onShipLeave(from);

        s.jumping = true;
        s.jumpDest = dest;
        s.markDirty();
        jumpTicks = 0;
        jumpTotal = dest == Loc.DERELICT ? 460 : 400;
        event = null;
        PLANNED.clear();
        plan(s, dest);

        bar = new ServerBossBar(Game.t("Hyperspace -> " + dest.display, Formatting.AQUA), BossBar.Color.BLUE, BossBar.Style.NOTCHED_10);
        for (ServerPlayerEntity p : Game.aboard()) bar.addPlayer(p);

        List<ServerPlayerEntity> aboard = Game.aboard();
        Game.title(aboard, Game.t("JUMP!", Formatting.AQUA), Game.t("Next stop: " + dest.display, Formatting.WHITE));
        Game.sound(aboard, SoundEvents.BLOCK_BEACON_ACTIVATE, 1f, 0.6f);
        Game.sound(aboard, SoundEvents.BLOCK_PORTAL_TRAVEL, 0.25f, 1.6f);
        Game.to(aboard, Game.name(pilot) + " punched in the coordinates for " + dest.display + ". Hold on to your snacks!", Formatting.AQUA);
        for (ServerPlayerEntity p : Game.in(from)) {
            Game.msg(p, "The " + Game.SHIP_NAME + " jumped to " + dest.display + " without you! Once it docks, use a Docking Beacon to catch a shuttle aboard.", Formatting.GOLD);
        }
        Tutorial.onJumpStart(aboard);
    }

    private static void plan(CrewState s, Loc dest) {
        List<ServerPlayerEntity> aboard = Game.aboard();
        boolean newbies = false, tutScan = false;
        for (ServerPlayerEntity p : aboard) {
            int st = Tutorial.step(p);
            if (st >= 0 && st <= Tutorial.JUMP) newbies = true;
            if (st == Tutorial.SCAN) tutScan = true;
        }
        if (forceChase) {
            PLANNED.add(new Planned(90, JumpEvent.Type.CHASE, false));
            return;
        }
        if (tutScan) {
            PLANNED.add(new Planned(80, JumpEvent.Type.SCAN, true));
            return; // the tutorial pirate attack is queued after the scan
        }
        if (newbies) return; // very first jump: nice and calm

        double pScan = 0.12 + s.heat / 160.0 + ((dest.lawful || from.lawful) ? 0.35 : 0);
        if (dest == Loc.BURROW) pScan *= 0.5;
        double pPir = 0.22 + Math.max(0, -s.rep("rattlejaw")) / 250.0 + (dest == Loc.DERELICT ? 0.3 : 0);
        List<JumpEvent.Type> picks = new ArrayList<>();
        if (s.heat >= 70 && Game.R.nextDouble() < 0.18) picks.add(JumpEvent.Type.CHASE);
        else if (Game.R.nextDouble() < Math.min(0.9, pScan)) picks.add(JumpEvent.Type.SCAN);
        if (Game.R.nextDouble() < Math.min(0.8, pPir)) picks.add(JumpEvent.Type.PIRATES);
        if (picks.size() < 2 && Game.R.nextDouble() < 0.15) picks.add(JumpEvent.Type.SALVAGE);
        java.util.Collections.shuffle(picks, Game.R);
        int at = 70 + Game.R.nextInt(40);
        for (JumpEvent.Type t : picks) {
            PLANNED.add(new Planned(at, t, false));
            at += 150 + Game.R.nextInt(60);
        }
    }

    // ------------------------------------------------------------------ tick
    static void tick(MinecraftServer server) {
        CrewState s = Game.S();
        if (!s.jumping) return;
        if (event != null) {
            tickEvent(event);
            if (event.done) finishEvent();
        } else {
            jumpTicks++;
            for (Planned pl : new ArrayList<>(PLANNED)) {
                if (jumpTicks >= pl.at) {
                    PLANNED.remove(pl);
                    startEvent(pl.type, pl.tutorial);
                    break;
                }
            }
            if (event == null && jumpTicks >= jumpTotal && PLANNED.isEmpty()) arrive(server);
            else if (event == null && jumpTicks >= jumpTotal) jumpTicks = jumpTotal - 1;
        }
        if (Game.tick % 4 == 0) particles(server);
        if (bar != null) {
            bar.setPercent(Math.min(1f, jumpTicks / (float) jumpTotal));
            if (event != null) {
                bar.setName(Game.t(event.type.display.toUpperCase() + "!  (" + event.secondsLeft() + "s)", Formatting.RED));
                bar.setColor(BossBar.Color.RED);
            } else {
                int eta = Math.max(0, (jumpTotal - jumpTicks) / 20);
                bar.setName(Game.t("Hyperspace -> " + s.jumpDest.display + "  (ETA " + eta + "s)", Formatting.AQUA));
                bar.setColor(BossBar.Color.BLUE);
            }
        }
        if (Game.tick % 100 == 0) {
            int n = 0;
            for (boolean b : SPARKS) if (b) n++;
            if (n > 0) {
                s.hull = Math.max(1, s.hull - n);
                s.markDirty();
                for (ServerPlayerEntity p : Game.aboard()) Game.bar(p, n + " spark(s) are damaging the hull! Mechanic: Engineering Panel!", Formatting.GOLD);
            }
        }
    }

    static void tickSecond(MinecraftServer server) {
        if (bar == null) return;
        List<ServerPlayerEntity> aboard = Game.aboard();
        for (ServerPlayerEntity p : new ArrayList<>(bar.getPlayers())) if (!aboard.contains(p)) bar.removePlayer(p);
        for (ServerPlayerEntity p : aboard) if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
    }

    private static void particles(MinecraftServer server) {
        ServerWorld w = server.getWorld(Loc.SHIP);
        if (w == null || Game.aboard().isEmpty()) return;
        for (int i = 0; i < 6; i++) {
            double x = (Game.R.nextDouble() - 0.5) * 50;
            double y = 100 + (Game.R.nextDouble() - 0.5) * 24;
            if (Math.abs(x) < 8 && Math.abs(y - 102) < 6) x += Math.signum(x == 0 ? 1 : x) * 9;
            w.spawnParticles(ParticleTypes.END_ROD, x, y, -30 + Game.R.nextDouble() * 10, 0, 0, 0, 1, 1.6);
        }
    }

    /** Queue an event to start shortly (used when the pilot runs from a scan). */
    public static void queue(JumpEvent.Type type, int delay) {
        PLANNED.add(new Planned(jumpTicks + delay, type, false));
        if (jumpTotal < jumpTicks + delay + 40) jumpTotal = jumpTicks + delay + 40;
    }

    // ------------------------------------------------------------------ events
    public static void startEvent(JumpEvent.Type type, boolean tutorial) {
        JumpEvent e = new JumpEvent(type, tutorial);
        int crew = Math.max(1, Game.aboard().size());
        List<ServerPlayerEntity> aboard = Game.aboard();
        switch (type) {
            case SCAN -> {
                e.duration = tutorial ? 900 : 400;
                Game.title(aboard, Game.t("PATROL SCAN!", Formatting.AQUA), Game.t("Pilot: answer the hail at the Ship Console", Formatting.WHITE));
                Game.sound(aboard, SoundEvents.BLOCK_BELL_USE, 1f, 1.2f);
                Game.to(aboard, "A Concordat patrol cutter hails you: \"Cut your engines and prepare for a cargo scan.\"", Formatting.AQUA);
            }
            case PIRATES -> {
                e.duration = tutorial ? 1200 : 600;
                e.squadronHp = tutorial ? 4 : 5 + 2 * (crew - 1);
                e.spawnEvery = tutorial ? 30 : 14;
                e.life = tutorial ? 90 : 42;
                e.maxTargets = tutorial ? 2 : 3 + Math.min(2, crew - 1);
                e.speed = tutorial ? 4 : 3;
                e.zoneW = tutorial ? 3 : 2;
                Game.title(aboard, Game.t("PIRATES!", Formatting.RED), Game.t("Gunner: Gunnery Station  |  Pilot: Ship Console", Formatting.WHITE));
                Game.sound(aboard, SoundEvents.EVENT_RAID_HORN.value(), 0.7f, 1.3f);
                Game.to(aboard, "Rattlejaw pirates drop out of hyperspace, cackling over the radio!", Formatting.RED);
            }
            case CHASE -> {
                e.duration = 520;
                e.escapeNeed = 5;
                e.spawnEvery = 22;
                e.life = 45;
                e.maxTargets = 3;
                e.speed = 3;
                e.zoneW = 2;
                Game.title(aboard, Game.t("POLICE CHASE!", Formatting.BLUE), Game.t("Pilot: steer!  Gunner: shoot the tracking drones!", Formatting.WHITE));
                Game.sound(aboard, SoundEvents.BLOCK_BELL_USE, 1f, 0.6f);
                Game.to(aboard, "Sirens! A Concordat interceptor is right behind you. Lose them!", Formatting.BLUE);
            }
            case SALVAGE -> {
                e.duration = 300;
                e.speed = 3;
                e.zoneW = 2;
                Game.title(aboard, Game.t("Drifting salvage!", Formatting.GOLD), Game.t("Pilot: catch it with the Ship Console", Formatting.WHITE));
                Game.sound(aboard, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
                Game.to(aboard, "A lost cargo crate tumbles past the window. Free stuff!", Formatting.GOLD);
            }
        }
        e.zone = Game.R.nextInt(9 - e.zoneW + 1);
        event = e;
        Tutorial.onEventStart(aboard, e);
    }

    private static void tickEvent(JumpEvent e) {
        e.age++;
        if (e.flashTicks > 0) e.flashTicks--;
        if (e.pilotCooldown > 0) e.pilotCooldown--;
        if (e.usesBar() && e.age % e.speed == 0) {
            e.cursor += e.dir;
            if (e.cursor >= 8) { e.cursor = 8; e.dir = -1; }
            if (e.cursor <= 0) { e.cursor = 0; e.dir = 1; }
        }
        CrewState s = Game.S();
        switch (e.type) {
            case SCAN -> {
                if (e.age >= e.duration) {
                    Game.to(Game.aboard(), "Nobody answered the hail... the patrol scans you anyway.", Formatting.AQUA);
                    Scans.scan(false);
                    e.done = true;
                }
            }
            case PIRATES, CHASE -> {
                for (int i = 0; i < 45; i++) {
                    if (e.targets[i] > 0) {
                        e.targets[i]--;
                        if (e.targets[i] == 0) expire(e);
                    }
                }
                if (e.age % e.spawnEvery == 0 && e.targetCount() < e.maxTargets) {
                    for (int tries = 0; tries < 20; tries++) {
                        int i = Game.R.nextInt(45);
                        if (i == 4 || e.targets[i] > 0) continue;
                        e.targets[i] = e.life;
                        break;
                    }
                }
                if (e.type == JumpEvent.Type.PIRATES) {
                    if (e.squadronHp <= 0) {
                        int loot = 25 + Game.R.nextInt(40);
                        s.pot += loot;
                        s.addRep("rattlejaw", -2);
                        Game.title(Game.aboard(), Game.t("Pirates fled!", Formatting.GREEN), Game.t("+" + loot + " credits of salvage in the Crew Pot", Formatting.GOLD));
                        Game.sound(Game.aboard(), SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                        e.done = true;
                    } else if (s.hull <= 0) {
                        disabled(s);
                        e.done = true;
                    } else if (e.age >= e.duration) {
                        s.hull = Math.max(1, s.hull - 8);
                        Game.to(Game.aboard(), "The pirates get bored and leave... taking some of your hull plating as a souvenir.", Formatting.GOLD);
                        e.done = true;
                    }
                } else {
                    if (e.escape >= e.escapeNeed) {
                        Game.title(Game.aboard(), Game.t("You lost them!", Formatting.GREEN), Game.t("...but they got a good look at your ship", Formatting.WHITE));
                        Game.sound(Game.aboard(), SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                        Heat.add(4, "escaped a chase");
                        s.addRep("concordat", -2);
                        e.done = true;
                    } else if (e.age >= e.duration) {
                        Game.title(Game.aboard(), Game.t("CAUGHT!", Formatting.RED), Game.t("Strict search incoming...", Formatting.WHITE));
                        Heat.add(8, "caught after a chase");
                        Scans.scan(true);
                        e.done = true;
                    }
                }
            }
            case SALVAGE -> {
                if (e.age >= e.duration) {
                    Game.to(Game.aboard(), "The salvage drifts away into the dark. Bye, free stuff.", Formatting.GRAY);
                    e.done = true;
                }
            }
        }
        s.markDirty();
    }

    private static void expire(JumpEvent e) {
        CrewState s = Game.S();
        if (e.type == JumpEvent.Type.PIRATES) {
            if (e.dodge > 0) {
                e.dodge--;
                e.flash("The pilot dodged a shot!");
            } else {
                damageHull(e.tutorial ? 2 : 5);
                e.flash("A pirate shot hit us! Hull " + s.hull + "%");
                Game.sound(Game.aboard(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 0.5f, 1.4f);
                if (!e.tutorial && !e.boardersSpawned && s.hull < 50) {
                    e.boardersSpawned = true;
                    Mobs.spawnBoarders(Game.server, Math.max(1, Game.aboard().size() / 2 + 1));
                }
            }
        } else {
            e.escape = Math.max(0, e.escape - 0.5);
            e.flash("A tracking drone tagged us!");
        }
    }

    public static void damageHull(int n) {
        CrewState s = Game.S();
        s.hull = Math.max(0, s.hull - n);
        s.markDirty();
        if (Game.R.nextDouble() < 0.6) {
            for (int tries = 0; tries < 10; tries++) {
                int i = Game.R.nextInt(9);
                if (!SPARKS[i]) { SPARKS[i] = true; break; }
            }
        }
    }

    private static void disabled(CrewState s) {
        Game.title(Game.aboard(), Game.t("SHIP DISABLED!", Formatting.DARK_RED), Game.t("Pirates rummage through the Cargo Hold...", Formatting.WHITE));
        int taken = 0;
        for (int i = 0; i < s.cargo.size(); i++) {
            ItemStack st = s.cargo.getStack(i);
            if (ModItems.isContraband(st) && Game.R.nextInt(3) == 0) { taken += st.getCount(); s.cargo.setStack(i, ItemStack.EMPTY); }
        }
        int lostPot = s.pot / 5;
        s.pot -= lostPot;
        s.hull = 25;
        Game.to(Game.aboard(), "They took " + taken + " cargo, " + lostPot + " credits from the pot, and (worst of all) the good snacks.", Formatting.RED);
    }

    private static void finishEvent() {
        JumpEvent e = event;
        event = null;
        java.util.Arrays.fill(e.targets, 0);
        Tutorial.onEventEnd(Game.aboard(), e);
        if (e.tutorial && e.type == JumpEvent.Type.SCAN) {
            PLANNED.add(new Planned(jumpTicks + 100, JumpEvent.Type.PIRATES, true));
            if (jumpTotal < jumpTicks + 220) jumpTotal = jumpTicks + 220;
        }
    }

    // ------------------------------------------------------------------ player input
    public static void pilotClick(ServerPlayerEntity p) {
        JumpEvent e = event;
        if (e == null || !e.usesBar()) return;
        if (e.pilotCooldown > 0) { e.flash("Steady... wait a moment!"); return; }
        if (e.cursorInZone()) {
            Game.sound(p, SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1f, 1.6f);
            switch (e.type) {
                case PIRATES -> { e.dodge = Math.min(4, e.dodge + 2); e.flash("Evasive maneuver! Dodges ready: " + e.dodge); }
                case CHASE -> { e.escape += 1; e.flash("Great turn! Escape " + fmt(e.escape) + "/" + e.escapeNeed); }
                case SALVAGE -> { grabSalvage(p); e.done = true; }
                default -> {}
            }
            e.zone = Game.R.nextInt(9 - e.zoneW + 1);
            e.pilotCooldown = 8;
        } else {
            Game.sound(p, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1f, 0.6f);
            e.flash("Overcorrected! Wait for the green zone.");
            e.pilotCooldown = 20;
        }
    }

    public static void gunnerClick(ServerPlayerEntity p, int slot) {
        JumpEvent e = event;
        if (e == null || !e.usesTargets() || slot < 0 || slot >= 45) return;
        if (e.targets[slot] > 0) {
            e.targets[slot] = 0;
            e.hits++;
            Game.sound(p, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1.4f);
            if (e.type == JumpEvent.Type.PIRATES) {
                e.squadronHp--;
                e.flash("Direct hit! Enemies left: " + Math.max(0, e.squadronHp));
            } else {
                e.escape += 0.5;
                e.flash("Drone down! Escape " + fmt(e.escape) + "/" + e.escapeNeed);
            }
        } else {
            Game.sound(p, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 0.6f, 0.8f);
        }
    }

    public static void mechanicBoost(ServerPlayerEntity p) {
        JumpEvent e = event;
        if (e == null) return;
        if (e.age < e.mechReadyAt) { Game.bar(p, "Still recharging...", Formatting.GRAY); return; }
        if (e.type == JumpEvent.Type.PIRATES) {
            e.dodge = Math.min(4, e.dodge + 1);
            e.flash("Shields boosted! Dodges ready: " + e.dodge);
            e.mechReadyAt = e.age + 160;
        } else if (e.type == JumpEvent.Type.CHASE) {
            e.escape += 1;
            e.flash("Engines overcharged! Escape " + fmt(e.escape) + "/" + e.escapeNeed);
            e.mechReadyAt = e.age + 160;
        } else return;
        Game.sound(p, SoundEvents.BLOCK_BEACON_POWER_SELECT, 1f, 1.2f);
    }

    public static void fixSpark(ServerPlayerEntity p, int i) {
        if (i < 0 || i >= 9 || !SPARKS[i]) return;
        SPARKS[i] = false;
        CrewState s = Game.S();
        s.hull = Math.min(100, s.hull + 2);
        s.markDirty();
        Game.sound(p, SoundEvents.BLOCK_CHAIN_PLACE, 1f, 1.5f);
    }

    private static void grabSalvage(ServerPlayerEntity p) {
        CrewState s = Game.S();
        List<Item> pool = new ArrayList<>(ModItems.CONTRABAND);
        pool.remove(ModItems.GRUMBLEBACK_EGG);
        Item it = pool.get(Game.R.nextInt(pool.size()));
        int n = 2 + Game.R.nextInt(3);
        ItemStack rest = s.cargo.addStack(new ItemStack(it, n));
        if (!rest.isEmpty()) p.getInventory().offerOrDrop(rest);
        Game.to(Game.aboard(), "Salvage grabbed: " + n + "x " + it.getName().getString() + " (now in the Cargo Hold - hide it before a scan!)", Formatting.GOLD);
    }

    static String fmt(double d) {
        return d == Math.floor(d) ? String.valueOf((int) d) : String.format("%.1f", d);
    }

    // ------------------------------------------------------------------ arrive
    private static void arrive(MinecraftServer server) {
        CrewState s = Game.S();
        Loc dest = s.jumpDest;
        s.jumping = false;
        s.shipLoc = dest;
        s.jumpsSinceHunter++;
        s.markDirty();
        Structures.ensureBuilt(server, s, dest);
        if (bar != null) bar.clearPlayers();
        bar = null;
        java.util.Arrays.fill(SPARKS, false);
        event = null;
        PLANNED.clear();
        Mobs.clearShip(server);

        List<ServerPlayerEntity> aboard = Game.aboard();
        Game.title(aboard, Game.t(dest.display, dest.color), Game.t(dest.lore[0], Formatting.WHITE));
        Game.sound(aboard, SoundEvents.BLOCK_BEACON_DEACTIVATE, 1f, 1.4f);
        Game.to(aboard, "Docked at " + dest.display + ". Use the HATCH in the cargo bay to step outside.", Formatting.AQUA);
        for (ServerPlayerEntity p : Game.players()) {
            if (!aboard.contains(p)) Game.msg(p, "The ship docked at " + dest.display + ". Use a Docking Beacon to catch a shuttle aboard.", Formatting.GRAY);
        }
        if (dest == Loc.BURROW && s.hull < 100) {
            s.hull = 100;
            Game.to(aboard, "The Burrow's grease-monkey drones patch the hull back to 100%. Free of charge!", Formatting.GREEN);
        }
        Jobs.onArrive(dest);
        Missions.onArrive(server, dest);
        Mobs.onArrive(server, dest);
        Tutorial.onArrive(aboard, dest);
    }
}
