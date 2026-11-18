package dev.forgottenisland;

import dev.forgottenisland.content.Content;
import dev.forgottenisland.core.Cond;
import dev.forgottenisland.core.StateData;
import dev.forgottenisland.core.StateView;

import java.util.*;

/**
 * Plays dialogue files the same way DialogueManager does, without a game: conditions, effects,
 * inventory (give/take), zones (at:), time of day, and the story events that change state.
 */
public class DialogueSim implements StateView {
    public final Content content;
    public final StateData state = new StateData();
    public final Map<String, Integer> inventory = new HashMap<>();
    public final Set<String> at = new HashSet<>();
    public long time = 6000;
    public final List<String> log = new ArrayList<>();
    private String npc, node; private List<Content.Option> shown = new ArrayList<>();

    public DialogueSim(Content c) { this.content = c; }

    @Override public boolean flag(String n) { return n.startsWith("at:") ? at.contains(n.substring(3)) : state.flag(n); }
    @Override public int var(String n) { return state.var(n); }
    @Override public boolean hasItem(String id) { return inventory.getOrDefault(id, 0) > 0; }
    @Override public long timeOfDay() { return time; }

    public void give(String id) { inventory.merge(id, 1, Integer::sum); state.set("got." + id); }

    /** Starts a conversation; returns the node id entered. */
    public String talk(String who) {
        Content.Dialogue d = content.dialogues.get(who);
        if (d == null) throw new AssertionError("no dialogue " + who);
        for (Content.Entry e : d.start) if (Cond.test(e.if_, this)) { npc = who; state.set("met." + who); enter(e.node); return node; }
        throw new AssertionError("no start entry matched for " + who);
    }

    private void enter(String id) {
        Content.Node n = content.dialogues.get(npc).nodes.get(id);
        if (n == null) throw new AssertionError("missing node " + npc + "." + id);
        node = id;
        log.add(npc + "." + id);
        for (String e : n.doo) effect(e);
        shown = new ArrayList<>();
        for (Content.Option o : n.options) if (Cond.test(o.if_, this)) shown.add(o);
        if (shown.isEmpty()) { Content.Option o = new Content.Option(); o.to = n.to == null ? "end" : n.to; o.text = "[continue]"; shown.add(o); }
    }

    public boolean open() { return npc != null; }
    public String node() { return node; }
    public List<String> options() { return shown.stream().map(o -> o.text).toList(); }

    /** Picks the first visible option containing the given text (case-insensitive). */
    public void choose(String text) {
        for (Content.Option o : shown) if (o.text.toLowerCase().contains(text.toLowerCase())) { pick(o); return; }
        throw new AssertionError("no option '" + text + "' at " + npc + "." + node + "; options=" + options());
    }
    public void next() { pick(shown.get(0)); }
    private void pick(Content.Option o) {
        for (String e : o.doo) effect(e);
        if (npc == null) return;
        if (o.to == null || o.to.equals("end")) { npc = null; return; }
        enter(o.to);
    }
    /** Clicks the first option repeatedly until the conversation ends (max 40 steps). */
    public void finish() {
        for (int i = 0; i < 40 && open(); i++) {
            Content.Option exit = shown.stream().filter(o -> o.to == null || o.to.equals("end")).findFirst().orElse(null);
            pick(exit != null ? exit : shown.get(0));
        }
        if (open()) throw new AssertionError("conversation did not end: " + node);
    }

    void effect(String e) {
        int i = e.indexOf(':');
        String v = i < 0 ? e : e.substring(0, i), a = i < 0 ? "" : e.substring(i + 1);
        switch (v) {
            case "set" -> state.set(a);
            case "clear" -> state.clear(a);
            case "var" -> { String[] kv = a.split("="); state.setVar(kv[0].trim(), Integer.parseInt(kv[1].trim())); }
            case "add" -> { String[] kv = a.split(":"); state.addVar(kv[0], Integer.parseInt(kv[1])); }
            case "rel" -> { String[] kv = a.split(":"); state.addVar("rel." + kv[0], Integer.parseInt(kv[1])); }
            case "give" -> give(a.split(":")[0]);
            case "take" -> { String id = a.split(":")[0]; inventory.computeIfPresent(id, (k, n) -> 0); state.set("used." + id); }
            case "event" -> event(a.split(":", 2)[0], a.contains(":") ? a.split(":", 2)[1] : "");
            case "checkpoint", "sound", "toast", "secret", "book" -> {}
            default -> throw new AssertionError("unknown effect " + e);
        }
    }

    void event(String name, String arg) {
        switch (name) {
            case "open" -> state.set("gate." + arg);
            case "close" -> state.clear("gate." + arg);
            case "warden_rest" -> { state.set("warden_rest"); give("ember"); state.set("gate.sinkhole"); }
            case "sail_out" -> { npc = null; state.set("sol_sailing"); }
            case "sail_back" -> { npc = null; state.clear("sol_sailing"); }
            case "credits" -> state.set("credits_seen");
            case "festival" -> state.set("festival");
            case "wait_evening" -> time = 11800;
            case "wait_night" -> time = 14000;
            case "rest" -> time = 0;
            case "hint" -> npc = null;
            default -> {}
        }
    }

    public Content.Objective objective(String id) { return content.objectives.stream().filter(o -> o.id.equals(id)).findFirst().orElseThrow(); }
    public boolean active(String id) { var o = objective(id); return Cond.test(o.active, this) && !Cond.test(o.done, this); }
    public boolean done(String id) { return Cond.test(objective(id).done, this); }
}
