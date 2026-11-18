package dev.forgottenisland.game;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** The final encounter: vanilla zombie combat AI plus a thrown spear, a tide surge and summoned adds. */
public class WardenEntity extends ZombieEntity {
    private final ServerBossBar bar = new ServerBossBar(Text.literal("The Drowned Warden").formatted(Formatting.DARK_AQUA), BossBar.Color.BLUE, BossBar.Style.NOTCHED_10);
    private int phase = 1, spearCd = 80, surgeCd = 160, invuln;
    public double homeX, homeY, homeZ;

    public WardenEntity(EntityType<? extends ZombieEntity> type, World world) {
        super(type, world);
        experiencePoints = 50;
        setPersistent();
    }

    public static DefaultAttributeContainer.Builder attributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 150)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6)
                .add(EntityAttributes.GENERIC_ARMOR, 6)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.7)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40)
                .add(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS, 0);
    }

    @Override protected void initCustomGoals() {
        goalSelector.add(2, new ZombieAttackGoal(this, 1.0, false));
        goalSelector.add(7, new WanderAroundFarGoal(this, 0.8));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
    }

    public void setup(double x, double y, double z) {
        homeX = x; homeY = y; homeZ = z;
        equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
        setEquipmentDropChance(EquipmentSlot.MAINHAND, 0f);
        setCustomName(Text.literal("The Drowned Warden"));
        setHealth(getMaxHealth());
    }

    @Override protected boolean burnsInDaylight() { return false; }
    @Override protected boolean canConvertInWater() { return false; }
    @Override public boolean canBreakDoors() { return false; }
    @Override public boolean isBaby() { return false; }
    @Override public boolean canImmediatelyDespawn(double d) { return false; }
    @Override protected boolean isDisallowedInPeaceful() { return false; }
    @Override public boolean shouldSave() { return false; }   // the fight is re-created on entry

    @Override public void onStartedTrackingBy(ServerPlayerEntity p) { super.onStartedTrackingBy(p); bar.addPlayer(p); }
    @Override public void onStoppedTrackingBy(ServerPlayerEntity p) { super.onStoppedTrackingBy(p); bar.removePlayer(p); }

    @Override public boolean damage(DamageSource source, float amount) {
        if (getWorld().isClient) return super.damage(source, amount);
        if (invuln > 0 && !source.isOf(net.minecraft.entity.damage.DamageTypes.OUT_OF_WORLD) && !source.isOf(net.minecraft.entity.damage.DamageTypes.GENERIC_KILL)) return false;
        if (source.getAttacker() instanceof PlayerEntity p && Game.state(p.getServer()).flag("dev_boss_weak")) amount *= 10;   // test/dev aid only (/fi boss weak)
        return super.damage(source, amount);
    }

    @Override protected void mobTick() {
        super.mobTick();
        bar.setPercent(getHealth() / getMaxHealth());
        ServerWorld w = (ServerWorld) getWorld();
        if (invuln > 0) { invuln--; w.spawnParticles(ParticleTypes.BUBBLE_POP, getX(), getY() + 1, getZ(), 4, 0.4, 0.8, 0.4, 0.02); }
        float f = getHealth() / getMaxHealth();
        int want = f > 0.66f ? 1 : f > 0.33f ? 2 : 3;
        if (want > phase) { phase = want; phaseShift(w); }
        LivingEntity t = getTarget();
        if (t == null) return;
        if (--spearCd <= 0 && squaredDistanceTo(t) > 16 && canSee(t)) { throwSpear(w, t); spearCd = phase == 3 ? 50 : 80; }
        if (phase >= 2 && --surgeCd <= 0) { surge(w); surgeCd = phase == 3 ? 120 : 180; }
        if (squaredDistanceTo(homeX, homeY, homeZ) > 24 * 24) refreshPositionAndAngles(homeX, homeY, homeZ, getYaw(), 0);
    }

    private void throwSpear(ServerWorld w, LivingEntity t) {
        TridentEntity tr = new TridentEntity(w, this, new ItemStack(Items.TRIDENT));
        double dx = t.getX() - getX(), dy = t.getBodyY(0.33) - tr.getY(), dz = t.getZ() - getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        tr.setVelocity(dx, dy + d * 0.2, dz, 1.6f, 6f);
        tr.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
        tr.addCommandTag("fi_boss_add");
        swingHand(net.minecraft.util.Hand.MAIN_HAND);
        w.playSound(null, getBlockPos(), SoundEvents.ITEM_TRIDENT_THROW.value(), SoundCategory.HOSTILE, 1f, 0.8f);
        w.spawnEntity(tr);
    }

    private void surge(ServerWorld w) {
        w.playSound(null, getBlockPos(), SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 0.6f, 1.4f);
        w.playSound(null, getBlockPos(), SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.HOSTILE, 1.5f, 0.6f);
        for (int i = 0; i < 40; i++) {
            double a = i / 40.0 * Math.PI * 2;
            w.spawnParticles(ParticleTypes.SPLASH, getX() + Math.cos(a) * 3, getY() + 0.3, getZ() + Math.sin(a) * 3, 6, 0.3, 0.1, 0.3, 0.1);
        }
        for (PlayerEntity p : w.getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(5), e -> !e.isSpectator())) {
            Vec3d push = p.getPos().subtract(getPos()).normalize().multiply(1.4);
            p.addVelocity(push.x, 0.5, push.z);
            p.velocityModified = true;
            p.damage(getDamageSources().mobAttack(this), 4f);
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
        }
    }

    private void phaseShift(ServerWorld w) {
        invuln = 60;
        refreshPositionAndAngles(homeX, homeY, homeZ, getYaw(), 0);
        w.playSound(null, getBlockPos(), SoundEvents.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 1.2f, 1.3f);
        for (PlayerEntity p : w.getPlayers()) if (p.squaredDistanceTo(this) < 40 * 40) {
            p.sendMessage(Text.literal(phase == 2 ? "The water rises with his grief." : "\"I will not lose them again!\"").formatted(Formatting.DARK_AQUA, Formatting.ITALIC), true);
            if (phase == 3) p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 0));
        }
        int n = phase == 2 ? 3 : 2;
        for (int i = 0; i < n; i++) {
            DrownedEntity d = EntityType.DROWNED.create(w);
            if (d == null) continue;
            double a = i * Math.PI * 2 / n;
            d.refreshPositionAndAngles(homeX + Math.cos(a) * 6, homeY, homeZ + Math.sin(a) * 6, 0, 0);
            d.initialize(w, w.getLocalDifficulty(getBlockPos()), SpawnReason.EVENT, null);
            d.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            d.setCustomName(Text.literal("Tide-Wraith"));
            d.setPersistent();
            d.addCommandTag("fi_boss_add");
            d.setTarget(getTarget());
            w.spawnEntity(d);
            w.spawnParticles(ParticleTypes.SPLASH, d.getX(), d.getY() + 1, d.getZ(), 30, 0.4, 0.8, 0.4, 0.1);
        }
    }

    @Override public void onDeath(DamageSource source) {
        super.onDeath(source);
        bar.clearPlayers();
        if (!getWorld().isClient) Story.bossDefeated(getServer());
    }

    @Override public void remove(RemovalReason reason) { bar.clearPlayers(); super.remove(reason); }

    @Override public void writeCustomDataToNbt(NbtCompound n) { super.writeCustomDataToNbt(n); }
}
