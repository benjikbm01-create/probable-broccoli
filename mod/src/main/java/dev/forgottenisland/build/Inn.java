package dev.forgottenisland.build;

import dev.forgottenisland.game.Interactions;
import dev.forgottenisland.game.Gates;
import dev.forgottenisland.game.Secrets;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Direction;

import static net.minecraft.util.math.Direction.*;

/**
 * The Gull &amp; Lantern: Gullhaven's inn. Stone ground floor (tavern, kitchen, keeper's room), a jettied
 * timber upper floor with guest rooms, a cellar with a hidden room. Entrance faces the market square (south).
 */
public final class Inn implements Layout.Part {
    static final T t = new T(86, 65, 84, 0);   // local x 0..20, z 0..16, floor y 0
    public String name() { return "inn"; }

    public void places() {
        t.pad(-3, -14, 23, 19, 0, 6);
        t.place("hester_bar", 12, 0, 6, 90);
        t.place("hester_kitchen", 16, 0, 6, 180);
        t.place("hester_bed", 19, 0, 13, 0);         // bed head block
        t.place("hester_tea", 9, 0.5, 4, 90);        // chair at T2 facing west
        t.place("ione_tea", 7, 0.5, 4, -90);
        t.place("wren_inn", 7, 0.5, 12, -90);
        t.place("sol_inn", 9, 0.5, 12, 90);
        t.place("tobias_inn", 3, 0.5, 4, -90);
        t.place("corin_inn", 6, 0.5, 9, 90);
        t.place("inn_hearth", 7, 0, 3, 180);
        t.place("inn_door", 6, 0, 18, 0);
        t.place("inn_garden", 10, 0, -8, 180);
        t.place("cp_inn", 4, 6, 4, 0);               // the player's rented room upstairs
        t.place("player_bed", 2, 6, 2, 0);
        t.zone("inn_cellar", 15, -5, 1, 19, -2, 7, null);
        t.zone("secret_inn_cellar", 15, -5, -4, 19, -2, -1, p -> Secrets.discover(p, "inn_cellar"));
        // hidden shelf in the cellar slides away when the button on the barrel is pressed
        var g = Gates.add("inn_cellar_shelf", SoundEvents.BLOCK_WOODEN_DOOR_OPEN);
        for (int y = -4; y <= -3; y++) for (int x = 16; x <= 17; x++) g.closed().put(t.pos(x, y, 0), B.s("bookshelf"));
        Interactions.lever(t.pos(19, -3, 1), "inn_cellar_shelf", null);
    }

