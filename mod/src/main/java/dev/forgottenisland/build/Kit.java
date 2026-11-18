package dev.forgottenisland.build;

import net.minecraft.util.math.Direction;

/** Reusable architectural pieces, always authored in a builder's local coordinates. */
public final class Kit {
    private Kit() {}

    static String face(Direction d) { return d.asString(); }
    static Direction opp(Direction d) { return d.getOpposite(); }

    /** Timber-framed walls: posts at corners and every 'bay' blocks, sill and top plates, infill between. */
    public static void timberWalls(B b, int x1, int z1, int x2, int z2, int y1, int y2, String log, Pal infill, int bay) {
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) for (int z : new int[]{z1, z2}) {
                boolean post = x == x1 || x == x2 || (x - x1) % bay == 0;
                if (post) b.set(x, y, z, log + "[axis=y]");
                else if (y == y1 || y == y2) b.set(x, y, z, log + "[axis=x]");
                else b.set(x, y, z, infill);
            }
            for (int z = z1 + 1; z < z2; z++) for (int x : new int[]{x1, x2}) {
                boolean post = (z - z1) % bay == 0;
                if (post) b.set(x, y, z, log + "[axis=y]");
                else if (y == y1 || y == y2) b.set(x, y, z, log + "[axis=z]");
                else b.set(x, y, z, infill);
            }
        }
    }

    /** Diagonal braces (stairs) in a timber bay for a hand-built look. */
    public static void brace(B b, int x, int y, int z, String stair, Direction facing) {
        b.set(x, y, z, stair + "[facing=" + face(facing) + ",half=top]");
    }

    /** A window in a wall at (x,y..y+h-1,z). 'out' is the outward direction. Adds shutters and a flower box. */
    public static void window(B b, int x, int y, int z, int h, Direction out, String pane, String shutter, String flower) {
        for (int i = 0; i < h; i++) b.set(x, y + i, z, pane);
        Direction left = out.rotateYCounterclockwise(), right = out.rotateYClockwise();
        int ox = out.getOffsetX(), oz = out.getOffsetZ();
        if (shutter != null) {
            for (int i = 0; i < h; i++) {
                b.setIfAir(x + ox + left.getOffsetX(), y + i, z + oz + left.getOffsetZ(), shutter + "[facing=" + face(left) + ",open=true,half=top]");
                b.setIfAir(x + ox + right.getOffsetX(), y + i, z + oz + right.getOffsetZ(), shutter + "[facing=" + face(right) + ",open=true,half=top]");
            }
        }
        if (flower != null) {
            b.setIfAir(x + ox, y - 1, z + oz, shutter != null ? shutter + "[facing=" + face(out) + ",open=true,half=bottom]" : "spruce_trapdoor[facing=" + face(out) + ",open=true]");
            b.setIfAir(x + ox, y, z + oz, flower);
        }
    }

    /** Gable roof with the ridge along local X, covering z1..z2 (inclusive walls), eaves overhang 1. */
    public static void gableX(B b, int x1, int z1, int x2, int z2, int y, String stair, String slab, Pal gable, boolean flare) {
        int depth = z2 - z1 + 3;
        int layers = (depth + 1) / 2;
        for (int k = 0; k < layers; k++) {
            int zn = z1 - 1 + k, zs = z2 + 1 - k, yk = y + k;
            for (int x = x1 - 1; x <= x2 + 1; x++) {
                if (zn == zs) { b.set(x, yk, zn, slab + "[type=bottom]"); continue; }
                b.set(x, yk, zn, stair + "[facing=south,half=bottom]");
                b.set(x, yk, zs, stair + "[facing=north,half=bottom]");
                if (k == 0 && flare) {
                    b.setIfAir(x, yk - 1, zn, stair + "[facing=north,half=top]");
                    b.setIfAir(x, yk - 1, zs, stair + "[facing=south,half=top]");
                }
            }
            if (zs - zn == 1) for (int x = x1 - 1; x <= x2 + 1; x++) b.set(x, yk + 1, zn, slab + "[type=bottom]");
            if (zs - zn == 1) for (int x = x1 - 1; x <= x2 + 1; x++) b.set(x, yk + 1, zs, slab + "[type=bottom]");
            // gable ends + ceiling fill under the slope at both ends
            for (int z = zn + 1; z < zs; z++) { b.set(x1, yk, z, gable); b.set(x2, yk, z, gable); }
        }
    }

    /** Same as gableX with the ridge along local Z. */
    public static void gableZ(B b, int x1, int z1, int x2, int z2, int y, String stair, String slab, Pal gable, boolean flare) {
        // rotate: local X' = z, Z' = -x  (use a rotated child builder)
        B r = b.sub(0, 0, 0, 1);
        // in r, local (X', Z') maps to b-local (-Z', X')  => b x = -Z', b z = X'
        gableX(r, z1, -x2, z2, -x1, y, stair, slab, gable, flare);
    }

    public static void chimney(B b, int x, int z, int y1, int y2, Pal p) {
        for (int y = y1; y <= y2; y++) b.set(x, y, z, p);
        b.set(x, y2 + 1, z, "campfire[lit=true,signal_fire=false]");
    }

    public static void door(B b, int x, int y, int z, Direction facing, String door, boolean rightHinge) {
        String h = rightHinge ? "right" : "left";
        b.set(x, y, z, door + "[facing=" + face(facing) + ",half=lower,hinge=" + h + "]");
        b.set(x, y + 1, z, door + "[facing=" + face(facing) + ",half=upper,hinge=" + h + "]");
    }

    /** Bed with its head toward 'toward'. (x,y,z) is the head block. */
    public static void bed(B b, int x, int y, int z, Direction toward, String color) {
        b.set(x, y, z, color + "_bed[facing=" + face(toward) + ",part=head]");
        b.set(x - toward.getOffsetX(), y, z - toward.getOffsetZ(), color + "_bed[facing=" + face(toward) + ",part=foot]");
    }

    public static void table(B b, int x, int y, int z, String wood) {
        b.set(x, y, z, wood + "_fence");
        b.set(x, y + 1, z, wood + "_pressure_plate");
    }
    public static void tableTop(B b, int x, int y, int z, String wood, String onTop) {
        b.set(x, y, z, wood + "_fence");
        b.set(x, y + 1, z, onTop);
    }
    /** A chair: stair facing away from the table so the seat back is behind the sitter. */
    public static void chair(B b, int x, int y, int z, Direction faces, String stair) {
        b.set(x, y, z, stair + "[facing=" + face(opp(faces)) + ",half=bottom]");
    }
    public static void hangLantern(B b, int x, int y, int z, int chain, boolean soul) {
        for (int i = 0; i < chain; i++) b.set(x, y - i, z, "chain[axis=y]");
        b.set(x, y - chain, z, (soul ? "soul_lantern" : "lantern") + "[hanging=true]");
    }
    public static void carpet(B b, int x1, int z1, int x2, int z2, int y, String color) {
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) if (b.air(x, y, z)) b.set(x, y, z, color + "_carpet");
    }
    /** Shelf: trapdoor ledge with a few items on it is impossible; use barrels/bookshelves mix. */
    public static void shelfWall(B b, int x1, int z1, int x2, int z2, int y1, int y2, Pal p) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) for (int y = y1; y <= y2; y++) b.set(x, y, z, p);
    }
    public static void path(B b, int x, int z, Pal surface) {
        int wx = b.x(x, z), wz = b.z(x, z);
        int top = topSolid(b, wx, wz);
        b.wset(wx, top, wz, surface.pick(wx, top, wz));
    }
    public static int topSolid(B b, int wx, int wz) {
        return b.w.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, wx, wz) - 1;
    }
}
