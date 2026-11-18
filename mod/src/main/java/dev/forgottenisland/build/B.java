package dev.forgottenisland.build;

import dev.forgottenisland.ForgottenIsland;
import dev.forgottenisland.game.KeyItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

/** World-editing context: a transform plus helpers. All writes skip neighbour updates (static scenery). */
public class B extends T {
    public final ServerWorld w;
    public final MinecraftServer server;
    private static final Map<String, BlockState> CACHE = new HashMap<>();
    public static final int FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE | Block.SKIP_DROPS;
    public static long placed;

    public B(ServerWorld w, int ox, int oy, int oz, int rot) { super(ox, oy, oz, rot); this.w = w; this.server = w.getServer(); }
    public B(ServerWorld w, T t) { this(w, t.ox, t.oy, t.oz, t.rot); }
    public B at(int ox, int oy, int oz, int rot) { return new B(w, ox, oy, oz, rot); }
    /** A child transform relative to this one. */
    public B sub(int lx, int ly, int lz, int r) { return new B(w, x(lx, lz), y(ly), z(lx, lz), rot + r); }

    public static BlockState s(String spec) {
        return CACHE.computeIfAbsent(spec, k -> {
            String full = k.contains(":") && k.indexOf(':') < (k.contains("[") ? k.indexOf('[') : k.length()) ? k : "minecraft:" + k;
            try { return BlockArgumentParser.block(Registries.BLOCK.getReadOnlyWrapper(), full, false).blockState(); }
            catch (Exception e) { ForgottenIsland.LOG.error("Bad block spec {}", k); return Blocks.MAGENTA_WOOL.getDefaultState(); }
        });
    }

    BlockState rotated(BlockState st) { return rot == 0 ? st : st.rotate(BlockRotation.values()[rot]); }
    public Direction dir(Direction local) {
        if (!local.getAxis().isHorizontal()) return local;
        Direction d = local; for (int i = 0; i < rot; i++) d = d.rotateYClockwise(); return d;
    }

    // ------------------------------------------------------------ raw world access (world coords)
    public void wset(int x, int y, int z, BlockState st) { w.setBlockState(new BlockPos(x, y, z), st, FLAGS); placed++; }
    public BlockState wget(int x, int y, int z) { return w.getBlockState(new BlockPos(x, y, z)); }

