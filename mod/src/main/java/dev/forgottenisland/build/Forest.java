package dev.forgottenisland.build;

import dev.forgottenisland.game.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.TreeConfiguredFeatures;

import static net.minecraft.util.math.Direction.*;

/** The Mosswood and the island's wild places. Vegetation for the whole island is planted here. */
public final class Forest implements Layout.Part {
    public String name() { return "forest"; }
    static final T W = new T(0, 0, 0, 0);
    // sites
    static final int[] WENDEL = {20, 128}, LODGE = {-62, 38}, GLADE = {-20, 62}, STONES = {-25, -79}, HOLLOW = {-146, 104}, PINE = {-124, -36}, NEST = {-92, -62}, CAIRN = {-55, -35};
    static final int STONES_Y = 77, LODGE_Y = 73, GLADE_Y = 71, WENDEL_Y = 66, HOLLOW_Y = 70;

    public void places() {
        // pads for handcrafted sites
        Terrain.pad(WENDEL[0] - 6, WENDEL[1] - 6, WENDEL[0] + 10, WENDEL[1] + 6, WENDEL_Y - 1, 6);
        Terrain.pad(LODGE[0] - 9, LODGE[1] - 8, LODGE[0] + 9, LODGE[1] + 8, LODGE_Y - 1, 8);
        Terrain.pad(GLADE[0] - 12, GLADE[1] - 10, GLADE[0] + 12, GLADE[1] + 10, GLADE_Y - 1, 8);
        Terrain.pad(STONES[0] - 11, STONES[1] - 11, STONES[0] + 11, STONES[1] + 11, STONES_Y - 1, 7);
        Terrain.pad(HOLLOW[0] - 9, HOLLOW[1] - 9, HOLLOW[0] + 9, HOLLOW[1] + 9, HOLLOW_Y - 1, 8);
        Terrain.pad(-118, 16, -98, 35, 81, 6);      // terrace above the falls
        Terrain.pad(-124, 41, -103, 57, 66, 5);     // basin below the falls
        // Wendel
        W.place("wendel_home", WENDEL[0] + 2, WENDEL_Y, WENDEL[1], 180);
        W.place("wendel_bed", WENDEL[0] + 4, WENDEL_Y, WENDEL[1] - 3, 0);
        W.place("wendel_bench", WENDEL[0] + 1, WENDEL_Y + 0.5, WENDEL[1] + 5, 0);
        W.place("wendel_shore", WENDEL[0] + 6, 64, WENDEL[1] + 12, 0);
        // Wren
        W.place("wren_lodge", LODGE[0] + 3, LODGE_Y, LODGE[1] - 1, 180);
        W.place("wren_bed", LODGE[0] - 3, LODGE_Y, LODGE[1] - 3, 0);
        W.place("wren_porch", LODGE[0] + 1, LODGE_Y + 0.5, LODGE[1] + 5, 0);
        W.place("wren_range", LODGE[0] + 7, LODGE_Y, LODGE[1] + 7, -90);
        W.place("cp_forest", LODGE[0], LODGE_Y, LODGE[1] + 7, 0);
        W.place("wren_patrol_n", -20, Terrain.stand(-20, 10), 10, 180);
        W.place("wren_patrol_s", 0, Terrain.stand(0, 92), 92, 0);
        W.place("forest_cross", 2, Terrain.stand(2, 94), 94, 0);
        // glade
        W.place("glade_center", GLADE[0], GLADE_Y, GLADE[1], 0);
        W.place("glade_date", GLADE[0] + 4, GLADE_Y + 0.5, GLADE[1] - 4, 225);
        W.place("glade_player", GLADE[0] + 2, GLADE_Y, GLADE[1] - 2, 45);
        W.zone("glade", GLADE[0] - 10, GLADE_Y - 3, GLADE[1] - 9, GLADE[0] + 10, GLADE_Y + 8, GLADE[1] + 9, null);
        // stones
        W.place("cp_stones", STONES[0], STONES_Y, STONES[1] + 9, 180);
        W.place("stones_center", STONES[0], STONES_Y, STONES[1], 180);
        W.place("wren_stones", STONES[0] + 3, STONES_Y, STONES[1] + 6, 180);
        W.zone("stones", STONES[0] - 10, STONES_Y - 3, STONES[1] - 10, STONES[0] + 10, STONES_Y + 10, STONES[1] + 10, p -> Checkpoints.set(p, "cp_stones"));
        int[][] stonePos = {{0, 6}, {-6, 0}, {0, -6}, {6, 0}};   // S, W, N, E  (clockwise from the road)
        for (int i = 0; i < 4; i++) {
            String animal = PuzzleLayouts.STONES[i];
            int sx = STONES[0] + stonePos[i][0], sz = STONES[1] + stonePos[i][1];
            int bx = sx - Integer.signum(stonePos[i][0]), bz = sz - Integer.signum(stonePos[i][1]);
            BlockPos button = new BlockPos(bx, STONES_Y + 1, bz);
            Puzzles.STONE_BUTTONS.put(button, animal);
            Puzzles.STONE_TOPS.put(animal, new BlockPos(sx, STONES_Y + 3, sz));
            Puzzles.on(button, (p, pos, held) -> Puzzles.strikeStone(p, animal));
        }
        // thornwall across the gorge
        var g = Gates.add("thornwall", SoundEvents.BLOCK_MANGROVE_ROOTS_BREAK);
        for (int x = -31; x <= -19; x++) for (int z = -111; z <= -108; z++) for (int y = 79; y <= 92; y++) {
            double h = Noise.hash(x, y, z);
            String s = h < 0.45 ? "mangrove_roots" : h < 0.7 ? "muddy_mangrove_roots" : h < 0.85 ? "dark_oak_leaves[persistent=true]" : "moss_block";
            g.closed().put(new BlockPos(x, y, z), B.s(s));
        }
        W.zone("thornwall", -31, 76, -107, -19, 95, -100, null);
        // falls grotto, hollow oak, pine, nest, cairn
        W.zone("secret_falls_cave", -113, 66, 28, -106, 72, 35, p -> Secrets.discover(p, "falls_cave"));
        W.zoneRepeat("hollow_oak", HOLLOW[0] - 7, HOLLOW_Y - 2, HOLLOW[1] - 7, HOLLOW[0] + 7, HOLLOW_Y + 8, HOLLOW[1] + 7, p -> Encounters.trigger(p, "hollow_oak"));
        var enc = Encounters.add("hollow_oak", "!enc.hollow_oak");
        enc.spawns().add(new Encounters.Spawn(EntityType.SPIDER, HOLLOW[0] + 5, HOLLOW_Y, HOLLOW[1] + 3, null));
        enc.spawns().add(new Encounters.Spawn(EntityType.SPIDER, HOLLOW[0] - 5, HOLLOW_Y, HOLLOW[1] - 2, null));
        enc.spawns().add(new Encounters.Spawn(EntityType.SPIDER, HOLLOW[0] + 1, HOLLOW_Y + 6, HOLLOW[1] - 5, null));
        enc.spawns().add(new Encounters.Spawn(EntityType.CAVE_SPIDER, HOLLOW[0], HOLLOW_Y, HOLLOW[1], null));
        enc.spawns().add(new Encounters.Spawn(EntityType.CAVE_SPIDER, HOLLOW[0] + 1, HOLLOW_Y, HOLLOW[1] + 1, null));
        W.place("hollow_oak", HOLLOW[0], HOLLOW_Y, HOLLOW[1] + 8, 180);
        W.place("fallen_pine", PINE[0], Terrain.stand(PINE[0], PINE[1] + 3), PINE[1] + 3, 180);
        int ny = Terrain.stand(NEST[0], NEST[1]) + 22;
        W.zone("secret_watcher_nest", NEST[0] - 4, ny, NEST[1] - 4, NEST[0] + 5, ny + 4, NEST[1] + 5, p -> Secrets.discover(p, "watcher_nest"));
        int cy = Terrain.stand(CAIRN[0], CAIRN[1]);
        W.zone("secret_cairn", CAIRN[0] - 3, cy - 2, CAIRN[1] - 3, CAIRN[0] + 3, cy + 6, CAIRN[1] + 3, p -> Secrets.discover(p, "cairn"));
        W.place("cairn_top", CAIRN[0] + 2, cy, CAIRN[1] + 2, 0);
        // waystone lamps (Wendel's side quest)
        int[][] ways = {{152, 176}, {-100, 52}, {-40, -14}, {-22, -134}};
        for (int[] wpos : ways) {
            int y = wpos[1] == -134 ? 92 : Terrain.stand(wpos[0], wpos[1]);
            Interactions.waystone(new BlockPos(wpos[0], y + 2, wpos[1]));
        }
        W.place("gorge_top", -25, 92, -136, 180);
        W.place("waystone_harbor", 150, Terrain.stand(150, 176), 176, 0);
    }

