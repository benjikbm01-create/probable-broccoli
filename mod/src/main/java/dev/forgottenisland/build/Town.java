package dev.forgottenisland.build;

import dev.forgottenisland.game.Secrets;
import dev.forgottenisland.game.Story;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;

import static net.minecraft.util.math.Direction.*;

/** Gullhaven above the harbour: the market square and the buildings around it. */
public final class Town implements Layout.Part {
    public String name() { return "town"; }
    static final T W = new T(0, 0, 0, 0);
    static final T ARCH = new T(62, 65, 120, 3);   // archive: local x 0..20 (world z 120..100), z 0..14 (world x 62..76)
    static final T SMITH = new T(134, 65, 104, 1); // smithy: local x 0..14 (world z 104..118), z 0..12 (world x 134..122)
    static final T CORIN = new T(124, 65, 91, 0);  // cottage 0..9 x 0..8
    static final T STORE = new T(70, 65, 134, 3);  // store: local x 0..10 (world z 134..124), z 0..11 (world x 70..81)
    static final T CHAPEL = new T(114, 68, 64, 0); // chapel 0..14 x 0..20 (+ tower, graveyard)
    static final T ABAND = new T(57, 68, 77, 0);   // abandoned cottage 0..9 x 0..8
    static final T MILL = new T(149, 71, 73, 0);   // windmill base 0..6 x 0..6

    public void places() {
        Terrain.pad(82, 102, 121, 127, 64, 4);
        ARCH.pad(-2, -2, 22, 17, 0, 4);
        SMITH.pad(-2, -2, 16, 15, 0, 4);
        CORIN.pad(-2, -2, 11, 11, 0, 4);
        STORE.pad(-2, -2, 12, 14, 0, 3);
        CHAPEL.pad(-6, -4, 26, 26, 0, 6);
        ABAND.pad(-3, -3, 12, 12, 0, 5);
        MILL.pad(-6, -6, 12, 12, 0, 6);
        // square
        W.place("square_well", 104, 65, 113, 90);
        W.place("square_center", 101, 65, 116, 0);
        W.place("cp_square", 101, 65, 120, 180);
        W.place("stall_fish", 89, 65, 109, 0);
        W.place("stall_cloth", 113, 65, 109, 0);
        W.place("stall_veg", 89, 65, 123, 180);
        W.place("stall_odds", 113, 65, 123, 180);
        W.place("notice_board", 101, 65, 105, 180);
        W.place("square_bench", 95, 65.5, 126, 180);
        W.place("square_bench2", 107, 65.5, 126, 180);
        W.place("festival_hester", 99, 65, 117, 90);
        W.place("festival_ione", 97, 65, 114, -45);
        W.place("festival_wren", 96, 65, 117, -90);
        W.place("festival_sol", 98, 65, 120, 180);
        W.place("festival_tobias", 104, 65, 119, 135);
        W.place("festival_corin", 106, 65, 116, 90);
        W.place("festival_wendel", 103, 65, 121, 180);
        W.place("west_gate", 46, 65, 98, 90);
        W.zone("square", 82, 62, 102, 121, 75, 127, null);
        // archive
        ARCH.place("ione_desk", 16, 0, 9, 180);
        ARCH.place("ione_shelves", 8, 0, 7, 0);
        ARCH.place("ione_bed", 19, 6, 2, 0);
        ARCH.place("ione_upstairs", 10, 6, 7, 0);
        ARCH.place("archive_roof", 11, 13, 7, 180);
        ARCH.place("archive_door", 10, 0, 16, 0);
        ARCH.zone("archive_roof", 1, 12, 1, 19, 16, 13, null);
        // smithy
        SMITH.place("corin_anvil", 7, 0, 7, 180);
        SMITH.place("corin_forge", 5, 0, 4, 180);
        SMITH.place("smithy_front", 7, 0, 14, 0);
        CORIN.place("corin_bed", 7, 0, 1, 0);
        CORIN.place("corin_home", 4, 0, 4, 0);
        CORIN.place("corin_garden", 4, 0, 11, 0);
        // store
        STORE.place("store_front", 5, 0, 13, 0);
        // chapel
        CHAPEL.place("chapel_pew", 5, 0.5, 9, 180);
        CHAPEL.place("chapel_altar", 7, 0, 4, 180);
        CHAPEL.place("graveyard", 19, 0, 12, 90);
        CHAPEL.place("maud_grave", 21, 0, 8, 180);
        CHAPEL.place("chapel_door", 7, 0, 22, 0);
        CHAPEL.zone("secret_belltower", 0, 19, 16, 4, 23, 20, p -> Secrets.discover(p, "belltower"));
        // abandoned cottage
        ABAND.place("abandoned_door", 4, 0, 10, 0);
        // windmill
        MILL.place("windmill_door", 3, 0, 8, 0);
        MILL.zone("secret_windmill", 0, 11, 0, 6, 15, 6, p -> Secrets.discover(p, "windmill"));
        // festival lights (built unlit, lit when the festival begins)
        int[][] candles = {{99, 66, 112}, {103, 66, 112}, {97, 66, 119}, {105, 66, 119}};
        for (int[] c : candles) festival(new BlockPos(c[0], c[1], c[2]), "candle[candles=4,lit=false]", "candle[candles=4,lit=true]");
        int[][] braziers = {{86, 66, 104}, {116, 66, 104}, {86, 66, 125}, {116, 66, 125}, {101, 66, 103}};
        for (int[] c : braziers) festival(new BlockPos(c[0], c[1], c[2]), "campfire[lit=false]", "campfire[lit=true,signal_fire=false]");
    }

    static void festival(BlockPos p, String off, String on) {
        Story.FESTIVAL_OFF.put(p, B.s(off));
        Story.FESTIVAL_ON.put(p, B.s(on));
    }

    public void build(ServerWorld w) {
        B b = new B(w, W);
        streets(b);
        square(b);
        archive(new B(w, ARCH));
        smithy(new B(w, SMITH));
        corinCottage(new B(w, CORIN));
        store(new B(w, STORE));
        chapel(new B(w, CHAPEL));
        abandoned(new B(w, ABAND));
        windmill(new B(w, MILL));
        animals(b);
        Story.FESTIVAL_OFF.forEach((p, s) -> w.setBlockState(p, s, B.FLAGS));
    }

    // ------------------------------------------------------------ streets
    static final Pal COBBLE = Pal.of("cobblestone:4", "stone:2", "andesite:2", "gravel:1", "mossy_cobblestone:1", "stone_bricks:1");
    static final Pal TRAIL = Pal.of("dirt_path:6", "coarse_dirt:2", "gravel:1", "rooted_dirt:1");

