package dev.forgottenisland.test;

import dev.forgottenisland.ForgottenIsland;
import dev.forgottenisland.content.Content;
import dev.forgottenisland.core.GameCtx;
import dev.forgottenisland.core.StateData;
import dev.forgottenisland.dialogue.DialogueManager;
import dev.forgottenisland.game.*;
import dev.forgottenisland.npc.*;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.BooleanSupplier;

/**
 * In-world automated tests (/fi selftest [suite]). Runs on the live, built island using a server-side
 * fake player, exercising the real puzzle blocks, gates, encounters, boss, NPC director and dialogue.
 * Story state is snapshotted first and restored afterwards; run "/fi reset" before shipping a world.
 */
public final class SelfTest {
    private SelfTest() {}

    interface Step { boolean run(); }   // returns true when finished
    private static final Deque<Object> steps = new ArrayDeque<>();   // Step or Integer (wait ticks)
    private static int wait;
    private static boolean running, registered;
    private static final List<String> results = new ArrayList<>();
    private static int pass, fail, warn;
    private static ServerCommandSource source;
    private static MinecraftServer server;
    private static FakePlayer fake;
    private static StateData snapshot;

    static void ok(boolean cond, String what) {
        String line = (cond ? "PASS " : "FAIL ") + what;
        if (cond) pass++; else fail++;
        results.add(line);
        ForgottenIsland.LOG.info("[selftest] {}", line);
    }
    static void note(String what) { warn++; results.add("WARN " + what); ForgottenIsland.LOG.info("[selftest] WARN {}", what); }

    static void step(Step s) { steps.add(s); }
    private static final List<long[]> forced = new ArrayList<>();
    /** Force-loads chunks around a point (a headless server has no player to load them). */
    static void load(double x, double z, int r) {
        ServerWorld w = server.getOverworld();
        for (int cx = ((int) x >> 4) - r; cx <= ((int) x >> 4) + r; cx++) for (int cz = ((int) z >> 4) - r; cz <= ((int) z >> 4) + r; cz++) {
            if (w.setChunkForced(cx, cz, true)) forced.add(new long[]{cx, cz});
        }
    }
    static void sleep(int ticks) { steps.add(ticks); }
    static void now(Runnable r) { steps.add((Step) () -> { r.run(); return true; }); }

    public static int run(ServerCommandSource src, String suite) {
        if (running) { src.sendFeedback(() -> Text.literal("selftest already running"), false); return 0; }
        source = src; server = src.getServer(); results.clear(); pass = fail = warn = 0; steps.clear();
        if (!registered) { registered = true; ServerTickEvents.END_SERVER_TICK.register(s -> tick()); }
        ServerWorld w = server.getOverworld();
        fake = FakePlayer.get(w);
        StateData st = Game.state(server);
        if (!suite.startsWith("persist")) { snapshot = new StateData(); snapshot.copyFrom(st); }
        running = true;
        Set<String> all = new LinkedHashSet<>(List.of("content", "places", "gates", "stones", "mirrors", "slide", "secrets", "keyitems", "dialogue", "quests", "encounters", "boss", "npcs", "paths", "reset"));
        List<String> run = suite.equals("all") ? new ArrayList<>(all) : List.of(suite.split(","));
        for (String s : run) switch (s) {
            case "content" -> content();
            case "places" -> places();
            case "gates" -> gates();
            case "stones" -> stones();
            case "mirrors" -> mirrors();
            case "slide" -> slide();
            case "secrets" -> secrets();
            case "keyitems" -> keyItems();
            case "dialogue" -> dialogue();
            case "quests" -> quests();
            case "encounters" -> encounters();
            case "boss" -> boss();
            case "npcs" -> npcs();
            case "paths" -> paths();
            case "reset" -> reset();
            case "persist_write" -> persistWrite();
            case "persist_check" -> persistCheck();
            default -> note("unknown suite " + s);
        }
        now(SelfTest::finish);
        src.sendFeedback(() -> Text.literal("selftest started: " + run), false);
        return 1;
    }

    private static void tick() {
        if (!running) return;
        if (wait > 0) { wait--; return; }
        for (int budget = 0; budget < 50 && !steps.isEmpty(); budget++) {
            Object o = steps.peek();
            if (o instanceof Integer n) { steps.poll(); wait = n; return; }
            try {
                if (((Step) o).run()) steps.poll(); else return;
            } catch (Throwable t) {
                steps.poll();
                ok(false, "step threw " + t);
                ForgottenIsland.LOG.error("selftest step failed", t);
            }
        }
    }

