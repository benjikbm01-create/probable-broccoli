package dev.forgottenisland.game;

import dev.forgottenisland.core.Cond;
import dev.forgottenisland.core.GameCtx;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.function.Consumer;

/**
 * Hand-placed combat encounters using vanilla mobs. An encounter spawns when its zone is entered,
 * completes ("enc.&lt;id&gt;") when all its mobs are dead, and resets if the player dies mid-fight.
 */
public final class Encounters {
    private Encounters() {}
    public record Spawn(EntityType<? extends MobEntity> type, double x, double y, double z, Consumer<MobEntity> setup) {}
    public record Encounter(String id, String cond, List<Spawn> spawns) {}
    private static final Map<String, Encounter> ALL = new LinkedHashMap<>();
    private static final Map<String, List<UUID>> active = new HashMap<>();

    public static Encounter add(String id, String cond) {
        Encounter e = new Encounter(id, cond, new ArrayList<>());
        ALL.put(id, e);
        return e;
    }
    public static Map<String, Encounter> all() { Places.init(); return ALL; }

    public static Consumer<MobEntity> gear(net.minecraft.item.Item hand, net.minecraft.item.Item helmet) {
        return m -> {
            if (hand != null) m.equipStack(EquipmentSlot.MAINHAND, new ItemStack(hand));
            if (helmet != null) m.equipStack(EquipmentSlot.HEAD, new ItemStack(helmet));
            for (EquipmentSlot s : EquipmentSlot.values()) m.setEquipmentDropChance(s, 0f);
        };
    }

    /** Called by zone handlers. */
    public static void trigger(ServerPlayerEntity p, String id) {
        Encounter e = all().get(id);
        var st = Game.state(p.getServer());
        if (e == null || st.flag("enc." + id) || active.containsKey(id)) return;
        if (!Cond.test(e.cond(), new GameCtx(st, p))) return;
        ServerWorld w = p.getServerWorld();
        List<UUID> ids = new ArrayList<>();
        for (Spawn s : e.spawns()) {
            MobEntity m = s.type().create(w);
            if (m == null) continue;
            m.refreshPositionAndAngles(s.x(), s.y(), s.z(), w.random.nextFloat() * 360, 0);
            m.initialize(w, w.getLocalDifficulty(BlockPos.ofFloored(s.x(), s.y(), s.z())), SpawnReason.EVENT, null);
            for (EquipmentSlot sl : EquipmentSlot.values()) m.setEquipmentDropChance(sl, 0f);
            if (s.setup() != null) s.setup().accept(m);
            m.setPersistent();
            m.addCommandTag("fi_enc");
            m.addCommandTag("fi_enc_" + id);
            m.setTarget(p);
            w.spawnEntity(m);
            ids.add(m.getUuid());
        }
        active.put(id, ids);
    }

    public static boolean isActive(String id) { return active.containsKey(id); }

    public static void tick(MinecraftServer s, long t) {
        if (t % 20 != 0 || active.isEmpty()) return;
        ServerWorld w = s.getOverworld();
        for (String id : new ArrayList<>(active.keySet())) {
            boolean alive = false;
            for (UUID u : active.get(id)) { Entity e = w.getEntity(u); if (e != null && e.isAlive()) { alive = true; break; } }
            if (!alive) {
                active.remove(id);
                Game.state(s).set("enc." + id);
                for (ServerPlayerEntity p : w.getPlayers()) p.sendMessage(Text.literal("The area falls quiet.").formatted(net.minecraft.util.Formatting.GRAY, net.minecraft.util.Formatting.ITALIC), true);
            }
        }
    }

    /** Player died: remove live encounter mobs so the fight can be retried cleanly. */
    public static void resetActive(MinecraftServer s) {
        ServerWorld w = s.getOverworld();
        for (List<UUID> ids : active.values()) for (UUID u : ids) { Entity e = w.getEntity(u); if (e != null) e.discard(); }
        active.clear();
        Zones.resetInside();
    }

    /** Full reset (dev /fi reset): forget completion and clear any tagged mobs that are loaded. */
    public static void resetAll(MinecraftServer s) {
        resetActive(s);
        ServerWorld w = s.getOverworld();
        for (Entity e : w.iterateEntities()) if (e.getCommandTags().contains("fi_enc")) e.discard();
    }

}
