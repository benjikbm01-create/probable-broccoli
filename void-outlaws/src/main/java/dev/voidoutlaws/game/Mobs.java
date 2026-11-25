package dev.voidoutlaws.game;

import dev.voidoutlaws.state.CrewState;
import dev.voidoutlaws.world.Loc;
import dev.voidoutlaws.world.Structures;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.UnbreakableComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

/** Hostile NPCs: troopers, pirates, boarders and bounty hunters. All vanilla mobs in costume. */
public final class Mobs {
    private Mobs() {}

    private static final Map<RegistryKey<World>, Set<UUID>> TRACKED = new HashMap<>();
    private static ServerBossBar hunterBar;
    private static UUID hunterId;
    private static RegistryKey<World> hunterWorld;

    static final String[] HUNTER_NAMES = {"Sable Quint", "Mother Gorse", "The Tally Man", "Dex Marrowind", "Ivo 'Nine Lives' Krane", "Jessamy Coil"};

    static void reset() {
        TRACKED.clear();
        if (hunterBar != null) hunterBar.clearPlayers();
        hunterBar = null;
        hunterId = null;
    }

    private static void track(ServerWorld w, Entity e) {
        TRACKED.computeIfAbsent(w.getRegistryKey(), k -> new HashSet<>()).add(e.getUuid());
    }

    private static int alive(ServerWorld w, String tag) {
        int n = 0;
        for (UUID u : TRACKED.getOrDefault(w.getRegistryKey(), Set.of())) {
            Entity e = w.getEntity(u);
            if (e != null && e.isAlive() && e.getCommandTags().contains(tag)) n++;
        }
        return n;
    }

