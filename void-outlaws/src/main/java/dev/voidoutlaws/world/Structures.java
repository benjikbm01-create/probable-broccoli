package dev.voidoutlaws.world;

import dev.voidoutlaws.VoidOutlaws;
import dev.voidoutlaws.reg.ModBlocks;
import dev.voidoutlaws.reg.OutlawBlock.Kind;
import dev.voidoutlaws.state.CrewState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/** Builds every location in code (no structure files), and knows where things are. */
public final class Structures {
    private Structures() {}

    public static final String VERSION = "v1";

    // ------------------------------------------------------------------ fixed origins
    public static final BlockPos SHIP_O = new BlockPos(0, 100, 0);
    public static final BlockPos MERIDIAN_O = new BlockPos(0, 64, 0);
    public static final BlockPos MAW_O = new BlockPos(0, 80, 0);
    public static final BlockPos BURROW_O = new BlockPos(0, 64, 0);
    public static final BlockPos DERELICT_O = new BlockPos(0, 64, 0);

    public static final Vec3d SHIP_SPAWN = new Vec3d(0.5, 100, -4.5);

    // heist (Meridian, local to MERIDIAN_O)
    public static final int[][] VAULT_CRATES = {{24, 0, -3}, {24, 0, 0}, {24, 0, 3}};
    public static final int[][] BANK_SPAWNS = {{7, 0, -6}, {7, 0, 6}, {13, 0, 6}, {13, 0, -6}};
    // boarding (Derelict, local to DERELICT_O)
    public static final int[][] DERELICT_CRATES = {{-4, 0, -28}, {4, 0, -28}, {0, 0, -20}};
    public static final int[][] DERELICT_SPAWNS = {{0, 0, 3}, {-4, 0, 7}, {4, 0, 6}};

    private static Block b(Kind k) {
        return ModBlocks.get(k);
    }

    public static ServerWorld world(MinecraftServer server, RegistryKey<World> key) {
        ServerWorld w = server.getWorld(key);
        if (w == null) VoidOutlaws.LOG.error("Missing dimension {} - is the mod's data pack loaded?", key.getValue());
        return w;
    }

    public static BlockPos origin(CrewState s, Loc l) {
        return switch (l) {
            case MERIDIAN -> MERIDIAN_O;
            case GILDED_MAW -> MAW_O;
            case BURROW -> BURROW_O;
            case DERELICT -> DERELICT_O;
            case DUSTREACH -> new BlockPos(0, (int) (long) s.pads.getOrDefault("dustreach", 80L), 0);
        };
    }

    public static BlockPos abs(CrewState s, Loc l, int[] local) {
        return origin(s, l).add(local[0], local[1], local[2]);
    }

    /** Where players appear when they step off the ship. */
    public static Vec3d arrival(CrewState s, Loc l) {
        BlockPos o = origin(s, l);
        return switch (l) {
            case MERIDIAN, DUSTREACH -> new Vec3d(o.getX() + 0.5, o.getY(), o.getZ() + 3.5);
            case GILDED_MAW -> new Vec3d(o.getX() + 0.5, o.getY(), o.getZ() + 17.5);
            case BURROW -> new Vec3d(o.getX() + 0.5, o.getY(), o.getZ() + 6.5);
            case DERELICT -> new Vec3d(o.getX() + 0.5, o.getY(), o.getZ() + 8.5);
        };
    }

    // ------------------------------------------------------------------ protection
    public static boolean isProtected(CrewState s, RegistryKey<World> key, BlockPos p) {
        if (key.equals(Loc.SHIP)) return inBox(p, SHIP_O, -14, -6, -17, 14, 12, 21);
        Loc l = Loc.fromWorld(key);
        if (l == null) return false;
        BlockPos o = origin(s, l);
        return switch (l) {
            case MERIDIAN -> inBox(p, o, -31, -6, -31, 31, 30, 31);
            case DUSTREACH -> inBox(p, o, -31, -6, -31, 31, 40, 31);
            case GILDED_MAW -> inBox(p, o, -26, -12, -26, 26, 14, 26);
            case BURROW -> inBox(p, o, -23, -23, -23, 23, 23, 23);
            case DERELICT -> inBox(p, o, -9, -4, -36, 9, 10, 18);
        };
    }