    public void build(ServerWorld w) {
        B b = new B(w, t);
        Pal stone = Pal.of("cobblestone:4", "stone_bricks:4", "mossy_cobblestone:2", "andesite:2", "mossy_stone_bricks:1");
        Pal plaster = Pal.of("calcite:8", "diorite:1", "white_terracotta:1");
        Pal floor = Pal.of("spruce_planks:5", "stripped_spruce_wood:1");
        Pal oak = Pal.of("dark_oak_planks");
        String log = "stripped_dark_oak_log";
        // ground & foundations
        b.foundation(-1, -1, 21, 17, -2, stone);
        b.fill(0, -1, 0, 20, -1, 16, floor);
        b.fill(15, -1, 1, 19, -1, 7, Pal.of("stone_bricks:3", "polished_andesite:1"));
        b.clear(0, 0, 0, 20, 16, 18);
        // ---- ground floor walls (stone) y0..4
        b.walls(0, 0, 0, 20, 4, 16, stone);
        for (int y = 0; y <= 4; y++) for (int[] c : new int[][]{{0, 0}, {20, 0}, {0, 16}, {20, 16}}) b.set(c[0], y, c[1], "stone_bricks");
        // partitions
        b.fill(14, 0, 1, 14, 4, 15, Pal.of("spruce_planks"));
        b.fill(15, 0, 8, 19, 4, 8, Pal.of("spruce_planks"));
        Kit.door(b, 14, 0, 2, EAST, "spruce_door", false);
        Kit.door(b, 14, 0, 13, EAST, "spruce_door", true);
        // ceiling / upper floor y5 with exposed beams
        b.fill(0, 5, 0, 20, 5, 17, oak);
        for (int x = 2; x <= 18; x += 4) b.fill(x, 4, 1, x, 4, 15, log + "[axis=z]");
        // front door (double) + porch
        Kit.door(b, 6, 0, 16, SOUTH, "spruce_door", false);
        Kit.door(b, 7, 0, 16, SOUTH, "spruce_door", true);
        b.set(6, 2, 16, "spruce_planks"); b.set(7, 2, 16, "spruce_planks");
        b.fill(5, -1, 17, 8, -1, 18, Pal.of("stone_bricks"));
        b.set(5, -1, 19, "stone_brick_stairs[facing=north]"); b.set(6, -1, 19, "stone_brick_stairs[facing=north]");
        b.set(7, -1, 19, "stone_brick_stairs[facing=north]"); b.set(8, -1, 19, "stone_brick_stairs[facing=north]");
        // ground floor windows
        for (int x : new int[]{2, 3, 10, 11}) Kit.window(b, x, 1, 16, 2, SOUTH, "glass_pane", null, null);
        for (int x : new int[]{2, 10}) { b.set(x, 0, 17, "spruce_trapdoor[facing=south,open=true]"); b.set(x + 1, 0, 17, "spruce_trapdoor[facing=south,open=true]"); b.set(x, 1, 17, "potted_red_tulip"); b.set(x + 1, 1, 17, "potted_azure_bluet"); }
        b.set(2, 0, 17, "air"); b.set(3, 0, 17, "air"); b.set(10, 0, 17, "air"); b.set(11, 0, 17, "air");
        for (int x : new int[]{2, 3, 10, 11}) { b.set(x, 0, 17, "spruce_trapdoor[facing=south,open=true,half=top]"); b.set(x, 1, 17, x % 2 == 0 ? "potted_red_tulip" : "potted_azure_bluet"); }
        for (int x : new int[]{16, 17}) Kit.window(b, x, 1, 16, 2, SOUTH, "glass_pane", null, null);
        for (int z : new int[]{5, 6, 11, 12}) Kit.window(b, 0, 1, z, 2, WEST, "glass_pane", null, null);
        for (int z : new int[]{3, 12}) Kit.window(b, 20, 1, z, 2, EAST, "glass_pane", null, null);
        Kit.window(b, 4, 1, 0, 2, NORTH, "glass_pane", null, null);
        Kit.door(b, 10, 0, 0, NORTH, "spruce_door", false);   // back door to the garden
        // ---- upper floor: jettied timber frame y6..10, z0..17
        b.fill(-1, 5, 17, 21, 5, 17, oak);
        for (int x = 0; x <= 20; x += 4) b.set(x, 4, 17, "dark_oak_stairs[facing=north,half=top]");
        Kit.timberWalls(b, 0, 0, 20, 17, 6, 10, log, plaster, 4);
        for (int x = 1; x <= 19; x += 4) { Kit.brace(b, x, 9, 17, "dark_oak_stairs", EAST); Kit.brace(b, x + 2, 9, 17, "dark_oak_stairs", WEST); }
        for (int x : new int[]{2, 6, 10, 14, 18}) Kit.window(b, x, 7, 17, 2, SOUTH, "glass_pane", "dark_oak_trapdoor", x == 6 || x == 14 ? "potted_cornflower" : null);
        for (int x : new int[]{3, 9, 15}) Kit.window(b, x, 7, 0, 2, NORTH, "glass_pane", "dark_oak_trapdoor", null);
        Kit.window(b, 0, 7, 5, 2, WEST, "glass_pane", "dark_oak_trapdoor", null); Kit.window(b, 0, 7, 13, 2, WEST, "glass_pane", "dark_oak_trapdoor", null);
        Kit.window(b, 20, 7, 5, 2, EAST, "glass_pane", "dark_oak_trapdoor", null); Kit.window(b, 20, 7, 13, 2, EAST, "glass_pane", "dark_oak_trapdoor", null);
        // ---- roof (ridge along x), with gable fill
        Kit.gableX(b, 0, 0, 20, 17, 11, "dark_oak_stairs", "dark_oak_slab", plaster, true);
        // dormer
        b.fill(9, 11, 18, 11, 13, 18, Pal.of("air"));
        // chimneys
        Kit.chimney(b, 7, 0, -1, 15, Pal.of("bricks:5", "stone_bricks:2"));
        Kit.chimney(b, 20, 8, 0, 16, Pal.of("stone_bricks:3", "cobblestone:2", "mossy_stone_bricks:1"));
        // ---- tavern interior
        b.set(7, 0, 1, "campfire[lit=true,signal_fire=false]");
        b.set(6, 0, 1, "bricks"); b.set(8, 0, 1, "bricks"); b.set(6, 1, 1, "bricks"); b.set(8, 1, 1, "bricks");
        b.set(6, 2, 1, "brick_stairs[facing=east,half=top]"); b.set(7, 2, 1, "bricks"); b.set(8, 2, 1, "brick_stairs[facing=west,half=top]");
        b.set(7, 3, 1, "bricks"); b.set(6, 3, 1, "candle[candles=2,lit=true]"); b.set(8, 3, 1, "candle[candles=3,lit=true]");
        b.set(7, 1, 1, "air");
        b.frame(7, 3, 2, SOUTH, "fi:owl_whistle".replace("fi:owl_whistle", "minecraft:goat_horn"));
        // bar counter
        for (int z = 3; z <= 11; z++) b.set(11, 0, z, z == 7 ? "spruce_fence_gate[facing=east]" : "stripped_spruce_wood");
        for (int z = 3; z <= 11; z++) if (z != 7) b.set(11, 1, z, z % 3 == 0 ? "candle[lit=true]" : "air");
        for (int z = 3; z <= 11; z++) { b.set(13, 0, z, z % 2 == 0 ? "barrel[facing=up]" : "barrel[facing=west]"); b.set(13, 1, z, "spruce_slab[type=bottom]"); }
        b.set(13, 2, 4, "decorated_pot"); b.set(13, 2, 9, "decorated_pot");
        b.set(12, 2, 3, "lantern"); b.set(12, 0, 11, "brewing_stand");
        b.frame(13, 2, 6, WEST, "minecraft:potion"); b.frame(13, 2, 8, WEST, "minecraft:honey_bottle");
        // tables & chairs
        int[][] tables = {{4, 4}, {8, 4}, {5, 9}, {8, 12}};
        for (int[] tb : tables) {
            Kit.tableTop(b, tb[0], 0, tb[1], "spruce", tb[0] == 8 && tb[1] == 12 ? "candle[candles=2,lit=true]" : "spruce_pressure_plate");
            Kit.chair(b, tb[0] - 1, 0, tb[1], EAST, "spruce_stairs");
            Kit.chair(b, tb[0] + 1, 0, tb[1], WEST, "spruce_stairs");
        }
        b.set(4, 1, 4, "candle[candles=1,lit=true]");
        Kit.carpet(b, 3, 7, 9, 10, 0, "red");
        Kit.hangLantern(b, 5, 3, 6, 1, false); Kit.hangLantern(b, 9, 3, 11, 1, false); Kit.hangLantern(b, 4, 3, 12, 1, false);
        b.set(1, 0, 1, "barrel[facing=up]"); b.set(1, 1, 1, "barrel[facing=up]"); b.set(2, 0, 1, "barrel[facing=north]");
        b.set(12, 0, 15, "barrel[facing=up]"); b.set(13, 0, 15, "barrel[facing=up]"); b.set(13, 1, 15, "barrel[facing=up]");
        b.lectern(10, 0, 14, SOUTH, "lullaby");
        b.sign(12, 1, 14, "spruce_wall_sign[facing=north]", "Rooms: ask", "Hester", "", "Mind the step");
        // staircase along the west wall rising north
        for (int i = 0; i <= 5; i++) b.set(1, i, 15 - i, "spruce_stairs[facing=north]");
        for (int i = 0; i <= 4; i++) b.set(1, i - 1, 15 - i, "spruce_planks");
        b.clear(1, 5, 11, 1, 5, 15);
        for (int z = 11; z <= 15; z++) b.set(2, 6, z, "spruce_fence");
        b.set(2, 4, 15, "air");
        for (int i = 0; i <= 4; i++) b.set(2, i, 15 - i, i < 4 ? "air" : "air");
        // ---- kitchen
        b.set(15, 0, 1, "smoker[facing=south]"); b.set(16, 0, 1, "furnace[facing=south,lit=true]"); b.set(17, 0, 1, "water_cauldron[level=3]");
        b.set(18, 0, 1, "crafting_table"); b.set(19, 0, 1, "barrel[facing=up]"); b.set(19, 0, 2, "barrel[facing=west]");
        b.set(15, 1, 1, "spruce_trapdoor[facing=south,half=top,open=false]");
        Kit.tableTop(b, 16, 0, 4, "spruce", "spruce_pressure_plate"); b.set(17, 1, 4, "air");
        b.set(17, 0, 4, "spruce_fence"); b.set(17, 1, 4, "cake");
        b.set(15, 2, 1, "flower_pot"); b.set(19, 1, 1, "potted_fern");
        Kit.hangLantern(b, 17, 3, 3, 1, false);
        b.frame(15, 2, 3, EAST, "minecraft:cooked_cod"); b.frame(15, 2, 5, EAST, "minecraft:bread");
        // cellar trapdoor + ladder
        b.set(18, -1, 6, "spruce_trapdoor[facing=south,half=top,open=false]");
        for (int y = -4; y <= -2; y++) b.set(18, y, 7, "ladder[facing=north]");
        b.set(18, -1, 7, "stone_bricks");
        // ---- keeper's room
        Kit.bed(b, 19, 0, 13, EAST, "red");
        b.set(19, 0, 15, "chest[facing=west]"); b.set(15, 0, 15, "barrel[facing=up]"); b.set(15, 1, 15, "potted_lily_of_the_valley");
        b.set(19, 0, 10, "spruce_trapdoor[facing=west,open=true]"); b.set(18, 0, 10, "potted_white_tulip");
        Kit.carpet(b, 16, 11, 18, 14, 0, "green");
        b.lectern(15, 0, 10, EAST, "letter".equals("") ? "letter" : "lore_book_inn".replace("lore_book_inn", "lullaby"));
        b.set(15, 0, 10, "spruce_planks"); b.set(15, 1, 10, "candle[candles=3,lit=true]");
        Kit.hangLantern(b, 17, 3, 12, 1, false);
        // ---- upper floor rooms
        b.fill(1, 6, 9, 19, 9, 10, Pal.of("air"));
        b.fill(1, 6, 8, 19, 9, 8, Pal.of("spruce_planks"));
        b.fill(1, 6, 11, 19, 9, 11, Pal.of("spruce_planks"));
        b.fill(7, 6, 1, 7, 9, 7, Pal.of("spruce_planks")); b.fill(14, 6, 1, 14, 9, 7, Pal.of("spruce_planks"));
        b.fill(10, 6, 12, 10, 9, 16, Pal.of("spruce_planks"));
        Kit.door(b, 4, 6, 8, NORTH, "spruce_door", false); Kit.door(b, 10, 6, 8, NORTH, "spruce_door", false); Kit.door(b, 16, 6, 8, NORTH, "spruce_door", false);
        Kit.door(b, 5, 6, 11, SOUTH, "spruce_door", false); Kit.door(b, 15, 6, 11, SOUTH, "spruce_door", false);
        b.clear(1, 6, 9, 1, 9, 10);
        // player's room
        Kit.bed(b, 2, 6, 2, NORTH, "light_blue");
        b.set(1, 6, 1, "barrel[facing=up]"); b.set(1, 7, 1, "potted_blue_orchid");
        b.set(6, 6, 1, "chest[facing=south]");
        Kit.tableTop(b, 5, 6, 3, "spruce", "candle[candles=1,lit=true]");
        b.set(5, 6, 4, "spruce_stairs[facing=south]");
        Kit.carpet(b, 2, 4, 4, 6, 6, "light_blue");
        Kit.hangLantern(b, 4, 9, 4, 1, false);
        // room 2: a guest room kept ready
        Kit.bed(b, 12, 6, 2, NORTH, "brown"); Kit.bed(b, 9, 6, 2, NORTH, "brown");
        b.set(11, 6, 1, "barrel[facing=up]"); b.set(8, 6, 7, "chest[facing=east]"); Kit.carpet(b, 8, 4, 13, 6, 6, "brown");
        // room 3: linen store
        for (int z = 1; z <= 6; z += 2) { b.set(19, 6, z, "white_wool"); b.set(19, 7, z, "barrel[facing=west]"); }
        b.set(15, 6, 1, "chest[facing=south]"); b.set(17, 6, 1, "loom[facing=south]");
        // south rooms: dusty empty room + a sitting room with a view of the square
        Kit.bed(b, 2, 6, 15, WEST, "gray"); b.set(8, 6, 16, "cobweb"); b.set(1, 9, 16, "cobweb"); b.set(6, 6, 16, "barrel[facing=up]");
        b.set(18, 6, 14, "spruce_stairs[facing=east]"); b.set(18, 6, 16, "spruce_stairs[facing=east]"); Kit.tableTop(b, 18, 6, 15, "spruce", "potted_azalea_bush");
        b.set(12, 6, 16, "bookshelf"); b.set(13, 6, 16, "bookshelf"); b.set(12, 7, 16, "bookshelf"); Kit.carpet(b, 12, 12, 17, 15, 6, "yellow");
        Kit.hangLantern(b, 15, 9, 14, 1, false); Kit.hangLantern(b, 10, 9, 9, 1, false); Kit.hangLantern(b, 4, 9, 13, 1, false);
        // ---- cellar (y -5 floor, -4..-2 room) and hidden room behind the shelf
        b.fill(14, -6, -5, 20, -1, 8, Pal.of("stone_bricks:3", "cobblestone:2", "mossy_cobblestone:1"));
        b.clear(15, -4, 1, 19, -2, 7);
        b.clear(15, -4, -4, 19, -2, -1);
        b.fill(15, -5, -4, 19, -5, 7, Pal.of("cobblestone:2", "gravel:1", "stone:2"));
        b.set(18, -1, 7, "stone_bricks");
        for (int z = 2; z <= 6; z += 2) { b.set(15, -4, z, "barrel[facing=east]"); b.set(15, -3, z, "barrel[facing=east]"); }
        for (int x = 16; x <= 19; x++) b.set(x, -4, 1, x % 2 == 0 ? "barrel[facing=south]" : "barrel[facing=up]");
        b.set(18, -3, 1, "barrel[facing=up]"); b.set(19, -3, 1, "barrel[facing=south]");
        b.set(16, -4, 0, "bookshelf"); b.set(17, -4, 0, "bookshelf"); b.set(16, -3, 0, "bookshelf"); b.set(17, -3, 0, "bookshelf");
        b.set(19, -3, 1, "barrel[facing=up]");
        b.set(19, -3, 0, "stone_bricks");
        b.set(19, -3, 1, "barrel[facing=south]");
        b.set(19, -2, 1, "oak_button[face=floor,facing=south]");
        b.set(17, -2, 5, "candle[candles=2,lit=true]".replace("candle", "candle")); b.set(17, -3, 5, "barrel[facing=up]");
        b.set(19, -2, 7, "cobweb"); b.set(15, -2, 1, "cobweb");
        Kit.hangLantern(b, 17, -2, 4, 0, false);
        // hidden room
        b.set(16, -4, -2, "chest[facing=east]");
        b.container(16, -4, -2, "chest[facing=east]", "fi:lore_book_inn", "fi:emeralds", "minecraft:golden_apple", "fi:healing");
        b.set(18, -4, -3, "candle[candles=4,lit=true]"); b.set(19, -4, -4, "barrel[facing=up]");
        b.set(15, -2, -4, "cobweb"); b.set(19, -2, -1, "cobweb");
        Gates.close(b.server, "inn_cellar_shelf");
        // ---- exterior dressing
        b.sign(6, 3, 17, "spruce_hanging_sign[rotation=0]".replace("spruce_hanging_sign[rotation=0]", "spruce_wall_hanging_sign[facing=east]"), "The Gull", "& Lantern", "", "Rooms - Ale");
        b.set(5, 4, 18, "lantern[hanging=true]"); b.set(9, 4, 18, "lantern[hanging=true]");
        b.set(5, 4, 17, "air"); b.set(9, 4, 17, "air");
        b.set(5, 4, 18, "air"); b.set(9, 4, 18, "air");
        b.set(5, 4, 17, "lantern[hanging=true]"); b.set(9, 4, 17, "lantern[hanging=true]");
        // benches
        for (int x : new int[]{1, 2, 12, 13}) b.set(x, 0, 18, "spruce_stairs[facing=north]");
        b.set(0, 0, 18, "spruce_trapdoor[facing=west,open=true]"); b.set(3, 0, 18, "spruce_trapdoor[facing=east,open=true]");
        b.set(11, 0, 18, "spruce_trapdoor[facing=west,open=true]"); b.set(14, 0, 18, "spruce_trapdoor[facing=east,open=true]");
        b.set(18, 0, 18, "barrel[facing=up]"); b.set(19, 0, 18, "barrel[facing=up]"); b.set(19, 1, 18, "barrel[facing=up]"); b.set(17, 0, 18, "hay_block");
        b.set(21, 0, 3, "composter"); b.set(21, 0, 5, "barrel[facing=up]"); b.set(21, 0, 6, "water_cauldron[level=2]");
        // back garden (vegetable plots and a little orchard)
        b.fill(0, -1, -12, 20, -1, -2, Pal.of("grass_block"));
        for (int z = -11; z <= -3; z++) for (int x = 1; x <= 19; x++) {
            boolean row = x % 4 != 0;
            if (x == 10) { b.set(x, -1, z, "dirt_path"); continue; }
            if (!row) { b.set(x, -1, z, "water"); b.set(x, 0, z, z % 3 == 0 ? "lily_pad".replace("lily_pad", "air") : "air"); continue; }
            b.set(x, -1, z, "farmland[moisture=7]");
            String crop = x < 4 ? "carrots[age=7]" : x < 8 ? "potatoes[age=7]" : x < 10 ? "wheat[age=7]" : x < 12 ? "beetroots[age=3]" : x < 16 ? "wheat[age=6]" : "carrots[age=6]";
            b.set(x, 0, z, crop);
        }
        for (int x = -1; x <= 21; x++) { b.set(x, 0, -13, "spruce_fence"); }
        for (int z = -13; z <= -1; z++) { b.set(-1, 0, z, "spruce_fence"); b.set(21, 0, z, "spruce_fence"); }
        b.set(10, 0, -13, "spruce_fence_gate[facing=north]");
        b.stand(14.5, 0, -7.5, 30, "minecraft:carved_pumpkin", "minecraft:leather_chestplate", "", "", "minecraft:wheat");
    }
}
