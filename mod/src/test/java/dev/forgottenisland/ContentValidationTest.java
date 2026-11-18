package dev.forgottenisland;

import dev.forgottenisland.content.Content;
import dev.forgottenisland.core.Cond;
import dev.forgottenisland.game.KeyItems;
import dev.forgottenisland.game.Places;
import dev.forgottenisland.game.Story;
import dev.forgottenisland.npc.NpcDefs;
import org.junit.jupiter.api.Test;

import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ContentValidationTest {
    @Test void contentHasNoBrokenReferences() throws Exception {
        Content c = TestContent.load();
        List<String> errs = c.validate(Places.names(), KeyItems.ids(), Story.eventNames());
        assertTrue(errs.isEmpty(), "content problems:\n" + String.join("\n", errs));
    }

    @Test void everyNodeIsReachable() throws Exception {
        Content c = TestContent.load();
        List<String> orphans = new ArrayList<>();
        for (Content.Dialogue d : c.dialogues.values()) {
            Set<String> r = Content.reachable(d);
            for (String id : d.nodes.keySet()) if (!r.contains(id)) orphans.add(d.npc + "." + id);
        }
        assertTrue(orphans.isEmpty(), "unreachable dialogue nodes: " + orphans);
    }

    @Test void everyResidentHasDialogueScheduleAndSkin() throws Exception {
        Content c = TestContent.load();
        for (String id : NpcDefs.ALL.keySet()) {
            assertTrue(c.dialogues.containsKey(id), "dialogue for " + id);
            assertTrue(c.schedules.containsKey(id), "schedule for " + id);
            Path skin = Paths.get("src/main/resources/assets/forgotten_island/textures/entity/npc/" + id + ".png");
            assertTrue(Files.exists(skin), "skin for " + id);
            var img = javax.imageio.ImageIO.read(skin.toFile());
            assertEquals(64, img.getWidth()); assertEquals(64, img.getHeight());
            // must use the second (overlay) layer somewhere — layered skin detail
            // second-layer regions: hat, jacket, sleeves, trouser legs
            int[][] regions = {{32, 0, 64, 16}, {16, 32, 40, 48}, {40, 32, 56, 48}, {48, 48, 64, 64}, {0, 32, 16, 48}, {0, 48, 16, 64}};
            int overlayPixels = 0;
            for (int[] r : regions) for (int y = r[1]; y < r[3]; y++) for (int x = r[0]; x < r[2]; x++) if ((img.getRGB(x, y) >>> 24) != 0) overlayPixels++;
            assertTrue(overlayPixels >= 10, id + " skin uses the overlay layer for layered detail (" + overlayPixels + " px)");
        }
        assertTrue(Files.exists(Paths.get("src/main/resources/assets/forgotten_island/textures/entity/npc/warden.png")));
    }

    /** Catches typos: every flag tested by a condition must be set by some effect or by game code. */
    @Test void everyConditionFlagCanBecomeTrue() throws Exception {
        Content c = TestContent.load();
        Set<String> settable = new HashSet<>();
        List<String> allConds = new ArrayList<>();
        for (Content.Dialogue d : c.dialogues.values()) {
            for (Content.Entry e : d.start) allConds.add(e.if_);
            for (Content.Bark b : d.barks) allConds.add(b.if_);
            for (Content.Node n : d.nodes.values()) {
                collect(n.doo, settable);
                for (Content.Option o : n.options) { allConds.add(o.if_); collect(o.doo, settable); }
            }
        }
        for (Content.Objective o : c.objectives) { allConds.add(o.active); allConds.add(o.done); }
        for (var s : c.schedules.values()) for (var ov : s.overrides) allConds.add(ov.if_);
        for (var ch : c.chats) allConds.add(ch.if_);
        for (var p : c.people) for (var l : p.lines) allConds.add(l.if_);
        // flags set directly by game code
        settable.addAll(List.of("intro_done", "arrived", "found_log", "saw_empty_lantern", "puzzle1_solved", "puzzle2_solved", "puzzle3_solved",
                "boss_defeated", "warden_rest", "lantern_lit", "festival", "credits_seen", "game_complete", "sol_sailing", "poly_open", "sea_gate_found"));
        Set<String> prefixes = Set.of("met.", "entered.", "secret.", "got.", "used.", "gate.", "enc.", "waystone.", "rel.", "hint.", "at:", "has:", "time:");
        Set<String> vars = Set.of("dating", "secrets", "waystones_lit", "checkpoint");
        List<String> bad = new ArrayList<>();
        for (String cond : allConds) for (String name : Cond.names(cond)) {
            if (prefixes.stream().anyMatch(name::startsWith) || vars.contains(name) || settable.contains(name)) continue;
            bad.add(name + "  (in: " + cond + ")");
        }
        assertTrue(bad.isEmpty(), "flags tested but never set:\n" + String.join("\n", new TreeSet<>(bad)));
    }

    static void collect(List<String> effects, Set<String> out) {
        for (String e : effects) {
            if (e.startsWith("set:")) out.add(e.substring(4));
            if (e.startsWith("event:open:")) out.add("gate." + e.substring(11));
        }
    }

    @Test void objectivesFormASingleMainChain() throws Exception {
        Content c = TestContent.load();
        List<Content.Objective> main = c.objectives.stream().filter(Content.Objective::isMain).toList();
        assertTrue(main.size() >= 6, "about six main objectives");
        for (int i = 1; i < main.size(); i++) assertEquals(main.get(i - 1).done, main.get(i).active, "objective " + main.get(i).id + " starts when the previous one finishes");
        assertEquals(3, c.objectives.stream().filter(o -> "side".equals(o.kind)).count(), "three side quests");
        for (Content.Objective o : c.objectives) assertTrue(o.hints.size() >= 3, o.id + " has three tiers of hints");
    }

    @Test void secretsAreAtLeastTenAndEachHasAZone() throws Exception {
        Content c = TestContent.load();
        assertTrue(c.secrets.size() >= 10);
        for (Content.Secret s : c.secrets) assertNotNull(dev.forgottenisland.game.Zones.get("secret_" + s.id), "zone for secret " + s.id);
    }

    @Test void schedulesCoverTheDayAndResolveEverywhere() throws Exception {
        Content c = TestContent.load();
        var st = new dev.forgottenisland.core.StateData();
        for (var e : c.schedules.entrySet()) for (int h = 0; h < 24; h++) {
            var entry = dev.forgottenisland.npc.Schedules.resolve(e.getValue(), st, h * 1000L);
            assertNotNull(entry, e.getKey() + " at hour " + h);
            assertNotNull(Places.get(entry.at), "place " + entry.at);
        }
    }

    @Test void booksFitOnPages() throws Exception {
        for (var b : TestContent.load().books.entrySet()) {
            assertNotNull(KeyItems.ids().stream().filter(k -> true).findAny());
            for (String page : b.getValue().pages) assertTrue(page.length() <= 300, "book " + b.getKey() + " page too long: " + page.length());
            assertTrue(b.getValue().title.length() <= 32, "title length " + b.getKey());
        }
    }
}