    private static boolean inBox(BlockPos p, BlockPos o, int x1, int y1, int z1, int x2, int y2, int z2) {
        int x = p.getX() - o.getX(), y = p.getY() - o.getY(), z = p.getZ() - o.getZ();
        return x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2;
    }

    // ------------------------------------------------------------------ building
    /** Runs every builder without a world so the protection palette is known. */
    public static void dryRun() {
        CrewState dummy = new CrewState();
        buildShip(new Build(null, 0, 0, 0));
        buildMeridian(new Build(null, 0, 0, 0));
        buildDustreach(new Build(null, 0, 0, 0));
        buildMaw(new Build(null, 0, 0, 0));
        buildBurrow(new Build(null, 0, 0, 0));
        buildDerelict(new Build(null, 0, 0, 0), dummy);
    }

    public static void ensureShip(MinecraftServer server, CrewState s) {
        String tag = "ship:" + VERSION;
        if (s.built.contains(tag)) return;
        ServerWorld w = world(server, Loc.SHIP);
        if (w == null) return;
        buildShip(new Build(w, SHIP_O.getX(), SHIP_O.getY(), SHIP_O.getZ()));
        s.built.add(tag);
        s.markDirty();
    }

    public static void ensureBuilt(MinecraftServer server, CrewState s, Loc l) {
        String tag = l.id + ":" + VERSION;
        if (s.built.contains(tag)) return;
        rebuild(server, s, l);
    }

    public static void rebuild(MinecraftServer server, CrewState s, Loc l) {
        ServerWorld w = world(server, l.key);
        if (w == null) return;
        if (l == Loc.DUSTREACH) {
            int h = Build.surface(w, 0, 0);
            int y = MathHelper.clamp(h, 64, 140);
            s.pads.put("dustreach", (long) y);
        }
        BlockPos o = origin(s, l);
        Build bd = new Build(w, o.getX(), o.getY(), o.getZ());
        bd.clearLabels(-32, -10, -36, 32, 40, 32);
        switch (l) {
            case MERIDIAN -> buildMeridian(bd);
            case DUSTREACH -> buildDustreach(bd);
            case GILDED_MAW -> buildMaw(bd);
            case BURROW -> buildBurrow(bd);
            case DERELICT -> buildDerelict(bd, s);
        }
        s.built.add(l.id + ":" + VERSION);
        s.markDirty();
        VoidOutlaws.LOG.info("Built location {}", l.display);
    }

    public static void rebuildShip(MinecraftServer server, CrewState s) {
        s.built.remove("ship:" + VERSION);
        ServerWorld w = world(server, Loc.SHIP);
        if (w != null) new Build(w, SHIP_O.getX(), SHIP_O.getY(), SHIP_O.getZ()).clearLabels(-14, -6, -17, 14, 12, 21);
        ensureShip(server, s);
    }