    public static void road(B b, Pal p, int width, int[]... pts) {
        for (int i = 0; i + 1 < pts.length; i++) {
            int[] a = pts[i], c = pts[i + 1];
            int steps = Math.max(Math.abs(c[0] - a[0]), Math.abs(c[1] - a[1]));
            for (int s = 0; s <= steps; s++) {
                double t = steps == 0 ? 0 : s / (double) steps;
                int x = (int) Math.round(a[0] + (c[0] - a[0]) * t), z = (int) Math.round(a[1] + (c[1] - a[1]) * t);
                for (int dx = -width / 2; dx <= width / 2; dx++) for (int dz = -width / 2; dz <= width / 2; dz++) {
                    int wx = x + dx, wz = z + dz;
                    if (Noise.hash(wx, wz, 5) < 0.12 && (Math.abs(dx) == width / 2 || Math.abs(dz) == width / 2)) continue;   // ragged edges
                    int top = Kit.topSolid(b, wx, wz);
                    var st = b.wget(wx, top, wz);
                    if (!(st.isOf(net.minecraft.block.Blocks.GRASS_BLOCK) || st.isOf(net.minecraft.block.Blocks.DIRT) || st.isOf(net.minecraft.block.Blocks.COARSE_DIRT)
                            || st.isOf(net.minecraft.block.Blocks.PODZOL) || st.isOf(net.minecraft.block.Blocks.MOSS_BLOCK) || st.isOf(net.minecraft.block.Blocks.SAND) || st.isOf(net.minecraft.block.Blocks.GRAVEL))) continue;
                    var ps = p.pick(wx, top, wz);
                    if (ps.isOf(net.minecraft.block.Blocks.DIRT_PATH) && !b.wget(wx, top + 1, wz).isAir()) ps = B.s("coarse_dirt");
                    b.wset(wx, top, wz, ps);
                }
            }
        }
    }

    static void streets(B b) {
        road(b, COBBLE, 3, new int[]{101, 127}, new int[]{101, 142});
        road(b, COBBLE, 3, new int[]{115, 127}, new int[]{116, 142});
        road(b, COBBLE, 3, new int[]{84, 116}, new int[]{78, 112}, new int[]{70, 98}, new int[]{58, 96}, new int[]{46, 98});
        road(b, COBBLE, 3, new int[]{112, 102}, new int[]{112, 92}, new int[]{121, 88});
        road(b, COBBLE, 3, new int[]{121, 88}, new int[]{136, 84}, new int[]{148, 80});
        road(b, COBBLE, 3, new int[]{120, 113}, new int[]{122, 113});
        road(b, COBBLE, 2, new int[]{112, 96}, new int[]{128, 100});
        road(b, COBBLE, 2, new int[]{70, 98}, new int[]{62, 90});
        road(b, COBBLE, 2, new int[]{82, 128}, new int[]{82, 136});
        road(b, TRAIL, 3, new int[]{46, 98}, new int[]{30, 100}, new int[]{12, 97}, new int[]{0, 95});
        road(b, TRAIL, 3, new int[]{40, 99}, new int[]{34, 116}, new int[]{28, 126});
        // signpost at the west edge of the village
        b.wset(45, 65, 96, B.s("spruce_fence")); b.wset(45, 66, 96, B.s("spruce_fence"));
        b.sign(45, 67, 96, "spruce_sign[rotation=4]".replace("spruce_sign", "spruce_sign"), "<- Mosswood", "Old Road", "", "Gullhaven ->");
    }

    // ------------------------------------------------------------ market square
    static void square(B b) {
        Pal plaza = Pal.of("stone_bricks:4", "polished_andesite:3", "andesite:2", "cobblestone:2", "mossy_stone_bricks:1", "cracked_stone_bricks:1");
        for (int x = 84; x <= 119; x++) for (int z = 103; z <= 126; z++) {
            double d = Math.hypot((x - 101.5) / 18.0, (z - 114.5) / 12.5);
            if (d < 1.0 || Noise.hash(x, z, 33) < 0.25 * (1.15 - d)) b.wset(x, 64, z, plaza.pick(x, 64, z));
        }
        // the well
        B w = b.at(99, 65, 111, 0);
        for (int x = 0; x <= 4; x++) for (int z = 0; z <= 4; z++) {
            boolean edge = x == 0 || x == 4 || z == 0 || z == 4;
            w.set(x, -1, z, "stone_bricks");
            if (edge) w.set(x, 0, z, (x + z) % 2 == 0 ? "stone_brick_wall" : "stone_bricks");
            else { w.set(x, 0, z, "water"); for (int y = -8; y <= -1; y++) w.set(x, y, z, "water"); w.set(x, -9, z, "gravel"); }
        }
        for (int[] c : new int[][]{{0, 0}, {4, 0}, {0, 4}, {4, 4}}) { w.set(c[0], 1, c[1], "spruce_fence"); w.set(c[0], 2, c[1], "spruce_fence"); }
        w.set(0, 0, 0, "stone_bricks"); w.set(4, 0, 0, "stone_bricks"); w.set(0, 0, 4, "stone_bricks"); w.set(4, 0, 4, "stone_bricks");
        for (int x = -1; x <= 5; x++) { w.set(x, 3, -1, "spruce_stairs[facing=south]"); w.set(x, 3, 5, "spruce_stairs[facing=north]"); w.set(x, 4, 0, "spruce_stairs[facing=south]"); w.set(x, 4, 4, "spruce_stairs[facing=north]"); }
        for (int x = -1; x <= 5; x++) for (int z = 1; z <= 3; z++) w.set(x, 4, z, z == 2 ? "spruce_slab[type=bottom]" : "spruce_planks");
        for (int x = -1; x <= 5; x++) w.set(x, 5, 2, "spruce_slab[type=bottom]");
        for (int z = 0; z <= 4; z++) { w.set(0, 3, z, "spruce_planks"); w.set(4, 3, z, "spruce_planks"); }
        w.set(2, 3, 2, "chain[axis=y]"); w.set(2, 2, 2, "chain[axis=y]"); w.set(2, 1, 2, "lantern[hanging=true]");
        // market stalls (striped awnings)
        stall(b.at(87, 65, 107, 0), "red", "white", "barrel[facing=up]", "minecraft:cod", "minecraft:salmon");
        stall(b.at(111, 65, 107, 0), "blue", "white", "white_wool", "minecraft:string", "minecraft:white_dye");
        stall(b.at(87, 65, 121, 0), "green", "yellow", "melon", "minecraft:carrot", "minecraft:potato");
        stall(b.at(111, 65, 121, 0), "purple", "white", "chest[facing=north]", "minecraft:clock", "minecraft:candle");
        b.wset(91, 65, 124, B.s("pumpkin")); b.wset(91, 66, 124, B.s("carved_pumpkin[facing=south]".replace("carved_pumpkin[facing=south]", "air")));
        b.wset(86, 65, 125, B.s("hay_block")); b.wset(85, 65, 125, B.s("hay_block")); b.wset(85, 66, 125, B.s("hay_block"));
        // notice board
        B n = b.at(98, 65, 104, 0);
        for (int x = 0; x <= 6; x++) { n.set(x, 0, 0, x == 0 || x == 6 ? "stripped_spruce_log" : "air"); n.set(x, 1, 0, x == 0 || x == 6 ? "stripped_spruce_log" : "spruce_planks"); n.set(x, 2, 0, x == 0 || x == 6 ? "stripped_spruce_log" : "spruce_planks"); n.set(x, 3, 0, "spruce_slab[type=bottom]"); }
        n.sign(1, 2, 1, "birch_wall_sign[facing=south]", "MISSING", "Pell family", "sailed for help", "- any word?");
        n.sign(3, 2, 1, "birch_wall_sign[facing=south]", "NOTICE", "Do not row", "into the fog.", "-T. Wake");
        n.sign(5, 2, 1, "birch_wall_sign[facing=south]", "Lost: grey", "cat, answers", "to Biscuit.", "Ask Corin");
        n.sign(2, 1, 1, "birch_wall_sign[facing=south]", "Chronicle", "readings at", "the Archive,", "all welcome");
        n.sign(4, 1, 1, "birch_wall_sign[facing=south]", "WANTED", "sailcloth,", "brass, pine.", "See Reyes");
        // benches, lamp posts, planters
        for (int[] p : new int[][]{{94, 126}, {95, 126}, {96, 126}, {106, 126}, {107, 126}, {108, 126}}) b.wset(p[0], 65, p[1], B.s("spruce_stairs[facing=north]"));
        for (int[] p : new int[][]{{93, 126}, {105, 126}}) b.wset(p[0], 65, p[1], B.s("spruce_trapdoor[facing=west,open=true]"));
        for (int[] p : new int[][]{{97, 126}, {109, 126}}) b.wset(p[0], 65, p[1], B.s("spruce_trapdoor[facing=east,open=true]"));
        for (int[] p : new int[][]{{84, 104}, {118, 104}, {84, 126}, {118, 126}, {93, 103}, {109, 103}}) lamp(b, p[0], 65, p[1]);
        for (int[] p : new int[][]{{92, 115}, {110, 115}}) planter(b, p[0], 65, p[1]);
        // festival tables (candles placed via FESTIVAL_OFF)
        for (int[] p : new int[][]{{99, 112}, {103, 112}, {97, 119}, {105, 119}}) { b.wset(p[0], 65, p[1], B.s("spruce_fence")); }
        for (int[] p : new int[][]{{86, 104}, {116, 104}, {86, 125}, {116, 125}, {101, 103}}) b.wset(p[0], 65, p[1], B.s("cobblestone_wall"));
        // bunting between lamp posts (banners)
        b.wset(101, 69, 103, B.s("air"));
    }