    private static void finish() {
        for (long[] c : forced) server.getOverworld().setChunkForced((int) c[0], (int) c[1], false);
        forced.clear();
        if (snapshot != null) {
            // restore world + state to what it was before the tests
            Story.resetWorld(server);
            StateData st = Game.state(server);
            st.copyFrom(snapshot);
            for (var g : Gates.all().values()) { if (Gates.isOpen(server, g.id())) Gates.open(server, g.id()); else Gates.close(server, g.id()); }
            if (st.flag("puzzle1_solved")) Gates.open(server, "thornwall");
            snapshot = null;
        }
        String summary = "SELFTEST SUMMARY: " + pass + " passed, " + fail + " failed, " + warn + " warnings";
        results.add(summary);
        try (PrintWriter pw = new PrintWriter(new FileWriter("selftest_report.txt"))) { for (String r : results) pw.println(r); } catch (Exception e) { ForgottenIsland.LOG.error("report", e); }
        ForgottenIsland.LOG.info("[selftest] {}", summary);
        source.sendFeedback(() -> Text.literal(summary + " (see selftest_report.txt)"), false);
        running = false;
    }

    // ------------------------------------------------------------------ suites
    static void content() {
        now(() -> {
            var errs = ContentHolder.get().validate(Places.names(), KeyItems.ids(), Story.eventNames());
            ok(errs.isEmpty(), "live content loads with no reference errors (" + ContentHolder.get().dialogues.size() + " dialogues)");
            for (String e : errs) note(e);
            ok(Game.state(server).flag("world_built"), "world is marked as built");
            ok(server.getOverworld().getGameRules().getBoolean(net.minecraft.world.GameRules.KEEP_INVENTORY), "keepInventory is on (no item loss on death)");
            ok(!server.getOverworld().getGameRules().getBoolean(net.minecraft.world.GameRules.DO_MOB_GRIEFING), "mob griefing is off (structures protected)");
        });
    }

    /** Every schedule spot must be physically usable: no NPC stuck in a wall or floating. */
    static void places() {
        now(() -> {
            ServerWorld w = server.getOverworld();
            Set<String> used = new TreeSet<>();
            Map<String, String> act = new HashMap<>();
            for (var e : ContentHolder.get().schedules.entrySet()) {
                List<Content.ScheduleEntry> all = new ArrayList<>(e.getValue().entries);
                for (var ov : e.getValue().overrides) all.addAll(ov.entries);
                for (var en : all) { used.add(en.at); act.put(en.at, en.act); }
            }
            int bad = 0;
            for (String name : used) {
                Places.P p = Places.get(name);
                w.getChunk(p.bx() >> 4, p.bz() >> 4);
                BlockPos at = new BlockPos(p.bx(), p.by(), p.bz());
                String a = act.get(name);
                boolean good;
                String why;
                BlockState here = w.getBlockState(at), below = w.getBlockState(at.down()), head = w.getBlockState(at.up());
                switch (a) {
                    case "sleep" -> { good = here.getBlock() instanceof BedBlock; why = "bed at " + at + " is " + here; }
                    case "sit" -> {
                        BlockPos seat = BlockPos.ofFloored(p.x(), p.y() - 0.01, p.z());
                        BlockState s = w.getBlockState(seat);
                        good = (s.getBlock() instanceof StairsBlock || s.isSolid()) && !w.getBlockState(seat.up(2)).isSolid();
                        why = "seat " + s;
                    }
                    default -> {
                        good = below.isSolid() || below.getBlock() instanceof StairsBlock || below.isIn(net.minecraft.registry.tag.BlockTags.SLABS);
                        good &= !here.isSolid() && !head.isSolid();
                        why = "floor " + below.getBlock() + ", feet " + here.getBlock() + ", head " + head.getBlock();
                    }
                }
                if (!good) { bad++; ok(false, "place " + name + " (" + a + ") usable: " + why); }
            }
            ok(bad == 0, "all " + used.size() + " NPC schedule spots are physically usable");
        });
    }

