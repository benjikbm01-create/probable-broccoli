package dev.forgottenisland.game;

import com.google.gson.JsonObject;
import dev.forgottenisland.ForgottenIsland;
import dev.forgottenisland.core.StateData;
import dev.forgottenisland.dialogue.DialogueManager;
import dev.forgottenisland.net.Net;
import dev.forgottenisland.npc.ModEntities;
import dev.forgottenisland.npc.NpcDefs;
import dev.forgottenisland.npc.NpcDirector;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

import java.util.*;
import java.util.function.BiConsumer;

/** Scripted story events, cut-scenes and the boss/ending sequences. Story text itself lives in data files. */
public final class Story {
    private Story() {}
    private static final Map<String, BiConsumer<ServerPlayerEntity, String>> EVENTS = new LinkedHashMap<>();
    private record Timer(long at, Runnable r) {}
    private static final List<Timer> timers = new ArrayList<>();
    private static long now;
    /** Block states applied when the lantern is relit / the festival begins (unlit versions are built). */
    public static final Map<BlockPos, BlockState> LANTERN_LIT = new LinkedHashMap<>(), LANTERN_DARK = new LinkedHashMap<>();
    public static final Map<BlockPos, BlockState> FESTIVAL_ON = new LinkedHashMap<>(), FESTIVAL_OFF = new LinkedHashMap<>();
    public static BlockPos bossCenter, lanternBrazier;

