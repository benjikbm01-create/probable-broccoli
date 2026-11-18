package dev.forgottenisland.npc;

import dev.forgottenisland.ForgottenIsland;
import dev.forgottenisland.content.Content;
import dev.forgottenisland.core.Cond;
import dev.forgottenisland.core.GameCtx;
import dev.forgottenisland.core.StateData;
import dev.forgottenisland.dialogue.DialogueManager;
import dev.forgottenisland.game.ContentHolder;
import dev.forgottenisland.game.Game;
import dev.forgottenisland.game.Places;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * Keeps one live entity per character near the player and drives daily schedules.
 * Characters far from any player are "virtual": they are simply wherever their schedule says.
 */
public final class NpcDirector {
    private NpcDirector() {}
    static final double SPAWN_RANGE = 72, DESPAWN_RANGE = 104, SNAP_RANGE = 36;
    private static final Map<String, IslandNpcEntity> LIVE = new HashMap<>();
    private static final Random RNG = new Random();
    /** Running ambient conversation: lines left and ticks until the next one. */
    private static Content.Chat chat; private static int chatLine, chatWait, chatCooldown = 200;

    public static IslandNpcEntity live(String id) {
        IslandNpcEntity e = LIVE.get(id);
        return e != null && e.isAlive() && !e.isRemoved() ? e : null;
    }
    public static Collection<IslandNpcEntity> allLive() { return LIVE.values().stream().filter(e -> e.isAlive() && !e.isRemoved()).toList(); }

    public static void reset() { LIVE.clear(); chat = null; }

    /** Where a character should be right now. */
    public static Content.ScheduleEntry scheduled(String id, MinecraftServer server) {
        ServerWorld w = server.getOverworld();
        StateData st = Game.state(server);
        dev.forgottenisland.core.StateView view = new dev.forgottenisland.core.StateView() {
            @Override public boolean flag(String n) { return st.flag(n); }
            @Override public int var(String n) { return st.var(n); }
            @Override public long timeOfDay() { return w.getTimeOfDay(); }
        };
        return Schedules.resolve(ContentHolder.get().schedules.get(id), view, w.getTimeOfDay());
    }

    public static void tick(MinecraftServer server) {
        ServerWorld w = server.getOverworld();
        List<ServerPlayerEntity> players = w.getPlayers();
        StateData st = Game.state(server);
        if (!st.flag("world_built")) return;
        for (NpcDefs.Def d : NpcDefs.ALL.values()) {
            try { tickOne(w, players, d.id(), server); }
            catch (RuntimeException ex) { ForgottenIsland.LOG.error("NPC {} tick failed", d.id(), ex); }
        }
        tickChat(w, players, st);
    }

    private static double nearestPlayer(List<ServerPlayerEntity> players, double x, double y, double z) {
        double best = Double.MAX_VALUE;
        for (ServerPlayerEntity p : players) best = Math.min(best, p.squaredDistanceTo(x, y, z));
        return Math.sqrt(best);
    }

    private static void tickOne(ServerWorld w, List<ServerPlayerEntity> players, String id, MinecraftServer server) {
        if (Game.state(server).flag("hidden." + id)) { IslandNpcEntity e = live(id); if (e != null) { e.discard(); LIVE.remove(id); } return; }
        Content.ScheduleEntry entry = scheduled(id, server);
        if (entry == null) return;
        Places.P place = Places.get(entry.at);
        if (place == null) return;
        IslandNpcEntity e = live(id);
        if (e == null) {
            LIVE.remove(id);
            if (players.isEmpty() || nearestPlayer(players, place.x(), place.y(), place.z()) > SPAWN_RANGE) return;
            if (!w.isChunkLoaded(ChunkPos.toLong(place.bx() >> 4, place.bz() >> 4))) return;
            e = ModEntities.NPC.create(w);
            if (e == null) return;
            e.setNpcId(id);
            e.refreshPositionAndAngles(place.x(), place.y(), place.z(), place.yaw(), 0);
            e.addCommandTag("fi_npc");
            w.spawnEntity(e);
            LIVE.put(id, e);
            assign(e, entry, place);
            arrive(e);
            return;
        }
        if (players.isEmpty() || nearestPlayer(players, e.getX(), e.getY(), e.getZ()) > DESPAWN_RANGE) { e.discard(); LIVE.remove(id); return; }
        String key = entry.at + "/" + entry.act;
        if (!key.equals(e.activeKey)) { assign(e, entry, place); }
        if (DialogueManager.isTalking(e)) return;
        drive(e, players);
    }

