package dev.voidoutlaws.game;

import dev.voidoutlaws.state.CrewState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

import java.util.UUID;

/** A small floaty alien buddy (a renamed allay) that follows its owner around the galaxy. */
public final class Companion {
    private Companion() {}

    static final String[] NAMES = {"Zib", "Pip", "Blorbo", "Nuggin", "Squee", "Mochi", "Glimmer", "Bean"};

    private static Entity find(UUID id) {
        if (id == null) return null;
        for (ServerWorld w : Game.server.getWorlds()) {
            Entity e = w.getEntity(id);
            if (e != null && e.isAlive()) return e;
        }
        return null;
    }

    public static Entity nearby(ServerPlayerEntity p) {
        UUID id = Game.S().companion.get(p.getUuid());
        if (id == null) return null;
        Entity e = p.getServerWorld().getEntity(id);
        if (e != null && e.isAlive() && e.squaredDistanceTo(p) < 48 * 48) return e;
        return null;
    }

    /** 30% chance the buddy hides one stack during a scan (only if it's with you). */
    public static boolean swallowChance(ServerPlayerEntity p) {
        return nearby(p) != null && Game.R.nextDouble() < 0.3;
    }

    public static void call(ServerPlayerEntity p) {
        CrewState s = Game.S();
        Entity e = find(s.companion.get(p.getUuid()));
        Vec3d at = p.getPos().add(0.8, 1, 0.8);
        if (e != null) {
            e.teleportTo(new TeleportTarget(p.getServerWorld(), at, Vec3d.ZERO, p.getYaw(), 0, TeleportTarget.NO_OP));
            Game.bar(p, e.getName().getString() + " zooms over happily!", Formatting.LIGHT_PURPLE);
            Game.sound(p, SoundEvents.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 1f, 1f);
            if (e instanceof AllayEntity a) a.getBrain().remember(MemoryModuleType.LIKED_PLAYER, p.getUuid());
            return;
        }
        AllayEntity a = EntityType.ALLAY.create(p.getServerWorld());
        if (a == null) return;
        String name = NAMES[Game.R.nextInt(NAMES.length)];
        a.refreshPositionAndAngles(at.x, at.y, at.z, p.getYaw(), 0);
        a.setCustomName(Text.literal(name).formatted(Formatting.LIGHT_PURPLE));
        a.setCustomNameVisible(true);
        a.setPersistent();
        a.setInvulnerable(true);
        a.addCommandTag("vo_companion");
        a.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.AMETHYST_SHARD));
        a.getBrain().remember(MemoryModuleType.LIKED_PLAYER, p.getUuid());
        p.getServerWorld().spawnEntity(a);
        s.companion.put(p.getUuid(), a.getUuid());
        s.markDirty();
        Game.msg(p, "Meet " + name + ", your new alien buddy! It follows you, and sometimes hides contraband from scans by... eating it.", Formatting.LIGHT_PURPLE);
        Game.sound(p, SoundEvents.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 1f, 1.3f);
    }
}
