package dev.forgottenisland.build;

import dev.forgottenisland.game.Game;
import dev.forgottenisland.game.Secrets;
import dev.forgottenisland.game.Places;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Direction;

import static net.minecraft.util.math.Direction.*;

/** Gullhaven's waterfront: quay, piers, harbourmaster, boathouse, fish market, warehouse, harbour light. */
public final class Harbor implements Layout.Part {
    public String name() { return "harbor"; }
    static final T W = new T(0, 0, 0, 0);
    static final T HM = new T(88, 64, 129, 0);       // harbourmaster: local 0..12 x 0..11
    static final T BH = new T(112, 64, 151, 0);      // boathouse over the water: local 0..18 x 0..21
    static final T FC = new T(124, 64, 129, 0);      // fisher's cottage 0..8 x 0..7
    static final T WH = new T(138, 64, 127, 0);      // warehouse 0..15 x 0..13

    public void places() {
        Terrain.pad(56, 125, 160, 151, 63, 5);
        W.place("intro_boat", 105, 62.6, 238, 180);
        W.place("cp_dock", 105, 64, 186, 180);
        W.place("pier_end", 105, 64, 196, 0);
        W.place("tobias_greet", 105, 64, 189, 0);
        W.place("tobias_pier", 101, 64, 194, -90);
        W.place("sol_pier", 109, 64, 194, 90);
        W.place("wendel_pier", 79, 64, 168, 0);
        W.place("harbor_front", 100, 64, 147, 0);
        W.place("quay_bench", 84, 64, 149, 0);
        W.zone("arrival", 96, 60, 180, 114, 72, 202, p -> Game.state(p.getServer()).set("arrived"));
        W.zone("harbor", 56, 60, 126, 160, 80, 152, null);
        HM.place("tobias_office", 4, 0, 4, 180);
        HM.place("tobias_desk", 6, 0, 2, 180);
        HM.place("tobias_bed", 2, 5, 2, 0);
        HM.place("tobias_tower", 10, 14, 1, 0);
        BH.place("sol_hull", 9, 0, 8, -90);
        BH.place("sol_bench", 15, 0, 4, 90);
        BH.place("sol_bed", 16, 6, 3, 0);
        BH.place("sol_loft", 13, 6, 6, 0);
        W.place("fishmarket", 70, 64, 139, 0);
        W.place("warehouse_yard", 146, 64, 146, 0);
        W.place("date_gullrock_player", 184, 85, 215, 200);
        W.place("date_gullrock_sol", 186, 85, 216, 160);
        W.zone("secret_smugglers_cache", 81, 54, 156, 85, 60, 161, p -> Secrets.discover(p, "smugglers_cache"));
        W.zone("secret_gull_rock", 181, 84, 213, 191, 92, 223, p -> Secrets.discover(p, "gull_rock"));
    }

    public void build(ServerWorld w) {
        B b = new B(w, W);
        quay(b);
        piers(b);
        harbormaster(new B(w, HM));
        boathouse(new B(w, BH));
        fishMarket(b);
        fisherCottage(new B(w, FC));
        warehouse(new B(w, WH));
        harborLight(b);
        gullRock(b);
        smugglersCache(b);
    }

    static boolean water(B b, int x, int z) { return b.wget(x, 62, z).getFluidState().isStill() && !b.wget(x, 63, z).isSolid(); }

    static void quay(B b) {
        Pal wall = Pal.of("stone_bricks:5", "mossy_stone_bricks:3", "cracked_stone_bricks:1", "cobblestone:1");
        Pal deck = Pal.of("stone_bricks:3", "polished_andesite:2", "andesite:2", "cobblestone:1", "gravel:1");
        for (int x = 58; x <= 156; x++) {
            int edge = -1;
            for (int z = 135; z <= 176; z++) if (water(b, x, z)) { edge = z - 1; break; }
            if (edge < 0) continue;
            for (int z = 140; z <= edge; z++) b.wset(x, 63, z, deck.pick(x, 63, z));
            for (int y = 50; y <= 63; y++) b.wset(x, y, edge, wall.pick(x, y, edge));
            b.wset(x, 64, edge, x % 9 == 0 ? B.s("stripped_spruce_log") : B.s("air"));
            if (x % 9 == 0) b.wset(x, 65, edge, x % 18 == 0 ? B.s("lantern") : B.s("air"));
            if (x % 13 == 4) { for (int y = 54; y <= 62; y++) b.wset(x, y, edge + 1, B.s("ladder[facing=south]")); }
        }
        // promenade benches, barrels, nets
        b.set(83, 64, 149, "spruce_stairs[facing=north]"); b.set(84, 64, 149, "spruce_stairs[facing=north]"); b.set(85, 64, 149, "spruce_stairs[facing=north]");
        b.set(82, 64, 149, "spruce_trapdoor[facing=west,open=true]"); b.set(86, 64, 149, "spruce_trapdoor[facing=east,open=true]");
        for (int[] p : new int[][]{{112, 146}, {113, 146}, {112, 147}, {60, 147}, {61, 147}, {150, 149}, {98, 148}}) b.set(p[0], 64, p[1], "barrel[facing=up]");
        b.set(113, 65, 146, "barrel[facing=up]");
        cart(b, 116, 64, 144);
        b.set(62, 64, 147, "hay_block"); b.set(97, 64, 148, "chest[facing=north]");
    }