    private static void assign(IslandNpcEntity e, Content.ScheduleEntry entry, Places.P place) {
        e.activeKey = entry.at + "/" + entry.act;
        e.target = place; e.activity = entry.act; e.wanderRadius = Math.max(2, entry.r);
        e.stuckTicks = 0; e.lastDist = Double.MAX_VALUE; e.wanderGoal = null;
        if (e.squaredDistanceTo(place.x(), place.y(), place.z()) > 2.5) e.standUp();
    }

    /** Move toward the scheduled place; snap there when nobody is watching or the path is blocked. */
    private static void drive(IslandNpcEntity e, List<ServerPlayerEntity> players) {
        Places.P t = e.target;
        double dist = Math.sqrt(e.squaredDistanceTo(t.x(), t.y(), t.z()));
        boolean atSpot = dist < (e.activity.equals("wander") ? e.wanderRadius + 2 : 1.4);
        if (e.mode() == 1 || e.isSleeping()) { if (dist < 2.5) { act(e); return; } e.standUp(); }
        if (atSpot) { act(e); return; }
        double watcher = Math.min(nearestPlayer(players, e.getX(), e.getY(), e.getZ()), nearestPlayer(players, t.x(), t.y(), t.z()));
        if (watcher > SNAP_RANGE) { snap(e); return; }
        // walk
        if (dist < e.lastDist - 0.4) { e.lastDist = dist; e.stuckTicks = 0; }
        else e.stuckTicks += 10;
        if (e.stuckTicks > 200) {              // ~10 s without progress
            if (watcher > 14 || e.stuckTicks > 500) { snap(e); return; }
        }
        if (--e.repathTicks <= 0 || e.getNavigation().isIdle()) {
            e.repathTicks = 4;
            if (!e.getNavigation().startMovingTo(t.x(), t.y(), t.z(), 0.55)) e.stuckTicks += 30;
        }
    }

    private static void snap(IslandNpcEntity e) {
        Places.P t = e.target;
        e.getNavigation().stop();
        e.standUp();
        e.refreshPositionAndAngles(t.x(), t.y(), t.z(), t.yaw(), 0);
        e.faceYaw(t.yaw());
        e.lastDist = 0; e.stuckTicks = 0;
        arrive(e);
    }

    private static void arrive(IslandNpcEntity e) {
        switch (e.activity) {
            case "sit" -> e.sitAt(e.target);
            case "sleep" -> e.sleepAt(e.target);
            case "work" -> { e.setMode((byte) 2); e.faceYaw(e.target.yaw()); }
            default -> { if (!e.activity.equals("wander")) e.faceYaw(e.target.yaw()); }
        }
    }

