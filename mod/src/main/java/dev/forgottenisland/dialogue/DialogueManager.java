package dev.forgottenisland.dialogue;

import dev.forgottenisland.ForgottenIsland;
import dev.forgottenisland.content.Content;
import dev.forgottenisland.core.Cond;
import dev.forgottenisland.core.GameCtx;
import dev.forgottenisland.game.ContentHolder;
import dev.forgottenisland.game.Effects;
import dev.forgottenisland.game.Game;
import dev.forgottenisland.net.Net;
import dev.forgottenisland.npc.IslandNpcEntity;
import dev.forgottenisland.npc.NpcDefs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/** Server-authoritative dialogue sessions. The client only displays nodes and sends back the chosen index. */
public final class DialogueManager {
    private DialogueManager() {}

    public static final class Session {
        public final String npc; public final UUID npcEntity; public String node;
        public final List<Content.Option> shown = new ArrayList<>();
        Session(String npc, UUID e) { this.npc = npc; this.npcEntity = e; }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    /** Listeners used by tests/autopilot to observe the dialogue flow. */
    public static final List<java.util.function.BiConsumer<ServerPlayerEntity, String>> NODE_LISTENERS = new ArrayList<>();

    public static Session session(ServerPlayerEntity p) { return SESSIONS.get(p.getUuid()); }
    public static boolean isTalking(IslandNpcEntity npc) {
        for (Session s : SESSIONS.values()) if (s.npcEntity != null && s.npcEntity.equals(npc.getUuid())) return true;
        return false;
    }

    public static boolean start(ServerPlayerEntity p, String npcId, IslandNpcEntity entity) {
        Content.Dialogue d = ContentHolder.get().dialogues.get(npcId);
        if (d == null) { ForgottenIsland.LOG.warn("No dialogue for {}", npcId); return false; }
        GameCtx ctx = new GameCtx(Game.state(p.getServer()), p);
        for (Content.Entry e : d.start) {
            if (Cond.test(e.if_, ctx)) {
                Session s = new Session(npcId, entity == null ? null : entity.getUuid());
                SESSIONS.put(p.getUuid(), s);
                Game.state(p.getServer()).set("met." + npcId);
                enter(p, s, e.node);
                return true;
            }
        }
        return false;
    }

    private static void enter(ServerPlayerEntity p, Session s, String nodeId) {
        Content.Dialogue d = ContentHolder.get().dialogues.get(s.npc);
        Content.Node n = d.nodes.get(nodeId);
        if (n == null) { ForgottenIsland.LOG.error("Missing node {}.{}", s.npc, nodeId); close(p); return; }
        s.node = nodeId;
        for (var l : NODE_LISTENERS) l.accept(p, s.npc + "." + nodeId);
        Effects.run(p, n.doo);
        if (SESSIONS.get(p.getUuid()) != s) return;   // an effect ended the conversation (e.g. a cut-scene)
        GameCtx ctx = new GameCtx(Game.state(p.getServer()), p);
        s.shown.clear();
        for (Content.Option o : n.options) if (Cond.test(o.if_, ctx)) s.shown.add(o);
        List<String> labels = new ArrayList<>();
        boolean dev = Game.devMode(p.getServer());
        for (int i = 0; i < s.shown.size(); i++) {
            Content.Option o = s.shown.get(i);
            labels.add(dev ? "[" + (i + 1) + "] -> " + (o.to == null ? "end" : o.to) : fmt(p, o.text));
        }
        if (s.shown.isEmpty()) {
            Content.Option o = new Content.Option();
            o.to = n.to == null ? "end" : n.to;
            o.text = o.to.equals("end") ? "[Leave]" : "[Continue]";
            s.shown.add(o);
            labels.add(o.text);
        }
        String speaker = n.speaker != null ? n.speaker : NpcDefs.name(s.npc);
        String text = dev ? "[dev] " + s.npc + "." + nodeId + "  (" + n.text.length() + " chars, story text hidden)" : fmt(p, n.text);
        ServerPlayNetworking.send(p, new Net.Dialogue(s.npc, speaker, text, labels));
    }

    public static void choose(ServerPlayerEntity p, int index) {
        Session s = SESSIONS.get(p.getUuid());
        if (s == null || index < 0 || index >= s.shown.size()) { close(p); return; }
        Content.Option o = s.shown.get(index);
        Effects.run(p, o.doo);
        if (SESSIONS.get(p.getUuid()) != s) return;
        if (o.to == null || o.to.equals("end")) close(p);
        else enter(p, s, o.to);
    }

    public static void close(ServerPlayerEntity p) {
        if (SESSIONS.remove(p.getUuid()) != null) ServerPlayNetworking.send(p, new Net.Ui("close", ""));
    }

    /** Ends a session without notifying the client (used when the client closed the screen itself). */
    public static void drop(ServerPlayerEntity p) { SESSIONS.remove(p.getUuid()); }

    public static String fmt(ServerPlayerEntity p, String t) {
        return t.replace("{player}", p.getName().getString());
    }

    /** Called every second: ends conversations when the player walks away. */
    public static void tick(net.minecraft.server.MinecraftServer server) {
        for (UUID id : new ArrayList<>(SESSIONS.keySet())) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
            Session s = SESSIONS.get(id);
            if (p == null) { SESSIONS.remove(id); continue; }
            if (s.npcEntity != null) {
                var e = p.getServerWorld().getEntity(s.npcEntity);
                if (e == null || e.squaredDistanceTo(p) > 10 * 10) close(p);
            }
        }
    }
}