    /** A hand cart: two dark-oak wheels, a slab bed, fence handles and a load. */
    static void cart(B b, int x, int y, int z) {
        b.set(x, y, z, "spruce_slab[type=top]"); b.set(x + 1, y, z, "spruce_slab[type=top]");
        b.set(x, y, z - 1, "dark_oak_trapdoor[facing=north,open=true]"); b.set(x + 1, y, z + 1, "dark_oak_trapdoor[facing=south,open=true]");
        b.set(x, y, z + 1, "dark_oak_trapdoor[facing=south,open=true]"); b.set(x + 1, y, z - 1, "dark_oak_trapdoor[facing=north,open=true]");
        b.set(x - 1, y, z, "spruce_fence"); b.set(x, y + 1, z, "barrel[facing=up]"); b.set(x + 1, y + 1, z, "potted_fern".replace("potted_fern", "hay_block"));
    }

    static void piers(B b) {
        Pal deck = Pal.of("spruce_planks:5", "stripped_spruce_wood:1", "spruce_slab[type=top]:1".replace("spruce_slab[type=top]", "spruce_planks"));
        // main pier x 103..107, z 150..198 with a T-head
        for (int z = 150; z <= 199; z++) for (int x = 103; x <= 107; x++) b.wset(x, 63, z, deck.pick(x, 63, z));
        for (int z = 190; z <= 199; z++) for (int x = 96; x <= 114; x++) b.wset(x, 63, z, deck.pick(x, 63, z));
        for (int z = 152; z <= 199; z += 4) for (int x : new int[]{103, 107}) post(b, x, z);
        for (int x = 96; x <= 114; x += 6) { post(b, x, 190); post(b, x, 199); }
        for (int z = 150; z <= 189; z++) for (int x : new int[]{103, 107}) if (z % 4 != 0) b.wset(x, 64, z, B.s("spruce_fence"));
        for (int z = 150; z <= 189; z += 8) { b.wset(103, 64, z, B.s("stripped_spruce_log")); b.wset(103, 65, z, B.s("lantern")); b.wset(107, 64, z, B.s("stripped_spruce_log")); b.wset(107, 65, z, B.s("lantern")); }
        for (int x = 96; x <= 114; x++) { if (x % 4 != 0) b.wset(x, 64, 199, B.s("spruce_fence")); }
        for (int x : new int[]{96, 114}) for (int z = 190; z <= 199; z++) if (z % 3 != 0) b.wset(x, 64, z, B.s("spruce_fence"));
        b.wset(96, 64, 190, B.s("stripped_spruce_log")); b.wset(96, 65, 190, B.s("lantern")); b.wset(114, 64, 190, B.s("stripped_spruce_log")); b.wset(114, 65, 190, B.s("lantern"));
        b.wset(105, 64, 199, B.s("spruce_fence")); b.wset(104, 64, 199, B.s("air")); b.wset(106, 64, 199, B.s("air"));
        for (int y = 55; y <= 62; y++) b.wset(105, y, 200, B.s("ladder[facing=south]"));
        b.wset(105, 63, 200, B.s("air"));
        // bench at the end, crates and coiled ropes
        b.wset(104, 64, 197, B.s("spruce_stairs[facing=north]")); b.wset(105, 64, 197, B.s("spruce_stairs[facing=north]")); b.wset(106, 64, 197, B.s("spruce_stairs[facing=north]"));
        b.wset(98, 64, 192, B.s("barrel[facing=up]")); b.wset(99, 64, 192, B.s("barrel[facing=up]")); b.wset(98, 65, 192, B.s("barrel[facing=up]")); b.wset(112, 64, 193, B.s("chest[facing=west]"));
        b.wset(111, 64, 192, B.s("hay_block".replace("hay_block", "barrel[facing=up]")));
        // fishing pier x 78..80, z 150..172
        for (int z = 150; z <= 172; z++) for (int x = 78; x <= 80; x++) b.wset(x, 63, z, deck.pick(x, 63, z));
        for (int z = 152; z <= 172; z += 5) { post(b, 78, z); post(b, 80, z); }
        b.wset(80, 64, 172, B.s("stripped_spruce_log")); b.wset(80, 65, 172, B.s("lantern"));
        b.wset(78, 64, 170, B.s("barrel[facing=up]")); b.wset(79, 64, 171, B.s("spruce_stairs[facing=north]"));
        // moored boats
        b.boat(101, 63, 176, 0, BoatEntity.Type.OAK); b.boat(109, 63, 168, 180, BoatEntity.Type.SPRUCE);
        b.boat(76, 63, 163, 0, BoatEntity.Type.DARK_OAK); b.boat(82, 63, 158, 90, BoatEntity.Type.BIRCH);
        b.boat(95, 63, 194, 90, BoatEntity.Type.MANGROVE);
    }

