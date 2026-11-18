package dev.forgottenisland.game;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.forgottenisland.build.Layout;
import dev.forgottenisland.core.StateData;
import dev.forgottenisland.dialogue.DialogueManager;
import dev.forgottenisland.npc.NpcDefs;
import dev.forgottenisland.npc.NpcDirector;
import dev.forgottenisland.test.SelfTest;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.util.*;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/** /fi developer commands. All require operator permission (single-player: enable cheats). */
public final class Commands {
    private Commands() {}

    /** Spoiler-free progression jumps: cumulative effect lists. */
    public static final LinkedHashMap<String, List<String>> STAGES = new LinkedHashMap<>();
    static {
        STAGES.put("village", List.of("set:intro_done", "set:met.tobias", "set:arrived", "checkpoint:cp_inn"));
        STAGES.put("forest", List.of("set:letter_identified", "set:seek_song", "set:knows_song", "set:stones_awake", "set:wren_guided", "checkpoint:cp_stones"));
        STAGES.put("keep", List.of("set:puzzle1_solved", "event:open:thornwall", "checkpoint:cp_keep_gate"));
        STAGES.put("undercroft", List.of("set:found_log", "set:puzzle2_solved", "event:open:tower_door", "set:saw_empty_lantern", "give:warden_key", "checkpoint:cp_keep_court"));
        STAGES.put("vault", List.of("take:warden_key", "event:open:cistern", "set:echo_1", "set:echo_2", "set:echo_3", "event:open:echo_door", "checkpoint:cp_vault"));
        STAGES.put("boss", List.of("set:puzzle3_solved", "event:open:vault_door", "checkpoint:cp_wellspring"));
        STAGES.put("lantern", List.of("set:boss_defeated", "event:warden_rest", "checkpoint:cp_lantern"));
    }

    private static final SuggestionProvider<ServerCommandSource> PLACES = (c, b) -> CommandSource.suggestMatching(Places.names(), b);
    private static final SuggestionProvider<ServerCommandSource> STAGE_S = (c, b) -> CommandSource.suggestMatching(STAGES.keySet(), b);
    private static final SuggestionProvider<ServerCommandSource> NPCS = (c, b) -> CommandSource.suggestMatching(NpcDefs.ALL.keySet(), b);

    public static void register() {
        CommandRegistrationCallback.EVENT.register((d, reg, env) -> build(d));
    }

    static int ok(CommandContext<ServerCommandSource> c, String msg) { c.getSource().sendFeedback(() -> Text.literal(msg), false); return 1; }
    static StateData st(CommandContext<ServerCommandSource> c) { return Game.state(c.getSource().getServer()); }