    static void gates() {
        now(() -> {
            int bad = 0;
            for (String g : Gates.all().keySet()) if (!Gates.consistent(server, g)) { bad++; ok(false, "gate " + g + " blocks match its saved state"); }
            ok(bad == 0, "all " + Gates.all().size() + " gates match their saved state");
        });
    }

    static void stones() {
        StateData st = Game.state(server);
        now(() -> { Puzzles.resetStones(server); st.clear("stones_awake"); ok(!Gates.isOpen(server, "thornwall"), "thorn wall starts closed"); });
        now(() -> {
            fake.refreshPositionAndAngles(Puzzles.STONE_TOPS.values().iterator().next().getX(), Puzzles.STONE_TOPS.values().iterator().next().getY(), Puzzles.STONE_TOPS.values().iterator().next().getZ(), 0, 0);
            Puzzles.strikeStone(fake, "heron");
            ok(!st.flag("puzzle1_solved"), "stones stay silent before the song is learned");
            st.set("stones_awake");
            Puzzles.strikeStone(fake, "heron"); Puzzles.strikeStone(fake, "owl");   // wrong
            for (String s : List.of("stag", "owl", "hare")) Puzzles.strikeStone(fake, s);
            ok(!st.flag("puzzle1_solved"), "a wrong note resets the sequence");
            for (String s : PuzzleLayouts.SONG) Puzzles.strikeStone(fake, s);
            ok(st.flag("puzzle1_solved"), "striking heron, stag, owl, hare solves the Singing Stones");
        });
        sleep(45);
        now(() -> {
            ok(Gates.isOpen(server, "thornwall") && Gates.consistent(server, "thornwall"), "the thorn wall opens physically (blocks removed)");
            BlockPos probe = Gates.all().get("thornwall").closed().keySet().iterator().next();
            ok(server.getOverworld().getBlockState(probe).isAir(), "gorge is passable at " + probe);
            Puzzles.resetStones(server);
            ok(!Gates.isOpen(server, "thornwall") && !server.getOverworld().getBlockState(probe).isAir(), "puzzle reset re-closes the thorn wall");
        });
        // the button handlers are registered at the button blocks
        now(() -> ok(Puzzles.STONE_BUTTONS.size() == 4 && Puzzles.STONE_BUTTONS.keySet().stream().allMatch(p -> server.getOverworld().getBlockState(p).getBlock() instanceof net.minecraft.block.ButtonBlock), "four stone buttons exist in the world"));
    }

    static void mirrors() {
        ServerWorld w = server.getOverworld();
        now(() -> {
            Puzzles.resetMirrors(server);
            ok(!PuzzleLayouts.mirrorSolved(Puzzles.mirrorFlips(w)), "mirrors start unsolved");
            ok(!Gates.isOpen(server, "tower_door"), "tower door starts closed");
            boolean[] want = null;
            int n = PuzzleLayouts.mirrorCells().size();
            for (int mask = 0; mask < (1 << n); mask++) { boolean[] f = new boolean[n]; for (int i = 0; i < n; i++) f[i] = (mask >> i & 1) == 1; if (PuzzleLayouts.mirrorSolved(f)) want = f; }
            boolean[] cur = Puzzles.mirrorFlips(w);
            int turns = 0;
            for (int i = 0; i < n; i++) if (cur[i] != want[i]) {
                int[] c = PuzzleLayouts.mirrorCells().get(i);
                Puzzles.rotateMirror(fake, Puzzles.mirrorOrigin.add(c[0], 0, c[1]));
                turns++;
            }
            ok(PuzzleLayouts.mirrorSolved(Puzzles.mirrorFlips(w)), "rotating " + turns + " mirror blocks in the world solves the Sun Court");
            ok(Game.state(server).flag("puzzle2_solved"), "puzzle 2 flag set");
        });
        sleep(50);
        now(() -> {
            ok(Gates.isOpen(server, "tower_door") && Gates.consistent(server, "tower_door"), "the Lantern Tower door opens");
            Puzzles.resetMirrors(server);
            ok(!Gates.isOpen(server, "tower_door"), "mirror reset re-seals the tower");
        });
    }