    static void post(B b, int x, int z) {
        for (int y = 63; y >= 48; y--) {
            var s = b.wget(x, y, z);
            if (y < 63 && s.isSolid() && s.getFluidState().isEmpty()) break;
            b.wset(x, y, z, B.s("stripped_spruce_log"));
        }
        b.wset(x, 64, z, B.s("spruce_fence"));
    }

    static void harbormaster(B b) {
        Pal stone = Pal.of("stone_bricks:4", "cobblestone:2", "mossy_stone_bricks:1", "andesite:1");
        Pal planks = Pal.of("spruce_planks:5", "stripped_spruce_wood:1");
        b.foundation(0, 0, 12, 11, -1, stone);
        b.clear(0, 0, 0, 12, 18, 12);
        b.fill(0, -1, 0, 12, -1, 11, Pal.of("spruce_planks"));
        b.walls(0, 0, 0, 12, 3, 11, stone);
        Kit.timberWalls(b, 0, 0, 8, 11, 4, 7, "stripped_spruce_log", Pal.of("white_terracotta:3", "calcite:2"), 4);
        b.fill(0, 4, 0, 12, 4, 11, planks);
        b.fill(1, 4, 1, 7, 4, 10, Pal.of("spruce_planks"));
        Kit.gableX(b, 0, 0, 8, 11, 8, "spruce_stairs", "spruce_slab", Pal.of("white_terracotta"), true);
        // lookout tower (local x 9..12, z 0..3), stone, with a balcony and bell
        b.walls(9, 0, 0, 12, 13, 3, stone);
        b.clear(10, 0, 1, 11, 13, 2);
        for (int y = 0; y <= 13; y++) b.set(11, y, 1, "ladder[facing=west]".replace("west", "east"));
        b.fill(8, 14, -1, 13, 14, 4, Pal.of("spruce_planks"));
        for (int x = 8; x <= 13; x++) { b.set(x, 15, -1, "spruce_fence"); b.set(x, 15, 4, "spruce_fence"); }
        for (int z = -1; z <= 4; z++) { b.set(8, 15, z, "spruce_fence"); b.set(13, 15, z, "spruce_fence"); }
        b.set(11, 14, 1, "spruce_trapdoor[facing=north,half=top]");
        for (int[] c : new int[][]{{9, 0}, {12, 0}, {9, 3}, {12, 3}}) for (int y = 15; y <= 17; y++) b.set(c[0], y, c[1], "stripped_spruce_log");
        b.fill(8, 18, -1, 13, 18, 4, Pal.of("spruce_slab[type=bottom]"));
        b.fill(9, 19, 0, 12, 19, 3, Pal.of("spruce_slab[type=bottom]"));
        b.set(10, 17, 2, "bell[attachment=ceiling,facing=north]");
        b.set(11, 17, 1, "lantern[hanging=true]");
        for (int y = 5; y <= 12; y += 4) Kit.window(b, 12, y, 2, 2, EAST, "glass_pane", null, null);
        // door, windows
        Kit.door(b, 4, 0, 11, SOUTH, "spruce_door", false);
        b.set(3, 2, 12, "lantern[hanging=true]".replace("lantern[hanging=true]", "air"));
        b.sign(5, 2, 12, "spruce_wall_sign[facing=south]", "HARBOURMASTER", "T. Wake", "", "Knock loud");
        for (int x : new int[]{1, 2, 7}) Kit.window(b, x, 1, 11, 2, SOUTH, "glass_pane", "spruce_trapdoor", x == 7 ? "potted_dead_bush" : null);
        Kit.window(b, 0, 1, 5, 2, WEST, "glass_pane", "spruce_trapdoor", null);
        Kit.window(b, 3, 5, 11, 2, SOUTH, "glass_pane", "spruce_trapdoor", null); Kit.window(b, 6, 5, 11, 2, SOUTH, "glass_pane", "spruce_trapdoor", null);
        Kit.window(b, 4, 5, 0, 2, NORTH, "glass_pane", "spruce_trapdoor", null);
        Kit.chimney(b, 0, 8, 0, 12, Pal.of("cobblestone:2", "stone_bricks:1"));
        // office: chart table, desk, map frames, ship's wheel-ish decor
        b.set(4, 0, 4, "cartography_table"); b.set(6, 0, 2, "spruce_stairs[facing=south]");
        Kit.tableTop(b, 6, 0, 1, "spruce", "candle[candles=2,lit=true]");
        b.set(1, 0, 1, "chest[facing=east]"); b.set(1, 0, 2, "barrel[facing=east]"); b.set(1, 1, 1, "barrel[facing=up]");
        for (int z = 6; z <= 9; z++) b.set(1, 0, z, "bookshelf");
        b.frame(4, 2, 1, SOUTH, "minecraft:map"); b.frame(5, 2, 1, SOUTH, "minecraft:compass"); b.frame(3, 2, 1, SOUTH, "minecraft:clock");
        b.frame(8, 2, 5, WEST, "minecraft:nautilus_shell"); b.frame(8, 2, 7, WEST, "minecraft:fishing_rod");
        b.set(8, 0, 9, "water_cauldron[level=1]"); Kit.hangLantern(b, 4, 3, 6, 1, false); Kit.carpet(b, 3, 5, 6, 8, 0, "blue");
        b.fill(8, 0, 1, 8, 3, 10, Pal.of("spruce_planks")); Kit.door(b, 8, 0, 6, EAST, "spruce_door", false);
        // stairs up
        for (int i = 0; i < 4; i++) b.set(7, i, 9 - i, "spruce_stairs[facing=north]");
        b.clear(7, 4, 6, 7, 4, 8);
        // bedroom
        Kit.bed(b, 2, 5, 2, NORTH, "blue"); b.set(1, 5, 1, "chest[facing=east]"); b.set(3, 5, 1, "potted_cactus");
        b.set(5, 5, 1, "barrel[facing=up]"); b.set(5, 6, 1, "candle[candles=1,lit=true]"); Kit.carpet(b, 2, 4, 5, 8, 5, "light_gray");
        b.lectern(1, 5, 9, EAST, "logbook".replace("logbook", "lullaby").replace("lullaby", "lore_book_miller").replace("lore_book_miller", "lullaby"));
        b.set(1, 5, 9, "spruce_planks"); b.set(1, 6, 9, "potted_spruce_sapling");
        Kit.hangLantern(b, 4, 7, 5, 1, false);
        // outside: crates, nets, anchor
        b.set(-1, 0, 9, "barrel[facing=up]"); b.set(-1, 0, 10, "barrel[facing=up]"); b.set(-1, 1, 10, "barrel[facing=up]");
        b.set(9, 0, 12, "anvil[facing=east]"); b.set(10, 0, 12, "chain[axis=x]"); b.set(11, 0, 12, "chain[axis=x]");
    }