    // ------------------------------------------------------------------ build
    public void build(ServerWorld w) {
        B b = new B(w, W);
        trails(b);
        wendel(b);
        lodge(b);
        glade(b);
        stones(b);
        Gates.close(w.getServer(), "thornwall");
        gorge(b);
        fallsCave(b);
        hollowOak(b);
        fallenPine(b);
        cairn(b);
        for (BlockPos p : Interactions.WAYSTONES) waystone(b, p);
        vegetation(b);
        watcherNest(b);
        wildlife(b);
    }

    static void trails(B b) {
        Pal trail = Town.TRAIL;
        Town.road(b, trail, 3, new int[]{0, 95}, new int[]{-4, 70}, new int[]{-8, 45}, new int[]{-14, 20}, new int[]{-20, -10}, new int[]{-24, -40}, new int[]{-25, -70});
        Town.road(b, trail, 3, new int[]{-25, -86}, new int[]{-25, -94});
        Town.road(b, trail, 2, new int[]{-8, 45}, new int[]{-30, 42}, new int[]{-52, 40});
        Town.road(b, trail, 2, new int[]{-6, 58}, new int[]{-14, 60});
        Town.road(b, trail, 2, new int[]{-52, 40}, new int[]{-75, 44}, new int[]{-95, 50}, new int[]{-102, 56});
        Town.road(b, trail, 2, new int[]{-102, 56}, new int[]{-120, 70}, new int[]{-136, 92}, new int[]{-144, 100});
        Town.road(b, trail, 2, new int[]{-20, -10}, new int[]{-34, -16}, new int[]{-46, -28}, new int[]{-53, -33});
        Town.road(b, trail, 2, new int[]{-24, -40}, new int[]{-60, -50}, new int[]{-90, -58});
        Town.road(b, trail, 2, new int[]{-60, -50}, new int[]{-100, -42}, new int[]{-120, -38});
        Town.road(b, trail, 2, new int[]{28, 126}, new int[]{22, 130});
        Town.road(b, Pal.of("cobblestone:3", "gravel:2", "coarse_dirt:2", "mossy_cobblestone:2"), 3, new int[]{-25, -113}, new int[]{-25, -128}, new int[]{-12, -129}, new int[]{8, -127}, new int[]{16, -125});
    }