    // ================================================================== THE SHIP
    static void buildShip(Build bd) {
        Block wall = Blocks.LIGHT_GRAY_CONCRETE;
        bd.shell(-6, -1, -14, 6, 5, 14, wall);
        bd.fill(-5, -1, -13, 5, -1, 13, Blocks.POLISHED_DEEPSLATE);
        bd.fill(-1, -1, -13, 1, -1, 13, Blocks.DEEPSLATE_TILES);
        bd.fill(-5, 5, -13, 5, 5, 13, Blocks.SMOOTH_STONE);
        for (int z = -12; z <= 12; z += 4) {
            bd.set(-3, 5, z, Blocks.SEA_LANTERN);
            bd.set(3, 5, z, Blocks.SEA_LANTERN);
        }
        bd.fill(-6, 0, -14, -6, 0, 14, Blocks.GRAY_CONCRETE);
        bd.fill(6, 0, -14, 6, 0, 14, Blocks.GRAY_CONCRETE);
        // windows
        bd.fill(-4, 1, -14, 4, 3, -14, Blocks.GLASS);
        bd.fill(-6, 1, -12, -6, 3, -9, Blocks.GLASS);
        bd.fill(6, 1, -12, 6, 3, -9, Blocks.GLASS);
        bd.fill(-6, 2, -5, -6, 2, 0, Blocks.GLASS);
        bd.fill(6, 2, -5, 6, 2, 0, Blocks.GLASS);
        // exterior: wings + engines
        bd.fill(-12, 1, -2, -7, 1, 8, Blocks.GRAY_CONCRETE);
        bd.fill(7, 1, -2, 12, 1, 8, Blocks.GRAY_CONCRETE);
        bd.fill(-4, 0, 15, -2, 3, 17, Blocks.BLACKSTONE);
        bd.fill(2, 0, 15, 4, 3, 17, Blocks.BLACKSTONE);
        bd.fill(-4, 0, 18, -2, 3, 18, Blocks.LIGHT_BLUE_STAINED_GLASS);
        bd.fill(2, 0, 18, 4, 3, 18, Blocks.LIGHT_BLUE_STAINED_GLASS);
        // dividers
        for (int z : new int[]{-7, 2, 10}) {
            bd.fill(-5, 0, z, 5, 4, z, wall);
            bd.clear(-1, 0, z, 1, 2, z);
            bd.set(-2, 3, z, Blocks.YELLOW_CONCRETE);
            bd.set(2, 3, z, Blocks.YELLOW_CONCRETE);
        }
        // bridge
        bd.set(0, 0, -12, b(Kind.SHIP_CONSOLE));
        bd.set(-2, 0, -13, Blocks.POLISHED_BLACKSTONE);
        bd.set(2, 0, -13, Blocks.POLISHED_BLACKSTONE);
        bd.set(-2, 1, -13, Blocks.LIGHT_BLUE_STAINED_GLASS);
        bd.set(2, 1, -13, Blocks.LIME_STAINED_GLASS);
        bd.label(0.5, 1.6, -11.5, "SHIP CONSOLE\n(Pilot)", Formatting.AQUA);
        bd.set(-5, 0, -9, b(Kind.GUNNERY_STATION));
        bd.label(-4.5, 1.6, -8.5, "GUNNERY STATION\n(Gunner)", Formatting.RED);
        // common room
        bd.set(5, 0, -3, b(Kind.LOOT_LOCKER));
        bd.label(4.5, 1.6, -2.5, "LOOT LOCKER\n(split the pot)", Formatting.GOLD);
        bd.fill(-4, 0, -3, -3, 0, -1, Blocks.SPRUCE_PLANKS);
        bd.set(5, 0, 0, Blocks.BOOKSHELF);
        bd.set(5, 1, 0, Blocks.BOOKSHELF);
        bd.set(-5, 0, -6, Blocks.POTTED_FERN);
        bd.set(5, 0, -6, Blocks.POTTED_FERN);
        // cargo bay
        bd.set(5, 0, 4, b(Kind.CARGO_HOLD));
        bd.set(5, 0, 7, b(Kind.CARGO_HOLD));
        bd.label(4.5, 1.6, 5.5, "CARGO HOLD\n(gets scanned!)", Formatting.YELLOW);
        bd.fill(4, 0, 9, 5, 0, 9, Blocks.STRIPPED_SPRUCE_WOOD);
        bd.set(5, 1, 9, Blocks.SPRUCE_PLANKS);
        bd.set(-3, -1, 6, b(Kind.HIDDEN_COMPARTMENT));
        bd.label(-2.5, 0.8, 6.5, "(loose floor plate)", Formatting.DARK_GRAY);
        bd.set(-6, 0, 5, b(Kind.SHIP_HATCH));
        bd.set(-6, 1, 5, b(Kind.SHIP_HATCH));
        bd.set(-6, 0, 4, Blocks.YELLOW_CONCRETE);
        bd.set(-6, 1, 4, Blocks.BLACK_CONCRETE);
        bd.set(-6, 2, 4, Blocks.YELLOW_CONCRETE);
        bd.set(-6, 2, 5, Blocks.BLACK_CONCRETE);
        bd.set(-6, 2, 6, Blocks.YELLOW_CONCRETE);
        bd.set(-6, 1, 6, Blocks.BLACK_CONCRETE);
        bd.set(-6, 0, 6, Blocks.YELLOW_CONCRETE);
        bd.label(-4.8, 2.7, 5.5, "HATCH\n(exit the ship)", Formatting.YELLOW);
        // engine room
        bd.set(0, 0, 13, b(Kind.ENGINEERING_PANEL));
        bd.label(0.5, 1.6, 12.5, "ENGINEERING PANEL\n(Mechanic)", Formatting.GREEN);
        for (int y = 0; y <= 4; y++) {
            Block core = y % 2 == 0 ? Blocks.SEA_LANTERN : Blocks.CYAN_STAINED_GLASS;
            bd.set(-4, y, 12, core);
            bd.set(4, y, 12, core);
        }
        bd.set(-2, 4, 11, Blocks.CHAIN);
        bd.set(2, 4, 11, Blocks.CHAIN);
    }