    static void boathouse(B b) {
        Pal wall = Pal.of("spruce_planks:6", "stripped_spruce_wood:1");
        // stilts and floor (deck at local y -1 => world 63)
        for (int x = 0; x <= 18; x++) for (int z = 0; z <= 21; z++) {
            boolean slip = x >= 6 && x <= 12 && z >= 3;
            if (!slip) b.set(x, -1, z, (x + z) % 7 == 0 ? "stripped_spruce_wood" : "spruce_planks");
            else if (b.get(x, -1, z).isSolid()) b.set(x, -1, z, "water");
        }
        for (int x = 0; x <= 18; x += 6) for (int z = 0; z <= 21; z += 7) b.column(x, -2, z, Pal.of("stripped_spruce_log"));
        b.column(18, -2, 21, Pal.of("stripped_spruce_log")); b.column(0, -2, 21, Pal.of("stripped_spruce_log"));
        // slipway water + rails
        for (int z = 3; z <= 21; z++) for (int x = 6; x <= 12; x++) { b.set(x, -1, z, "water"); b.set(x, -2, z, "water"); }
        for (int z = 3; z <= 18; z++) { b.set(7, -2, z, "stripped_spruce_wood"); b.set(11, -2, z, "stripped_spruce_wood"); }
        b.clear(0, 0, 0, 18, 13, 21);
        // walls (open sea door at z=21 between x 6..12)
        for (int y = 0; y <= 6; y++) for (int x = 0; x <= 18; x++) for (int z = 0; z <= 21; z++) {
            boolean edge = x == 0 || x == 18 || z == 0 || z == 21;
            if (!edge) continue;
            if (z == 21 && x >= 5 && x <= 13 && y <= 5) continue;
            if (z == 0 && x >= 8 && x <= 10 && y <= 2) continue;
            boolean post = (x % 6 == 0 && (z == 0 || z == 21)) || (z % 7 == 0 && (x == 0 || x == 18));
            b.set(x, y, z, post ? "stripped_spruce_log" : wall.pick(x, y, z).toString().isEmpty() ? "spruce_planks" : (y == 3 ? "stripped_spruce_wood" : "spruce_planks"));
        }
        for (int z = 2; z <= 19; z += 4) { Kit.window(b, 0, 2, z, 2, WEST, "glass_pane", null, null); Kit.window(b, 18, 2, z, 2, EAST, "glass_pane", null, null); }
        Kit.door(b, 8, 0, 0, NORTH, "spruce_door", false); Kit.door(b, 9, 0, 0, NORTH, "spruce_door", true); b.set(10, 0, 0, "spruce_planks"); b.set(10, 1, 0, "spruce_planks");
        b.sign(9, 3, -1, "spruce_wall_sign[facing=north]", "REYES", "Boatwright", "", "Repairs & Rigging");
        // loft over the side bays (x 13..17 and 1..5) at local y 5
        b.fill(13, 5, 1, 17, 5, 20, Pal.of("spruce_planks")); b.fill(1, 5, 1, 5, 5, 20, Pal.of("spruce_planks"));
        for (int z = 1; z <= 20; z++) { b.set(12, 6, z, "spruce_fence"); b.set(6, 6, z, "spruce_fence"); }
        for (int y = 0; y <= 5; y++) b.set(17, y, 10, "ladder[facing=west]");
        b.set(17, 5, 10, "air");
        // roof: tall gable along z
        Kit.gableZ(b, 0, 0, 18, 21, 7, "spruce_stairs", "spruce_slab", Pal.of("spruce_planks"), true);
        // the hull under construction on the slipway
        int[] beam = {0, 1, 2, 2, 3, 3, 3, 3, 3, 3, 2, 2, 1};
        for (int i = 0; i < beam.length; i++) {
            int z = 5 + i, half = beam[i];
            b.set(9, -1, z, "stripped_dark_oak_wood");             // keel
            for (int dx = -half; dx <= half; dx++) {
                if (dx == 0) continue;
                b.set(9 + dx, 0, z, Math.abs(dx) == half ? "dark_oak_planks" : "air");
            }
            if (i % 2 == 0) { b.set(9 - half, 1, z, "dark_oak_fence"); b.set(9 + half, 1, z, "dark_oak_fence"); }
            else { b.set(9 - half, 1, z, "dark_oak_slab[type=bottom]"); b.set(9 + half, 1, z, "dark_oak_slab[type=bottom]"); }
        }
        b.set(9, 0, 4, "dark_oak_stairs[facing=south]"); b.set(9, 1, 4, "dark_oak_fence"); b.set(9, 2, 4, "dark_oak_fence");
        for (int y = 0; y <= 5; y++) b.set(9, y, 10, y == 0 ? "stripped_dark_oak_log" : "dark_oak_fence");
        b.set(9, 5, 10, "air");
        // scaffold & tools
        for (int y = 0; y <= 2; y++) { b.set(5, y, 8, "scaffolding[distance=0,bottom=false]"); b.set(13, y, 14, "scaffolding[distance=0,bottom=false]"); }
        b.set(15, 0, 3, "crafting_table"); b.set(16, 0, 3, "stonecutter[facing=south]"); b.set(17, 0, 3, "smithing_table");
        b.set(15, 0, 5, "barrel[facing=up]"); b.set(16, 0, 5, "barrel[facing=up]"); b.set(17, 0, 5, "chest[facing=west]");
        b.set(1, 0, 1, "white_wool"); b.set(2, 0, 1, "white_wool"); b.set(1, 1, 1, "white_wool");
        b.set(1, 0, 18, "stripped_spruce_log[axis=z]"); b.set(1, 0, 19, "stripped_spruce_log[axis=z]"); b.set(2, 0, 18, "stripped_spruce_log[axis=z]");
        b.set(3, 0, 14, "chain[axis=y]".replace("chain[axis=y]", "barrel[facing=up]"));
        b.frame(17, 2, 7, WEST, "minecraft:iron_axe"); b.frame(17, 2, 8, WEST, "minecraft:shears"); b.frame(17, 2, 6, WEST, "minecraft:lead");
        for (int z = 4; z <= 18; z += 7) Kit.hangLantern(b, 9, 10, z, 3, false);
        // Sol's loft
        Kit.bed(b, 16, 6, 3, NORTH, "red"); b.set(14, 6, 1, "chest[facing=south]"); b.set(17, 6, 1, "barrel[facing=up]"); b.set(17, 7, 1, "potted_bamboo");
        Kit.carpet(b, 13, 4, 17, 8, 6, "orange"); b.set(13, 6, 12, "white_wool"); b.set(14, 6, 12, "white_carpet".replace("white_carpet", "white_wool"));
        b.frame(18, 7, 5, WEST, "fi:star_chart".replace("fi:star_chart", "minecraft:filled_map".replace("minecraft:filled_map", "minecraft:map")));
        b.set(16, 6, 16, "spruce_stairs[facing=east]"); Kit.tableTop(b, 15, 6, 16, "spruce", "candle[candles=3,lit=true]");
        // jetty along the boathouse west side
        for (int z = 1; z <= 21; z++) b.set(-1, -1, z, "spruce_planks");
    }