    // ------------------------------------------------------------ local access
    public void set(int x, int y, int z, BlockState st) { wset(x(x, z), y(y), z(x, z), rotated(st)); }
    public void set(int x, int y, int z, String spec) { set(x, y, z, s(spec)); }
    public void set(int x, int y, int z, Pal p) { set(x, y, z, p.pick(x(x, z), y(y), z(x, z))); }
    public BlockState get(int x, int y, int z) { return wget(x(x, z), y(y), z(x, z)); }
    public boolean air(int x, int y, int z) { return get(x, y, z).isAir(); }
    public void setIfAir(int x, int y, int z, String spec) { if (air(x, y, z)) set(x, y, z, spec); }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, String spec) { fill(x1, y1, z1, x2, y2, z2, Pal.of(spec)); }
    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, Pal p) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, p);
    }
    public void clear(int x1, int y1, int z1, int x2, int y2, int z2) { fill(x1, y1, z1, x2, y2, z2, "air"); }
    /** Four walls (no floor/ceiling). */
    public void walls(int x1, int y1, int z1, int x2, int y2, int z2, Pal p) {
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) { set(x, y, z1, p); set(x, y, z2, p); }
            for (int z = z1; z <= z2; z++) { set(x1, y, z, p); set(x2, y, z, p); }
        }
    }
    /** Solid foundation from local y down to the ground (fills air/water/plants below). */
    public void foundation(int x1, int z1, int x2, int z2, int ly, Pal p) {
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) column(x, ly, z, p);
    }
    public void column(int x, int ly, int z, Pal p) {
        for (int y = ly; y > ly - 40; y--) {
            BlockState st = get(x, y, z);
            if (y < ly && !st.isAir() && !st.isReplaceable() && st.getFluidState().isEmpty() && !st.isIn(net.minecraft.registry.tag.BlockTags.LEAVES)) break;
            set(x, y, z, p);
        }
    }

    // ------------------------------------------------------------ containers, books, signs
    public static ItemStack item(MinecraftServer server, String spec) {
        int n = 1;
        String id = spec;
        if (spec.contains("*")) { n = Integer.parseInt(spec.substring(spec.indexOf('*') + 1)); id = spec.substring(0, spec.indexOf('*')); }
        if (id.startsWith("fi:")) return KeyItems.create(id.substring(3), n > 1 ? n : -1, server);
        return new ItemStack(Registries.ITEM.get(Identifier.of(id.contains(":") ? id : "minecraft:" + id)), n);
    }

    public void container(int x, int y, int z, String spec, String... items) {
        set(x, y, z, spec);
        BlockEntity be = w.getBlockEntity(pos(x, y, z));
        if (be instanceof LootableContainerBlockEntity c) {
            int slot = 0;
            for (String it : items) { if (slot >= c.size()) break; c.setStack(slot, item(server, it)); slot += 2 + (slot % 3); if (slot >= c.size()) slot = 1; }
            c.markDirty();
        }
    }

    public void lectern(int x, int y, int z, Direction facing, String keyItem) {
        set(x, y, z, Blocks.LECTERN.getDefaultState().with(net.minecraft.block.LecternBlock.FACING, facing).with(net.minecraft.block.LecternBlock.HAS_BOOK, true));
        if (w.getBlockEntity(pos(x, y, z)) instanceof LecternBlockEntity le) { le.setBook(KeyItems.create(keyItem, server)); le.markDirty(); }
    }

    public void sign(int x, int y, int z, String spec, String... lines) {
        set(x, y, z, spec);
        if (w.getBlockEntity(pos(x, y, z)) instanceof SignBlockEntity sb) {
            sb.changeText(t -> {
                for (int i = 0; i < Math.min(4, lines.length); i++) t = t.withMessage(i, Text.literal(lines[i]));
                return t;
            }, true);
            sb.setWaxed(true);
            sb.markDirty();
        }
    }

    // ------------------------------------------------------------ entities (decoration)
    public void frame(int x, int y, int z, Direction localFacing, String itemSpec) {
        ItemFrameEntity f = new ItemFrameEntity(w, pos(x, y, z), dir(localFacing));
        f.setHeldItemStack(item(server, itemSpec), false);
        f.setInvulnerable(true);
        f.addCommandTag("fi_deco");
        w.spawnEntity(f);
    }

    public ArmorStandEntity stand(double x, int y, double z, float localYaw, String... gear) {
        ArmorStandEntity a = EntityType.ARMOR_STAND.create(w);
        if (a == null) return null;
        a.refreshPositionAndAngles(x(0, 0) + rx(x, z), y(y), z(0, 0) + rz(x, z), yaw(localYaw), 0);
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
        for (int i = 0; i < gear.length && i < slots.length; i++) if (!gear[i].isEmpty()) a.equipStack(slots[i], item(server, gear[i]));
        a.setInvulnerable(true);
        a.addCommandTag("fi_deco");
        w.spawnEntity(a);
        return a;
    }
    double rx(double lx, double lz) { return switch (rot) { case 0 -> lx; case 1 -> -lz; case 2 -> -lx; default -> lz; }; }
    double rz(double lx, double lz) { return switch (rot) { case 0 -> lz; case 1 -> lx; case 2 -> -lz; default -> -lx; }; }

    public <E extends MobEntity> E mob(EntityType<E> type, int x, int y, int z) {
        E m = type.create(w);
        if (m == null) return null;
        m.refreshPositionAndAngles(x(x, z) + 0.5, y(y), z(x, z) + 0.5, w.random.nextFloat() * 360, 0);
        m.setPersistent();
        m.addCommandTag("fi_ambient");
        w.spawnEntity(m);
        return m;
    }

    public Entity boat(int x, int y, int z, float localYaw, net.minecraft.entity.vehicle.BoatEntity.Type type) {
        var b = EntityType.BOAT.create(w);
        if (b == null) return null;
        b.refreshPositionAndAngles(x(x, z) + 0.5, y(y), z(x, z) + 0.5, yaw(localYaw), 0);
        b.setVariant(type);
        b.addCommandTag("fi_deco");
        w.spawnEntity(b);
        return b;
    }

    public static List<String> L(String... s) { return Arrays.asList(s); }
}