    static void stall(B s, String c1, String c2, String good, String i1, String i2) {
        for (int[] c : new int[][]{{0, 0}, {4, 0}, {0, 3}, {4, 3}}) for (int y = 0; y <= 2; y++) s.set(c[0], y, c[1], "spruce_fence");
        for (int x = 0; x <= 4; x++) for (int z = -1; z <= 3; z++) s.set(x, 3, z, ((x + (z < 0 ? 1 : 0)) % 2 == 0 ? c1 : c2) + "_carpet");
        for (int x = 0; x <= 4; x++) s.set(x, 3, -1, "air");
        for (int x = 0; x <= 4; x++) { s.set(x, 2, -1, ((x % 2 == 0) ? c1 : c2) + "_wool"); }
        for (int x = 1; x <= 3; x++) s.set(x, 0, 0, x == 2 ? good : "stripped_spruce_wood");
        s.frame(1, 0, -1, NORTH, i1); s.frame(3, 0, -1, NORTH, i2);
        s.set(1, 0, 3, "barrel[facing=up]"); s.set(3, 0, 3, "barrel[facing=up]");
    }

    static void lamp(B b, int x, int y, int z) {
        b.wset(x, y, z, B.s("cobblestone_wall")); b.wset(x, y + 1, z, B.s("spruce_fence")); b.wset(x, y + 2, z, B.s("spruce_fence")); b.wset(x, y + 3, z, B.s("lantern"));
    }
    static void planter(B b, int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) b.wset(x + dx, y, z + dz, dx == 0 && dz == 0 ? B.s("rooted_dirt") : B.s("spruce_trapdoor[facing=north,half=top]"));
        b.wset(x, y, z, B.s("moss_block"));
        b.wset(x, y + 1, z, B.s("flowering_azalea"));
    }

    // ------------------------------------------------------------ archive (Ione)
    static void archive(B b) {
        Pal wall = Pal.of("stone_bricks:6", "mossy_stone_bricks:1", "cracked_stone_bricks:1", "tuff_bricks:2");
        String trim = "polished_deepslate";
        b.foundation(0, 0, 20, 14, -1, Pal.of("cobbled_deepslate:2", "stone_bricks:3"));
        b.clear(0, 0, 0, 20, 20, 16);
        b.fill(0, -1, 0, 20, -1, 14, Pal.of("dark_oak_planks:4", "spruce_planks:1"));
        b.walls(0, 0, 0, 20, 11, 14, wall);
        for (int y = 0; y <= 11; y++) for (int[] c : new int[][]{{0, 0}, {20, 0}, {0, 14}, {20, 14}}) b.set(c[0], y, c[1], trim);
        for (int x = 0; x <= 20; x++) { b.set(x, 5, 0, trim); b.set(x, 5, 14, trim); b.set(x, 11, 0, "polished_deepslate_slab[type=top]".replace("_slab[type=top]", "")); b.set(x, 11, 14, trim); }
        for (int z = 0; z <= 14; z++) { b.set(0, 5, z, trim); b.set(20, 5, z, trim); b.set(0, 11, z, trim); b.set(20, 11, z, trim); }
        // buttresses
        for (int x = 4; x <= 16; x += 4) for (int z : new int[]{-1, 15}) { for (int y = -1; y <= 3; y++) b.set(x, y, z, "stone_bricks"); b.set(x, 4, z, "stone_brick_stairs[facing=" + (z < 0 ? "south" : "north") + "]"); }
        // tall windows
        for (int x = 2; x <= 18; x += 4) {
            for (int y = 1; y <= 3; y++) { b.set(x, y, 0, "glass_pane"); b.set(x, y, 14, "glass_pane"); b.set(x, y + 6, 0, "glass_pane"); b.set(x, y + 6, 14, "glass_pane"); }
            b.set(x, 4, 0, "stone_brick_stairs[facing=north,half=top]".replace("north", "south")); b.set(x, 4, 14, "stone_brick_stairs[facing=north,half=top]");
        }
        for (int z = 3; z <= 11; z += 4) for (int y = 1; y <= 3; y++) { b.set(0, y + 6, z, "glass_pane"); b.set(20, y, z, "glass_pane"); b.set(20, y + 6, z, "glass_pane"); }
        // entrance (east face = local z 14) with steps and a little porch roof
        b.clear(9, 0, 14, 11, 3, 14);
        b.set(9, 3, 14, "stone_brick_stairs[facing=east,half=top]"); b.set(11, 3, 14, "stone_brick_stairs[facing=west,half=top]"); b.set(10, 3, 14, trim);
        Kit.door(b, 10, 0, 14, SOUTH, "dark_oak_door", false);
        b.set(9, 0, 14, "stone_bricks"); b.set(11, 0, 14, "stone_bricks"); b.set(9, 1, 14, "stone_bricks"); b.set(11, 1, 14, "stone_bricks"); b.set(9, 2, 14, "stone_bricks"); b.set(11, 2, 14, "stone_bricks");
        b.sign(10, 4, 15, "dark_oak_wall_sign[facing=south]", "THE ARCHIVE", "of Gullhaven", "", "What is written stays");
        b.set(8, 2, 15, "lantern[hanging=false]".replace("lantern[hanging=false]", "air")); 
        // floors
        b.fill(1, 5, 1, 19, 5, 13, Pal.of("dark_oak_planks"));
        b.fill(1, 11, 1, 19, 11, 13, Pal.of("stone_bricks"));
        // ground floor: reading hall
        for (int x = 2; x <= 18; x++) if (x % 4 != 0) { b.set(x, 0, 1, "bookshelf"); b.set(x, 1, 1, "bookshelf"); b.set(x, 2, 1, x % 3 == 0 ? "chiseled_bookshelf[facing=south]" : "bookshelf"); }
        for (int x = 3; x <= 13; x += 5) for (int z = 4; z <= 5; z++) { b.set(x, 0, z, "bookshelf"); b.set(x + 1, 0, z, "bookshelf"); b.set(x, 1, z, "bookshelf"); b.set(x + 1, 1, z, z == 4 ? "bookshelf" : "chiseled_bookshelf[facing=south]"); }
        // reading tables with candles
        for (int x = 3; x <= 13; x += 5) { Kit.tableTop(b, x, 0, 9, "dark_oak", "candle[candles=2,lit=true]"); Kit.tableTop(b, x + 1, 0, 9, "dark_oak", "dark_oak_pressure_plate"); b.set(x, 0, 10, "dark_oak_stairs[facing=south]"); b.set(x + 1, 0, 10, "dark_oak_stairs[facing=south]"); }
        // Ione's desk by the south-west window
        b.lectern(16, 0, 8, SOUTH, "lore_book_kings".replace("lore_book_kings", "lullaby"));
        b.set(16, 0, 8, "dark_oak_planks"); b.set(16, 1, 8, "candle[candles=3,lit=true]");
        b.set(17, 0, 8, "dark_oak_planks"); b.set(17, 1, 8, "potted_fern"); b.set(15, 0, 8, "barrel[facing=up]"); b.set(15, 1, 8, "decorated_pot");
        b.frame(19, 2, 7, WEST, "minecraft:writable_book"); b.frame(19, 2, 9, WEST, "minecraft:feather"); b.frame(19, 3, 8, WEST, "minecraft:map");
        // wall of maps
        for (int x = 2; x <= 7; x++) for (int y = 2; y <= 3; y++) b.frame(x, y, 13, NORTH, "minecraft:map");
        Kit.carpet(b, 6, 7, 14, 11, 0, "red");
        for (int x = 4; x <= 16; x += 6) Kit.hangLantern(b, x, 4, 7, 1, false);
        // stairs to the upper floor (along x at z 12..13)
        for (int i = 0; i <= 5; i++) { b.set(18 - i, i, 12, "dark_oak_stairs[facing=west]"); b.set(18 - i, i, 13, "dark_oak_stairs[facing=west]"); }
        b.clear(13, 5, 12, 18, 5, 13);
        for (int i = 0; i <= 4; i++) { b.set(19 - i, i - 1, 12, "dark_oak_planks"); }
        // upper floor: stacks and Ione's alcove
        for (int x = 2; x <= 12; x += 2) for (int z = 2; z <= 9; z++) if (z != 6) { b.set(x, 6, z, "bookshelf"); b.set(x, 7, z, z % 3 == 0 ? "chiseled_bookshelf[facing=east]" : "bookshelf"); b.set(x, 8, z, "bookshelf"); }
        Kit.bed(b, 19, 6, 2, NORTH, "blue");
        b.set(19, 6, 4, "chest[facing=west]"); b.set(17, 6, 1, "barrel[facing=up]"); b.set(17, 7, 1, "potted_blue_orchid");
        b.set(15, 6, 1, "dark_oak_stairs[facing=south]".replace("south", "north"));
        Kit.tableTop(b, 15, 6, 3, "dark_oak", "candle[candles=1,lit=true]");
        b.set(14, 6, 1, "bookshelf"); b.set(14, 7, 1, "bookshelf");
        Kit.carpet(b, 16, 1, 19, 6, 6, "light_blue");
        b.frame(19, 8, 7, WEST, "minecraft:spyglass");
        for (int x = 4; x <= 16; x += 6) Kit.hangLantern(b, x, 10, 7, 1, false);
        // ladder to the roof terrace
        for (int y = 6; y <= 11; y++) b.set(1, y, 7, "ladder[facing=east]");
        b.set(1, 11, 7, "dark_oak_trapdoor[facing=east,half=top]".replace("[facing=east,half=top]", "[facing=east,half=bottom]"));
        b.set(1, 11, 7, "air");
        // roof terrace: parapet, telescope, benches, plants
        for (int x = 0; x <= 20; x++) { b.set(x, 12, 0, x % 2 == 0 ? "stone_brick_wall" : "stone_bricks"); b.set(x, 12, 14, x % 2 == 0 ? "stone_brick_wall" : "stone_bricks"); }
        for (int z = 0; z <= 14; z++) { b.set(0, 12, z, z % 2 == 0 ? "stone_brick_wall" : "stone_bricks"); b.set(20, 12, z, z % 2 == 0 ? "stone_brick_wall" : "stone_bricks"); }
        b.set(1, 12, 7, "air"); b.set(0, 12, 7, "stone_bricks");
        b.set(11, 12, 3, "spruce_fence"); b.set(11, 13, 3, "lightning_rod[facing=north]");
        b.set(10, 12, 3, "spruce_fence"); b.set(12, 12, 3, "spruce_fence");
        for (int x = 9; x <= 13; x++) b.set(x, 12, 10, "spruce_stairs[facing=south]".replace("south", "north"));
        b.set(8, 12, 10, "spruce_trapdoor[facing=west,open=true]"); b.set(14, 12, 10, "spruce_trapdoor[facing=east,open=true]");
        for (int[] p : new int[][]{{2, 2}, {18, 2}, {2, 12}, {18, 12}}) b.set(p[0], 12, p[1], "potted_azalea_bush");
        b.set(5, 12, 2, "lantern"); b.set(16, 12, 12, "lantern"); b.set(17, 12, 2, "barrel[facing=up]");
        // little bell turret on the roof corner
        for (int y = 12; y <= 15; y++) b.set(19, y, 13, y < 15 ? "stone_brick_wall" : "polished_deepslate");
        b.set(19, 16, 13, "deepslate_tile_slab[type=bottom]");
    }

    // ------------------------------------------------------------ smithy (Corin)
    static void smithy(B b) {
        Pal stone = Pal.of("cobblestone:3", "stone_bricks:2", "andesite:2", "mossy_cobblestone:1");
        b.foundation(0, 0, 14, 12, -1, stone);
        b.clear(0, 0, 0, 14, 12, 13);
        b.fill(0, -1, 0, 14, -1, 12, Pal.of("stone:2", "cobblestone:2", "gravel:1", "coarse_dirt:1"));
        b.walls(0, 0, 0, 14, 4, 0, stone);
        for (int z = 0; z <= 8; z++) for (int y = 0; y <= 4; y++) { b.set(0, y, z, stone); b.set(14, y, z, stone); }
        for (int x : new int[]{0, 5, 9, 14}) for (int y = 0; y <= 4; y++) b.set(x, y, 12, "stripped_spruce_log");
        for (int x = 0; x <= 14; x++) b.set(x, 5, 12, "stripped_spruce_log[axis=x]");
        for (int z = 0; z <= 12; z++) { b.set(0, 5, z, "stripped_spruce_log[axis=z]"); b.set(14, 5, z, "stripped_spruce_log[axis=z]"); }
        Kit.gableX(b, 0, 0, 14, 12, 6, "spruce_stairs", "spruce_slab", Pal.of("spruce_planks"), true);
        // forge with hood and chimney
        b.fill(5, 0, 1, 9, 0, 3, Pal.of("bricks"));
        b.set(6, 0, 1, "blast_furnace[facing=south,lit=true]"); b.set(8, 0, 1, "furnace[facing=south,lit=true]");
        b.set(7, 0, 2, "lava_cauldron"); b.set(7, 0, 1, "bricks");
        b.set(6, 1, 1, "campfire[lit=true,signal_fire=false]".replace("campfire", "campfire")); b.set(8, 1, 1, "bricks");
        for (int x = 5; x <= 9; x++) { b.set(x, 2, 1, "bricks"); b.set(x, 3, 2, "brick_stairs[facing=north,half=top]"); }
        b.set(5, 2, 2, "brick_stairs[facing=east,half=top]"); b.set(9, 2, 2, "brick_stairs[facing=west,half=top]");
        Kit.chimney(b, 7, 1, 3, 11, Pal.of("bricks:4", "stone_bricks:1"));
        // work area
        b.set(7, 0, 6, "anvil[facing=east]"); b.set(9, 0, 6, "water_cauldron[level=3]"); b.set(4, 0, 5, "grindstone[face=floor,facing=east]");
        b.set(11, 0, 2, "smithing_table"); b.set(12, 0, 2, "barrel[facing=up]"); b.set(13, 0, 2, "barrel[facing=up]"); b.set(13, 1, 2, "barrel[facing=up]");
        b.set(1, 0, 2, "chest[facing=east]"); b.set(1, 0, 3, "barrel[facing=east]"); b.set(1, 0, 5, "coal_block"); b.set(1, 1, 5, "coal_block".replace("coal_block", "barrel[facing=up]"));
        b.frame(2, 2, 1, SOUTH, "minecraft:iron_pickaxe"); b.frame(3, 2, 1, SOUTH, "minecraft:iron_axe"); b.frame(4, 2, 1, SOUTH, "minecraft:iron_shovel");
        b.frame(10, 2, 1, SOUTH, "minecraft:iron_sword"); b.frame(11, 2, 1, SOUTH, "minecraft:shield"); b.frame(12, 2, 1, SOUTH, "minecraft:iron_hoe");
        b.stand(12.5, 0, 8.5, 200, "minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:iron_boots");
        b.stand(2.5, 0, 9.5, 160, "minecraft:chainmail_helmet", "minecraft:chainmail_chestplate", "", "");
        for (int z = 9; z <= 11; z++) b.set(13, 0, z, z % 2 == 0 ? "barrel[facing=up]" : "iron_block".replace("iron_block", "barrel[facing=west]"));
        Kit.hangLantern(b, 4, 4, 7, 1, false); Kit.hangLantern(b, 10, 4, 7, 1, false);
        b.sign(7, 4, 13, "spruce_wall_sign[facing=south]", "ASHBY", "Smith & Farrier", "", "Mind the sparks");
    }

    static void corinCottage(B b) {
        Pal brick = Pal.of("bricks:5", "stone_bricks:1", "mud_bricks:2");
        b.foundation(0, 0, 9, 8, -1, Pal.of("cobblestone"));
        b.clear(0, 0, 0, 9, 9, 8);
        b.fill(0, -1, 0, 9, -1, 8, Pal.of("oak_planks"));
        b.walls(0, 0, 0, 9, 3, 8, brick);
        for (int[] c : new int[][]{{0, 0}, {9, 0}, {0, 8}, {9, 8}}) for (int y = 0; y <= 3; y++) b.set(c[0], y, c[1], "stripped_oak_log");
        Kit.gableX(b, 0, 0, 9, 8, 4, "dark_oak_stairs".replace("dark_oak", "mangrove"), "mangrove_slab", Pal.of("mangrove_planks"), true);
        Kit.door(b, 4, 0, 8, SOUTH, "oak_door", false);
        Kit.window(b, 2, 1, 8, 2, SOUTH, "glass_pane", "oak_trapdoor", "potted_orange_tulip");
        Kit.window(b, 7, 1, 8, 2, SOUTH, "glass_pane", "oak_trapdoor", "potted_poppy");
        Kit.window(b, 9, 1, 4, 2, EAST, "glass_pane", null, null);
        Kit.chimney(b, 0, 4, 0, 8, Pal.of("cobblestone"));
        Kit.bed(b, 7, 0, 1, NORTH, "orange"); b.set(8, 0, 1, "chest[facing=west]"); b.set(1, 0, 1, "furnace[facing=east]"); b.set(1, 0, 2, "smoker[facing=east]".replace("smoker", "barrel").replace("[facing=east]", "[facing=up]"));
        Kit.tableTop(b, 4, 0, 4, "oak", "candle[candles=1,lit=true]"); b.set(3, 0, 4, "oak_stairs[facing=west]"); b.set(5, 0, 4, "oak_stairs[facing=east]");
        b.frame(1, 2, 4, EAST, "minecraft:iron_ingot"); Kit.carpet(b, 6, 3, 8, 6, 0, "brown");
        // little vegetable garden + fence
        for (int x = 1; x <= 8; x++) for (int z = 10; z <= 12; z++) { b.set(x, -1, z, z == 11 ? "farmland[moisture=7]" : "dirt"); if (z == 11) b.set(x, 0, z, x % 2 == 0 ? "beetroots[age=3]" : "carrots[age=7]"); }
        b.set(0, -1, 11, "water");
        for (int x = 0; x <= 9; x++) b.set(x, 0, 13, "oak_fence");
    }

    // ------------------------------------------------------------ Pell's Provisions (closed store)
    static void store(B b) {
        b.foundation(0, 0, 10, 11, -1, Pal.of("cobblestone"));
        b.clear(0, 0, 0, 10, 10, 12);
        b.fill(0, -1, 0, 10, -1, 11, Pal.of("birch_planks:3", "oak_planks:1"));
        b.walls(0, 0, 0, 10, 4, 11, Pal.of("birch_planks:4", "stripped_birch_wood:1"));
        for (int[] c : new int[][]{{0, 0}, {10, 0}, {0, 11}, {10, 11}}) for (int y = 0; y <= 4; y++) b.set(c[0], y, c[1], "stripped_dark_oak_log");
        for (int x = 0; x <= 10; x++) { b.set(x, 0, 11, "stone_bricks"); b.set(x, 0, 0, "stone_bricks"); }
        Kit.gableX(b, 0, 0, 10, 11, 5, "deepslate_tile_stairs", "deepslate_tile_slab", Pal.of("birch_planks"), true);
        Kit.door(b, 5, 0, 11, SOUTH, "birch_door", false);
        for (int x : new int[]{2, 3, 7, 8}) Kit.window(b, x, 1, 11, 2, SOUTH, "glass_pane", null, null);
        b.sign(5, 3, 12, "birch_wall_sign[facing=south]", "PELL'S", "PROVISIONS", "~ CLOSED ~", "back soon");
        // awning
        for (int x = 1; x <= 9; x++) b.set(x, 3, 12, (x % 2 == 0 ? "white" : "green") + "_wool".replace("_wool", "_wool"));
        for (int x = 1; x <= 9; x++) { b.set(x, 3, 12, "air"); b.set(x, 3, 13, (x % 2 == 0 ? "white" : "green") + "_carpet"); }
        for (int x = 1; x <= 9; x += 8) for (int y = 0; y <= 2; y++) b.set(x, y, 13, "birch_fence");
        // counter and empty shelves
        for (int x = 2; x <= 8; x++) b.set(x, 0, 6, x == 5 ? "birch_fence_gate[facing=south]" : "stripped_birch_wood");
        for (int x = 1; x <= 9; x++) { b.set(x, 0, 1, x % 2 == 0 ? "barrel[facing=south]" : "chest[facing=south]"); b.set(x, 1, 1, "birch_slab[type=bottom]"); b.set(x, 2, 1, x % 3 == 0 ? "flower_pot" : "air"); }
        b.set(3, 1, 6, "candle[candles=1,lit=false]"); b.set(7, 1, 6, "lantern");
        b.lectern(8, 0, 4, SOUTH, "lore_book_miller".replace("lore_book_miller", "lore_book_miller"));
        b.set(8, 0, 4, "birch_planks");
        b.set(1, 3, 10, "cobweb"); b.set(9, 3, 2, "cobweb"); b.set(1, 0, 10, "barrel[facing=up]");
        b.set(8, 0, 4, "barrel[facing=up]");
    }

    // ------------------------------------------------------------ chapel with bell tower and graveyard
    static void chapel(B b) {
        Pal wall = Pal.of("stone_bricks:5", "andesite:2", "mossy_stone_bricks:2", "cracked_stone_bricks:1");
        b.foundation(0, 0, 14, 20, -1, Pal.of("stone_bricks"));
        b.clear(0, 0, 0, 14, 16, 21);
        b.fill(0, -1, 0, 14, -1, 20, Pal.of("polished_andesite:3", "stone_bricks:2"));
        b.walls(0, 0, 0, 14, 7, 20, wall);
        for (int z = 0; z <= 20; z += 4) for (int x : new int[]{-1, 15}) { for (int y = -1; y <= 5; y++) b.set(x, y, z, "stone_bricks"); b.set(x, 6, z, "stone_brick_stairs[facing=" + (x < 0 ? "east" : "west") + "]"); }
        // stained windows
        for (int z = 2; z <= 18; z += 4) for (int y = 2; y <= 5; y++) {
            String c = y == 5 ? "yellow" : (z / 4) % 2 == 0 ? "light_blue" : "white";
            b.set(0, y, z, c + "_stained_glass_pane"); b.set(14, y, z, c + "_stained_glass_pane");
        }
        // rose window on the altar end
        for (int x = 5; x <= 9; x++) for (int y = 3; y <= 7; y++) if (Math.hypot(x - 7, y - 5) < 2.4) b.set(x, y, 0, (x + y) % 2 == 0 ? "yellow_stained_glass_pane" : "orange_stained_glass_pane");
        // steep roof (along z)
        Kit.gableZ(b, 0, 0, 14, 20, 8, "deepslate_tile_stairs", "deepslate_tile_slab", Pal.of("stone_bricks"), true);
        // door and porch
        b.clear(6, 0, 20, 8, 3, 20);
        Kit.door(b, 6, 0, 20, SOUTH, "spruce_door", false); Kit.door(b, 8, 0, 20, SOUTH, "spruce_door", true);
        b.set(7, 0, 20, "air"); b.set(7, 1, 20, "air"); b.set(7, 2, 20, "air");
        b.set(6, 2, 20, "stone_bricks"); b.set(8, 2, 20, "stone_bricks"); b.set(6, 3, 20, "stone_brick_stairs[facing=east,half=top]"); b.set(8, 3, 20, "stone_brick_stairs[facing=west,half=top]");
        b.set(7, 3, 20, "chiseled_stone_bricks");
        // front: pointed portal, rose window, corner buttresses, finial
        for (int y = 0; y <= 4; y++) { b.set(5, y, 21, "stone_bricks"); b.set(9, y, 21, "stone_bricks"); }
        b.set(5, 5, 21, "stone_brick_stairs[facing=east]"); b.set(9, 5, 21, "stone_brick_stairs[facing=west]");
        b.set(6, 4, 21, "stone_brick_stairs[facing=east,half=top]"); b.set(8, 4, 21, "stone_brick_stairs[facing=west,half=top]"); b.set(7, 4, 21, "chiseled_stone_bricks");
        b.set(7, 5, 21, "stone_brick_slab[type=bottom]");
        for (int x = 5; x <= 9; x++) for (int y = 8; y <= 12; y++) if (Math.hypot(x - 7, y - 10) < 2.4) b.set(x, y, 20, Math.hypot(x - 7, y - 10) < 1 ? "yellow_stained_glass_pane" : (x + y) % 2 == 0 ? "light_blue_stained_glass_pane" : "white_stained_glass_pane");
        for (int[] c : new int[][]{{-1, 21}, {15, 21}, {15, 19}}) { for (int y = -1; y <= 6; y++) b.set(c[0], y, c[1], "stone_bricks"); b.set(c[0], 7, c[1], "stone_brick_wall"); }
        for (int y = 2; y <= 5; y++) { b.set(3, y, 20, "light_blue_stained_glass_pane"); b.set(11, y, 20, "light_blue_stained_glass_pane"); }
        b.set(7, 16, 20, "stone_brick_wall"); b.set(7, 17, 20, "stone_brick_wall"); b.set(6, 17, 20, "stone_brick_wall".replace("stone_brick_wall", "air"));
        // pews and altar
        for (int z = 8; z <= 17; z += 2) for (int x = 2; x <= 12; x++) if (x != 6 && x != 7 && x != 8) b.set(x, 0, z, "spruce_stairs[facing=south]".replace("south", "south"));
        Kit.carpet(b, 6, 4, 8, 19, 0, "red");
        b.fill(4, 0, 2, 10, 0, 4, Pal.of("polished_andesite"));
        b.set(7, 1, 2, "chiseled_stone_bricks"); b.set(6, 1, 2, "candle[candles=3,lit=true]"); b.set(8, 1, 2, "candle[candles=3,lit=true]");
        b.set(5, 1, 3, "candle[candles=4,lit=true]"); b.set(9, 1, 3, "candle[candles=4,lit=true]");
        b.set(7, 2, 2, "lantern");
        for (int z = 6; z <= 18; z += 6) Kit.hangLantern(b, 7, 7, z, 2, false);
        b.lectern(7, 1, 4, SOUTH, "lullaby");
        b.set(7, 0, 4, "polished_andesite");
        // bell tower (local x -4..0, z 16..20)
        B t = b.sub(-5, 0, 16, 0);
        t.foundation(0, 0, 4, 4, -1, Pal.of("stone_bricks"));
        t.walls(0, 0, 0, 4, 18, 4, wall);
        t.clear(1, 0, 1, 3, 22, 3);
        for (int y = 0; y <= 19; y++) t.set(2, y, 1, "ladder[facing=south]");
        for (int y : new int[]{6, 12}) t.fill(1, y, 1, 3, y, 3, Pal.of("spruce_planks"));
        t.set(2, 6, 1, "ladder[facing=south]"); t.set(2, 12, 1, "ladder[facing=south]");
        for (int y = 15; y <= 18; y++) for (int i = 1; i <= 3; i++) { String a = (y == 18 && i != 2) ? "stone_bricks" : (i == 2 || y <= 17) && y >= 15 ? "air" : "stone_bricks"; t.set(i, y, 0, a); t.set(i, y, 4, a); t.set(0, y, i, a); t.set(4, y, i, a); }
        for (int y = 3; y <= 11; y += 4) { t.set(2, y, 0, "air"); t.set(2, y + 1, 0, "air"); t.set(0, y, 2, "glass_pane"); }
        for (int[] c : new int[][]{{-1, -1}, {5, -1}, {-1, 5}, {5, 5}}) { for (int y = -1; y <= 3; y++) t.set(c[0], y, c[1], "stone_bricks"); t.set(c[0], 4, c[1], "stone_brick_wall"); }
        t.fill(1, 19, 1, 3, 19, 3, Pal.of("spruce_planks"));
        t.set(2, 19, 1, "ladder[facing=south]"); t.set(2, 20, 1, "ladder[facing=south]");
        t.set(2, 18, 2, "bell[attachment=ceiling,facing=north]".replace("ceiling", "ceiling"));
        t.set(2, 18, 2, "air"); t.set(2, 19, 2, "spruce_planks"); t.set(2, 18, 2, "bell[attachment=ceiling,facing=north]");
        // spire roof
        for (int k = 0; k <= 2; k++) {
            int lo = -1 + k, hi = 5 - k, y = 20 + k;
            for (int i = lo; i <= hi; i++) { t.set(i, y, lo, "deepslate_tile_stairs[facing=south]"); t.set(i, y, hi, "deepslate_tile_stairs[facing=north]"); t.set(lo, y, i, "deepslate_tile_stairs[facing=east]"); t.set(hi, y, i, "deepslate_tile_stairs[facing=west]"); }
        }
        t.set(2, 23, 2, "deepslate_tile_wall"); t.set(2, 24, 2, "lightning_rod");
        t.fill(1, 20, 1, 3, 20, 3, Pal.of("spruce_planks"));
        t.set(2, 20, 1, "ladder[facing=south]");
        t.container(3, 21, 3, "barrel[facing=up]", "fi:owl_whistle", "minecraft:feather*3");
        t.set(1, 21, 3, "cobweb"); t.set(3, 21, 1, "candle[candles=1,lit=false]");
        b.clear(0, 0, 17, 0, 2, 17);   // doorway from nave into the tower
        t.clear(4, 0, 1, 4, 1, 1);
        // graveyard
        for (int x = 16; x <= 24; x++) { b.set(x, 0, 2, "spruce_fence"); b.set(x, 0, 20, "spruce_fence"); }
        for (int z = 2; z <= 20; z++) b.set(24, 0, z, "spruce_fence");
        b.set(20, 0, 20, "spruce_fence_gate[facing=south]");
        String[][] graves = {{"Annis V.", "Lost to the sea", "in the great storm", ""}, {"Maud Pike", "She found what", "the sea gave back", ""},
                {"Edwin Moss", "Innkeeper", "", ""}, {"Rowan Ashdown", "Ranger of the", "Mosswood", ""}, {"Here lies", "a name we", "can no longer", "remember"}, {"Pell", "Merchant", "", ""}};
        int gi = 0;
        for (int z = 5; z <= 17; z += 4) for (int x = 18; x <= 22; x += 4) {
            if (gi >= graves.length) break;
            b.set(x, 0, z, "mossy_stone_brick_wall"); b.set(x, 1, z, "stone_brick_slab[type=bottom]");
            b.sign(x, 0, z + 1, "spruce_wall_sign[facing=south]", graves[gi]);
            b.set(x + 1, 0, z + 1, gi % 2 == 0 ? "poppy" : "lily_of_the_valley");
            gi++;
        }
        b.set(21, 0, 8, "air");
        b.set(22, 0, 9, "white_tulip");
        // a yew by the wall
        for (int y = 0; y <= 4; y++) b.set(16, y, 17, "spruce_log");
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int y = 3; y <= 6; y++)
            if (Math.abs(dx) + Math.abs(dz) + Math.max(0, y - 4) <= 3 && !(dx == 0 && dz == 0 && y <= 4)) b.setIfAir(16 + dx, y, 17 + dz, "spruce_leaves[persistent=true]");
    }

    // ------------------------------------------------------------ abandoned cottage
    static void abandoned(B b) {
        Pal wall = Pal.of("cobblestone:3", "mossy_cobblestone:4", "stone:1");
        b.foundation(0, 0, 9, 8, -1, wall);
        b.clear(0, 0, 0, 9, 9, 8);
        b.fill(0, -1, 0, 9, -1, 8, Pal.of("oak_planks:2", "coarse_dirt:1", "moss_block:1"));
        b.walls(0, 0, 0, 9, 3, 8, wall);
        b.clear(6, 2, 0, 8, 3, 0); b.clear(9, 3, 3, 9, 3, 5);
        Kit.gableX(b, 0, 0, 9, 8, 4, "mossy_cobblestone_stairs", "mossy_cobblestone_slab", Pal.of("oak_planks"), false);
        for (int x = 3; x <= 7; x++) for (int y = 4; y <= 8; y++) for (int z = 2; z <= 6; z++) if (Noise.hash(x, y, z * 3) < 0.55) b.set(x, y, z, "air");
        b.set(4, 0, 8, "air"); b.set(4, 1, 8, "air");
        b.set(4, 0, 8, "oak_door[facing=south,half=lower,open=true]"); b.set(4, 1, 8, "oak_door[facing=south,half=upper,open=true]");
        for (int y = 0; y <= 3; y++) for (int x = 0; x <= 9; x += 9) for (int z = 1; z <= 7; z += 3) b.setIfAir(x == 0 ? -1 : 10, y, z, "vine[" + (x == 0 ? "east" : "west") + "=true]");
        b.set(2, 0, 2, "oak_stairs[facing=east]".replace("east", "west")); b.set(6, 0, 5, "oak_slab[type=bottom]"); b.set(1, 0, 6, "cobweb"); b.set(8, 2, 1, "cobweb");
        b.set(7, 0, 1, "red_bed[facing=north,part=head]".replace("red_bed[facing=north,part=head]", "brown_carpet"));
        b.set(3, 0, 3, "moss_carpet"); b.set(4, 0, 3, "moss_carpet"); b.set(5, 0, 6, "fern");
        b.lectern(1, 0, 1, EAST, "lore_book_miller");
        b.sign(5, 1, 7, "oak_wall_sign[facing=north]", "We are rowing", "out to find the", "mainland. If you", "read this - wait");
    }

    // ------------------------------------------------------------ windmill
    static void windmill(B b) {
        Pal stone = Pal.of("cobblestone:3", "stone_bricks:3", "mossy_cobblestone:1");
        b.foundation(-1, -1, 7, 7, -1, stone);
        b.clear(-1, 0, -1, 7, 20, 9);
        for (int y = 0; y <= 14; y++) for (int x = 0; x <= 6; x++) for (int z = 0; z <= 6; z++) {
            boolean corner = (x == 0 || x == 6) && (z == 0 || z == 6);
            int inset = y > 6 ? 1 : 0;
            boolean edge = x == inset || x == 6 - inset || z == inset || z == 6 - inset;
            boolean cornerIn = (x == inset || x == 6 - inset) && (z == inset || z == 6 - inset);
            if (y <= 6 && corner) continue;
            if (y > 6 && cornerIn) continue;
            if (!edge || x < inset || x > 6 - inset || z < inset || z > 6 - inset) continue;
            b.set(x, y, z, y <= 6 ? stone : ((x + y) % 4 == 0 ? Pal.of("stripped_spruce_log") : Pal.of("spruce_planks")));
        }
        b.fill(1, -1, 1, 5, -1, 5, Pal.of("spruce_planks"));
        b.fill(2, 7, 2, 4, 7, 4, Pal.of("spruce_planks")); b.fill(1, 6, 1, 5, 6, 5, Pal.of("spruce_planks"));
        b.fill(2, 11, 2, 4, 11, 4, Pal.of("spruce_planks"));
        for (int y = 0; y <= 11; y++) b.set(3, y, 1, "ladder[facing=south]");
        b.set(3, 6, 1, "ladder[facing=south]"); b.set(3, 11, 1, "ladder[facing=south]");
        b.set(3, 6, 2, "air"); b.set(3, 7, 2, "ladder[facing=south]".replace("south", "south")); 
        // cap
        for (int k = 0; k <= 3; k++) for (int x = k; x <= 6 - k; x++) for (int z = k; z <= 6 - k; z++)
            if (x == k || x == 6 - k || z == k || z == 6 - k) b.set(x, 15 + k, z, "spruce_planks".replace("spruce_planks", k == 3 ? "spruce_slab[type=bottom]" : "dark_oak_planks"));
        // door and window
        Kit.door(b, 3, 0, 6, SOUTH, "spruce_door", false);
        Kit.window(b, 0, 2, 3, 2, WEST, "glass_pane", null, null); Kit.window(b, 6, 9, 3, 1, EAST, "glass_pane", null, null);
        // hub and four sails on the south face
        b.set(3, 12, 7, "stripped_spruce_log[axis=z]"); b.set(3, 12, 8, "spruce_fence");
        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        for (int[] d : dirs) for (int i = 1; i <= 7; i++) {
            int x = 3 + d[0] * i, y = 12 + d[1] * i;
            b.set(x, y, 8, "spruce_fence");
            int ox = d[1] != 0 ? 1 : 0, oy = d[0] != 0 ? 1 : 0;
            if (i >= 2) { b.set(x + ox, y + oy, 8, "white_wool"); if (i >= 3) b.set(x + ox * 2, y + oy * 2, 8, i % 2 == 0 ? "white_wool" : "light_gray_wool"); }
        }
        // grain sacks and the miller's secret
        b.set(1, 0, 1, "hay_block"); b.set(5, 0, 1, "hay_block"); b.set(5, 1, 1, "hay_block"); b.set(1, 0, 4, "barrel[facing=up]"); b.set(4, 0, 4, "grindstone[face=floor,facing=north]");
        b.container(4, 12, 4, "chest[facing=west]", "fi:lore_book_miller", "fi:emeralds", "minecraft:bread*8", "fi:golden_apple");
        b.set(2, 12, 2, "cobweb"); b.set(2, 12, 4, "lantern");
        // sheep pen beside the mill
        for (int x = -6; x <= -1; x++) { b.set(x, 0, -2, "oak_fence"); b.set(x, 0, 10, "oak_fence"); }
        for (int z = -2; z <= 10; z++) b.set(-6, 0, z, "oak_fence");
        b.set(-1, 0, 4, "oak_fence"); b.set(-1, 0, -1, "oak_fence"); b.set(-1, 0, 9, "oak_fence"); b.set(-1, 0, 0, "oak_fence"); b.set(-1, 0, 8, "oak_fence");
        b.set(-3, 0, 4, "hay_block"); b.set(-4, 0, 7, "water_cauldron[level=3]");
    }

    static void animals(B b) {
        for (int i = 0; i < 5; i++) b.mob(EntityType.SHEEP, 145 - (i % 3), 71, 75 + i * 2);
        CatEntity c1 = b.mob(EntityType.CAT, 96, 65, 118); if (c1 != null) c1.setSitting(true);
        CatEntity c2 = b.mob(EntityType.CAT, 92, 66, 96); if (c2 != null) { c2.setCustomName(net.minecraft.text.Text.literal("Biscuit")); }
        b.mob(EntityType.CHICKEN, 140, 66, 110); b.mob(EntityType.CHICKEN, 141, 66, 112);
        b.mob(EntityType.RABBIT, 60, 68, 90);
        DyeColor.WHITE.getId();
    }
}