    static void fishMarket(B b) {
        T t = new T(64, 64, 132, 0);
        B m = new B(b.w, t);
        // open hall 0..13 x 0..12
        for (int x = 0; x <= 13; x++) for (int z = 0; z <= 12; z++) m.set(x, -1, z, (x + z) % 5 == 0 ? "andesite" : "stone_bricks");
        m.clear(0, 0, 0, 13, 9, 12);
        for (int x = 0; x <= 13; x += 13) for (int z = 0; z <= 12; z += 4) for (int y = 0; y <= 3; y++) m.set(x, y, z, "stripped_spruce_log");
        for (int z = 0; z <= 12; z += 12) for (int x = 0; x <= 13; x += 13) for (int y = 0; y <= 3; y++) m.set(x, y, z, "stripped_spruce_log");
        for (int x = 0; x <= 13; x++) { m.set(x, 4, 0, "stripped_spruce_log[axis=x]"); m.set(x, 4, 12, "stripped_spruce_log[axis=x]"); }
        Kit.gableX(m, 0, 0, 13, 12, 5, "spruce_stairs", "spruce_slab", Pal.of("spruce_planks"), true);
        // stalls: counters with fish, ice, barrels
        for (int x = 2; x <= 11; x += 3) {
            m.set(x, 0, 3, "stripped_spruce_wood"); m.set(x + 1, 0, 3, "stripped_spruce_wood");
            m.set(x, 1, 3, "blue_ice".replace("blue_ice", "packed_ice")); m.set(x + 1, 1, 3, "barrel[facing=up]");
            m.frame(x, 2, 3, SOUTH, x == 2 ? "minecraft:cod" : x == 5 ? "minecraft:salmon" : x == 8 ? "minecraft:tropical_fish" : "minecraft:pufferfish");
            m.set(x, 0, 9, "barrel[facing=up]"); m.set(x + 1, 0, 9, "barrel[facing=east]");
        }
        m.set(12, 0, 6, "water_cauldron[level=3]"); m.set(1, 0, 6, "composter");
        Kit.hangLantern(m, 4, 4, 6, 1, false); Kit.hangLantern(m, 9, 4, 6, 1, false);
        m.sign(6, 3, -1, "spruce_wall_sign[facing=north]", "FISH MARKET", "", "closed until", "the boats return");
        // smokehouse
        B s = m.sub(-7, 0, 1, 0);
        s.foundation(0, 0, 5, 6, -1, Pal.of("cobblestone"));
        s.walls(0, 0, 0, 5, 3, 6, Pal.of("cobblestone:3", "mossy_cobblestone:2", "stone:1"));
        s.clear(1, 0, 1, 4, 4, 5);
        Kit.gableX(s, 0, 0, 5, 6, 4, "cobblestone_stairs", "cobblestone_slab", Pal.of("cobblestone"), false);
        s.set(2, 0, 3, "campfire[lit=true,signal_fire=false]"); s.set(3, 0, 3, "campfire[lit=true,signal_fire=false]");
        for (int x = 1; x <= 4; x++) s.set(x, 3, 3, "spruce_fence");
        Kit.door(s, 2, 0, 6, SOUTH, "spruce_door", false);
        s.set(3, 6, 3, "campfire[lit=true,signal_fire=false]"); s.set(3, 5, 3, "cobblestone");
    }