    // ================================================================== MERIDIAN PRIME
    static void buildMeridian(Build bd) {
        bd.fill(-30, -1, -30, 30, -1, 30, Blocks.SMOOTH_STONE);
        bd.clear(-30, 0, -30, 30, 12, 30);
        for (int i = -30; i <= 30; i += 6) {
            bd.fill(i, -1, -30, i, -1, 30, Blocks.POLISHED_ANDESITE);
            bd.fill(-30, -1, i, 30, -1, i, Blocks.POLISHED_ANDESITE);
        }
        pad(bd, Blocks.WHITE_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE);
        // lamp posts
        for (int[] p : new int[][]{{-8, -8}, {8, -8}, {-8, 8}, {8, 8}, {-8, 20}, {8, 20}, {-22, 12}, {22, 14}}) {
            bd.fill(p[0], 0, p[1], p[0], 2, p[1], Blocks.QUARTZ_PILLAR);
            bd.set(p[0], 3, p[1], Blocks.SEA_LANTERN);
        }
        // planters
        for (int x = -26; x <= 26; x += 13) {
            bd.set(x, -1, 26, Blocks.GRASS_BLOCK);
            bd.set(x, 0, 26, Blocks.POPPY);
            bd.set(x + 1, -1, 26, Blocks.GRASS_BLOCK);
            bd.set(x + 1, 0, 26, Blocks.AZURE_BLUET);
        }
        // customs office
        bd.shell(-21, -1, -7, -9, 6, 5, Blocks.QUARTZ_BLOCK);
        bd.fill(-20, -1, -6, -10, -1, 4, Blocks.POLISHED_DIORITE);
        for (int z = -5; z <= 3; z += 2) {
            bd.set(-21, 2, z, Blocks.LIGHT_BLUE_STAINED_GLASS);
            bd.set(-9, 2, z, Blocks.LIGHT_BLUE_STAINED_GLASS);
        }
        bd.clear(-9, 0, -1, -9, 1, 0);
        bd.fill(-16, 0, -5, -16, 0, 3, Blocks.SMOOTH_QUARTZ);
        bd.clear(-16, 0, -1, -16, 0, -1);
        bd.set(-16, 0, -1, Blocks.QUARTZ_SLAB);
        bd.set(-15, 5, -1, Blocks.SEA_LANTERN);
        bd.set(-12, 5, -1, Blocks.SEA_LANTERN);
        bd.set(-19, 5, -1, Blocks.SEA_LANTERN);
        bd.set(-12, 0, 3, b(Kind.FINE_KIOSK));
        bd.label(-11.5, 1.6, 3.5, "FINE PAYMENT KIOSK\n(pay to lower Heat)", Formatting.AQUA);
        bd.label(-8.5, 4.0, -0.5, "CUSTOMS OFFICE", Formatting.AQUA);
        // Lumen Trust bank
        bd.shell(10, -1, -8, 26, 7, 8, Blocks.QUARTZ_BRICKS);
        bd.fill(11, -1, -7, 25, -1, 7, Blocks.POLISHED_DIORITE);
        for (int[] c : new int[][]{{10, -8}, {26, -8}, {10, 8}, {26, 8}}) bd.fill(c[0], 0, c[1], c[0], 6, c[1], Blocks.GOLD_BLOCK);
        for (int z = -6; z <= 6; z += 3) {
            bd.set(10, 3, z, Blocks.YELLOW_STAINED_GLASS);
            bd.set(26, 3, z, Blocks.YELLOW_STAINED_GLASS);
        }
        bd.clear(10, 0, -1, 10, 2, 1);
        for (int z = -6; z <= 6; z += 4) bd.set(15, 7, z, Blocks.SEA_LANTERN);
        bd.set(23, 7, 0, Blocks.SEA_LANTERN);
        bd.fill(20, 0, -7, 20, 6, 7, Blocks.IRON_BLOCK);
        resetVaultBlocks(bd);
        bd.set(19, 0, 3, b(Kind.VAULT_TERMINAL));
        bd.label(18.5, 1.6, 3.5, "VAULT TERMINAL", Formatting.RED);
        bd.label(9.5, 4.0, 0.5, "LUMEN TRUST BANK", Formatting.YELLOW);
        bd.fill(14, 0, -6, 17, 0, -6, Blocks.SMOOTH_QUARTZ);
        // dead drop in the alley
        bd.fill(-8, 0, 12, -8, 2, 18, Blocks.QUARTZ_BLOCK);
        bd.fill(-3, 0, 12, -3, 2, 18, Blocks.QUARTZ_BLOCK);
        bd.set(-5, 0, 16, b(Kind.DEAD_DROP));
        bd.label(-4.5, 1.6, 16.5, "DEAD DROP\n(deliveries)", Formatting.GRAY);
    }

