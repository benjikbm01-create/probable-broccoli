package dev.voidoutlaws.game;

import dev.voidoutlaws.state.CrewState;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** The crew status panel on the right of the screen (a vanilla scoreboard sidebar). */
public final class Sidebar {
    private Sidebar() {}

    private static final String OBJ = "vo_crew";
    private static final List<String> LAST = new ArrayList<>();

    static void init(MinecraftServer server) {
        Scoreboard sb = server.getScoreboard();
        ScoreboardObjective obj = sb.getNullableObjective(OBJ);
        if (obj == null) {
            obj = sb.addObjective(OBJ, ScoreboardCriterion.DUMMY, Text.literal("✦ " + Game.SHIP_NAME + " ✦").formatted(Formatting.GOLD, Formatting.BOLD),
                    ScoreboardCriterion.RenderType.INTEGER, false, BlankNumberFormat.INSTANCE);
        }
        sb.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, obj);
        LAST.clear();
    }

    private static String cut(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n - 1) + "…";
    }

    static void update(MinecraftServer server) {
        Scoreboard sb = server.getScoreboard();
        ScoreboardObjective obj = sb.getNullableObjective(OBJ);
        if (obj == null) { init(server); obj = sb.getNullableObjective(OBJ); if (obj == null) return; }
        CrewState s = Game.S();
        List<Text> lines = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        String where = s.jumping ? "Jumping -> " + s.jumpDest.display : "Docked: " + s.shipLoc.display;
        add(lines, keys, Text.literal(where).formatted(s.jumping ? Formatting.AQUA : s.shipLoc.color));
        add(lines, keys, Text.literal("Heat: ").formatted(Formatting.GRAY)
                .append(Text.literal(Heat.stars(s.heat) + " " + s.heat).formatted(Heat.color(s.heat))));
        add(lines, keys, Text.literal("Hull: ").formatted(Formatting.GRAY)
                .append(Text.literal(s.hull + "%").formatted(s.hull > 60 ? Formatting.GREEN : s.hull > 30 ? Formatting.YELLOW : Formatting.RED)));
        add(lines, keys, Text.literal("Crew Pot: ").formatted(Formatting.GRAY).append(Text.literal(s.pot + " cr").formatted(Formatting.GOLD)));
        add(lines, keys, Text.literal("Job: ").formatted(Formatting.GRAY)
                .append(Text.literal(s.job == null ? "none" : cut(s.job.shortLine(), 30)).formatted(Formatting.WHITE)));
        add(lines, keys, Text.literal("Goal: ").formatted(Formatting.GRAY).append(Text.literal(cut(Story.goal(), 30)).formatted(Formatting.LIGHT_PURPLE)));
        add(lines, keys, Text.literal("/guide  /crew  /tutorial").formatted(Formatting.DARK_GRAY));

        for (int i = 0; i < lines.size(); i++) {
            String key = keys.get(i);
            if (i < LAST.size() && LAST.get(i).equals(key)) continue;
            ScoreAccess a = sb.getOrCreateScore(ScoreHolder.fromName("vo_line" + i), obj);
            a.setScore(lines.size() - i);
            a.setDisplayText(lines.get(i));
        }
        LAST.clear();
        LAST.addAll(keys);
    }

    private static void add(List<Text> lines, List<String> keys, Text t) {
        lines.add(t);
        keys.add(t.getString() + "|" + t.hashCode());
    }
}