    static void fisherCottage(B b) {
        Pal stone = Pal.of("cobblestone:3", "mossy_cobblestone:2", "stone:1");
        b.foundation(0, 0, 8, 7, -1, stone);
        b.clear(0, 0, 0, 8, 9, 7);
        b.fill(0, -1, 0, 8, -1, 7, Pal.of("spruce_planks"));
        b.walls(0, 0, 0, 8, 3, 7, Pal.of("spruce_planks:3", "stripped_spruce_wood:1"));
        for (int[] c : new int[][]{{0, 0}, {8, 0}, {0, 7}, {8, 7}}) for (int y = 0; y <= 3; y++) b.set(c[0], y, c[1], "stripped_spruce_log");
        b.walls(0, 0, 0, 8, 0, 7, stone);
        Kit.gableX(b, 0, 0, 8, 7, 4, "mossy_cobblestone_stairs".replace("mossy_", "mossy_"), "mossy_cobblestone_slab", Pal.of("spruce_planks"), true);
        Kit.door(b, 4, 0, 7, SOUTH, "spruce_door", false);
        Kit.window(b, 2, 1, 7, 2, SOUTH, "glass_pane", "spruce_trapdoor", "potted_dead_bush");
        Kit.window(b, 6, 1, 7, 2, SOUTH, "glass_pane", "spruce_trapdoor", null);
        Kit.window(b, 0, 1, 3, 2, WEST, "glass_pane", null, null);
        Kit.bed(b, 7, 0, 1, EAST, "gray"); b.set(1, 0, 1, "barrel[facing=up]"); Kit.tableTop(b, 3, 0, 3, "spruce", "flower_pot");
        b.set(7, 2, 6, "cobweb"); b.set(1, 3, 6, "cobweb"); b.set(5, 0, 1, "chest[facing=south]");
        b.lectern(1, 0, 5, EAST, "lore_book_miller".replace("lore_book_miller", "lore_book_miller"));
        b.set(1, 0, 5, "spruce_planks"); b.sign(1, 1, 5, "spruce_sign[rotation=4]", "Gone out past", "the fog to fetch", "help. Back by", "spring. -The Hales");
        Kit.chimney(b, 8, 4, 0, 8, Pal.of("cobblestone"));
        b.set(8, 9, 4, "cobblestone");
        // drying rack with nets
        for (int x = 1; x <= 6; x++) b.set(x, 0, -2, x == 1 || x == 6 ? "spruce_fence" : "air");
        b.set(1, 1, -2, "spruce_fence"); b.set(6, 1, -2, "spruce_fence");
        for (int x = 2; x <= 5; x++) b.set(x, 1, -2, "tripwire[attached=true]".replace("tripwire[attached=true]", "chain[axis=x]"));
    }