    static {
        ev("food", (p, a) -> { if (KeyItems.count(p, "bread") + count(p, net.minecraft.item.Items.BREAD) < 6) KeyItems.give(p, "bread", -1); });
        ev("heal", (p, a) -> { p.setHealth(p.getMaxHealth()); p.getHungerManager().setFoodLevel(20); p.clearStatusEffects(); });
        ev("rest", (p, a) -> {
            fade(p, a.isEmpty() ? "You sleep soundly." : a);
            later(30, () -> { setTime(p.getServer(), 0); p.setHealth(p.getMaxHealth()); p.getHungerManager().setFoodLevel(20); });
        });
        ev("wait_evening", (p, a) -> { fade(p, a.isEmpty() ? "The afternoon slips by..." : a); later(30, () -> setTime(p.getServer(), 11800)); });
        ev("wait_night", (p, a) -> { fade(p, a.isEmpty() ? "Night settles over the island..." : a); later(30, () -> setTime(p.getServer(), 14000)); });
        ev("open", (p, a) -> Gates.open(p.getServer(), a));
        ev("close", (p, a) -> Gates.close(p.getServer(), a));
        ev("fade", Story::fade);
        ev("title", (p, a) -> title(p, a.contains("|") ? a.substring(0, a.indexOf('|')) : a, a.contains("|") ? a.substring(a.indexOf('|') + 1) : ""));
        ev("teleport", (p, a) -> { Places.P t = Places.req(a); p.teleport(p.getServerWorld(), t.x(), t.y(), t.z(), t.yaw(), 0); });
        ev("snap", (p, a) -> NpcDirector.forceSnap(a));
        ev("sail_out", (p, a) -> {
            DialogueManager.close(p);
            fade(p, "Sol rows you out past the breakwater, into the gold of the evening...");
            Game.state(p.getServer()).set("sol_sailing");
            later(30, () -> { Places.P t = Places.req("date_gullrock_player"); p.teleport(p.getServerWorld(), t.x(), t.y(), t.z(), t.yaw(), 0); NpcDirector.forceSnap("sol"); });
        });
        ev("sail_back", (p, a) -> {
            DialogueManager.close(p);
            fade(p, "The oars creak. The harbour lamps draw closer.");
            later(30, () -> { Game.state(p.getServer()).clear("sol_sailing"); Places.P t = Places.req("pier_end"); p.teleport(p.getServerWorld(), t.x(), t.y(), t.z(), t.yaw(), 0); });
        });
        ev("warden_rest", (p, a) -> {
            var st = Game.state(p.getServer());
            if (st.set("warden_rest")) {
                KeyItems.give(p, "ember", 1);
                Gates.open(p.getServer(), "lift");
                Gates.open(p.getServer(), "sinkhole");
                Checkpoints.set(p, "cp_wellspring");
                ServerWorld w = p.getServerWorld();
                if (bossCenter != null) w.spawnParticles(ParticleTypes.END_ROD, bossCenter.getX() + 0.5, bossCenter.getY() + 1, bossCenter.getZ() + 0.5, 80, 0.5, 2, 0.5, 0.05);
                w.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.5f, 0.7f);
            }
        });
        ev("festival", (p, a) -> startFestival(p));
        ev("credits", (p, a) -> credits(p));
        ev("clear_weather", (p, a) -> p.getServerWorld().setWeather(24000, 0, false, false));
        ev("journal", (p, a) -> Journal.open(p));
    }
    private static void ev(String n, BiConsumer<ServerPlayerEntity, String> f) { EVENTS.put(n, f); }
    public static Set<String> eventNames() { return EVENTS.keySet(); }

    private static int count(ServerPlayerEntity p, net.minecraft.item.Item item) {
        int n = 0; var inv = p.getInventory();
        for (int i = 0; i < inv.size(); i++) if (inv.getStack(i).isOf(item) && KeyItems.idOf(inv.getStack(i)) == null) n += inv.getStack(i).getCount();
        return n;
    }

    public static void fire(ServerPlayerEntity p, String name, String arg) {
        var f = EVENTS.get(name);
        if (f == null) { ForgottenIsland.LOG.warn("Unknown event {}", name); return; }
        f.accept(p, arg);
    }

    public static void later(int ticks, Runnable r) { timers.add(new Timer(now + ticks, r)); }

    public static void tick(MinecraftServer s, long t) {
        now = t;
        if (!timers.isEmpty()) {
            List<Timer> due = new ArrayList<>();
            timers.removeIf(x -> { if (x.at() <= t) { due.add(x); return true; } return false; });
            for (Timer x : due) { try { x.r().run(); } catch (RuntimeException e) { ForgottenIsland.LOG.error("Timer failed", e); } }
        }
        if (t % 4 == 0) ambient(s);
    }

    public static void clearTimers() { timers.clear(); }

    // ---------------------------------------------------------------- helpers
    public static void fade(ServerPlayerEntity p, String text) { ServerPlayNetworking.send(p, new Net.Ui("fade", text)); }

    public static void title(ServerPlayerEntity p, String title, String sub) {
        p.networkHandler.sendPacket(new TitleFadeS2CPacket(20, 80, 30));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(sub).formatted(Formatting.GRAY)));
        p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(title).formatted(Formatting.GOLD)));
    }

    public static void setTime(MinecraftServer s, long timeOfDay) {
        ServerWorld w = s.getOverworld();
        long day = w.getTimeOfDay() / 24000L;
        long target = day * 24000L + timeOfDay;
        if (target < w.getTimeOfDay()) target += 24000L;
        w.setTimeOfDay(target);
    }

    // ---------------------------------------------------------------- intro
    public static void onJoin(ServerPlayerEntity p) {
        StateData st = Game.state(p.getServer());
        if (!st.flag("world_built")) return;
        if (!st.flag("intro_done")) {
            startIntro(p);
        } else {
            Checkpoints.apply(p);
        }
        Game.syncHud(p, true);
    }

    public static void startIntro(ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        if (!Game.devMode(p.getServer()) || p.interactionManager.getGameMode() != GameMode.CREATIVE) p.changeGameMode(GameMode.ADVENTURE);
        p.getInventory().clear();
        p.setHealth(p.getMaxHealth());
        p.getHungerManager().setFoodLevel(20);
        w.setWeather(12000, 0, false, false);
        setTime(p.getServer(), 23200);
        Places.P b = Places.req("intro_boat");
        p.teleport(w, b.x(), b.y() + 0.5, b.z(), b.yaw(), 0);
        BoatEntity boat = EntityType.BOAT.create(w);
        if (boat != null) {
            boat.refreshPositionAndAngles(b.x(), b.y(), b.z(), b.yaw(), 0);
            boat.setVariant(BoatEntity.Type.SPRUCE);
            w.spawnEntity(boat);
            p.startRiding(boat, true);
        }
        ServerPlayNetworking.send(p, new Net.Ui("intro", ""));
    }

    public static void finishIntro(ServerPlayerEntity p) {
        StateData st = Game.state(p.getServer());
        if (st.flag("intro_done")) return;
        st.set("intro_done");
        KeyItems.give(p, "journal", 1);
        KeyItems.give(p, "letter", 1);
        KeyItems.give(p, "bread", 4);
        Checkpoints.set(p, "cp_dock");
        title(p, "Gullhaven", "The Isle of Ysmere");
        Game.toast(p, "Your journal", "Press J (or use the journal) to see your objective. H shows a hint.");
        Game.syncHud(p, true);
    }

    // ---------------------------------------------------------------- boss
    public static Entity boss(MinecraftServer s) {
        for (Entity e : s.getOverworld().iterateEntities()) if (e instanceof WardenEntity && e.isAlive()) return e;
        return null;
    }

    public static void startBoss(ServerPlayerEntity p) {
        MinecraftServer s = p.getServer();
        StateData st = Game.state(s);
        if (st.flag("boss_defeated") || !st.flag("puzzle3_solved") || boss(s) != null || bossCenter == null) return;
        ServerWorld w = p.getServerWorld();
        Gates.close(s, "arena");
        WardenEntity b = ModEntities.WARDEN.create(w);
        if (b == null) return;
        b.refreshPositionAndAngles(bossCenter.getX() + 0.5, bossCenter.getY(), bossCenter.getZ() + 0.5, 0, 0);
        b.setup(bossCenter.getX() + 0.5, bossCenter.getY(), bossCenter.getZ() + 0.5);
        b.setTarget(p);
        w.spawnEntity(b);
        w.spawnParticles(ParticleTypes.SPLASH, b.getX(), b.getY() + 1, b.getZ(), 120, 1, 2, 1, 0.2);
        w.playSound(null, bossCenter, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1.5f, 0.5f);
        title(p, "The Drowned Warden", "");
        st.set("boss_started");
    }

    public static void abortBoss(MinecraftServer s) {
        for (Entity e : new ArrayList<Entity>(toList(s.getOverworld().iterateEntities()))) if (e instanceof WardenEntity || e.getCommandTags().contains("fi_boss_add")) e.discard();
        if (!Game.state(s).flag("boss_defeated")) Gates.open(s, "arena");
    }
    private static List<Entity> toList(Iterable<Entity> it) { List<Entity> l = new ArrayList<>(); it.forEach(l::add); return l; }

    public static void bossDefeated(MinecraftServer s) {
        StateData st = Game.state(s);
        if (!st.set("boss_defeated")) return;
        for (Entity e : toList(s.getOverworld().iterateEntities())) if (e.getCommandTags().contains("fi_boss_add")) e.discard();
        Gates.open(s, "arena");
        later(70, () -> { for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) DialogueManager.start(p, "warden", null); });
    }

    // ---------------------------------------------------------------- ending
    public static boolean relight(ServerPlayerEntity p) {
        MinecraftServer s = p.getServer();
        StateData st = Game.state(s);
        if (st.flag("lantern_lit")) return false;
        if (KeyItems.count(p, "ember") == 0) {
            p.sendMessage(Text.literal("The great lantern is cold and empty.").formatted(Formatting.GRAY, Formatting.ITALIC), true);
            return true;
        }
        KeyItems.take(p, "ember", 1);
        st.set("lantern_lit");
        ServerWorld w = p.getServerWorld();
        LANTERN_LIT.forEach((pos, bs) -> w.setBlockState(pos, bs, 2 | 16));
        w.setWeather(48000, 0, false, false);
        if (lanternBrazier != null) {
            w.spawnParticles(ParticleTypes.END_ROD, lanternBrazier.getX() + 0.5, lanternBrazier.getY() + 1.5, lanternBrazier.getZ() + 0.5, 200, 1.5, 3, 1.5, 0.15);
            w.spawnParticles(ParticleTypes.FLAME, lanternBrazier.getX() + 0.5, lanternBrazier.getY() + 1, lanternBrazier.getZ() + 0.5, 60, 0.5, 0.5, 0.5, 0.05);
        }
        w.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 2f, 0.7f);
        w.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.MASTER, 0.6f, 1.2f);
        Checkpoints.set(p, "cp_lantern");
        title(p, "The Lantern Burns", "and the island remembers");
        Game.syncHud(p, true);
        later(220, () -> startFestival(p));
        return true;
    }

    public static void startFestival(ServerPlayerEntity p) {
        MinecraftServer s = p.getServer();
        StateData st = Game.state(s);
        if (st.flag("festival")) return;
        fade(p, "That evening, all of Gullhaven gathered beneath the lantern light...");
        later(35, () -> {
            st.set("festival");
            setTime(s, 12600);
            ServerWorld w = s.getOverworld();
            FESTIVAL_ON.forEach((pos, bs) -> w.setBlockState(pos, bs, 2 | 16));
            Places.P t = Places.req("cp_square");
            p.teleport(w, t.x(), t.y(), t.z(), t.yaw(), 0);
            Checkpoints.set(p, "cp_square");
            for (String id : NpcDefs.ALL.keySet()) NpcDirector.forceSnap(id);
        });
    }

    public static void credits(ServerPlayerEntity p) {
        StateData st = Game.state(p.getServer());
        st.set("credits_seen");
        JsonObject j = new JsonObject();
        for (String r : StateData.ROMANCES) j.addProperty(r, st.flag(r + "_dating") ? "dating" : st.flag(r + "_bestfriend") ? "friend" : st.var("rel." + r) >= 3 ? "friendly" : "");
        j.addProperty("secrets", st.var("secrets"));
        j.addProperty("secretTotal", ContentHolder.get().secrets.size());
        j.addProperty("side", (st.flag("side_corin_done") ? 1 : 0) + (st.flag("side_tobias_done") ? 1 : 0) + (st.flag("side_wendel_done") ? 1 : 0));
        j.addProperty("told", st.flag("told_warden"));
        ServerPlayNetworking.send(p, new Net.Ui("credits", j.toString()));
    }

    public static void finishCredits(ServerPlayerEntity p) {
        StateData st = Game.state(p.getServer());
        if (!st.flag("credits_seen")) return;
        st.set("game_complete");
        Game.toast(p, "Thank you for playing", "Gullhaven is yours to wander. Unfinished stories still wait.");
    }

    // ---------------------------------------------------------------- reset (developer)
    public static void resetWorld(MinecraftServer s) {
        ServerWorld w = s.getOverworld();
        abortBoss(s);
        LANTERN_DARK.forEach((pos, bs) -> w.setBlockState(pos, bs, 2 | 16));
        FESTIVAL_OFF.forEach((pos, bs) -> w.setBlockState(pos, bs, 2 | 16));
        Puzzles.resetStones(s); Puzzles.resetMirrors(s); Puzzles.resetSlide(s, true);
        for (String g : Gates.all().keySet()) if (!g.equals("arena")) Gates.close(s, g);
        Gates.open(s, "arena");
        Interactions.resetWaystones(s);
        Encounters.resetAll(s);
        clearTimers();
    }

    // ---------------------------------------------------------------- ambience
    private static void ambient(MinecraftServer s) {
        ServerWorld w = s.getOverworld();
        long tod = w.getTimeOfDay() % 24000;
        boolean night = tod > 12800 && tod < 23200;
        for (ServerPlayerEntity p : w.getPlayers()) {
            if (night) {
                Places.P g = Places.get("glade_center");
                if (g != null && p.squaredDistanceTo(g.x(), g.y(), g.z()) < 40 * 40) {
                    for (int i = 0; i < 3; i++)
                        w.spawnParticles(ParticleTypes.WAX_OFF, g.x() + (w.random.nextDouble() - 0.5) * 22, g.y() + 0.5 + w.random.nextDouble() * 3, g.z() + (w.random.nextDouble() - 0.5) * 22, 1, 0, 0, 0, 0);
                }
            }
            if (Game.state(s).flag("lantern_lit") && lanternBrazier != null && p.squaredDistanceTo(lanternBrazier.getX(), lanternBrazier.getY(), lanternBrazier.getZ()) < 64 * 64)
                w.spawnParticles(ParticleTypes.END_ROD, lanternBrazier.getX() + 0.5, lanternBrazier.getY() + 1.2, lanternBrazier.getZ() + 0.5, 1, 0.6, 0.6, 0.6, 0.02);
        }
    }
}