    /** Perform the current activity at the spot. */
    private static void act(IslandNpcEntity e) {
        e.actTicks += 10;
        switch (e.activity) {
            case "sit" -> { if (e.mode() != 1) e.sitAt(e.target); }
            case "sleep" -> { if (!e.isSleeping()) e.sleepAt(e.target); }
            case "work" -> {
                if (e.mode() != 2) { e.getNavigation().stop(); e.setMode((byte) 2); e.faceYaw(e.target.yaw()); }
                if (e.actTicks % 40 == 0 && RNG.nextInt(3) > 0) {
                    e.swingHand(RNG.nextInt(4) == 0 ? Hand.OFF_HAND : Hand.MAIN_HAND);
                    if (e.npcId().equals("corin")) e.getWorld().playSound(null, e.getBlockPos(), SoundEvents.BLOCK_ANVIL_USE, SoundCategory.NEUTRAL, 0.35f, 0.9f + RNG.nextFloat() * 0.2f);
                    if (e.npcId().equals("sol")) e.getWorld().playSound(null, e.getBlockPos(), SoundEvents.BLOCK_WOOD_HIT, SoundCategory.NEUTRAL, 0.6f, 0.8f);
                }
            }
            case "wander" -> {
                if (e.getNavigation().isIdle() && e.actTicks % 60 == 0 && RNG.nextInt(3) == 0) {
                    Places.P t = e.target;
                    for (int tries = 0; tries < 6; tries++) {
                        int x = (int) Math.floor(t.x() + RNG.nextInt(e.wanderRadius * 2 + 1) - e.wanderRadius);
                        int z = (int) Math.floor(t.z() + RNG.nextInt(e.wanderRadius * 2 + 1) - e.wanderRadius);
                        BlockPos g = ground(e.serverWorld(), x, t.by(), z);
                        if (g != null) { e.getNavigation().startMovingTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5, 0.45); break; }
                    }
                }
            }
            case "talk" -> {
                IslandNpcEntity other = null; double best = 36;
                for (IslandNpcEntity o : allLive()) if (o != e && o.squaredDistanceTo(e) < best) { best = o.squaredDistanceTo(e); other = o; }
                if (other != null) e.getLookControl().lookAt(other, 20f, 20f);
            }
            default -> { if (e.actTicks % 200 == 0) e.faceYaw(e.target.yaw()); }
        }
    }

    private static BlockPos ground(ServerWorld w, int x, int y, int z) {
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos p = new BlockPos(x, y + dy, z);
            if (w.getBlockState(p.down()).isSolidBlock(w, p.down()) && w.getBlockState(p).isAir() && w.getBlockState(p.up()).isAir()) return p;
        }
        return null;
    }

    // ---------------------------------------------------------------- barks & ambient conversation
    private static void tickChat(ServerWorld w, List<ServerPlayerEntity> players, StateData st) {
        if (players.isEmpty()) return;
        ServerPlayerEntity p = players.get(0);
        GameCtx ctx = new GameCtx(st, p);
        if (chat != null) {
            if (--chatWait > 0) return;
            if (chatLine >= chat.lines.size()) { chat = null; chatCooldown = 600 + RNG.nextInt(600); return; }
            String line = chat.lines.get(chatLine++);
            int c = line.indexOf(':');
            IslandNpcEntity who = live(line.substring(0, c).trim());
            if (who != null && who.squaredDistanceTo(p) < 20 * 20) who.setBark(line.substring(c + 1).trim(), 80);
            chatWait = 9;     // director ticks every 10 game ticks -> ~4.5 s per line
            return;
        }
        if (--chatCooldown > 0) {
            // single-character barks
            if (RNG.nextInt(90) != 0) return;
            for (IslandNpcEntity e : allLive()) {
                if (e.squaredDistanceTo(p) > 10 * 10 || DialogueManager.isTalking(e) || e.isSleeping() || !e.bark().isEmpty()) continue;
                Content.Dialogue d = ContentHolder.get().dialogues.get(e.npcId());
                if (d == null) continue;
                List<String> opts = new ArrayList<>();
                for (Content.Bark b : d.barks) if (Cond.test(b.if_, ctx)) opts.add(b.text);
                if (!opts.isEmpty()) { e.setBark(opts.get(RNG.nextInt(opts.size())), 70); return; }
            }
            return;
        }
        for (Content.Chat c : ContentHolder.get().chats) {
            IslandNpcEntity a = live(c.a), b = live(c.b);
            if (a == null || b == null || a.squaredDistanceTo(b) > 8 * 8 || a.squaredDistanceTo(p) > 18 * 18) continue;
            if (a.isSleeping() || b.isSleeping() || DialogueManager.isTalking(a) || DialogueManager.isTalking(b)) continue;
            if (!Cond.test(c.if_, ctx)) continue;
            String seenFlag = "chat." + c.a + "." + c.b + "." + Integer.toHexString(c.lines.hashCode());
            if (st.flag(seenFlag) && RNG.nextInt(4) != 0) continue;
            st.set(seenFlag);
            chat = c; chatLine = 0; chatWait = 1;
            a.getLookControl().lookAt(b, 30, 30); b.getLookControl().lookAt(a, 30, 30);
            return;
        }
        chatCooldown = 200;
    }

    /** Teleport a live character to their scheduled place now (used by cut-scenes and tests). */
    public static void forceSnap(String id) {
        IslandNpcEntity e = live(id);
        if (e != null && e.target != null) snap(e);
    }
}