    static void warehouse(B b) {
        Pal stone = Pal.of("stone_bricks:3", "cobblestone:2", "mossy_cobblestone:1", "andesite:1");
        b.foundation(0, 0, 15, 13, -1, stone);
        b.clear(0, 0, 0, 15, 14, 13);
        b.fill(0, -1, 0, 15, -1, 13, Pal.of("spruce_planks:3", "stone_bricks:1"));
        b.walls(0, 0, 0, 15, 3, 13, stone);
        Kit.timberWalls(b, 0, 0, 15, 13, 4, 7, "stripped_oak_log", Pal.of("oak_planks:4", "stripped_oak_wood:1"), 5);
        b.fill(1, 4, 1, 14, 4, 12, Pal.of("oak_planks"));
        b.clear(3, 4, 3, 5, 4, 5);
        Kit.gableX(b, 0, 0, 15, 13, 8, "dark_oak_stairs", "dark_oak_slab", Pal.of("oak_planks"), true);
        // big doors facing the quay
        b.clear(6, 0, 13, 9, 2, 13);
        Kit.door(b, 6, 0, 13, SOUTH, "spruce_door", false); Kit.door(b, 9, 0, 13, SOUTH, "spruce_door", true);
        b.set(7, 0, 13, "air"); b.set(8, 0, 13, "air"); b.set(7, 1, 13, "air"); b.set(8, 1, 13, "air");
        b.fill(6, 3, 13, 9, 3, 13, Pal.of("stripped_spruce_log[axis=x]"));
        b.sign(7, 5, 14, "oak_wall_sign[facing=south]", "GULLHAVEN", "STORES", "", "est. long ago");
        for (int z : new int[]{3, 9}) { Kit.window(b, 0, 1, z, 2, WEST, "glass_pane", null, null); Kit.window(b, 15, 1, z, 2, EAST, "glass_pane", null, null); }
        Kit.window(b, 3, 5, 13, 2, SOUTH, "glass_pane", null, null); Kit.window(b, 12, 5, 13, 2, SOUTH, "glass_pane", null, null);
        // crates
        for (int x = 1; x <= 4; x++) for (int z = 1; z <= 3; z++) for (int y = 0; y <= (x + z) % 3; y++) b.set(x, y, z, (x + z + y) % 2 == 0 ? "barrel[facing=up]" : "spruce_planks");
        for (int x = 11; x <= 14; x++) for (int z = 1; z <= 4; z++) for (int y = 0; y <= (x * z) % 3; y++) b.set(x, y, z, (x + y) % 3 == 0 ? "chest[facing=south]" : "barrel[facing=north]");
        for (int z = 7; z <= 11; z++) { b.set(1, 0, z, "hay_block"); b.set(14, 0, z, z % 2 == 0 ? "barrel[facing=west]" : "white_wool"); }
        b.set(14, 1, 8, "white_wool");
        for (int y = 0; y <= 3; y++) b.set(3, y, 3, "ladder[facing=south]".replace("south", "north"));
        b.set(3, 4, 3, "air");
        Kit.hangLantern(b, 7, 3, 6, 0, false); Kit.hangLantern(b, 7, 7, 8, 1, false);
        // the quay crane
        B c = b.sub(8, 0, 19, 0);
        for (int y = 0; y <= 6; y++) c.set(0, y, 0, "stripped_spruce_log");
        for (int z = -2; z <= 4; z++) c.set(0, 7, z, "stripped_spruce_log[axis=z]");
        c.set(0, 6, -1, "spruce_stairs[facing=south,half=top]"); c.set(0, 6, 1, "spruce_stairs[facing=north,half=top]");
        for (int y = 4; y <= 6; y++) c.set(0, y, 4, "chain[axis=y]");
        c.set(0, 3, 4, "barrel[facing=up]");
        c.set(1, 0, 0, "spruce_trapdoor[facing=east,open=true]"); c.set(-1, 0, 0, "spruce_trapdoor[facing=west,open=true]");
    }

