package dev.voidoutlaws.game;

import dev.voidoutlaws.state.CrewState;
import dev.voidoutlaws.world.Loc;
import dev.voidoutlaws.world.Structures;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

/** All teleports go through here so companions tag along. */
public final class Tp {
    private Tp() {}

    public static void to(ServerPlayerEntity p, ServerWorld w, Vec3d pos, float yaw) {
        if (w == null) return;
        Entity buddy = Companion.nearby(p);
        p.closeHandledScreen();
        p.teleport(w, pos.x, pos.y, pos.z, yaw, 0);
        p.fallDistance = 0;
        if (buddy != null) {
            buddy.teleportTo(new TeleportTarget(w, pos.add(0.8, 0.5, 0.8), Vec3d.ZERO, yaw, 0, TeleportTarget.NO_OP));
        }
    }

    public static void toShip(ServerPlayerEntity p) {
        CrewState s = Game.S();
        Structures.ensureShip(Game.server, s);
        to(p, Structures.world(Game.server, Loc.SHIP), Structures.SHIP_SPAWN, 180f);
    }

    public static void toLoc(ServerPlayerEntity p, Loc l) {
        CrewState s = Game.S();
        Structures.ensureBuilt(Game.server, s, l);
        to(p, Structures.world(Game.server, l.key), Structures.arrival(s, l), 180f);
    }
}
