package dev.voidoutlaws.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.HashSet;
import java.util.Set;

/**
 * Tiny structure-building helper. Coordinates are local to an origin.
 * When the world is null it runs "dry", only recording which blocks a structure uses
 * (so block protection works after a restart).
 */
public class Build {
    /** Every block type placed by any structure: these are protected from breaking. */
    public static final Set<Block> PALETTE = new HashSet<>();

    public final ServerWorld w;
    public final int ox, oy, oz;

    public Build(ServerWorld w, int ox, int oy, int oz) {
        this.w = w;
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
    }

    public BlockPos at(int x, int y, int z) {
        return new BlockPos(ox + x, oy + y, oz + z);
    }

    public void set(int x, int y, int z, BlockState s) {
        if (!s.isAir()) PALETTE.add(s.getBlock());
        if (w == null) return;
        w.setBlockState(at(x, y, z), s, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
    }

    public void set(int x, int y, int z, Block b) {
        set(x, y, z, b.getDefaultState());
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    set(x, y, z, s);
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
        fill(x1, y1, z1, x2, y2, z2, b.getDefaultState());
    }

    public void clear(int x1, int y1, int z1, int x2, int y2, int z2) {
        fill(x1, y1, z1, x2, y2, z2, Blocks.AIR.getDefaultState());
    }

    /** Hollow box: shell of {@code b}, air inside. */
    public void shell(int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
        fill(x1, y1, z1, x2, y2, z2, b);
        clear(Math.min(x1, x2) + 1, Math.min(y1, y2) + 1, Math.min(z1, z2) + 1,
                Math.max(x1, x2) - 1, Math.max(y1, y2) - 1, Math.max(z1, z2) - 1);
    }

    /** Surface height (first free y) of generated terrain at an absolute column. */
    public static int surface(ServerWorld w, int x, int z) {
        if (w == null) return 64;
        w.getChunk(x >> 4, z >> 4);
        return w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    /** Floating text label (vanilla text display entity). */
    public void label(double x, double y, double z, String text, Formatting color) {
        if (w == null) return;
        DisplayEntity.TextDisplayEntity td = EntityType.TEXT_DISPLAY.create(w);
        if (td == null) return;
        td.setText(Text.literal(text).formatted(color));
        td.setBillboardMode(DisplayEntity.BillboardMode.CENTER);
        td.refreshPositionAndAngles(ox + x, oy + y, oz + z, 0, 0);
        td.addCommandTag("vo_label");
        w.spawnEntity(td);
    }

    /** Removes our labels inside a local box (used before rebuilding). */
    public void clearLabels(int x1, int y1, int z1, int x2, int y2, int z2) {
        if (w == null) return;
        net.minecraft.util.math.Box box = new net.minecraft.util.math.Box(
                ox + x1, oy + y1, oz + z1, ox + x2 + 1, oy + y2 + 1, oz + z2 + 1);
        for (DisplayEntity.TextDisplayEntity e : w.getEntitiesByClass(DisplayEntity.TextDisplayEntity.class, box,
                e -> e.getCommandTags().contains("vo_label"))) {
            e.discard();
        }
    }
}