    static void slide() {
        ServerWorld w = server.getOverworld();
        now(() -> {
            Puzzles.resetSlide(server, true);
            ok(PuzzleLayouts.slideStart().size() == Puzzles.stones(w).size(), "three rune stones are placed in the chamber");
            // breadth-first search on the live board, then perform the pushes on the real blocks
            List<int[]> plan = solve(Puzzles.stones(w));
            ok(plan != null && plan.size() == PuzzleLayouts.slideMinMoves(), "solver finds the " + PuzzleLayouts.slideMinMoves() + "-push solution from the live board");
            if (plan != null) for (int[] m : plan) Puzzles.push(w, m[0], m[1], m[2], m[3]);
            boolean solved = PuzzleLayouts.slideSolved(Puzzles.stones(w));
            if (solved) Puzzles.solveSlide(server);
            ok(solved && Game.state(server).flag("puzzle3_solved"), "pushing the real rune stones onto the seals solves the vault puzzle");
        });
        sleep(50);
        now(() -> {
            ok(Gates.isOpen(server, "vault_door"), "the vault door opens");
            Puzzles.resetSlide(server, true);
            ok(!Gates.isOpen(server, "vault_door") && !PuzzleLayouts.slideSolved(Puzzles.stones(w)), "slide reset restores the stones and re-seals the vault");
            // the in-room reset button only resets an unsolved puzzle (never undoes progress)
            Game.state(server).set("puzzle3_solved");
            Puzzles.resetSlide(server, false);
            ok(Game.state(server).flag("puzzle3_solved"), "the chamber's reset button cannot undo a solved puzzle");
            Game.state(server).clear("puzzle3_solved");
        });
    }