    static void harborLight(B b) {
        Pal p = Pal.of("stone_bricks:4", "mossy_stone_bricks:2", "cobblestone:1");
        int y = 63;
        for (int x = 128; x <= 152; x++) for (int z = 183; z <= 185; z++) {
            for (int yy = 50; yy <= y; yy++) if (!b.wget(x, yy, z).isSolid() || b.wget(x, yy, z).getFluidState().isStill()) b.wset(x, yy, z, p.pick(x, yy, z));
        }
        for (int x = 128; x <= 152; x += 4) b.wset(x, 64, 183, B.s("stone_brick_wall")); 
        for (int x = 124; x <= 128; x++) for (int z = 182; z <= 186; z++) for (int yy = 50; yy <= 70; yy++) {
            boolean edge = x == 124 || x == 128 || z == 182 || z == 186;
            if (yy <= 63 || edge) b.wset(x, yy, z, p.pick(x, yy, z)); else b.wset(x, yy, z, B.s("air"));
        }
        b.wset(128, 64, 184, B.s("air")); b.wset(128, 65, 184, B.s("air"));
        for (int yy = 64; yy <= 70; yy++) b.wset(126, yy, 184, B.s("ladder[facing=east]".replace("east", "east")));
        b.wset(125, 64, 184, B.s("stone_bricks"));
        for (int yy = 64; yy <= 70; yy++) b.wset(125, yy, 184, B.s("stone_bricks"));
        for (int x = 124; x <= 128; x++) for (int z = 182; z <= 186; z++) b.wset(x, 71, z, B.s("stone_brick_slab[type=bottom]"));
        b.wset(126, 71, 184, B.s("spruce_trapdoor[half=top]"));
        b.wset(126, 72, 183, B.s("glass")); b.wset(126, 72, 185, B.s("glass")); b.wset(125, 72, 184, B.s("glass")); b.wset(127, 72, 184, B.s("glass"));
        b.wset(126, 72, 184, B.s("glowstone".replace("glowstone", "copper_bulb[lit=true]")));
        b.wset(126, 73, 184, B.s("waxed_cut_copper_slab[type=bottom]"));
        b.wset(125, 72, 183, B.s("waxed_copper_grate")); b.wset(127, 72, 185, B.s("waxed_copper_grate")); b.wset(125, 72, 185, B.s("waxed_copper_grate")); b.wset(127, 72, 183, B.s("waxed_copper_grate"));
    }

    static void gullRock(B b) {
        // a spiral of ledges up the sea stack, with nesting straw and the secret sea-lily
        int cx = 186, cz = 218;
        for (int i = 0; i < 64; i++) {
            double a = i * 0.32;
            int r = 9;
            int x = cx + (int) Math.round(Math.cos(a) * r), z = cz + (int) Math.round(Math.sin(a) * (r - 1));
            int y = 63 + i / 3;
            if (y > 84) break;
            b.wset(x, y, z, Pal.of("stone:3", "andesite:2", "mossy_cobblestone:1").pick(x, y, z));
            b.wset(x, y + 1, z, B.s("air")); b.wset(x, y + 2, z, B.s("air"));
        }
        b.wset(186, 85, 218, B.s("hay_block")); b.wset(187, 85, 218, B.s("hay_block"));
        b.wset(185, 85, 219, B.s("lily_of_the_valley"));
        b.wset(185, 84, 219, B.s("grass_block"));
        new B(b.w, 0, 0, 0, 0).container(187, 85, 217, "barrel[facing=up]", "fi:sea_lily", "minecraft:feather*6");
    }

    static void smugglersCache(B b) {
        // beneath the fishing pier: a cleft in the quay wall, reached from the water at low level
        for (int x = 81; x <= 85; x++) for (int z = 156; z <= 161; z++) for (int y = 55; y <= 59; y++) b.wset(x, y, z, B.s("air"));
        Pal rock = Pal.of("stone:3", "mossy_cobblestone:2", "gravel:1");
        for (int x = 80; x <= 86; x++) for (int z = 155; z <= 162; z++) { b.wset(x, 54, z, rock.pick(x, 54, z)); b.wset(x, 60, z, rock.pick(x, 60, z)); }
        for (int x = 80; x <= 86; x++) for (int y = 54; y <= 60; y++) { b.wset(x, y, 162, rock.pick(x, y, 162)); }
        for (int z = 155; z <= 162; z++) for (int y = 54; y <= 60; y++) { b.wset(86, y, z, rock.pick(86, y, z)); }
        for (int x = 80; x <= 86; x++) for (int y = 54; y <= 60; y++) b.wset(x, y, 155, rock.pick(x, y, 155));
        for (int z = 155; z <= 162; z++) for (int y = 54; y <= 60; y++) b.wset(80, y, z, rock.pick(80, y, z));
        b.wset(80, 55, 158, B.s("water")); b.wset(80, 56, 158, B.s("water"));
        new B(b.w, 0, 0, 0, 0).container(84, 55, 160, "chest[facing=west]", "minecraft:gold_ingot*4", "fi:emeralds", "minecraft:rum".replace("minecraft:rum", "minecraft:honey_bottle"), "fi:diver_helm");
        b.wset(82, 55, 160, B.s("barrel[facing=up]")); b.wset(82, 56, 160, B.s("lantern"));
        b.wset(85, 55, 157, B.s("barrel[facing=up]")); b.wset(83, 59, 161, B.s("cobweb"));
        // hint: a chain hangs from the fishing pier down to the hidden entrance
        for (int y = 56; y <= 62; y++) b.wset(79, y, 158, B.s("chain[axis=y]"));
    }

}
