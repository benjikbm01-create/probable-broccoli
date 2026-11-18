package dev.forgottenisland.build;

import dev.forgottenisland.game.Places;
import dev.forgottenisland.game.Zones;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.function.Consumer;

/**
 * A placement transform: structures are authored in local coordinates (entrance facing local +Z / south)
 * and placed at an origin with a clockwise rotation in quarter turns. Pure math, usable without a world.
 */
public class T {
    public final int ox, oy, oz, rot;
    public T(int ox, int oy, int oz, int rot) { this.ox = ox; this.oy = oy; this.oz = oz; this.rot = ((rot % 4) + 4) % 4; }

    public int x(int lx, int lz) { return switch (rot) { case 0 -> ox + lx; case 1 -> ox - lz; case 2 -> ox - lx; default -> ox + lz; }; }
    public int z(int lx, int lz) { return switch (rot) { case 0 -> oz + lz; case 1 -> oz + lx; case 2 -> oz - lz; default -> oz - lx; }; }
    public int y(int ly) { return oy + ly; }
    public BlockPos pos(int lx, int ly, int lz) { return new BlockPos(x(lx, lz), y(ly), z(lx, lz)); }
    /** Local yaw (0 = facing local south/+Z) to world yaw. */
    public float yaw(float local) { return local + rot * 90f; }

    /** Registers a named standing spot at the centre of local block (lx, ly, lz). */
    public Places.P place(String name, int lx, int ly, int lz, float localYaw) {
        return Places.add(name, x(lx, lz) + 0.5, y(ly), z(lx, lz) + 0.5, yaw(localYaw));
    }
    public Places.P place(String name, int lx, double ly, int lz, float localYaw) {
        return Places.add(name, x(lx, lz) + 0.5, oy + ly, z(lx, lz) + 0.5, yaw(localYaw));
    }

    public void zone(String name, int x1, int y1, int z1, int x2, int y2, int z2, Consumer<ServerPlayerEntity> onEnter) {
        Zones.add(name, x(x1, z1), y(y1), z(x1, z1), x(x2, z2), y(y2), z(x2, z2), onEnter);
    }
    public void zoneRepeat(String name, int x1, int y1, int z1, int x2, int y2, int z2, Consumer<ServerPlayerEntity> onEnter) {
        Zones.add(name, x(x1, z1), y(y1), z(x1, z1), x(x2, z2), y(y2), z(x2, z2), onEnter, true);
    }

    /** Flattens the terrain under a local rectangle to local y-1 (i.e. ground top = oy + ly - 1). */
    public void pad(int x1, int z1, int x2, int z2, int ly, int fall) {
        Terrain.pad(x(x1, z1), z(x1, z1), x(x2, z2), z(x2, z2), y(ly) - 1, fall);
    }
}
