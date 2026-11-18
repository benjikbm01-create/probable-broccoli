package dev.forgottenisland.content;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import dev.forgottenisland.core.Cond;

import java.util.*;

/**
 * All data-driven game content. Loaded from data/forgotten_island/fi/... (see ContentLoader)
 * or straight from the source tree in unit tests. Pure Java + Gson so it can be validated offline.
 */
public final class Content {
    public static final Gson GSON = new GsonBuilder().create();

    // ---------------------------------------------------------------- model
    public static final class Option { public String text; public String to; public String if_; public List<String> doo = List.of(); }
    public static final class Node { public String text; public String speaker; public String to; public List<String> doo = List.of(); public List<Option> options = List.of(); }
    public static final class Entry { public String if_; public String node; }
    public static final class Bark { public String if_; public String text; }
    public static final class Dialogue {
        public String npc; public List<Entry> start = List.of(); public Map<String, Node> nodes = new LinkedHashMap<>(); public List<Bark> barks = List.of();
    }
    public static final class Objective {
        public String id, kind, title, text, active, done; public int order; public List<String> hints = List.of();
        public boolean isMain() { return "main".equals(kind); }
    }
    public static final class ScheduleEntry { public int from, to; public String at, act; public int r; }
    public static final class ScheduleOverride { public String if_; public List<ScheduleEntry> entries = List.of(); }
    public static final class Schedule { public List<ScheduleEntry> entries = List.of(); public List<ScheduleOverride> overrides = List.of(); }
    public static final class Chat { public String a, b, if_; public List<String> lines = List.of(); }
    public static final class Book { public String title, author; public List<String> pages = List.of(); }
    public static final class PersonLine { public String if_; public String text; }
    public static final class Person { public String npc, name, role; public List<PersonLine> lines = List.of(); public String relvar; }
    public static final class Secret { public String id, name, hint; }

    public final Map<String, Dialogue> dialogues = new LinkedHashMap<>();
    public final List<Objective> objectives = new ArrayList<>();
    public final Map<String, Schedule> schedules = new LinkedHashMap<>();
    public final List<Chat> chats = new ArrayList<>();
    public final Map<String, Book> books = new LinkedHashMap<>();
    public final List<Person> people = new ArrayList<>();
    public final List<Secret> secrets = new ArrayList<>();
    public final Map<String, String> strings = new LinkedHashMap<>();

    // ---------------------------------------------------------------- loading
    /** "if" and "do" are reserved words in Java; the JSON uses them, so rename before mapping. */
    private static String fixKeys(String json) {
        return json.replace("\"if\":", "\"if_\":").replace("\"do\":", "\"doo\":");
    }

    public void addDialogue(String json) {
        Dialogue d = GSON.fromJson(fixKeys(json), Dialogue.class);
        dialogues.put(d.npc, d);
    }
    public void addQuests(String json) {
        Objective[] arr = GSON.fromJson(JsonParser.parseString(fixKeys(json)).getAsJsonObject().get("objectives"), Objective[].class);
        objectives.addAll(Arrays.asList(arr));
        objectives.sort(Comparator.comparingInt(o -> o.order));
    }
    public void addSchedules(String json) {
        var root = JsonParser.parseString(fixKeys(json)).getAsJsonObject();
        for (var e : root.getAsJsonObject("npcs").entrySet()) schedules.put(e.getKey(), GSON.fromJson(e.getValue(), Schedule.class));
        if (root.has("chats")) chats.addAll(Arrays.asList(GSON.fromJson(root.get("chats"), Chat[].class)));
    }
    public void addBooks(String json) {
        var root = JsonParser.parseString(json).getAsJsonObject();
        for (var e : root.entrySet()) books.put(e.getKey(), GSON.fromJson(e.getValue(), Book.class));
    }
    public void addPeople(String json) {
        var root = JsonParser.parseString(fixKeys(json)).getAsJsonObject();
        people.addAll(Arrays.asList(GSON.fromJson(root.get("people"), Person[].class)));
        if (root.has("secrets")) secrets.addAll(Arrays.asList(GSON.fromJson(root.get("secrets"), Secret[].class)));
        if (root.has("strings")) for (var e : root.getAsJsonObject("strings").entrySet()) strings.put(e.getKey(), e.getValue().getAsString());
    }

    public String str(String key, String fallback) { return strings.getOrDefault(key, fallback); }

    // ---------------------------------------------------------------- validation
    /** Known effect verbs; see Effects for semantics. */
    public static final Set<String> EFFECT_VERBS = Set.of("set", "clear", "var", "add", "rel", "give", "take", "event", "sound", "toast", "checkpoint", "secret", "book");

