package dev.forgottenisland.npc;

import dev.forgottenisland.dialogue.DialogueManager;
import dev.forgottenisland.game.Places;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * A named island resident rendered with a player-shaped model and a custom skin.
 * Entities are not saved with chunks: the {@link NpcDirector} keeps exactly one live entity per
 * character near the player, placed wherever that character's schedule says they should be.
 */
public class IslandNpcEntity extends PathAwareEntity {
    public static final TrackedData<String> NPC_ID = DataTracker.registerData(IslandNpcEntity.class, TrackedDataHandlerRegistry.STRING);
    public static final TrackedData<String> BARK = DataTracker.registerData(IslandNpcEntity.class, TrackedDataHandlerRegistry.STRING);
    /** 0 = normal, 1 = sitting, 2 = working */
    public static final TrackedData<Byte> MODE = DataTracker.registerData(IslandNpcEntity.class, TrackedDataHandlerRegistry.BYTE);

    public int barkTicks;
    // schedule bookkeeping (server side)
    String activeKey = "";
    Places.P target;
    String activity = "stand";
    int wanderRadius;
    int stuckTicks, repathTicks, actTicks, noPath;
    double lastDist = Double.MAX_VALUE;
    Vec3d wanderGoal;

    public IslandNpcEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        setPersistent();
        setCanPickUpLoot(false);
        if (getNavigation() instanceof MobNavigation nav) { nav.setCanPathThroughDoors(true); nav.setCanEnterOpenDoors(true); nav.setCanSwim(true); }
        setPathfindingPenalty(PathNodeType.WATER, -1f);          // residents never swim (they would get stuck under piers)
        setPathfindingPenalty(PathNodeType.WATER_BORDER, 6f);
        setPathfindingPenalty(PathNodeType.DANGER_FIRE, 16f);
        setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, -1f);
    }

    public static DefaultAttributeContainer.Builder attributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.5)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 72)
                .add(EntityAttributes.GENERIC_STEP_HEIGHT, 1.0);
    }

    @Override protected void initDataTracker(DataTracker.Builder b) {
        super.initDataTracker(b);
        b.add(NPC_ID, "");
        b.add(BARK, "");
        b.add(MODE, (byte) 0);
    }

    @Override protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new LongDoorInteractGoal(this, true));
        goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6f, 0.04f) {
            @Override public boolean canStart() { return mode() != 1 && !isSleeping() && super.canStart(); }
        });
    }

    public String npcId() { return dataTracker.get(NPC_ID); }
    public void setNpcId(String id) {
        dataTracker.set(NPC_ID, id);
        NpcDefs.Def d = NpcDefs.get(id);
        if (d != null) {
            setCustomName(Text.literal(d.name()));
            equip(EquipmentSlot.MAINHAND, d.mainHand());
            equip(EquipmentSlot.OFFHAND, d.offHand());
        }
    }
    private void equip(EquipmentSlot slot, String item) {
        equipStack(slot, item == null || item.isEmpty() ? ItemStack.EMPTY : new ItemStack(Registries.ITEM.get(Identifier.of(item))));
        setEquipmentDropChance(slot, 0f);
    }
    public byte mode() { return dataTracker.get(MODE); }
    public void setMode(byte m) { dataTracker.set(MODE, m); }
    public String bark() { return dataTracker.get(BARK); }
    public void setBark(String s, int ticks) { dataTracker.set(BARK, s); barkTicks = ticks; }

    // ---------------------------------------------------------------- behaviour
    @Override public boolean shouldSave() { return false; }
    @Override public boolean canImmediatelyDespawn(double d) { return false; }
    @Override public boolean isInvulnerableTo(DamageSource s) { return !s.isOf(net.minecraft.entity.damage.DamageTypes.GENERIC_KILL); }
    @Override public boolean isPushable() { return mode() != 1 && !isSleeping(); }
    @Override protected void pushAway(net.minecraft.entity.Entity e) { if (mode() != 1) super.pushAway(e); }
    @Override public boolean canBeLeashed() { return false; }
    @Override protected boolean isDisallowedInPeaceful() { return false; }

    @Override protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        if (player instanceof ServerPlayerEntity sp) {
            if (DialogueManager.session(sp) != null) return ActionResult.SUCCESS;
            getNavigation().stop();
            lookAtEntity(player, 180f, 180f);
            swingHand(Hand.MAIN_HAND);
            DialogueManager.start(sp, npcId(), this);
        }
        return ActionResult.success(getWorld().isClient);
    }

    @Override public void tick() {
        super.tick();
        if (!getWorld().isClient) {
            if (barkTicks > 0 && --barkTicks == 0) dataTracker.set(BARK, "");
            if (mode() == 1 && target != null) {             // sitting: stay put on the seat
                setVelocity(Vec3d.ZERO);
                setNoGravity(true);
                refreshPositionAndAngles(target.x(), target.y(), target.z(), target.yaw(), 0);
                setHeadYaw(getHeadYaw());
                bodyYaw = target.yaw();
            }
            if (DialogueManager.isTalking(this)) {
                getNavigation().stop();
                PlayerEntity p = getWorld().getClosestPlayer(this, 8);
                if (p != null && mode() != 1 && !isSleeping()) getLookControl().lookAt(p, 30f, 30f);
                else if (p != null) getLookControl().lookAt(p, 30f, 30f);
            }
        }
    }

    // Called by the director to apply an activity at the target.
    void standUp() {
        if (isSleeping()) wakeUp();
        if (mode() == 1) { setMode((byte) 0); setNoGravity(false); if (target != null) refreshPositionAndAngles(target.x(), Math.ceil(target.y()), target.z(), getYaw(), 0); }
        if (mode() == 2) setMode((byte) 0);
    }

    void sitAt(Places.P p) {
        if (isSleeping()) wakeUp();
        getNavigation().stop();
        setMode((byte) 1);
        setNoGravity(true);
        refreshPositionAndAngles(p.x(), p.y(), p.z(), p.yaw(), 0);
        setHeadYaw(p.yaw()); bodyYaw = p.yaw();
    }

    void sleepAt(Places.P p) {
        if (isSleeping()) return;
        getNavigation().stop();
        if (mode() == 1) { setMode((byte) 0); setNoGravity(false); }
        sleep(new BlockPos(p.bx(), p.by(), p.bz()));
    }

    @Override public void sleep(BlockPos pos) {
        // like LivingEntity.sleep but without marking the bed occupied (the player may still use it)
        if (hasVehicle()) stopRiding();
        setPose(EntityPose.SLEEPING);
        setPosition(pos.getX() + 0.5, pos.getY() + 0.6875, pos.getZ() + 0.5);
        setSleepingPosition(pos);
        setVelocity(Vec3d.ZERO);
        velocityDirty = true;
    }

    @Override public void wakeUp() {
        getSleepingPosition().ifPresent(pos -> setPosition(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5));
        clearSleepingPosition();
        setPose(EntityPose.STANDING);
    }

    public void faceYaw(float yaw) {
        setYaw(yaw); bodyYaw = yaw; setHeadYaw(yaw);
    }

    public static float yawTo(double fx, double fz, double tx, double tz) {
        return (float) (MathHelper.atan2(tz - fz, tx - fx) * (180 / Math.PI)) - 90f;
    }

    public ServerWorld serverWorld() { return (ServerWorld) getWorld(); }
}