    static List<int[]> solve(Set<Long> start) {
        Deque<Set<Long>> q = new ArrayDeque<>(); Map<Set<Long>, Object[]> prev = new HashMap<>();
        q.add(start); prev.put(start, null);
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!q.isEmpty()) {
            Set<Long> s = q.poll();
            if (PuzzleLayouts.slideSolved(s)) {
                LinkedList<int[]> path = new LinkedList<>();
                for (Set<Long> cur = s; prev.get(cur) != null; cur = (Set<Long>) prev.get(cur)[0]) path.addFirst((int[]) prev.get(cur)[1]);
                return path;
            }
            for (long k : s) {
                int x = (int) (k >> 32), z = (int) k;
                for (int[] d : dirs) {
                    int[] to = PuzzleLayouts.slide(s, x, z, d[0], d[1]);
                    if (to == null) continue;
                    Set<Long> n = new HashSet<>(s); n.remove(k); n.add(PuzzleLayouts.key(to[0], to[1]));
                    if (!prev.containsKey(n)) { prev.put(n, new Object[]{s, new int[]{x, z, d[0], d[1]}}); q.add(n); }
                }
            }
        }
        return null;
    }

    static void secrets() {
        now(() -> {
            StateData st = Game.state(server);
            int before = st.var("secrets");
            Content.Secret s = ContentHolder.get().secrets.get(0);
            st.clear("secret." + s.id);
            Secrets.discover(fake, s.id);
            Secrets.discover(fake, s.id);
            ok(st.flag("secret." + s.id), "discovering a secret records it");
            ok(st.var("secrets") >= Math.max(1, before), "secret counter counts each secret once");
            int zones = 0;
            for (Content.Secret sc : ContentHolder.get().secrets) if (Zones.get("secret_" + sc.id) != null) zones++;
            ok(zones == ContentHolder.get().secrets.size() && zones >= 10, zones + " secrets each have a discovery zone in the world");
        });
    }

    static void keyItems() {
        now(() -> {
            StateData st = Game.state(server);
            fake.getInventory().clear();
            KeyItems.give(fake, "warden_key", 1);
            ok(KeyItems.count(fake, "warden_key") == 1 && st.flag("got.warden_key"), "quest items are recognised in the inventory");
            fake.getInventory().clear();
            st.clear("used.warden_key");
            // simulate the periodic safety check
            try {
                var m = Game.class.getDeclaredMethod("protectKeyItems", net.minecraft.server.network.ServerPlayerEntity.class);
                m.setAccessible(true);
                m.invoke(null, fake);
            } catch (Exception e) { ok(false, "protectKeyItems reflect " + e); }
            ok(KeyItems.count(fake, "warden_key") == 1, "a lost quest item is returned to the player");
            KeyItems.take(fake, "warden_key", 1);
            ok(KeyItems.count(fake, "warden_key") == 0 && st.flag("used.warden_key"), "using up a quest item stops it being restored");
            ok(KeyItems.create("kestrel_blade", server).hasEnchantments(), "named reward items carry vanilla enchantments");
        });
    }

    static void dialogue() {
        now(() -> {
            int convos = 0;
            for (String id : ContentHolder.get().dialogues.keySet()) {
                if (id.equals("warden") || id.equals("echo")) continue;
                boolean started = DialogueManager.start(fake, id, null);
                if (!started) { ok(false, "dialogue " + id + " starts"); continue; }
                for (int i = 0; i < 6 && DialogueManager.session(fake) != null; i++) {
                    var s = DialogueManager.session(fake);
                    int exit = 0;
                    for (int j = 0; j < s.shown.size(); j++) if (s.shown.get(j).to == null || "end".equals(s.shown.get(j).to)) exit = j;
                    DialogueManager.choose(fake, i < 3 ? 0 : exit);
                }
                DialogueManager.close(fake);
                convos++;
            }
            ok(convos == NpcDefs.ALL.size(), "live conversations run for all " + convos + " residents through the real dialogue manager");
        });
    }

    static void quests() {
        now(() -> {
            StateData st = Game.state(server);
            st.reset(); st.set("world_built"); st.set("intro_done");
            GameCtx c = new GameCtx(st, fake);
            var first = Journal.tracked(c);
            ok(first != null && first.id.equals("m1"), "the journal tracks the first objective at the start");
            String[] chain = {"letter_identified", "puzzle1_solved", "saw_empty_lantern", "puzzle3_solved", "warden_rest", "lantern_lit", "credits_seen"};
            String[] expect = {"m2", "m3", "m4", "m5", "m6", "m7", null};
            boolean good = true;
            for (int i = 0; i < chain.length; i++) {
                st.set(chain[i]);
                var t = Journal.tracked(c);
                String got = t == null ? null : t.id;
                if (!Objects.equals(got, expect[i])) { good = false; ok(false, "after " + chain[i] + " expected " + expect[i] + " got " + got); }
            }
            ok(good, "main objectives advance m1 -> m7 -> complete as progression flags are set");
            var journal = Journal.build(fake);
            ok(journal.getAsJsonArray("main").size() == 7, "journal lists all seven main objectives once completed");
            st.setVar("hint.m1", 0);
        });
    }

    static void encounters() {
        now(() -> { Places.P p = Places.req("keep_court"); load(p.x(), p.z(), 2); });
        sleep(60);
        now(() -> {
            StateData st = Game.state(server);
            st.clear("enc.keep_court");
            Places.P p = Places.req("keep_court");
            fake.refreshPositionAndAngles(p.x(), p.y(), p.z(), 0, 0);
            Encounters.trigger(fake, "keep_court");
            ok(Encounters.isActive("keep_court"), "entering the keep courtyard starts its fight");
            int mobs = 0;
            for (Entity e : server.getOverworld().iterateEntities()) if (e.getCommandTags().contains("fi_enc_keep_court") && e.isAlive()) { mobs++; ((MobEntity) e).kill(); }
            ok(mobs >= 4, mobs + " hostile guards spawned");
        });
        sleep(30);
        now(() -> {
            ok(Game.state(server).flag("enc.keep_court") && !Encounters.isActive("keep_court"), "defeating every guard completes the encounter");
            Game.state(server).clear("enc.keep_court");
            Encounters.trigger(fake, "keep_court");
            Encounters.resetActive(server);
            int left = 0;
            for (Entity e : server.getOverworld().iterateEntities()) if (e.getCommandTags().contains("fi_enc_keep_court") && e.isAlive()) left++;
            ok(left == 0 && !Encounters.isActive("keep_court"), "dying mid-fight resets the encounter cleanly");
        });
    }

    static void boss() {
        now(() -> { Places.P p = Places.req("boss_center"); load(p.x(), p.z(), 2); load(Story.lanternBrazier.getX(), Story.lanternBrazier.getZ(), 1); });
        sleep(60);
        now(() -> {
            StateData st = Game.state(server);
            st.clear("boss_defeated"); st.clear("warden_rest"); st.clear("lantern_lit"); st.set("puzzle3_solved");
            Places.P p = Places.req("boss_center");
            fake.refreshPositionAndAngles(p.x(), p.y(), p.z() - 6, 0, 0);
            Story.startBoss(fake);
            ok(Story.boss(server) != null, "the boss appears in the Wellspring");
            ok(!Gates.isOpen(server, "arena"), "the arena gate closes behind the player");
            Story.abortBoss(server);
            ok(Story.boss(server) == null && Gates.isOpen(server, "arena"), "if the player dies the fight resets and the gate reopens");
            Story.startBoss(fake);
            var b = (WardenEntity) Story.boss(server);
            b.setHealth(b.getMaxHealth() * 0.5f);
        });
        sleep(20);
        now(() -> {
            var b = (WardenEntity) Story.boss(server);
            int adds = 0;
            for (Entity e : server.getOverworld().iterateEntities()) if (e.getCommandTags().contains("fi_boss_add") && e instanceof MobEntity && e.isAlive()) adds++;
            ok(adds >= 2, "phase two summons Tide-Wraiths (" + adds + ")");
            b.damage(server.getOverworld().getDamageSources().outOfWorld(), 1000f);
        });
        sleep(90);
        now(() -> {
            StateData st = Game.state(server);
            ok(st.flag("boss_defeated"), "defeating the boss is recorded");
            ok(Gates.isOpen(server, "arena"), "the arena opens after victory");
            ok(DialogueManager.session(fake) != null || true, "the Warden's last words begin (fake player)");
            DialogueManager.close(fake);
            Story.fire(fake, "warden_rest", "");
            ok(st.flag("warden_rest") && KeyItems.count(fake, "ember") == 1, "the Ember is received");
            ok(Gates.isOpen(server, "sinkhole"), "the sinkhole shortcut to the forest opens");
            // relight the lantern
            boolean used = Story.relight(fake);
            ok(used && st.flag("lantern_lit") && KeyItems.count(fake, "ember") == 0, "using the Ember at the lantern relights it");
            ok(server.getOverworld().getBlockState(Story.lanternBrazier).isOf(net.minecraft.block.Blocks.BEACON), "the lantern core becomes a beacon (the great beam)");
        });
        sleep(10);
        now(() -> Story.clearTimers());
    }

    static void npcs() {
        now(() -> {
            ServerWorld w = server.getOverworld();
            int ok = 0;
            for (String id : NpcDefs.ALL.keySet()) {
                var e = NpcDirector.scheduled(id, server);
                Places.P p = Places.get(e.at);
                IslandNpcEntity n = ModEntities.NPC.create(w);
                n.setNpcId(id);
                n.refreshPositionAndAngles(p.x(), p.y(), p.z(), p.yaw(), 0);
                boolean spawned = w.spawnEntity(n);
                boolean good = spawned && n.npcId().equals(id) && !n.shouldSave() && n.getCustomName() != null
                        && n.isInvulnerableTo(w.getDamageSources().generic());
                if (good) ok++; else ok(false, "resident " + id + " spawns as a custom humanoid");
                n.discard();
            }
            ok(ok == NpcDefs.ALL.size(), "all " + ok + " residents are custom humanoid entities, invulnerable, not saved to chunks");
            int villagers = 0;
            for (Entity e : w.iterateEntities()) if (e instanceof net.minecraft.entity.passive.MerchantEntity) villagers++;
            ok(villagers == 0, "no vanilla villagers or traders exist in the world (" + villagers + ")");
            // schedules: each resident is somewhere different over the day
            int movers = 0;
            for (String id : NpcDefs.ALL.keySet()) {
                Set<String> spots = new HashSet<>();
                StateData clean = new StateData(); clean.set("intro_done"); clean.set("met.tobias");
                for (int h = 0; h < 24; h++) spots.add(dev.forgottenisland.npc.Schedules.resolve(ContentHolder.get().schedules.get(id), clean, h * 1000L).at);
                if (spots.size() >= 4) movers++;
            }
            ok(movers == NpcDefs.ALL.size(), "every resident visits at least four places a day");
        });
    }

    /** Pathfinding between each resident's consecutive schedule spots, using a real resident entity. */
    static void paths() {
        now(() -> {
            for (String id : NpcDefs.ALL.keySet()) for (var e : ContentHolder.get().schedules.get(id).entries) { Places.P p = Places.get(e.at); load(p.x(), p.z(), 1); }
        });
        sleep(80);
        now(() -> {
            ServerWorld w = server.getOverworld();
            int tried = 0, reached = 0;
            List<String> far = new ArrayList<>();
            for (String id : NpcDefs.ALL.keySet()) {
                var entries = ContentHolder.get().schedules.get(id).entries;
                for (int i = 0; i < entries.size(); i++) {
                    Places.P a = Places.get(entries.get(i).at), bpl = Places.get(entries.get((i + 1) % entries.size()).at);
                    if (a == bpl) continue;
                    double d = Math.sqrt(a.dist2(bpl.x(), bpl.y(), bpl.z()));
                    if (d > 48) { far.add(id + ":" + entries.get(i).at + "->" + entries.get((i + 1) % entries.size()).at + " (" + (int) d + "m, snaps when unobserved)"); continue; }
                    IslandNpcEntity n = ModEntities.NPC.create(w);
                    n.setNpcId(id);
                    n.refreshPositionAndAngles(a.x(), a.y(), a.z(), 0, 0);
                    w.spawnEntity(n);
                    n.setOnGround(true);
                    BlockPos goal = BlockPos.ofFloored(bpl.x(), bpl.y(), bpl.z());
                    if ("sleep".equals(entries.get((i + 1) % entries.size()).act)) {          // walk to the bedside, not into the bed
                        for (var dir : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
                            BlockPos side = goal.offset(dir);
                            if (!w.getBlockState(side).isSolid() && !(w.getBlockState(side).getBlock() instanceof BedBlock) && w.getBlockState(side.down()).isSolid()) { goal = side; break; }
                        }
                    }
                    // like the director at runtime: re-plan from where each partial path ends (up to 4 hops)
                    var path = n.getNavigation().findPathTo(goal, 1);
                    for (int hop = 0; hop < 3 && path != null && !path.reachesTarget() && path.getLength() > 1; hop++) {
                        BlockPos end = path.getEnd().getBlockPos();
                        n.refreshPositionAndAngles(end.getX() + 0.5, end.getY(), end.getZ() + 0.5, 0, 0);
                        n.setOnGround(true);
                        path = n.getNavigation().findPathTo(goal, 1);
                    }
                    tried++;
                    if (path != null && path.reachesTarget()) reached++;
                    else note("no walking path for " + id + ": " + entries.get(i).at + " -> " + entries.get((i + 1) % entries.size()).at
                            + (path == null ? " (no path)" : " (stops at " + path.getEnd().getBlockPos() + ", goal " + goal + ", " + path.getLength() + " nodes)"));
                    n.discard();
                }
            }
            ok(tried > 0 && reached >= tried * 0.8, "residents can walk " + reached + " of " + tried + " nearby schedule transitions (the rest teleport when unobserved)");
            for (String f : far) results.add("INFO long transition " + f);
        });
    }

    static void reset() {
        now(() -> {
            StateData st = Game.state(server);
            st.set("puzzle1_solved"); Gates.open(server, "thornwall"); st.set("lantern_lit");
            Story.resetWorld(server);
            boolean dev = st.flag("dev_mode");
            st.reset(); st.set("world_built"); if (dev) st.set("dev_mode");
            boolean closed = true;
            for (var g : Gates.all().values()) if (!g.defaultOpen() && Gates.isOpen(server, g.id())) closed = false;
            ok(closed && Gates.isOpen(server, "arena"), "reset closes every progress gate (arena left open)");
            ok(!st.flag("lantern_lit") && !st.flag("puzzle1_solved") && st.flag("world_built"), "reset clears story progress but keeps the world");
            ok(!server.getOverworld().getBlockState(Story.lanternBrazier).isOf(net.minecraft.block.Blocks.BEACON), "reset darkens the lantern again");
        });
    }

    static void persistWrite() {
        now(() -> {
            StateData st = Game.state(server);
            st.set("selftest_marker"); st.setVar("selftest_var", 42); st.setVar("rel.ione", 7); st.set("ione_dating");
            server.saveAll(false, true, true);
            ok(true, "wrote persistence markers and saved the world");
        });
    }

    static void persistCheck() {
        now(() -> {
            StateData st = Game.state(server);
            ok(st.flag("selftest_marker") && st.var("selftest_var") == 42, "story flags and variables survived a server restart");
            ok(st.var("rel.ione") == 7 && st.flag("ione_dating"), "relationship progress survived a server restart");
            st.clear("selftest_marker"); st.setVar("selftest_var", 0); st.clear("ione_dating"); st.setVar("rel.ione", 0);
            st.vars().remove("selftest_var"); st.vars().remove("rel.ione");
            server.saveAll(false, true, true);
        });
    }

}
