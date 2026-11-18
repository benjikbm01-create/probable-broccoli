package dev.forgottenisland.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.forgottenisland.content.Content;
import dev.forgottenisland.core.Cond;
import dev.forgottenisland.core.GameCtx;
import dev.forgottenisland.core.StateData;
import dev.forgottenisland.net.Net;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

/** Builds the journal (objectives, people, secrets, hints) from content + state. */
public final class Journal {
    private Journal() {}

    public static boolean active(Content.Objective o, GameCtx c) { return Cond.test(o.active, c); }
    public static boolean done(Content.Objective o, GameCtx c) { return Cond.test(o.done, c); }

    /** The objective the HUD tracks: first unfinished main objective, else first unfinished other. */
    public static Content.Objective tracked(GameCtx c) {
        Content.Objective fallback = null;
        for (Content.Objective o : ContentHolder.get().objectives) {
            if (!active(o, c) || done(o, c)) continue;
            if (o.isMain()) return o;
            if (fallback == null) fallback = o;
        }
        return fallback;
    }

    public static String status(String npc, StateData s) {
        if (s.flag(npc + "_dating")) return "Girlfriend";
        int r = s.var("rel." + npc);
        if (!s.flag("met." + npc)) return "Not yet met";
        if (s.flag(npc + "_bestfriend") || r >= 6) return "Close friend";
        if (r >= 3) return "Friend";
        return "Acquaintance";
    }

    public static JsonObject build(ServerPlayerEntity p) {
        StateData s = Game.state(p.getServer());
        GameCtx c = new GameCtx(s, p);
        boolean dev = Game.devMode(p.getServer());
        JsonObject root = new JsonObject();
        JsonArray main = new JsonArray(), side = new JsonArray(), personal = new JsonArray();
        for (Content.Objective o : ContentHolder.get().objectives) {
            boolean act = active(o, c), dn = done(o, c);
            if (!act && !dn) continue;
            JsonObject j = new JsonObject();
            j.addProperty("t", dev ? o.id + " (" + o.kind + ")" : o.title);
            j.addProperty("d", dev ? "active=" + o.active + " | done=" + o.done : o.text);
            j.addProperty("done", dn);
            (o.isMain() ? main : "side".equals(o.kind) ? side : personal).add(j);
        }
        root.add("main", main); root.add("side", side); root.add("personal", personal);
        JsonArray people = new JsonArray();
        for (Content.Person pe : ContentHolder.get().people) {
            JsonObject j = new JsonObject();
            boolean met = s.flag("met." + pe.npc);
            j.addProperty("name", met || dev ? pe.name : "???");
            j.addProperty("role", met || dev ? pe.role : "Someone you have not met yet.");
            j.addProperty("status", status(pe.npc, s));
            String line = "";
            if (met) for (Content.PersonLine l : pe.lines) if (Cond.test(l.if_, c)) { line = l.text; break; }
            j.addProperty("line", dev ? "rel=" + s.var("rel." + pe.npc) : line);
            people.add(j);
        }
        root.add("people", people);
        JsonArray found = new JsonArray();
        int total = ContentHolder.get().secrets.size(), n = 0;
        for (Content.Secret sc : ContentHolder.get().secrets) if (s.flag("secret." + sc.id)) { n++; found.add(sc.name); }
        root.add("secrets", found);
        root.addProperty("secretTotal", total);
        root.addProperty("secretFound", n);
        Content.Objective t = tracked(c);
        JsonObject hint = new JsonObject();
        if (t != null) {
            int lvl = s.var("hint." + t.id);
            hint.addProperty("for", dev ? t.id : t.title);
            JsonArray shown = new JsonArray();
            for (int i = 0; i < Math.min(lvl, t.hints.size()); i++) shown.add(dev ? "[hint " + (i + 1) + " hidden in dev mode]" : t.hints.get(i));
            hint.add("shown", shown);
            hint.addProperty("more", lvl < t.hints.size());
        }
        root.add("hint", hint);
        root.addProperty("dev", dev);
        root.addProperty("finished", s.flag("game_complete"));
        return root;
    }

    public static void open(ServerPlayerEntity p) {
        ServerPlayNetworking.send(p, new Net.Ui("journal", build(p).toString()));
    }

    /** Reveal the next hint for the tracked objective. */
    public static void hint(ServerPlayerEntity p) {
        StateData s = Game.state(p.getServer());
        Content.Objective t = tracked(new GameCtx(s, p));
        if (t == null) { Game.toast(p, "No hints", "There is nothing pressing right now. Explore!"); return; }
        int lvl = s.var("hint." + t.id);
        if (lvl < t.hints.size()) { s.setVar("hint." + t.id, lvl + 1); lvl++; }
        String text = Game.devMode(p.getServer()) ? "[hint " + lvl + " of " + t.hints.size() + " for " + t.id + "]" : t.hints.get(lvl - 1);
        ServerPlayNetworking.send(p, new Net.Ui("hint", (Game.devMode(p.getServer()) ? t.id : t.title) + "\n" + text));
    }
}
