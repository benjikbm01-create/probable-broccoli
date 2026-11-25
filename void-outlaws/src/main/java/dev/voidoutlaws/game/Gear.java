package dev.voidoutlaws.game;

import dev.voidoutlaws.reg.GearItem;
import dev.voidoutlaws.state.CrewState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3f;

/** Right-click behaviour of all the gear items. */
public final class Gear {
    private Gear() {}

    public static boolean disguised(ServerPlayerEntity p) {
        Long until = Game.S().disguiseUntil.get(p.getUuid());
        return until != null && until > p.getServerWorld().getTime();
    }

    public static boolean use(GearItem.Kind kind, ServerPlayerEntity p, Hand hand, ItemStack stack) {
        CrewState s = Game.S();
        switch (kind) {
            case BLASTER -> { blaster(p); p.getItemCooldownManager().set(stack.getItem(), 12); return true; }
            case JETPACK -> {
                Vec3d look = p.getRotationVector();
                p.setVelocity(look.x * 0.5, 0.95, look.z * 0.5);
                push(p);
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 50, 0, false, false));
                p.getServerWorld().spawnParticles(ParticleTypes.FLAME, p.getX(), p.getY(), p.getZ(), 20, 0.2, 0.1, 0.2, 0.02);
                p.getServerWorld().spawnParticles(ParticleTypes.CLOUD, p.getX(), p.getY(), p.getZ(), 8, 0.2, 0.1, 0.2, 0.02);
                Game.sound(p, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 0.7f);
                p.getItemCooldownManager().set(stack.getItem(), 30);
                return true;
            }
            case GRAPPLE -> { grapple(p); p.getItemCooldownManager().set(stack.getItem(), 25); return true; }
            case DISGUISE -> {
                s.disguiseUntil.put(p.getUuid(), p.getServerWorld().getTime() + 6000);
                s.markDirty();
                stack.decrement(1);
                Game.msg(p, "You put on a fake mustache and a very convincing hat. (5 minutes: troopers ignore you and pocket scans skip you.)", Formatting.LIGHT_PURPLE);
                Game.sound(p, SoundEvents.ENTITY_VILLAGER_YES, 1f, 1.5f);
                p.getServerWorld().spawnParticles(ParticleTypes.POOF, p.getX(), p.getY() + 1, p.getZ(), 15, 0.3, 0.5, 0.3, 0.02);
                return true;
            }
            case HULL_PATCH -> {
                if (!Game.isAboard(p)) { Game.msg(p, "Use this aboard the ship (or at the Engineering Panel).", Formatting.YELLOW); return false; }
                if (s.hull >= 100) { Game.msg(p, "The hull is already at 100%.", Formatting.GRAY); return false; }
                s.hull = Math.min(100, s.hull + 30);
                s.markDirty();
                stack.decrement(1);
                Game.to(Game.aboard(), Game.name(p) + " slapped a Hull Patch on. Hull: " + s.hull + "%", Formatting.GREEN);
                Game.sound(p, SoundEvents.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
                return true;
            }
            case WHISTLE -> { Companion.call(p); p.getItemCooldownManager().set(stack.getItem(), 40); return true; }
            case TOMATO -> {
                SnowballEntity ball = new SnowballEntity(p.getServerWorld(), p);
                ball.setItem(new ItemStack(stack.getItem()));
                ball.setVelocity(p, p.getPitch(), p.getYaw(), 0f, 1.5f, 1f);
                p.getServerWorld().spawnEntity(ball);
                Game.sound(p, SoundEvents.ENTITY_SNOWBALL_THROW, 1f, 0.6f);
                if (!p.isCreative()) stack.decrement(1);
                return true;
            }
            case HANDBOOK, LEDGER -> {
                Books.apply(stack, kind == GearItem.Kind.LEDGER);
                p.currentScreenHandler.sendContentUpdates();
                p.useBook(stack, hand);
                return true;
            }
            default -> { return false; }
        }
    }

    private static void push(ServerPlayerEntity p) {
        p.velocityModified = true;
        p.fallDistance = 0;
        p.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(p));
    }

    private static void blaster(ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        Vec3d start = p.getEyePos();
        Vec3d dir = p.getRotationVector();
        Vec3d end = start.add(dir.multiply(40));
        BlockHitResult bh = w.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
        if (bh.getType() != HitResult.Type.MISS) end = bh.getPos();
        Box box = p.getBoundingBox().stretch(end.subtract(start)).expand(1.0);
        EntityHitResult eh = ProjectileUtil.raycast(p, start, end, box, e -> e instanceof LivingEntity && e.isAlive() && e != p && !e.isSpectator(), start.squaredDistanceTo(end));
        if (eh != null) end = eh.getPos();
        DustParticleEffect red = new DustParticleEffect(new Vector3f(1f, 0.25f, 0.2f), 1.0f);
        double len = start.distanceTo(end);
        for (double d = 1; d < len; d += 0.6) {
            Vec3d at = start.add(dir.multiply(d));
            w.spawnParticles(red, at.x, at.y - 0.1, at.z, 1, 0, 0, 0, 0);
        }
        Game.sound(p, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 1.8f);
        if (eh == null) return;
        Entity hit = eh.getEntity();
        if (hit instanceof ServerPlayerEntity target) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 2));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 60, 0));
            Game.msg(target, Game.name(p) + " stunned you with a blaster! (Set to 'friendly'.)", Formatting.YELLOW);
            w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1, target.getZ(), 15, 0.3, 0.5, 0.3, 0.1);
        } else if (hit instanceof LivingEntity le) {
            le.damage(p.getDamageSources().playerAttack(p), 6f);
            w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, le.getX(), le.getY() + 1, le.getZ(), 10, 0.3, 0.5, 0.3, 0.1);
        }
    }

    private static void grapple(ServerPlayerEntity p) {
        HitResult hr = p.raycast(32, 0f, false);
        if (!(hr instanceof BlockHitResult bh) || hr.getType() != HitResult.Type.BLOCK) {
            Game.bar(p, "Nothing to hook onto!", Formatting.GRAY);
            return;
        }
        Vec3d target = bh.getPos();
        Vec3d from = p.getPos().add(0, 1, 0);
        Vec3d d = target.subtract(from);
        double dist = d.length();
        Vec3d v = d.normalize().multiply(Math.min(2.2, 0.4 + dist * 0.22)).add(0, 0.35, 0);
        p.setVelocity(v);
        push(p);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 40, 0, false, false));
        ServerWorld w = p.getServerWorld();
        for (double t = 0; t < dist; t += 0.7) {
            Vec3d at = from.add(d.normalize().multiply(t));
            w.spawnParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
        Game.sound(p, SoundEvents.ENTITY_FISHING_BOBBER_THROW, 1f, 0.6f);
    }

    static void tickSecond(MinecraftServer server) {
        CrewState s = Game.S();
        for (ServerPlayerEntity p : Game.players()) {
            Long until = s.disguiseUntil.get(p.getUuid());
            if (until == null) continue;
            long left = until - p.getServerWorld().getTime();
            if (left <= 0) {
                s.disguiseUntil.remove(p.getUuid());
                s.markDirty();
                Game.msg(p, "Your fake mustache fell off. You're recognisable again!", Formatting.GRAY);
            } else if (left % 400 < 20) {
                Game.bar(p, "Disguised (" + (left / 20) + "s left)", Formatting.LIGHT_PURPLE);
            }
        }
    }
}