    static void resetVaultBlocks(Build bd) {
        bd.fill(20, 0, -1, 20, 2, 1, Blocks.GOLD_BLOCK);
        for (int[] c : VAULT_CRATES) bd.set(c[0], c[1], c[2], b(Kind.LOOT_CRATE));
    }

    /** A landing pad with the docking beacon at the local origin. */
    static void pad(Build bd, Block inner, Block ring) {
        bd.fill(-5, -1, -5, 5, -1, 5, ring);
        bd.fill(-4, -1, -4, 4, -1, 4, inner);
        for (int[] c : new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) bd.set(c[0], -1, c[1], Blocks.SEA_LANTERN);
        bd.set(0, 0, 0, b(Kind.DOCKING_BEACON));
        bd.label(0.5, 1.6, 0.5, "DOCKING BEACON\n(back to the ship)", Formatting.AQUA);
    }

    // ================================================================== DUSTREACH
    static void buildDustreach(Build bd) {
        for (int x = -30; x <= 30; x++) {
            for (int z = -30; z <= 30; z++) {
                int d = Math.max(Math.abs(x), Math.abs(z));
                int floorTop = -1;
                int clearFrom = d <= 26 ? 0 : (d - 26) * 3;
                for (int y = -5; y <= floorTop; y++) {
                    if (d <= 26 || y < clearFrom) {
                        BlockState st = y == floorTop
                                ? (((x * 31 + z * 17) & 7) == 0 ? Blocks.COARSE_DIRT : Blocks.RED_SAND).getDefaultState()
                                : Blocks.RED_SANDSTONE.getDefaultState();
                        bd.set(x, y, z, st);
                    }
                }
                for (int y = clearFrom; y <= 36; y++) bd.set(x, y, z, Blocks.AIR.getDefaultState());
            }
        }
        pad(bd, Blocks.TERRACOTTA, Blocks.ORANGE_TERRACOTTA);
        // cantina "The Thirsty Comet"
        bd.shell(8, -1, -7, 22, 6, 7, Blocks.SPRUCE_PLANKS);
        for (int[] c : new int[][]{{8, -7}, {22, -7}, {8, 7}, {22, 7}}) bd.fill(c[0], 0, c[1], c[0], 5, c[1], Blocks.STRIPPED_SPRUCE_LOG);
        bd.fill(8, 6, -7, 22, 6, 7, Blocks.DARK_OAK_PLANKS);
        for (int z = -5; z <= 5; z += 3) {
            bd.set(8, 2, z, Blocks.GLASS);
            bd.set(22, 2, z, Blocks.GLASS);
        }
        bd.clear(8, 0, -1, 8, 2, 1);
        for (int x = 11; x <= 20; x += 3)
            for (int z = -4; z <= 4; z += 4) bd.set(x, 6, z, Blocks.GLOWSTONE);
        bd.fill(18, 0, -5, 18, 0, 1, Blocks.STRIPPED_DARK_OAK_WOOD);
        bd.set(18, 1, -4, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, false));
        bd.set(18, 1, 0, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, false));
        bd.set(12, 0, 4, b(Kind.CARD_TABLE));
        bd.label(12.5, 1.6, 4.5, "CARD TABLE\n(Comet Cards)", Formatting.GOLD);
        bd.set(21, 0, 5, b(Kind.BLACK_MARKET));
        bd.label(20.5, 1.6, 5.5, "BLACK MARKET", Formatting.DARK_PURPLE);
        bd.set(10, 0, -6, b(Kind.JOB_BOARD));
        bd.label(10.5, 1.6, -5.5, "JOB BOARD", Formatting.GREEN);
        bd.label(7.5, 4.2, 0.5, "THE THIRSTY COMET (cantina)", Formatting.GOLD);
        bd.fill(14, 0, -3, 14, 0, -3, Blocks.SPRUCE_FENCE);
        bd.fill(14, 0, 0, 14, 0, 0, Blocks.SPRUCE_FENCE);
        // shacks
        for (int zz : new int[]{-9, 4}) {
            bd.shell(-15, -1, zz, -9, 4, zz + 5, Blocks.ACACIA_PLANKS);
            bd.fill(-15, 4, zz, -9, 4, zz + 5, Blocks.ACACIA_SLAB);
            bd.clear(-9, 0, zz + 2, -9, 1, zz + 2);
            bd.set(-12, 3, zz + 2, Blocks.GLOWSTONE);
        }
        for (int[] p : new int[][]{{-20, -20}, {20, -18}, {-22, 18}, {24, 22}, {0, -22}}) {
            bd.fill(p[0], 0, p[1], p[0], 2, p[1], Blocks.CACTUS);
            bd.set(p[0], -1, p[1], Blocks.SAND);
        }
        for (int[] p : new int[][]{{-6, -14}, {6, 14}, {-17, 0}, {3, -18}}) bd.set(p[0], 0, p[1], Blocks.DEAD_BUSH);
    }

    // ================================================================== THE GILDED MAW
    static void buildMaw(Build bd) {
        for (int x = -25; x <= 25; x++) {
            for (int z = -25; z <= 25; z++) {
                double r = Math.sqrt(x * x + z * z);
                if (r > 24.5) continue;
                Block f = r > 23.3 ? Blocks.GOLD_BLOCK : ((x + z) % 7 == 0 ? Blocks.POLISHED_BLACKSTONE_BRICKS : Blocks.POLISHED_BLACKSTONE);
                bd.set(x, -1, z, f);
                if (r > 23.3) bd.set(x, 0, z, Blocks.IRON_BARS);
                for (int y = -2; y >= -9; y--) {
                    double rr = 24 - (-y - 1) * 2.4;
                    if (r <= rr) bd.set(x, y, z, (x * 7 + z * 3 + y) % 11 == 0 ? Blocks.SEA_LANTERN : Blocks.BLACKSTONE);
                }
            }
        }
        // docking pad (south)
        bd.fill(-4, -1, 11, 4, -1, 19, Blocks.GOLD_BLOCK);
        bd.fill(-3, -1, 12, 3, -1, 18, Blocks.PURPLE_CONCRETE);
        Build padB = new Build(bd.w, bd.ox, bd.oy, bd.oz + 14);
        padB.set(0, 0, 0, b(Kind.DOCKING_BEACON));
        padB.label(0.5, 1.6, 0.5, "DOCKING BEACON\n(back to the ship)", Formatting.AQUA);
        // light posts
        for (int a = 0; a < 360; a += 45) {
            int x = (int) Math.round(Math.cos(Math.toRadians(a)) * 20), z = (int) Math.round(Math.sin(Math.toRadians(a)) * 20);
            if (z > 8 && Math.abs(x) < 6) continue;
            bd.fill(x, 0, z, x, 2, z, Blocks.POLISHED_BLACKSTONE_WALL);
            bd.set(x, 3, z, Blocks.END_ROD);
        }
        // the hall
        bd.shell(-10, -1, -15, 10, 7, 4, Blocks.POLISHED_BLACKSTONE_BRICKS);
        bd.fill(-9, -1, -14, 9, -1, 3, Blocks.POLISHED_BLACKSTONE);
        for (int z = -13; z <= 2; z += 3) {
            bd.fill(-10, 2, z, -10, 4, z, Blocks.PURPLE_STAINED_GLASS);
            bd.fill(10, 2, z, 10, 4, z, Blocks.PURPLE_STAINED_GLASS);
        }
        for (int x = -6; x <= 6; x += 4)
            for (int z = -12; z <= 2; z += 4) bd.set(x, 7, z, Blocks.GLOWSTONE);
        bd.clear(-1, 0, 4, 1, 2, 4);
        bd.fill(0, 0, -10, 0, 0, 3, Blocks.PURPLE_CARPET);
        bd.fill(-2, 0, -14, 2, 0, -11, Blocks.GOLD_BLOCK);
        bd.fill(-3, 0, -14, 3, 5, -14, Blocks.GOLD_BLOCK);
        bd.set(-9, 0, 0, b(Kind.JOB_BOARD));
        bd.label(-8.5, 1.6, 0.5, "JOB BOARD", Formatting.GREEN);
        bd.set(9, 0, 0, b(Kind.BLACK_MARKET));
        bd.label(8.5, 1.6, 0.5, "BLACK MARKET", Formatting.DARK_PURPLE);
        bd.set(7, 0, -8, b(Kind.CARD_TABLE));
        bd.label(7.5, 1.6, -7.5, "CARD TABLE\n(Comet Cards)", Formatting.GOLD);
        bd.label(0.5, 4.2, 4.5, "THE GILDED MAW", Formatting.LIGHT_PURPLE);
    }

    // ================================================================== THE BURROW
    static void buildBurrow(Build bd) {
        for (int x = -21; x <= 21; x++) {
            for (int y = -21; y <= 21; y++) {
                for (int z = -21; z <= 21; z++) {
                    double r = Math.sqrt(x * x + y * y + z * z);
                    double edge = 17.5 + 2.5 * Math.sin(x * 0.35) * Math.cos(z * 0.3) + 1.5 * Math.sin(y * 0.5 + x * 0.2);
                    if (r > edge) continue;
                    double cav = (x / 11.0) * (x / 11.0) + (y / 6.0) * (y / 6.0) + (z / 11.0) * (z / 11.0);
                    boolean tunnel = Math.abs(x) <= 2 && y >= 0 && y <= 3 && z >= 6;
                    if ((cav < 1.0 && y >= 0) || tunnel) {
                        bd.set(x, y, z, Blocks.AIR.getDefaultState());
                        continue;
                    }
                    int hsh = (x * 73856093) ^ (y * 19349663) ^ (z * 83492791);
                    int m = Math.floorMod(hsh, 40);
                    Block blk = m < 14 ? Blocks.STONE : m < 24 ? Blocks.TUFF : m < 32 ? Blocks.ANDESITE : m < 38 ? Blocks.COBBLED_DEEPSLATE
                            : m == 38 ? Blocks.AMETHYST_BLOCK : Blocks.RAW_IRON_BLOCK;
                    bd.set(x, y, z, blk);
                }
            }
        }
        bd.fill(-10, -1, -10, 10, -1, 10, Blocks.COBBLED_DEEPSLATE);
        bd.fill(-2, -1, 6, 2, -1, 20, Blocks.POLISHED_TUFF);
        bd.set(0, 0, 3, b(Kind.DOCKING_BEACON));
        bd.label(0.5, 1.6, 3.5, "DOCKING BEACON\n(back to the ship)", Formatting.AQUA);
        bd.label(0.5, 3.2, 0.5, "THE BURROW\nLie low here to cool off Heat", Formatting.GREEN);
        for (int z = -4; z <= 2; z += 3) {
            bd.fill(-8, 0, z, -7, 0, z + 1, Blocks.RED_WOOL);
        }
        bd.set(6, 0, -5, Blocks.CHEST);
        bd.set(7, 0, -5, Blocks.CHEST);
        bd.set(0, 0, -3, Blocks.CAMPFIRE);
        for (int[] p : new int[][]{{-6, 4, -4}, {6, 4, 4}, {0, 5, -6}, {-5, 4, 5}, {5, 4, -6}, {0, 4, 7}}) bd.set(p[0], p[1], p[2], Blocks.SHROOMLIGHT);
        bd.set(-3, 0, -7, Blocks.CRAFTING_TABLE);
        bd.set(-2, 0, -7, Blocks.FURNACE);
    }

    // ================================================================== DERELICT FREIGHTER
    static void buildDerelict(Build bd, CrewState s) {
        for (int x = -7; x <= 7; x++) {
            for (int y = -1; y <= 6; y++) {
                for (int z = -34; z <= 12; z++) {
                    boolean edge = x == -7 || x == 7 || y == -1 || y == 6 || z == -34 || z == 12;
                    if (!edge) { bd.set(x, y, z, Blocks.AIR.getDefaultState()); continue; }
                    int m = Math.floorMod(x * 31 + y * 7 + z * 13, 9);
                    Block blk = m < 3 ? Blocks.WEATHERED_COPPER : m < 6 ? Blocks.OXIDIZED_COPPER : m < 8 ? Blocks.EXPOSED_COPPER : Blocks.IRON_BLOCK;
                    if (y == -1) blk = m < 5 ? Blocks.OXIDIZED_CUT_COPPER : Blocks.WEATHERED_CUT_COPPER;
                    bd.set(x, y, z, blk);
                }
            }
        }
        // docking tube
        bd.shell(-3, -1, 12, 3, 4, 17, Blocks.IRON_BLOCK);
        bd.clear(-2, 0, 12, 2, 3, 12);
        bd.fill(-2, 3, 13, 2, 3, 16, Blocks.SEA_LANTERN);
        bd.clear(-2, 0, 13, 2, 2, 16);
        Build padB = new Build(bd.w, bd.ox, bd.oy, bd.oz + 10);
        padB.set(0, 0, 0, b(Kind.DOCKING_BEACON));
        padB.label(0.5, 1.6, 0.5, "DOCKING BEACON\n(back to the ship)", Formatting.AQUA);
        // lights + junk
        for (int z = 8; z >= -32; z -= 8) bd.set(0, 6, z, z > 0 ? Blocks.SEA_LANTERN : Blocks.GLOWSTONE);
        for (int[] p : new int[][]{{-6, 4, -10}, {5, 4, -15}, {-5, 4, -24}, {6, 0, -30}, {-6, 0, -5}, {4, 3, 5}}) bd.set(p[0], p[1], p[2], Blocks.COBWEB);
        for (int[] p : new int[][]{{-6, 0, -12}, {-6, 1, -12}, {6, 0, -18}, {-6, 0, -31}, {6, 0, -8}}) bd.set(p[0], p[1], p[2], Blocks.STRIPPED_SPRUCE_WOOD);
        // bulkhead
        bd.fill(-6, 0, 0, 6, 5, 0, Blocks.WAXED_COPPER_BLOCK);
        bd.set(3, 0, 1, b(Kind.BULKHEAD_PANEL));
        bd.label(3.5, 1.6, 1.5, "BULKHEAD PANEL\n(reroute power)", Formatting.GREEN);
        resetDerelictBlocks(bd);
    }

    static void resetDerelictBlocks(Build bd) {
        bd.fill(-1, 0, 0, 1, 2, 0, Blocks.IRON_BLOCK);
        for (int[] c : DERELICT_CRATES) bd.set(c[0], c[1], c[2], b(Kind.LOOT_CRATE));
    }

    // ------------------------------------------------------------------ mission helpers
    public static void resetVault(MinecraftServer server, CrewState s) {
        ServerWorld w = world(server, Loc.MERIDIAN.key);
        if (w == null) return;
        ensureBuilt(server, s, Loc.MERIDIAN);
        resetVaultBlocks(new Build(w, MERIDIAN_O.getX(), MERIDIAN_O.getY(), MERIDIAN_O.getZ()));
    }

    public static void openVault(MinecraftServer server) {
        ServerWorld w = world(server, Loc.MERIDIAN.key);
        if (w == null) return;
        new Build(w, MERIDIAN_O.getX(), MERIDIAN_O.getY(), MERIDIAN_O.getZ()).clear(20, 0, -1, 20, 2, 1);
    }

    public static void resetDerelict(MinecraftServer server, CrewState s) {
        ServerWorld w = world(server, Loc.DERELICT.key);
        if (w == null) return;
        ensureBuilt(server, s, Loc.DERELICT);
        resetDerelictBlocks(new Build(w, DERELICT_O.getX(), DERELICT_O.getY(), DERELICT_O.getZ()));
    }

    public static void openBulkhead(MinecraftServer server) {
        ServerWorld w = world(server, Loc.DERELICT.key);
        if (w == null) return;
        new Build(w, DERELICT_O.getX(), DERELICT_O.getY(), DERELICT_O.getZ()).clear(-1, 0, 0, 1, 2, 0);
    }

    public static List<int[]> vaultCrates() { return List.of(VAULT_CRATES); }
}