    private static ItemStack unbreakable(Item item) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(false));
        return s;
    }

    private static <T extends MobEntity> T make(EntityType<T> type, ServerWorld w, BlockPos pos, String name, Formatting color, String tag) {
        T m = type.create(w);
        if (m == null) return null;
        m.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Game.R.nextFloat() * 360f, 0);
        m.initialize(w, w.getLocalDifficulty(pos), SpawnReason.EVENT, null);
        m.setCustomName(Text.literal(name).formatted(color));
        m.setPersistent();
        m.addCommandTag("vo_mob");
        m.addCommandTag(tag);
        for (EquipmentSlot slot : EquipmentSlot.values()) m.setEquipmentDropChance(slot, 0f);
        return m;
    }

    private static void spawn(ServerWorld w, MobEntity m) {
        w.spawnEntity(m);
        track(w, m);
        w.spawnParticles(net.minecraft.particle.ParticleTypes.POOF, m.getX(), m.getY() + 1, m.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
    }

    public static void spawnTroopers(ServerWorld w, List<BlockPos> pts, int n, int cap) {
        for (int i = 0; i < n && alive(w, "vo_trooper") < cap; i++) {
            BlockPos pos = pts.get(Game.R.nextInt(pts.size()));
            var m = make(EntityType.SKELETON, w, pos, "Concordat Trooper", Formatting.AQUA, "vo_trooper");
            if (m == null) continue;
            m.equipStack(EquipmentSlot.HEAD, unbreakable(Items.IRON_HELMET));
            m.equipStack(EquipmentSlot.CHEST, unbreakable(Items.IRON_CHESTPLATE));
            m.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            spawn(w, m);
        }
    }

    public static void spawnPirates(ServerWorld w, List<BlockPos> pts, int n, int cap) {
        for (int i = 0; i < n && alive(w, "vo_pirate") < cap; i++) {
            BlockPos pos = pts.get(Game.R.nextInt(pts.size()));
            MobEntity m;
            if (Game.R.nextInt(3) == 0) {
                m = make(EntityType.VINDICATOR, w, pos, "Rattlejaw Brute", Formatting.DARK_RED, "vo_pirate");
                if (m != null) m.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
            } else {
                m = make(EntityType.PILLAGER, w, pos, "Rattlejaw Pirate", Formatting.RED, "vo_pirate");
                if (m != null) m.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            }
            if (m == null) continue;
            m.equipStack(EquipmentSlot.HEAD, unbreakable(Items.LEATHER_HELMET));
            spawn(w, m);
        }
    }

    /** Pirates jump through the airlock into the cargo bay during a bad pirate attack. */
    public static void spawnBoarders(MinecraftServer server, int n) {
        ServerWorld w = server.getWorld(Loc.SHIP);
        if (w == null) return;
        List<BlockPos> pts = List.of(Structures.SHIP_O.add(0, 0, 6), Structures.SHIP_O.add(2, 0, 8));
        spawnPirates(w, pts, n, 4);
        List<ServerPlayerEntity> aboard = Game.aboard();
        Game.title(aboard, Game.t("BOARDERS!", Formatting.DARK_RED), Game.t("Pirates are in the cargo bay!", Formatting.WHITE));
        Game.sound(aboard, SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1f, 0.8f);
    }

    public static Entity spawnHunter(ServerWorld w, BlockPos pos, String name, boolean story) {
        var m = make(EntityType.STRAY, w, pos, "Bounty Hunter " + name, Formatting.DARK_RED, story ? "vo_story_hunter" : "vo_hunter");
        if (m == null) return null;
        m.addCommandTag("vo_hunter_any");
        m.equipStack(EquipmentSlot.HEAD, unbreakable(Items.NETHERITE_HELMET));
        m.equipStack(EquipmentSlot.CHEST, unbreakable(Items.IRON_CHESTPLATE));
        m.equipStack(EquipmentSlot.LEGS, unbreakable(Items.CHAINMAIL_LEGGINGS));
        m.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        int crew = Math.max(1, Game.players().size());
        double hp = (story ? 70 : 45) + 15 * (crew - 1);
        EntityAttributeInstance a = m.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (a != null) a.setBaseValue(hp);
        m.setHealth((float) hp);
        m.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, StatusEffectInstance.INFINITE, 0, false, false));
        m.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, StatusEffectInstance.INFINITE, 0, false, false));
        m.setCustomNameVisible(true);
        spawn(w, m);
        if (hunterBar != null) hunterBar.clearPlayers();
        hunterBar = new ServerBossBar(Text.literal("Bounty Hunter " + name).formatted(Formatting.DARK_RED), BossBar.Color.RED, BossBar.Style.PROGRESS);
        hunterId = m.getUuid();
        hunterWorld = w.getRegistryKey();
        List<ServerPlayerEntity> here = new ArrayList<>();
        for (ServerPlayerEntity p : Game.players()) if (p.getServerWorld() == w) here.add(p);
        Game.title(here, Game.t("BOUNTY HUNTER!", Formatting.DARK_RED), Game.t(name + " has come to collect!", Formatting.WHITE));
        Game.sound(here, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.2f);
        Game.crew("⚠ Bounty hunter " + name + " tracked the crew down! Fight them off, or jump away.", Formatting.DARK_RED);
        return m;
    }

    // ------------------------------------------------------------------ lifecycle
    static void onShipLeave(Loc from) {
        discardIn(Game.server.getWorld(from.key));
    }

    static void clearShip(MinecraftServer server) {
        discardIn(server.getWorld(Loc.SHIP));
    }

    private static void discardIn(ServerWorld w) {
        if (w == null) return;
        Set<UUID> set = TRACKED.remove(w.getRegistryKey());
        if (set == null) return;
        for (UUID u : set) {
            Entity e = w.getEntity(u);
            if (e != null) e.discard();
        }
        if (hunterWorld != null && hunterWorld.equals(w.getRegistryKey())) clearHunterBar();
    }

    private static void clearHunterBar() {
        if (hunterBar != null) hunterBar.clearPlayers();
        hunterBar = null;
        hunterId = null;
        hunterWorld = null;
    }

    static void onArrive(MinecraftServer server, Loc dest) {
        CrewState s = Game.S();
        ServerWorld w = server.getWorld(dest.key);
        if (w == null) return;
        Vec3d arr = Structures.arrival(s, dest);
        BlockPos base = BlockPos.ofFloored(arr);
        if (dest == Loc.MERIDIAN && s.heat >= 40) {
            int n = 2 + (s.heat - 40) / 20;
            List<BlockPos> pts = List.of(base.add(9, 0, -9), base.add(-9, 0, -9), base.add(9, 0, 8), base.add(-9, 0, 8));
            Sched.later(80, () -> {
                if (Game.S().shipLoc == Loc.MERIDIAN && !Game.S().jumping) {
                    spawnTroopers(w, pts, n, 6);
                    Game.crew("Concordat patrols are sweeping the landing pad. (Your Heat is " + Game.S().heat + ".)", Formatting.AQUA);
                }
            });
        }
        if (dest != Loc.BURROW && s.heat >= 60 && s.jumpsSinceHunter >= 2 && Story.storyHunterPending() == false) {
            double chance = s.heat >= 80 ? 0.75 : 0.4;
            if (Game.R.nextDouble() < chance) {
                s.jumpsSinceHunter = 0;
                s.markDirty();
                String name = HUNTER_NAMES[Game.R.nextInt(HUNTER_NAMES.length)];
                BlockPos at = dest == Loc.DERELICT ? base.add(0, 0, -5) : base.add(-6, 0, -7);
                Sched.later(200, () -> {
                    if (Game.S().shipLoc == dest && !Game.S().jumping) spawnHunter(w, at, name, false);
                });
            }
        }
        Story.onArrive(server, dest);
    }

    static void tickSecond(MinecraftServer server) {
        // bossbar for the current hunter
        if (hunterBar != null && hunterId != null) {
            ServerWorld w = server.getWorld(hunterWorld);
            Entity e = w == null ? null : w.getEntity(hunterId);
            if (e instanceof LivingEntity le && le.isAlive()) {
                hunterBar.setPercent(Math.max(0f, Math.min(1f, le.getHealth() / le.getMaxHealth())));
                for (ServerPlayerEntity p : Game.players()) {
                    boolean near = p.getServerWorld() == w && p.squaredDistanceTo(e) < 64 * 64;
                    if (near && !hunterBar.getPlayers().contains(p)) hunterBar.addPlayer(p);
                    if (!near && hunterBar.getPlayers().contains(p)) hunterBar.removePlayer(p);
                }
            } else {
                clearHunterBar();
            }
        }
        disguiseCheck(server);
    }

    /** Mobs lose interest in disguised players. */
    static void disguiseCheck(MinecraftServer server) {
        for (Map.Entry<RegistryKey<World>, Set<UUID>> en : TRACKED.entrySet()) {
            ServerWorld w = server.getWorld(en.getKey());
            if (w == null) continue;
            en.getValue().removeIf(u -> {
                Entity e = w.getEntity(u);
                return e != null && !e.isAlive();
            });
            for (UUID u : en.getValue()) {
                if (w.getEntity(u) instanceof MobEntity m && m.getTarget() instanceof ServerPlayerEntity sp && Gear.disguised(sp)
                        && !m.getCommandTags().contains("vo_hunter_any")) {
                    m.setTarget(null);
                }
            }
        }
    }

    public static void onDeath(LivingEntity e) {
        Set<String> tags = e.getCommandTags();
        if (!tags.contains("vo_mob")) return;
        CrewState s = Game.S();
        if (tags.contains("vo_hunter_any")) {
            if (hunterId != null && hunterId.equals(e.getUuid())) clearHunterBar();
            if (tags.contains("vo_story_hunter")) {
                Story.onStoryHunterDefeated();
                return;
            }
            s.pot += 100;
            s.markDirty();
            Game.title(Game.players(), Game.t("Hunter defeated!", Formatting.GREEN), Game.t("+100 credits in the Crew Pot", Formatting.GOLD));
            Heat.add(-10, "a hunter won't be reporting back");
        } else if (tags.contains("vo_trooper")) {
            if (Game.R.nextInt(3) == 0) Heat.add(1, "trooper knocked out");
        } else if (tags.contains("vo_pirate")) {
            s.addRep("rattlejaw", -1);
            if (Game.R.nextInt(2) == 0) { s.pot += 5; s.markDirty(); }
        }
    }
}