    static void wendel(B b) {
        B h = b.at(WENDEL[0], WENDEL_Y, WENDEL[1] - 5, 0);   // hut local 0..7 x 0..6, door south
        Pal wall = Pal.of("spruce_planks:3", "stripped_spruce_wood:1", "oak_planks:1");
        h.foundation(0, 0, 7, 6, -1, Pal.of("cobblestone"));
        h.clear(0, 0, 0, 7, 8, 7);
        h.fill(0, -1, 0, 7, -1, 6, Pal.of("spruce_planks"));
        h.walls(0, 0, 0, 7, 3, 6, wall);
        for (int[] c : new int[][]{{0, 0}, {7, 0}, {0, 6}, {7, 6}}) for (int y = -1; y <= 3; y++) h.set(c[0], y, c[1], "stripped_spruce_log");
        Kit.gableX(h, 0, 0, 7, 6, 4, "mossy_stone_brick_stairs", "mossy_stone_brick_slab", Pal.of("spruce_planks"), true);
        Kit.door(h, 2, 0, 6, SOUTH, "spruce_door", false);
        Kit.window(h, 5, 1, 6, 2, SOUTH, "glass_pane", "spruce_trapdoor", "potted_allium");
        Kit.window(h, 7, 1, 3, 2, EAST, "glass_pane", null, null); Kit.window(h, 0, 1, 3, 1, WEST, "glass_pane", null, null);
        Kit.chimney(h, 0, 1, 0, 7, Pal.of("cobblestone:2", "mossy_cobblestone:1"));
        Kit.bed(h, 4, 0, 2, NORTH, "light_gray"); h.set(1, 0, 1, "furnace[facing=east,lit=true]"); h.set(6, 0, 1, "chest[facing=west]");
        Kit.tableTop(h, 2, 0, 3, "spruce", "candle[candles=1,lit=true]"); h.set(1, 0, 3, "spruce_stairs[facing=west]");
        h.frame(6, 2, 3, WEST, "minecraft:fishing_rod"); h.frame(6, 2, 4, WEST, "minecraft:cod");
        h.lectern(6, 0, 5, WEST, "maud_diary".equals("") ? "lullaby" : "lullaby");
        h.set(6, 0, 5, "barrel[facing=up]"); h.set(6, 1, 5, "potted_oxeye_daisy");
        Kit.carpet(h, 3, 3, 5, 5, 0, "gray");
        // porch bench facing the sea, nets, a beached rowboat, a little garden
        h.set(1, 0, 10, "spruce_stairs[facing=north]"); h.set(2, 0, 10, "spruce_stairs[facing=north]"); h.set(0, 0, 10, "spruce_trapdoor[facing=west,open=true]");
        for (int x = 4; x <= 8; x++) h.set(x, 0, 8, x == 4 || x == 8 ? "spruce_fence" : "air");
        h.set(4, 1, 8, "spruce_fence"); h.set(8, 1, 8, "spruce_fence"); for (int x = 5; x <= 7; x++) h.set(x, 1, 8, "chain[axis=x]");
        h.set(9, 0, 2, "barrel[facing=up]"); h.set(9, 0, 3, "barrel[facing=up]"); h.set(9, 1, 3, "lantern");
        for (int x = -4; x <= -1; x++) for (int z = 1; z <= 4; z++) { h.set(x, -1, z, (x + z) % 3 == 0 ? "water" : "farmland[moisture=7]"); if ((x + z) % 3 != 0) h.set(x, 0, z, x % 2 == 0 ? "potatoes[age=7]" : "beetroots[age=3]"); }
        b.boat(WENDEL[0] + 8, 64, WENDEL[1] + 13, 30, net.minecraft.entity.vehicle.BoatEntity.Type.OAK);
    }

    static void lodge(B b) {
        B h = b.at(LODGE[0] - 5, LODGE_Y, LODGE[1] - 5, 0);  // cabin local 0..10 x 0..8, porch to z 11
        h.foundation(-1, -1, 11, 9, -1, Pal.of("cobblestone:2", "mossy_cobblestone:2"));
        h.clear(-1, 0, -1, 11, 10, 12);
        h.fill(0, -1, 0, 10, -1, 8, Pal.of("spruce_planks"));
        for (int y = 0; y <= 3; y++) for (int x = 0; x <= 10; x++) for (int z = 0; z <= 8; z++) {
            boolean edgeX = x == 0 || x == 10, edgeZ = z == 0 || z == 8;
            if (!(edgeX || edgeZ)) continue;
            if (edgeX && edgeZ) h.set(x, y, z, "spruce_log[axis=y]");
            else if (edgeZ) h.set(x, y, z, y % 2 == 0 ? "spruce_log[axis=x]" : "stripped_spruce_log[axis=x]");
            else h.set(x, y, z, y % 2 == 0 ? "spruce_log[axis=z]" : "stripped_spruce_log[axis=z]");
        }
        Kit.gableX(h, 0, 0, 10, 8, 4, "dark_oak_stairs", "dark_oak_slab", Pal.of("spruce_log[axis=x]"), true);
        Kit.door(h, 5, 0, 8, SOUTH, "spruce_door", false);
        for (int x : new int[]{2, 8}) Kit.window(h, x, 1, 8, 2, SOUTH, "glass_pane", "spruce_trapdoor", null);
        Kit.window(h, 0, 1, 4, 2, WEST, "glass_pane", "spruce_trapdoor", null); Kit.window(h, 10, 1, 4, 2, EAST, "glass_pane", "spruce_trapdoor", null);
        Kit.chimney(h, 10, 2, 0, 8, Pal.of("cobblestone:2", "mossy_cobblestone:2"));
        // porch
        h.fill(0, -1, 9, 10, -1, 11, Pal.of("spruce_planks"));
        for (int x : new int[]{0, 10}) for (int y = 0; y <= 2; y++) h.set(x, y, 11, "spruce_fence");
        for (int x = -1; x <= 11; x++) h.set(x, 3, 10, "spruce_slab[type=bottom]"); for (int x = -1; x <= 11; x++) h.set(x, 3, 11, "spruce_slab[type=bottom]");
        for (int x = -1; x <= 11; x++) h.set(x, 3, 9, "spruce_planks");
        h.set(6, 0, 10, "spruce_stairs[facing=north]"); h.set(7, 0, 10, "spruce_stairs[facing=north]");
        h.set(5, 2, 9, "lantern[hanging=true]".replace("lantern[hanging=true]", "air"));
        h.set(3, 2, 10, "lantern[hanging=true]");
        // interior: ranger's things
        Kit.bed(h, 2, 0, 2, NORTH, "green"); h.set(1, 0, 1, "chest[facing=east]"); h.set(4, 0, 1, "barrel[facing=up]"); h.set(4, 1, 1, "potted_fern");
        h.set(8, 0, 1, "fletching_table"); h.set(9, 0, 1, "barrel[facing=up]"); h.set(9, 1, 1, "barrel[facing=up]"); h.set(7, 0, 1, "crafting_table");
        Kit.tableTop(h, 7, 0, 5, "spruce", "candle[candles=2,lit=true]"); h.set(6, 0, 5, "spruce_stairs[facing=west]"); h.set(8, 0, 5, "spruce_stairs[facing=east]");
        h.frame(9, 2, 3, WEST, "minecraft:bow"); h.frame(9, 2, 4, WEST, "minecraft:arrow"); h.frame(9, 2, 5, WEST, "minecraft:feather");
        h.frame(1, 2, 4, EAST, "minecraft:rabbit_hide"); h.frame(1, 2, 5, EAST, "minecraft:leather");
        h.set(1, 0, 7, "spruce_stairs[facing=east]"); h.set(1, 0, 6, "spruce_stairs[facing=east]");
        Kit.carpet(h, 3, 3, 6, 6, 0, "brown"); Kit.hangLantern(h, 5, 3, 4, 0, false);
        // archery range: hay butts and targets
        for (int z = 13; z <= 17; z++) h.set(14, -1, z, "dirt_path".replace("dirt_path", "coarse_dirt"));
        h.set(18, 0, 13, "hay_block"); h.set(18, 1, 13, "target"); h.set(18, 0, 17, "hay_block"); h.set(18, 1, 17, "hay_block"); h.set(18, 2, 17, "target");
        // firepit with log seats
        h.set(4, -1, 15, "campfire[lit=true,signal_fire=false]".replace("campfire[lit=true,signal_fire=false]", "cobblestone"));
        h.set(4, 0, 15, "campfire[lit=true,signal_fire=false]");
        h.set(2, 0, 15, "stripped_spruce_log[axis=z]"); h.set(6, 0, 15, "stripped_spruce_log[axis=z]"); h.set(4, 0, 17, "stripped_spruce_log[axis=x]");
        // woodpile and drying rack
        for (int z = 1; z <= 5; z++) for (int y = 0; y <= 1; y++) h.set(12, y, z, "spruce_log[axis=x]");
        h.set(-2, 0, 2, "spruce_fence"); h.set(-2, 0, 6, "spruce_fence"); h.set(-2, 1, 2, "spruce_fence"); h.set(-2, 1, 6, "spruce_fence");
        for (int z = 3; z <= 5; z++) h.set(-2, 1, z, "chain[axis=z]");
    }