    /**
     * Checks references across all content. Returns human-readable problems (empty = valid).
     * @param places  known place names (null to skip)
     * @param items   known item ids (null to skip)
     * @param events  known event names (null to skip)
     */
    public List<String> validate(Set<String> places, Set<String> items, Set<String> events) {
        List<String> errs = new ArrayList<>();
        for (Dialogue d : dialogues.values()) {
            String w = "dialogue " + d.npc;
            if (d.start.isEmpty()) errs.add(w + ": no start entries");
            for (Entry e : d.start) { cond(errs, w + " start", e.if_); if (!d.nodes.containsKey(e.node)) errs.add(w + ": start -> missing node " + e.node); }
            for (var ne : d.nodes.entrySet()) {
                String nw = w + "." + ne.getKey();
                Node n = ne.getValue();
                if (n.text == null || n.text.isBlank()) errs.add(nw + ": empty text");
                if (n.to != null && !n.to.equals("end") && !d.nodes.containsKey(n.to)) errs.add(nw + ": to -> missing node " + n.to);
                if (n.to != null && !n.options.isEmpty()) errs.add(nw + ": has both 'to' and options");
                effects(errs, nw, n.doo, items, events);
                for (Option o : n.options) {
                    if (o.text == null || o.text.isBlank()) errs.add(nw + ": option without text");
                    cond(errs, nw + " option", o.if_);
                    effects(errs, nw + " option", o.doo, items, events);
                    if (o.to != null && !o.to.equals("end") && !d.nodes.containsKey(o.to)) errs.add(nw + ": option -> missing node " + o.to);
                }
            }
            for (Bark b : d.barks) cond(errs, w + " bark", b.if_);
        }
        Set<String> ids = new HashSet<>();
        for (Objective o : objectives) {
            if (!ids.add(o.id)) errs.add("objective " + o.id + ": duplicate id");
            cond(errs, "objective " + o.id + " active", o.active);
            cond(errs, "objective " + o.id + " done", o.done);
            if (o.hints.isEmpty()) errs.add("objective " + o.id + ": no hints");
            if (o.title == null || o.text == null) errs.add("objective " + o.id + ": missing title/text");
        }
        for (var se : schedules.entrySet()) {
            List<ScheduleEntry> all = new ArrayList<>(se.getValue().entries);
            for (ScheduleOverride ov : se.getValue().overrides) { cond(errs, "schedule " + se.getKey() + " override", ov.if_); all.addAll(ov.entries); }
            for (ScheduleEntry e : all) {
                if (places != null && !places.contains(e.at)) errs.add("schedule " + se.getKey() + ": unknown place " + e.at);
                if (!Set.of("work", "wander", "sit", "sleep", "stand", "talk").contains(e.act)) errs.add("schedule " + se.getKey() + ": unknown activity " + e.act);
                if (e.from < 0 || e.from > 24000 || e.to < 0 || e.to > 24000) errs.add("schedule " + se.getKey() + ": bad time range");
            }
            if (!coversDay(se.getValue().entries)) errs.add("schedule " + se.getKey() + ": base schedule does not cover the whole day");
        }
        for (Chat c : chats) cond(errs, "chat " + c.a + "/" + c.b, c.if_);
        for (Person p : people) for (PersonLine l : p.lines) cond(errs, "person " + p.npc, l.if_);
        return errs;
    }

    static boolean coversDay(List<ScheduleEntry> entries) {
        boolean[] cov = new boolean[24];
        for (ScheduleEntry e : entries) {
            for (int h = 0; h < 24; h++) {
                int t = h * 1000;
                boolean in = e.from <= e.to ? (t >= e.from && t < e.to) : (t >= e.from || t < e.to);
                if (in) cov[h] = true;
            }
        }
        for (boolean b : cov) if (!b) return false;
        return true;
    }

    private static void cond(List<String> errs, String where, String c) {
        if (c == null) return;
        try { Cond.compile(c); } catch (IllegalArgumentException e) { errs.add(where + ": " + e.getMessage()); }
    }

    private static void effects(List<String> errs, String where, List<String> effs, Set<String> items, Set<String> events) {
        for (String e : effs) {
            int i = e.indexOf(':');
            String verb = i < 0 ? e : e.substring(0, i);
            String arg = i < 0 ? "" : e.substring(i + 1);
            if (!EFFECT_VERBS.contains(verb)) { errs.add(where + ": unknown effect '" + e + "'"); continue; }
            if ((verb.equals("give") || verb.equals("take")) && items != null && !items.contains(arg.split(":")[0])) errs.add(where + ": unknown item in '" + e + "'");
            if (verb.equals("event") && events != null && !events.contains(arg.split(":")[0])) errs.add(where + ": unknown event in '" + e + "'");
            if (verb.equals("add") && arg.split(":").length != 2) errs.add(where + ": add needs name:amount in '" + e + "'");
            if (verb.equals("var") && !arg.contains("=")) errs.add(where + ": var needs name=value in '" + e + "'");
            if (verb.equals("rel") && arg.split(":").length != 2) errs.add(where + ": rel needs npc:amount in '" + e + "'");
        }
    }

    /** Every node reachable from the start entries (used to detect orphaned dialogue in tests). */
    public static Set<String> reachable(Dialogue d) {
        Set<String> seen = new HashSet<>();
        Deque<String> q = new ArrayDeque<>();
        for (Entry e : d.start) q.add(e.node);
        while (!q.isEmpty()) {
            String id = q.poll();
            if (id == null || id.equals("end") || !seen.add(id)) continue;
            Node n = d.nodes.get(id);
            if (n == null) continue;
            if (n.to != null) q.add(n.to);
            for (Option o : n.options) if (o.to != null) q.add(o.to);
        }
        return seen;
    }
}