    private static void build(CommandDispatcher<ServerCommandSource> d) {
        d.register(literal("fi").requires(s -> s.hasPermissionLevel(2))
            .then(literal("dev")
                .then(literal("on").executes(c -> { st(c).set("dev_mode"); refresh(c); return ok(c, "Developer mode ON: story text is hidden; dialogue shows node ids."); }))
                .then(literal("off").executes(c -> { st(c).clear("dev_mode"); refresh(c); return ok(c, "Developer mode OFF."); })))
            .then(literal("status").executes(c -> {
                StateData s = st(c);
                ok(c, "flags=" + s.flags().size() + " vars=" + s.vars().size() + " checkpoint=" + Checkpoints.current(c.getSource().getServer())
                        + " secrets=" + s.var("secrets") + " dating=" + s.var("dating") + " dev=" + s.flag("dev_mode"));
                for (String n : NpcDefs.ALL.keySet()) { var e = NpcDirector.scheduled(n, c.getSource().getServer()); ok(c, "  " + n + " -> " + (e == null ? "?" : e.at + " (" + e.act + ")") + (NpcDirector.live(n) != null ? " [live]" : "")); }
                return 1;
            }))
            .then(literal("flags").executes(c -> ok(c, String.join(", ", st(c).flags()) + " | " + st(c).vars())))
            .then(literal("flag")
                .then(literal("set").then(argument("name", StringArgumentType.greedyString()).executes(c -> { st(c).set(StringArgumentType.getString(c, "name")); return ok(c, "set"); })))
                .then(literal("clear").then(argument("name", StringArgumentType.greedyString()).executes(c -> { st(c).clear(StringArgumentType.getString(c, "name")); return ok(c, "cleared"); }))))
            .then(literal("var").then(argument("name", StringArgumentType.string()).then(argument("value", IntegerArgumentType.integer()).executes(c -> {
                st(c).setVar(StringArgumentType.getString(c, "name"), IntegerArgumentType.getInteger(c, "value")); return ok(c, "ok"); }))))
            .then(literal("effect").then(argument("effect", StringArgumentType.greedyString()).executes(c -> {
                Effects.run(c.getSource().getPlayerOrThrow(), StringArgumentType.getString(c, "effect")); return ok(c, "ran"); })))
            .then(literal("warp").then(argument("place", StringArgumentType.string()).suggests(PLACES).executes(c -> {
                Places.P p = Places.get(StringArgumentType.getString(c, "place"));
                if (p == null) return ok(c, "Unknown place");
                ServerPlayerEntity pl = c.getSource().getPlayerOrThrow();
                pl.stopRiding();
                pl.teleport(pl.getServerWorld(), p.x(), p.y(), p.z(), p.yaw(), 0);
                return ok(c, "Warped.");
            })))
            .then(literal("npc").then(argument("id", StringArgumentType.string()).suggests(NPCS).executes(c -> {
                String id = StringArgumentType.getString(c, "id");
                var e = NpcDirector.scheduled(id, c.getSource().getServer());
                Places.P p = e == null ? null : Places.get(e.at);
                if (p == null) return ok(c, "No schedule");
                ServerPlayerEntity pl = c.getSource().getPlayerOrThrow();
                pl.teleport(pl.getServerWorld(), p.x() + 1.5, p.y(), p.z() + 1.5, p.yaw(), 0);
                return ok(c, id + " should be at " + e.at + " (" + e.act + ")");
            })))
            .then(literal("stage").then(argument("stage", StringArgumentType.string()).suggests(STAGE_S).executes(c -> {
                String stage = StringArgumentType.getString(c, "stage");
                if (!STAGES.containsKey(stage)) return ok(c, "Unknown stage. Options: " + STAGES.keySet());
                ServerPlayerEntity pl = c.getSource().getPlayerOrThrow();
                if (!st(c).flag("intro_done")) { Story.finishIntro(pl); pl.stopRiding(); }
                for (var e : STAGES.entrySet()) { Effects.run(pl, e.getValue()); if (e.getKey().equals(stage)) break; }
                Checkpoints.respawn(pl);
                return ok(c, "Jumped to stage '" + stage + "'.");
            })))
            .then(literal("puzzle").then(argument("which", StringArgumentType.string()).suggests((c, b) -> CommandSource.suggestMatching(List.of("stones", "mirrors", "slide"), b))
                .then(literal("solve").executes(c -> { var s = c.getSource().getServer();
                    switch (StringArgumentType.getString(c, "which")) { case "stones" -> Puzzles.solveStones(s); case "mirrors" -> Puzzles.solveMirrors(s); default -> Puzzles.solveSlide(s); }
                    return ok(c, "solved"); }))
                .then(literal("reset").executes(c -> { var s = c.getSource().getServer();
                    switch (StringArgumentType.getString(c, "which")) { case "stones" -> Puzzles.resetStones(s); case "mirrors" -> Puzzles.resetMirrors(s); default -> Puzzles.resetSlide(s, true); }
                    return ok(c, "reset"); }))))
            .then(literal("boss")
                .then(literal("start").executes(c -> { Story.startBoss(c.getSource().getPlayerOrThrow()); return ok(c, "boss start requested"); }))
                .then(literal("abort").executes(c -> { Story.abortBoss(c.getSource().getServer()); return ok(c, "aborted"); }))
                .then(literal("weak").executes(c -> { if (st(c).flag("dev_boss_weak")) st(c).clear("dev_boss_weak"); else st(c).set("dev_boss_weak"); return ok(c, "boss takes x10 damage (testing aid): " + st(c).flag("dev_boss_weak")); })))
            .then(literal("time").then(argument("when", StringArgumentType.string()).suggests((c, b) -> CommandSource.suggestMatching(List.of("morning", "noon", "evening", "night", "midnight"), b)).executes(c -> {
                long t = switch (StringArgumentType.getString(c, "when")) { case "noon" -> 6000; case "evening" -> 11800; case "night" -> 14000; case "midnight" -> 18000; default -> 0; };
                Story.setTime(c.getSource().getServer(), t); return ok(c, "time set");
            })))
            .then(literal("hint").executes(c -> { Journal.hint(c.getSource().getPlayerOrThrow()); return 1; }))
            .then(literal("talk").then(argument("id", StringArgumentType.string()).executes(c -> {
                DialogueManager.start(c.getSource().getPlayerOrThrow(), StringArgumentType.getString(c, "id"), NpcDirector.live(StringArgumentType.getString(c, "id"))); return 1; })))
            .then(literal("choose").then(argument("index", IntegerArgumentType.integer(1)).executes(c -> {
                DialogueManager.choose(c.getSource().getPlayerOrThrow(), IntegerArgumentType.getInteger(c, "index") - 1); return 1; })))
            .then(literal("reset").executes(c -> {
                var s = c.getSource().getServer();
                boolean dev = st(c).flag("dev_mode");
                Story.resetWorld(s);
                StateData data = st(c);
                data.reset();
                data.set("world_built");
                if (dev) data.set("dev_mode");
                NpcDirector.reset();
                for (var e : s.getOverworld().iterateEntities()) if (e.getCommandTags().contains("fi_npc")) e.discard();
                for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) { DialogueManager.drop(p); Story.startIntro(p); }
                return ok(c, "Adventure reset to the very beginning.");
            }))
            .then(literal("selftest").executes(c -> SelfTest.run(c.getSource(), "all"))
                .then(argument("suite", StringArgumentType.string()).executes(c -> SelfTest.run(c.getSource(), StringArgumentType.getString(c, "suite")))))
            .then(literal("build").executes(c -> { Layout.buildAll(c.getSource()); return 1; })
                .then(argument("part", StringArgumentType.string()).executes(c -> { Layout.build(c.getSource(), StringArgumentType.getString(c, "part")); return 1; })))
            .then(literal("map").executes(c -> { Layout.renderMap(c.getSource()); return 1; }))
            .then(literal("dump").executes(c -> { dump(); return ok(c, "layout_dump.json written"); }))
            .then(literal("gamemode").executes(c -> {
                ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
                p.changeGameMode(p.interactionManager.getGameMode() == GameMode.CREATIVE ? GameMode.ADVENTURE : GameMode.CREATIVE);
                return ok(c, "toggled");
            }))
        );
    }

    /** Exports places and puzzle anchors (used by the automated client playthrough). */
    static void dump() {
        com.google.gson.JsonObject j = new com.google.gson.JsonObject();
        com.google.gson.JsonObject pl = new com.google.gson.JsonObject();
        Places.all().forEach((k, p) -> { var a = new com.google.gson.JsonArray(); a.add(p.x()); a.add(p.y()); a.add(p.z()); a.add(p.yaw()); pl.add(k, a); });
        j.add("places", pl);
        var stones = new com.google.gson.JsonObject();
        Puzzles.STONE_BUTTONS.forEach((pos, animal) -> stones.add(animal, arr(pos)));
        j.add("stone_buttons", stones);
        var mirrors = new com.google.gson.JsonArray();
        for (int[] c : PuzzleLayouts.mirrorCells()) mirrors.add(arr(Puzzles.mirrorOrigin.add(c[0], 0, c[1])));
        j.add("mirrors", mirrors);
        j.add("slide_origin", arr(Puzzles.slideOrigin));
        j.add("lantern", arr(Story.lanternBrazier));
        var gates = new com.google.gson.JsonObject();
        Gates.all().forEach((k, g) -> gates.add(k, arr(g.closed().keySet().iterator().next())));
        j.add("gates", gates);
        try (var w = new java.io.FileWriter("layout_dump.json")) { w.write(j.toString()); } catch (Exception e) { throw new RuntimeException(e); }
    }
    static com.google.gson.JsonArray arr(net.minecraft.util.math.BlockPos p) { var a = new com.google.gson.JsonArray(); a.add(p.getX()); a.add(p.getY()); a.add(p.getZ()); return a; }

    private static void refresh(CommandContext<ServerCommandSource> c) {
        for (ServerPlayerEntity p : c.getSource().getServer().getPlayerManager().getPlayerList()) Game.syncHud(p, true);
    }
}