    static void glade(B b) {
        int cx = GLADE[0], cz = GLADE[1], y = GLADE_Y;
        // pond
        for (int x = -6; x <= 6; x++) for (int z = -5; z <= 5; z++) {
            double d = Math.hypot(x / 6.0, z / 4.5) + Terrain.N3.at((cx + x) / 4.0, (cz + z) / 4.0) * 0.15;
            if (d < 0.75) { b.wset(cx + x - 2, y - 1, cz + z + 2, B.s("water")); b.wset(cx + x - 2, y - 2, cz + z + 2, B.s(d < 0.4 ? "water" : "clay")); b.wset(cx + x - 2, y - 3, cz + z + 2, B.s("clay")); if (Noise.hash(x, z, 4) < 0.12) b.wset(cx + x - 2, y, cz + z + 2, B.s("lily_pad")); }
            else if (d < 0.9) b.wset(cx + x - 2, y - 1, cz + z + 2, B.s(Noise.hash(x, z, 5) < 0.5 ? "moss_block" : "mud"));
        }
        // flowers
        String[] fl = {"azure_bluet", "oxeye_daisy", "cornflower", "lily_of_the_valley", "white_tulip", "allium", "blue_orchid"};
        for (int x = -11; x <= 11; x++) for (int z = -9; z <= 9; z++) {
            int wx = cx + x, wz = cz + z;
            if (!b.wget(wx, y - 1, wz).isOf(Blocks.GRASS_BLOCK) || !b.wget(wx, y, wz).isAir()) continue;
            double h = Noise.hash(wx, wz, 11);
            if (h < 0.18) b.wset(wx, y, wz, B.s(fl[(int) (Noise.hash(wx, wz, 12) * fl.length)]));
            else if (h < 0.4) b.wset(wx, y, wz, B.s("short_grass"));
        }
        // a fallen log to sit on, glowing mossy stones
        for (int i = 0; i < 5; i++) b.wset(cx + 3 + i, y, cz - 4, B.s("oak_log[axis=x]"));
        b.wset(cx + 3, y + 1, cz - 4, B.s("moss_carpet")); b.wset(cx + 5, y + 1, cz - 4, B.s("brown_mushroom"));
        for (int[] p : new int[][]{{-8, 6}, {7, 5}, {-6, -7}}) { b.wset(cx + p[0], y, cz + p[1], B.s("mossy_cobblestone")); b.wset(cx + p[0], y + 1, cz + p[1], B.s("glow_lichen[up=false,down=true]".replace("[up=false,down=true]", "[down=true]"))); }
    }

    static void stones(B b) {
        int cx = STONES[0], cz = STONES[1], y = STONES_Y;
        Pal ring = Pal.of("mossy_cobblestone:3", "cobblestone:2", "moss_block:2", "coarse_dirt:1");
        for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) { double d = Math.hypot(x, z); if (d > 6.5 && d < 7.6 && Noise.hash(x, z, 3) < 0.7) b.wset(cx + x, y - 1, cz + z, ring.pick(cx + x, y, cz + z)); }
        int[][] pos = {{0, 6}, {-6, 0}, {0, -6}, {6, 0}};
        String[][] carving = {{"An owl", "watches", "from the", "dark"}, {"A heron", "stands", "in still", "water"}, {"A hare", "runs for", "home", ""}, {"A stag", "lifts its", "crowned", "head"}};
        for (int i = 0; i < 4; i++) {
            int sx = cx + pos[i][0], sz = cz + pos[i][1];
            for (int yy = 0; yy <= 3; yy++) b.wset(sx, y + yy, sz, B.s(yy == 3 ? "chiseled_stone_bricks".replace("chiseled_stone_bricks", "forgotten_island:singing_stone") : "forgotten_island:singing_stone"));
            b.wset(sx, y + 4, sz, B.s("moss_carpet"));
            int dx = -Integer.signum(pos[i][0]), dz = -Integer.signum(pos[i][1]);
            Direction face = Direction.fromVector(dx, 0, dz);
            b.wset(sx + dx, y + 1, sz + dz, B.s("stone_button[face=wall,facing=" + face.asString() + "]"));
            B sb = b.at(0, 0, 0, 0);
            sb.sign(sx + dx, y + 2, sz + dz, "stone_brick_wall".equals("") ? "" : "spruce_wall_sign[facing=" + face.asString() + "]", carving[i]);
        }
        // central altar with an inscription
        b.wset(cx, y, cz, B.s("chiseled_stone_bricks")); b.wset(cx, y + 1, cz, B.s("mossy_stone_brick_slab[type=bottom]"));
        new B(b.w, 0, 0, 0, 0).sign(cx, y, cz + 1, "spruce_wall_sign[facing=south]", "Sing us awake", "in the order", "of the old song", "");
        // standing remains of an old arch at the gorge mouth
        for (int yy = 0; yy <= 5; yy++) { b.wset(cx - 5, y + yy, cz - 12, B.s("mossy_stone_bricks")); b.wset(cx + 5, y + yy, cz - 12, B.s(yy < 4 ? "mossy_stone_bricks" : "air")); }
        for (int x = -5; x <= 1; x++) b.wset(cx + x, y + 6, cz - 12, B.s("mossy_stone_brick_slab[type=bottom]"));
    }

    static void gorge(B b) {
        // rough stone on the gorge walls, hanging vines, a few fallen rocks
        for (int z = -131; z <= -113; z++) for (int x = -36; x <= -14; x++) {
            if (Math.abs(x + 25) <= 4) continue;
            for (int y = 76; y <= 96; y++) {
                BlockPos p = new BlockPos(x, y, z);
                if (!b.w.getBlockState(p).isSolid()) continue;
                int dir = x < -25 ? 1 : -1;
                if (b.w.getBlockState(p.east(dir)).isAir() && Noise.hash(x, y, z) < 0.18) b.wset(x + dir, y, z, B.s("vine[" + (dir > 0 ? "west" : "east") + "=true]"));
            }
        }
        for (int[] r : new int[][]{{-28, -118}, {-22, -124}, {-27, -126}}) { int y = Terrain.stand(r[0], r[1]); b.wset(r[0], y, r[1], B.s("mossy_cobblestone")); }
    }

    static void fallsCave(B b) {
        // grotto behind the waterfall: entrance from the basin, tucked under the terrace
        Pal rock = Pal.of("stone:3", "mossy_cobblestone:2", "tuff:1");
        for (int x = -113; x <= -106; x++) for (int z = 28; z <= 41; z++) for (int y = 66; y <= 72; y++) {
            boolean inner = x > -113 && x < -106 && z > 28 && y > 66 && y < 72;
            boolean tunnel = z >= 35 && x >= -111 && x <= -109 && y >= 67 && y <= 69;
            if (inner && (z <= 35 || tunnel)) b.wset(x, y, z, B.s("air"));
        }
        for (int x = -112; x <= -107; x++) for (int z = 29; z <= 34; z++) b.wset(x, 66, z, rock.pick(x, 66, z));
        new B(b.w, 0, 0, 0, 0).container(-108, 67, 30, "chest[facing=west]", "fi:ranger_bow", "minecraft:arrow*16", "fi:emeralds");
        b.wset(-112, 67, 30, B.s("glow_lichen[west=true]".replace("west", "west"))); b.wset(-110, 71, 31, B.s("glow_lichen[up=true]"));
        b.wset(-111, 67, 33, B.s("candle[candles=3,lit=true]")); b.wset(-107, 67, 33, B.s("moss_carpet"));
        b.wset(-109, 71, 29, B.s("pointed_dripstone[vertical_direction=down,thickness=tip]"));
    }

    static void hollowOak(B b) {
        int cx = HOLLOW[0], cz = HOLLOW[1], y = HOLLOW_Y;
        // huge hollow trunk radius ~3.5 with a split opening facing south
        for (int yy = -1; yy <= 22; yy++) for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
            double r = 3.6 - Math.max(0, yy - 12) * 0.12 + Terrain.N3.at((cx + x) / 2.0, (cz + z + yy) / 3.0) * 0.4;
            double d = Math.hypot(x, z);
            boolean shell = d <= r && d > r - 1.5;
            boolean opening = z > 1 && Math.abs(x) <= 1 && yy <= 3;
            if (yy >= 0 && d <= r - 1.5 && yy <= 10) b.wset(cx + x, y + yy, cz + z, B.s("air"));
            else if (shell && !opening) b.wset(cx + x, y + yy, cz + z, B.s(yy > 12 ? "dark_oak_wood" : Noise.hash(x, yy, z) < 0.15 ? "mossy_cobblestone".replace("mossy_cobblestone", "dark_oak_log") : "dark_oak_wood"));
            else if (opening && d <= r) b.wset(cx + x, y + yy, cz + z, B.s("air"));
        }
        // roots
        for (int a = 0; a < 8; a++) {
            double ang = a * Math.PI / 4 + 0.3;
            for (int i = 3; i <= 8; i++) {
                int x = cx + (int) Math.round(Math.cos(ang) * i), z = cz + (int) Math.round(Math.sin(ang) * i);
                int gy = Kit.topSolid(b, x, z);
                if (i < 7) b.wset(x, gy + 1, z, B.s("dark_oak_wood")); else b.wset(x, gy, z, B.s("rooted_dirt"));
            }
        }
        // branches + canopy
        for (int a = 0; a < 6; a++) {
            double ang = a * Math.PI / 3;
            for (int i = 2; i <= 9; i++) b.wset(cx + (int) Math.round(Math.cos(ang) * i), y + 18 + i / 3, cz + (int) Math.round(Math.sin(ang) * i), B.s("dark_oak_wood"));
        }
        for (int x = -14; x <= 14; x++) for (int z = -14; z <= 14; z++) for (int yy = 16; yy <= 28; yy++) {
            double d = Math.sqrt(x * x + z * z + (yy - 21) * (yy - 21) * 2.2);
            if (d < 12.5 + Terrain.N2.at((cx + x) / 4.0, (cz + z + yy) / 4.0) * 2.5 && b.wget(cx + x, y + yy, cz + z).isAir())
                b.wset(cx + x, y + yy, cz + z, B.s("dark_oak_leaves[persistent=true]"));
        }
        // inside: cobwebs, mushrooms, the old chest
        for (int[] p : new int[][]{{-1, 3, 1}, {1, 6, -1}, {0, 8, 0}, {-2, 1, -1}}) b.wset(cx + p[0], y + p[1], cz + p[2], B.s("cobweb"));
        b.wset(cx + 1, y, cz - 1, B.s("red_mushroom")); b.wset(cx - 1, y, cz + 1, B.s("brown_mushroom"));
        new B(b.w, 0, 0, 0, 0).container(cx, y, cz - 2, "chest[facing=south]", "fi:mother_bow", "minecraft:arrow*12", "minecraft:string*4");
        for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) if (Noise.hash(cx + x, cz + z, 8) < 0.06) {
            int gy = Kit.topSolid(b, cx + x, cz + z);
            if (b.wget(cx + x, gy + 1, cz + z).isAir() && Math.hypot(x, z) > 5) b.wset(cx + x, gy + 1, cz + z, B.s("cobweb"));
        }
    }

    static void fallenPine(B b) {
        int cx = PINE[0], cz = PINE[1];
        for (int i = -12; i <= 13; i++) {
            int x = cx + i, z = cz + i / 5;
            int gy = Kit.topSolid(b, x, z) + 1;
            for (int dz = 0; dz <= 1; dz++) for (int dy = 0; dy <= 1; dy++) b.wset(x, gy + dy, z + dz, B.s(i == 13 ? "stripped_spruce_wood" : "spruce_log[axis=x]"));
            if (Noise.hash(x, z, 2) < 0.3) b.wset(x, gy + 2, z, B.s("moss_carpet"));
        }
        int gy = Kit.topSolid(b, cx + 15, cz + 3) + 1;
        new B(b.w, 0, 0, 0, 0).container(cx + 15, gy, cz + 3, "barrel[facing=up]", "fi:mast_timber");
        b.wset(cx + 16, gy, cz + 3, B.s("spruce_log[axis=x]")); b.wset(cx + 16, gy + 1, cz + 3, B.s("iron_axe".equals("") ? "air" : "air"));
        new B(b.w, 0, 0, 0, 0).sign(cx + 15, gy, cz + 4, "spruce_wall_sign[facing=south]", "Heartwood cut", "for Reyes.", "Take it to her", "  - W.A.");
        // the stump
        for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++) { int y = Kit.topSolid(b, cx - 14 + dx, cz - 3 + dz) + 1; b.wset(cx - 14 + dx, y, cz - 3 + dz, B.s("spruce_log")); b.wset(cx - 14 + dx, y + 1, cz - 3 + dz, B.s("spruce_slab[type=bottom]")); }
    }

    static void cairn(B b) {
        int cx = CAIRN[0], cz = CAIRN[1];
        int y = Kit.topSolid(b, cx, cz) + 1;
        Pal st = Pal.of("cobblestone:3", "mossy_cobblestone:3", "andesite:2", "stone:1");
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            int h = 3 - (int) Math.round(Math.hypot(x, z));
            for (int yy = 0; yy < h; yy++) b.wset(cx + x, y + yy, cz + z, st.pick(cx + x, y + yy, cz + z));
        }
        b.wset(cx, y + 3, cz, B.s("cobblestone_wall"));
        new B(b.w, 0, 0, 0, 0).container(cx + 2, y, cz + 2, "barrel[facing=up]", "fi:cartographer_compass", "fi:emeralds", "minecraft:spyglass".replace("minecraft:spyglass", "minecraft:paper"));
        new B(b.w, 0, 0, 0, 0).sign(cx + 3, y, cz + 2, "spruce_sign[rotation=4]", "From here you", "can see the", "whole island.", "Remember it.");
    }

    static void waystone(B b, BlockPos lamp) {
        int x = lamp.getX(), y = lamp.getY() - 2, z = lamp.getZ();
        b.wset(x, y, z, B.s("stone_bricks")); b.wset(x, y + 1, z, B.s("mossy_stone_bricks")); b.wset(x, y + 2, z, B.s("campfire[lit=false,signal_fire=false]"));
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) b.wset(x + d[0], y, z + d[1], B.s("mossy_stone_brick_slab[type=bottom]"));
        b.wset(x, y + 3, z, B.s("air"));
        new B(b.w, 0, 0, 0, 0).sign(x + 1, y + 1, z, "spruce_wall_sign[facing=east]", "WAYSTONE", "", "light me", "");
    }

    static void watcherNest(B b) {
        int cx = NEST[0], cz = NEST[1];
        int y = Kit.topSolid(b, cx, cz) + 1;
        for (int yy = 0; yy <= 26; yy++) for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++) b.wset(cx + dx, y + yy, cz + dz, B.s("spruce_log"));
        for (int yy = 0; yy <= 21; yy++) b.wset(cx - 1, y + yy, cz, B.s("vine[east=true]"));
        int py = y + 22;
        for (int x = -3; x <= 4; x++) for (int z = -3; z <= 4; z++) if (Math.hypot(x - 0.5, z - 0.5) < 4.2 && !(x >= 0 && x <= 1 && z >= 0 && z <= 1)) b.wset(cx + x, py, cz + z, B.s("spruce_planks"));
        b.wset(cx - 1, py, cz, B.s("air"));
        for (int x = -3; x <= 4; x++) for (int z = -3; z <= 4; z++) { double d = Math.hypot(x - 0.5, z - 0.5); if (d >= 3.4 && d < 4.2) b.wset(cx + x, py + 1, cz + z, B.s("spruce_fence")); }
        b.wset(cx - 1, py + 1, cz, B.s("air"));
        for (int x = -6; x <= 7; x++) for (int z = -6; z <= 7; z++) for (int yy = 24; yy <= 34; yy++) {
            double d = Math.hypot(x - 0.5, z - 0.5);
            double r = (34 - yy) * 0.6 + 1;
            if (d < r && b.wget(cx + x, y + yy, cz + z).isAir()) b.wset(cx + x, y + yy, cz + z, B.s("spruce_leaves[persistent=true]"));
        }
        new B(b.w, 0, 0, 0, 0).container(cx + 3, py + 1, cz + 1, "chest[facing=west]", "fi:moss_crown", "minecraft:arrow*16", "fi:golden_apple");
        b.wset(cx + 2, py + 1, cz + 3, B.s("lantern"));
        new B(b.w, 0, 0, 0, 0).sign(cx + 3, py + 1, cz + 2, "spruce_sign[rotation=12]", "Watcher's Nest", "Rowan & Wren", "keep the watch", "");
    }

    // ------------------------------------------------------------------ vegetation
    static RegistryKey<ConfiguredFeature<?, ?>> pickTree(Terrain.Region r, int x, int z, double h) {
        if (r == Terrain.Region.DEEP_FOREST) return h < 0.45 ? TreeConfiguredFeatures.DARK_OAK : h < 0.65 ? TreeConfiguredFeatures.MEGA_SPRUCE : h < 0.85 ? TreeConfiguredFeatures.FANCY_OAK : TreeConfiguredFeatures.SPRUCE;
        if (x < -100 && z < 0) return h < 0.35 ? TreeConfiguredFeatures.MEGA_SPRUCE : h < 0.8 ? TreeConfiguredFeatures.SPRUCE : TreeConfiguredFeatures.PINE;
        if (r == Terrain.Region.PLATEAU) return h < 0.5 ? TreeConfiguredFeatures.SPRUCE : TreeConfiguredFeatures.OAK;
        if (r == Terrain.Region.VILLAGE) return h < 0.45 ? TreeConfiguredFeatures.FANCY_OAK : h < 0.8 ? TreeConfiguredFeatures.OAK : TreeConfiguredFeatures.BIRCH;
        return h < 0.3 ? TreeConfiguredFeatures.OAK : h < 0.5 ? TreeConfiguredFeatures.FANCY_OAK : h < 0.68 ? TreeConfiguredFeatures.BIRCH : h < 0.84 ? TreeConfiguredFeatures.SPRUCE : h < 0.92 ? TreeConfiguredFeatures.DARK_OAK : TreeConfiguredFeatures.OAK_BEES_002;
    }

    static boolean natural(BlockState s) {
        return s.isAir() || s.isReplaceable() || s.isIn(net.minecraft.registry.tag.BlockTags.LEAVES) || s.isIn(net.minecraft.registry.tag.BlockTags.LOGS) || s.isIn(net.minecraft.registry.tag.BlockTags.FLOWERS);
    }

    /** Places trees, undergrowth, boulders and fallen logs across the island, keeping clear of built things. */
    static void vegetation(B b) {
        ServerWorld w = b.w;
        var reg = w.getRegistryManager().get(RegistryKeys.CONFIGURED_FEATURE);
        var gen = w.getChunkManager().getChunkGenerator();
        Random rnd = Random.create(42);
        int min = TerrainBuilder.MIN, max = TerrainBuilder.MAX;
        int trees = 0;
        for (int x = min; x <= max; x++) for (int z = min; z <= max; z++) {
            Terrain.Column c = Terrain.column(x, z);
            Terrain.Region r = c.region;
            if (r == Terrain.Region.SEA || r == Terrain.Region.BEACH || r == Terrain.Region.CLIFF || r == Terrain.Region.GULL_ROCK) continue;
            int top = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockState g = w.getBlockState(new BlockPos(x, top, z));
            boolean soil = g.isOf(Blocks.GRASS_BLOCK) || g.isOf(Blocks.PODZOL) || g.isOf(Blocks.MOSS_BLOCK) || g.isOf(Blocks.COARSE_DIRT);
            if (!soil || top != c.top) continue;            // skip anything built on, or paths
            if (!w.getBlockState(new BlockPos(x, top + 1, z)).isAir()) continue;
            double n = Terrain.N2.fbm(x / 60.0, z / 60.0, 3);
            double h = Noise.hash(x, z, 77);
            double density = switch (r) {
                case DEEP_FOREST -> 0.07; case FOREST -> 0.035 + 0.035 * Math.max(0, n + 0.2); case VILLAGE -> 0.004; case PLATEAU -> 0.002; default -> 0;
            };
            double ridgeDist = z - Terrain.ridgeZ(x);
            if (r != Terrain.Region.PLATEAU && ridgeDist < 13 && ridgeDist > -2) density = 0;       // never let trees bridge the cliff
            if (Math.abs(x + 25) < 14 && z < -85 && z > -135) density = 0;                         // keep the gorge clear
            if (nearSite(x, z)) density *= 0.0;
            if (h < density && clearAround(w, x, top, z)) {
                var key = pickTree(r, x, z, Noise.hash(x, z, 78));
                var feat = reg.getEntry(key);
                if (feat.isPresent() && feat.get().value().generate(w, gen, rnd, new BlockPos(x, top + 1, z))) { trees++; continue; }
            }
            // undergrowth
            double u = Noise.hash(x, z, 91);
            if (r == Terrain.Region.FOREST || r == Terrain.Region.DEEP_FOREST) {
                double fern = r == Terrain.Region.DEEP_FOREST ? 0.32 : 0.22;
                if (u < 0.006 && !nearSite(x, z)) boulder(b, x, top + 1, z);
                else if (u < 0.009 && !nearSite(x, z)) fallenLog(b, x, top + 1, z);
                else if (u < fern * 0.35) plant(b, x, top + 1, z, "fern");
                else if (u < fern * 0.45) tall(b, x, top + 1, z, "large_fern");
                else if (u < fern * 0.8) plant(b, x, top + 1, z, "short_grass");
                else if (u < fern * 0.86) tall(b, x, top + 1, z, "tall_grass");
                else if (u < fern * 0.9) plant(b, x, top + 1, z, r == Terrain.Region.DEEP_FOREST ? "brown_mushroom" : "lily_of_the_valley");
                else if (u < fern * 0.95) plant(b, x, top + 1, z, g.isOf(Blocks.MOSS_BLOCK) || g.isOf(Blocks.PODZOL) ? "moss_carpet" : "dandelion");
                else if (u < fern && r == Terrain.Region.FOREST && n > 0.1) plant(b, x, top + 1, z, "azalea");
            } else if (r == Terrain.Region.VILLAGE) {
                if (u < 0.25) plant(b, x, top + 1, z, "short_grass");
                else if (u < 0.28) plant(b, x, top + 1, z, Noise.hash(x, z, 92) < 0.5 ? "dandelion" : "poppy");
                else if (u < 0.30) tall(b, x, top + 1, z, "tall_grass");
            } else if (r == Terrain.Region.PLATEAU) {
                if (u < 0.30) plant(b, x, top + 1, z, "short_grass");
                else if (u < 0.36) tall(b, x, top + 1, z, "tall_grass");
                else if (u < 0.37) plant(b, x, top + 1, z, "cornflower");
            }
        }
        dev.forgottenisland.ForgottenIsland.LOG.info("[build] planted {} trees", trees);
    }

    static boolean nearSite(int x, int z) {
        int[][] sites = {WENDEL, LODGE, GLADE, STONES, HOLLOW, CAIRN, NEST};
        int[] rad = {12, 14, 11, 12, 14, 5, 6};
        for (int i = 0; i < sites.length; i++) if (Math.hypot(x - sites[i][0], z - sites[i][1]) < rad[i]) return true;
        if (x > -118 && x < -98 && z > 16 && z < 58) return true;      // falls
        if (Math.abs(x - PINE[0]) < 18 && Math.abs(z - PINE[1]) < 6) return true;
        return false;
    }

    static boolean clearAround(ServerWorld w, int x, int y, int z) {
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = 0; dy <= 4; dy++) {
            BlockState s = w.getBlockState(new BlockPos(x + dx, y + dy, z + dz));
            if (dy == 0) { if (s.isOf(Blocks.DIRT_PATH) || s.isOf(Blocks.COBBLESTONE) || s.isOf(Blocks.GRAVEL) || s.isOf(Blocks.WATER)) return false; continue; }
            if (!natural(s)) return false;
        }
        return true;
    }

    static void plant(B b, int x, int y, int z, String s) { if (b.wget(x, y, z).isAir()) b.wset(x, y, z, B.s(s)); }
    static void tall(B b, int x, int y, int z, String s) {
        if (b.wget(x, y, z).isAir() && b.wget(x, y + 1, z).isAir()) { b.wset(x, y, z, B.s(s + "[half=lower]")); b.wset(x, y + 1, z, B.s(s + "[half=upper]")); }
    }
    static void boulder(B b, int x, int y, int z) {
        Pal p = Pal.of("mossy_cobblestone:3", "andesite:2", "stone:2", "cobblestone:1", "tuff:1");
        int r = 1 + (int) (Noise.hash(x, z, 4) * 2);
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) for (int dy = -1; dy <= r; dy++)
            if (dx * dx + dz * dz + dy * dy * 1.6 <= r * r + 0.5 && Noise.hash(x + dx, z + dz, dy) < 0.85) b.wset(x + dx, y + dy, z + dz, p.pick(x + dx, y + dy, z + dz));
        if (Noise.hash(x, z, 6) < 0.5) b.wset(x, y + r + 1, z, B.s("moss_carpet"));
    }
    static void fallenLog(B b, int x, int y, int z) {
        boolean ax = Noise.hash(x, z, 9) < 0.5;
        String log = Noise.hash(x, z, 10) < 0.5 ? "oak_log" : "birch_log";
        for (int i = 0; i < 4 + (int) (Noise.hash(x, z, 11) * 3); i++) {
            int lx = x + (ax ? i : 0), lz = z + (ax ? 0 : i);
            if (!b.wget(lx, y, lz).isAir() || b.wget(lx, y - 1, lz).isAir()) break;
            b.wset(lx, y, lz, B.s(log + "[axis=" + (ax ? "x" : "z") + "]"));
            if (Noise.hash(lx, lz, 12) < 0.35) b.wset(lx, y + 1, lz, B.s(Noise.hash(lx, lz, 13) < 0.5 ? "moss_carpet" : "brown_mushroom"));
        }
    }

    static void wildlife(B b) {
        int[][] rabbits = {{-30, 70}, {-80, 20}, {10, 40}, {-120, 80}, {-60, -10}};
        for (int[] r : rabbits) b.mob(EntityType.RABBIT, r[0], Terrain.stand(r[0], r[1]), r[1]);
        int[][] foxes = {{-110, -20}, {-150, 60}};
        for (int[] r : foxes) b.mob(EntityType.FOX, r[0], Terrain.stand(r[0], r[1]), r[1]);
    }
}
