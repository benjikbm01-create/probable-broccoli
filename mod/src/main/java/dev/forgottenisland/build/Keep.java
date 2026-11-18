package dev.forgottenisland.build;

import dev.forgottenisland.game.*;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

import static net.minecraft.util.math.Direction.*;

/** Kestrel Keep on the northern headland, and the great Lantern Tower. Ground level y=92. */
public final class Keep implements Layout.Part {
    public String name() { return "keep"; }
    static final T W = new T(0, 0, 0, 0);
    static final int G = 92;                                    // standing level
    static final int X1 = -30, X2 = 70, Z1 = -205, Z2 = -130;   // curtain wall
    static final int TX = 63, TZ = -198, TR = 7;                // lantern tower
    static final int TOWER_TOP = 142;
    // Sun Court mirror grid origin (cell 0,0)
    static final int MX = 44, MZ = -173;
    static final Pal WALL = Pal.of("stone_bricks:6", "mossy_stone_bricks:3", "cracked_stone_bricks:2", "andesite:1", "cobblestone:1");
    static final Pal DARK = Pal.of("deepslate_bricks:4", "cracked_deepslate_bricks:1", "deepslate_tiles:1");

    public void places() {
        Terrain.pad(X1 - 8, Z1 - 6, X2 + 8, Z2 + 6, G - 1, 6);
        W.place("cp_keep_gate", 20, G, -122, 180);
        W.place("cp_keep_court", 20, G, -140, 180);
        W.place("keep_court", 20, G, -150, 180);
        W.place("warden_study", -16, G, -190, 0);
        W.place("lantern_room", TX, TOWER_TOP, TZ + 3, 180);
        W.place("sun_court", 48, G, -165, 180);
        W.zone("keep_court", X1 + 3, G - 2, Z1 + 3, X2 - 3, G + 12, Z2 - 3, p -> { Checkpoints.set(p, "cp_keep_court"); Encounters.trigger(p, "keep_court"); });
        W.zone("keep_gate", 10, G - 3, -128, 30, G + 10, -112, p -> Checkpoints.set(p, "cp_keep_gate"));
        W.zone("warden_study", -27, G - 1, -201, -5, G + 6, -184, p -> Game.state(p.getServer()).set("found_log"));
        W.zone("lantern_top", TX - 6, TOWER_TOP - 1, TZ - 6, TX + 6, TOWER_TOP + 6, TZ + 6, p -> { Game.state(p.getServer()).set("saw_empty_lantern"); Checkpoints.set(p, "cp_lantern"); });
        W.zone("keep_armory", 57, G - 1, -160, 69, G + 6, -141, p -> Encounters.trigger(p, "keep_armory"));
        W.zone("secret_keep_vault", 66, G - 1, -158, 69, G + 4, -154, p -> Secrets.discover(p, "keep_vault"));
        W.zone("secret_cliff_cave", -94, 70, -170, -84, 80, -160, p -> Secrets.discover(p, "cliff_cave"));
        W.zone("keep_storeroom", -27, G - 1, -150, -10, G + 5, -134, null);
        // encounters
        var court = Encounters.add("keep_court", "true");
        double[][] sk = {{10, -150}, {30, -152}, {18, -165}, {38, -140}};
        for (double[] s : sk) court.spawns().add(new Encounters.Spawn(EntityType.SKELETON, s[0], G, s[1], Encounters.gear(Items.BOW, Items.CHAINMAIL_HELMET)));
        court.spawns().add(new Encounters.Spawn(EntityType.ZOMBIE, 24, G, -158, Encounters.gear(Items.IRON_SWORD, Items.CHAINMAIL_HELMET)));
        var arm = Encounters.add("keep_armory", "true");
        arm.spawns().add(new Encounters.Spawn(EntityType.SKELETON, 63, G, -150, Encounters.gear(Items.STONE_SWORD, Items.IRON_HELMET)));
        arm.spawns().add(new Encounters.Spawn(EntityType.ZOMBIE, 60, G, -145, Encounters.gear(Items.IRON_AXE, Items.CHAINMAIL_HELMET)));
        // puzzle anchors
        Puzzles.mirrorOrigin = new BlockPos(MX, G, MZ);
        Puzzles.mirrorSource = new BlockPos(MX - 2, G, MZ + PuzzleLayouts.MIRROR_ROW);
        Puzzles.mirrorTarget = new BlockPos(MX + PuzzleLayouts.MIRROR_TARGET_COL, G, MZ - 2);
        // gates
        var tower = Gates.add("tower_door", SoundEvents.BLOCK_IRON_DOOR_OPEN);
        for (int x = TX - 1; x <= TX + 1; x++) for (int y = G; y <= G + 2; y++) tower.closed().put(new BlockPos(x, y, TZ + TR), B.s(y == G + 2 ? "chiseled_stone_bricks" : "iron_bars"));
        var cistern = Gates.add("cistern", SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN);
        for (int x = 19; x <= 21; x++) for (int z = -161; z <= -159; z++) cistern.closed().put(new BlockPos(x, G - 1, z), B.s("iron_trapdoor[half=top]"));
        Interactions.lock(new BlockPos(20, G - 1, -160), "cistern", "warden_key", true, "A heavy iron grate, locked. A kestrel is stamped on the lock.");
        for (int x = 19; x <= 21; x++) for (int z = -161; z <= -159; z++) if (x != 20 || z != -160) Interactions.lock(new BlockPos(x, G - 1, z), "cistern", "warden_key", true, "A heavy iron grate, locked. A kestrel is stamped on the lock.");
        var vault = Gates.add("armory_vault", SoundEvents.BLOCK_PISTON_CONTRACT);
        for (int y = G; y <= G + 1; y++) vault.closed().put(new BlockPos(66, y, -153), B.s("bookshelf"));
        Interactions.lever(new BlockPos(68, G + 1, -151), "armory_vault", null);
        // the lantern (built dark; lit when the Ember returns)
        BlockPos core = new BlockPos(TX, TOWER_TOP, TZ);
        Story.lanternBrazier = core;
        Story.LANTERN_DARK.put(core, B.s("crying_obsidian"));
        Story.LANTERN_LIT.put(core, B.s("beacon"));
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) continue;
            if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
            BlockPos p = core.add(dx, 0, dz);
            Story.LANTERN_DARK.put(p, B.s("black_stained_glass"));
            Story.LANTERN_LIT.put(p, B.s((dx + dz) % 2 == 0 ? "sea_lantern" : "ochre_froglight"));
        }
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            BlockPos p = core.add(dx, 6, dz);
            Story.LANTERN_DARK.put(p, B.s("tinted_glass"));
            Story.LANTERN_LIT.put(p, B.s("yellow_stained_glass"));
        }
        Puzzles.on(core, (p, pos, held) -> Story.relight(p));
    }

    public void build(ServerWorld w) {
        B b = new B(w, W);
        // flatten the bailey
        for (int x = X1 - 6; x <= X2 + 6; x++) for (int z = Z1 - 4; z <= Z2 + 4; z++) for (int y = G; y <= G + 30; y++) b.wset(x, y, z, B.s("air"));
        for (int x = X1; x <= X2; x++) for (int z = Z1; z <= Z2; z++) {
            double n = Terrain.N3.at(x / 7.0, z / 7.0);
            b.wset(x, G - 1, z, B.s(n > 0.45 ? "gravel" : n < -0.5 ? "moss_block" : n > 0.3 ? "coarse_dirt" : "grass_block"));
            if (n < 0.2 && Noise.hash(x, z, 61) < 0.25) b.wset(x, G, z, B.s("short_grass"));
        }
        curtain(b);
        gatehouse(b);
        courtyard(b);
        greatHall(b);
        study(b);
        sunCourt(b);
        armory(b);
        barracks(b);
        lanternTower(b);
        postern(b);
        Gates.close(w.getServer(), "tower_door");
        Gates.close(w.getServer(), "cistern");
        Gates.close(w.getServer(), "armory_vault");
        Story.LANTERN_DARK.forEach((p, s) -> w.setBlockState(p, s, B.FLAGS));
        Puzzles.setMirrors(w, PuzzleLayouts.MIRROR_START);
    }

    static void crenels(B b, int x, int y, int z, boolean on) { b.wset(x, y, z, B.s(on ? "stone_bricks" : "stone_brick_slab[type=bottom]")); }

    static void curtain(B b) {
        int top = G + 11;
        for (int x = X1; x <= X2; x++) for (int z = Z1; z <= Z2; z++) {
            boolean edge = x <= X1 + 2 || x >= X2 - 2 || z <= Z1 + 2 || z >= Z2 - 2;
            if (!edge) continue;
            for (int y = G - 6; y <= top; y++) b.wset(x, y, z, WALL.pick(x, y, z));
            boolean outer = x == X1 || x == X2 || z == Z1 || z == Z2;
            if (outer) crenels(b, x, top + 1, z, (x + z) % 2 == 0);
            else b.wset(x, top + 1, z, B.s("air"));
        }
        // wall walk lanterns, arrow slits
        for (int x = X1; x <= X2; x += 6) for (int z : new int[]{Z1, Z2}) for (int y = G + 3; y <= G + 5; y++) if (y != G + 4) b.wset(x, y, z, B.s("stone_bricks")); else b.wset(x, y, z, B.s("iron_bars"));
        for (int z = Z1; z <= Z2; z += 6) for (int x : new int[]{X1, X2}) b.wset(x, G + 4, z, B.s("iron_bars"));
        // corner towers
        int[][] corners = {{X1 + 1, Z1 + 1}, {X1 + 1, Z2 - 1}, {X2 - 1, Z2 - 1}};
        for (int[] c : corners) roundTower(b, c[0], c[1], 5, G - 8, G + 17, true);
        // stairs up to the wall walk inside the south-west corner
        for (int i = 0; i < 11; i++) b.wset(X1 + 3, G + i, Z2 - 6 - i, B.s("stone_brick_stairs[facing=north]"));
        for (int i = 0; i < 11; i++) for (int y = G; y < G + i; y++) b.wset(X1 + 3, y, Z2 - 6 - i, WALL.pick(X1 + 3, y, Z2 - 6 - i));
        // ivy and moss on the walls
        for (int x = X1 - 1; x <= X2 + 1; x++) for (int z = Z1 - 1; z <= Z2 + 1; z++) {
            if (!(x == X1 - 1 || x == X2 + 1 || z == Z1 - 1 || z == Z2 + 1)) continue;
            if (Noise.hash(x, z, 51) > 0.18) continue;
            String face = x == X1 - 1 ? "east" : x == X2 + 1 ? "west" : z == Z1 - 1 ? "south" : "north";
            int len = 3 + (int) (Noise.hash(x, z, 52) * 8);
            for (int y = G + 9 - len; y <= G + 9; y++) if (b.wget(x, y, z).isAir()) b.wset(x, y, z, B.s("vine[" + face + "=true]"));
        }
    }

    static void roundTower(B b, int cx, int cz, int r, int y0, int y1, boolean crenellated) {
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
            double d = Math.hypot(x, z);
            if (d > r + 0.3) continue;
            boolean shell = d > r - 1.3;
            for (int y = y0; y <= y1; y++) b.wset(cx + x, y, cz + z, shell ? WALL.pick(cx + x, y, cz + z) : (y == G - 1 || y == G + 11 ? B.s("spruce_planks") : B.s("air")));
            if (shell && crenellated) crenels(b, cx + x, y1 + 1, cz + z, (x + z) % 2 == 0);
            if (!shell) b.wset(cx + x, y1, cz + z, B.s("stone_bricks"));
            if (shell && d > r - 0.6 && (Math.abs(x) <= 0 || Math.abs(z) <= 0)) { b.wset(cx + x, G + 4, cz + z, B.s("iron_bars")); b.wset(cx + x, G + 13, cz + z, B.s("iron_bars")); }
        }
    }

    static void gatehouse(B b) {
        int z0 = Z2 - 4, z1 = Z2 + 4;
        for (int x = 12; x <= 28; x++) for (int z = z0; z <= z1; z++) for (int y = G - 4; y <= G + 15; y++) {
            boolean tower = (x <= 15 || x >= 25);
            boolean arch = x >= 17 && x <= 23;
            if (tower) b.wset(x, y, z, WALL.pick(x, y, z));
            else if (y >= G + 6) b.wset(x, y, z, WALL.pick(x, y, z));
            else b.wset(x, y, z, B.s("air"));
        }
        for (int x = 12; x <= 28; x++) for (int z : new int[]{z0, z1}) crenels(b, x, G + 16, z, x % 2 == 0);
        for (int x = 16; x <= 24; x++) { b.wset(x, G + 5, z1, B.s("stone_brick_stairs[facing=south,half=top]")); b.wset(x, G + 5, z0, B.s("stone_brick_stairs[facing=north,half=top]")); }
        for (int x = 17; x <= 23; x++) for (int z = z0; z <= z1; z++) b.wset(x, G - 1, z, B.s(z % 2 == 0 ? "stone_bricks" : "cobblestone"));
        // raised portcullis (iron bars along the top of the arch) and kestrel banners
        for (int x = 17; x <= 23; x++) b.wset(x, G + 4, Z2, B.s("iron_bars"));
        b.wset(16, G + 3, z1 + 1, B.s("blue_wall_banner[facing=south]")); b.wset(24, G + 3, z1 + 1, B.s("blue_wall_banner[facing=south]"));
        b.wset(16, G + 2, z1 + 1, B.s("air"));
        b.wset(16, G + 1, z1 + 1, B.s("lantern"));
        b.wset(24, G + 1, z1 + 1, B.s("air"));
        new B(b.w, 0, 0, 0, 0).sign(20, G + 6, z1 + 1, "stone_brick_wall".equals("") ? "" : "dark_oak_wall_sign[facing=south]", "KESTREL KEEP", "", "Hold the light", "");
        // approach road and bridge over a dry ditch
        for (int x = 16; x <= 24; x++) for (int z = z1 + 1; z <= z1 + 6; z++) { for (int y = G - 6; y <= G - 2; y++) b.wset(x, y, z, B.s(x == 16 || x == 24 ? "air" : "air")); }
        for (int x = 8; x <= 32; x++) for (int z = z1 + 1; z <= z1 + 6; z++) { if (x >= 17 && x <= 23) continue; for (int y = G - 6; y <= G - 1; y++) b.wset(x, y, z, B.s(y == G - 6 ? "gravel" : "air")); }
        for (int x = 17; x <= 23; x++) for (int z = z1 + 1; z <= z1 + 6; z++) { b.wset(x, G - 1, z, B.s("spruce_planks")); for (int y = G - 6; y <= G - 2; y++) b.wset(x, y, z, B.s(x == 17 || x == 23 ? "stone_bricks" : "air")); }
        for (int z = z1 + 1; z <= z1 + 6; z++) { b.wset(17, G, z, B.s("spruce_fence")); b.wset(23, G, z, B.s("spruce_fence")); }
    }

    static void courtyard(B b) {
        Pal path = Pal.of("cobblestone:4", "stone_bricks:2", "mossy_cobblestone:2", "andesite:1");
        for (int z = Z2 - 3; z >= -182; z--) for (int x = 18; x <= 22; x++) b.wset(x, G - 1, z, path.pick(x, G - 1, z));
        for (int x = -4; x <= 56; x++) for (int z = -172; z <= -168; z++) b.wset(x, G - 1, z, path.pick(x, G - 1, z));
        // cistern well (the grate is a gate)
        for (int x = 18; x <= 22; x++) for (int z = -162; z <= -158; z++) {
            boolean ring = x == 18 || x == 22 || z == -162 || z == -158;
            if (ring) { b.wset(x, G - 1, z, B.s("stone_bricks")); b.wset(x, G, z, B.s((x + z) % 2 == 0 ? "stone_brick_wall" : "mossy_stone_bricks")); }
        }
        for (int[] c : new int[][]{{18, -162}, {22, -162}, {18, -158}, {22, -158}}) { b.wset(c[0], G + 1, c[1], B.s("stone_brick_wall")); b.wset(c[0], G + 2, c[1], B.s("stone_brick_wall")); }
        for (int x = 18; x <= 22; x++) { b.wset(x, G + 3, -162, B.s("stone_brick_slab[type=bottom]")); b.wset(x, G + 3, -158, B.s("stone_brick_slab[type=bottom]")); }
        for (int z = -161; z <= -159; z++) { b.wset(18, G + 3, z, B.s("stone_brick_slab[type=bottom]")); b.wset(22, G + 3, z, B.s("stone_brick_slab[type=bottom]")); }
        b.wset(20, G + 3, -160, B.s("stone_bricks")); b.wset(20, G + 2, -160, B.s("chain[axis=y]")); b.wset(20, G + 1, -160, B.s("lantern[hanging=true]"));
        // ruined cart, dead tree, a toppled statue
        Harbor.cart(b, 34, G, -146);
        for (int y = 0; y <= 5; y++) b.wset(4, G + y, -150, B.s("stripped_oak_log"));
        b.wset(5, G + 4, -150, B.s("stripped_oak_log[axis=x]")); b.wset(3, G + 5, -150, B.s("stripped_oak_log[axis=x]")); b.wset(4, G + 6, -151, B.s("stripped_oak_log[axis=z]"));
        for (int i = 0; i < 3; i++) b.wset(40 + i, G, -136, B.s("chiseled_stone_bricks".replace("chiseled_", i == 1 ? "chiseled_" : "")));
        b.wset(43, G, -137, B.s("carved_pumpkin[facing=up]".replace("carved_pumpkin[facing=up]", "stone_brick_stairs[facing=west]")));
        new B(b.w, 0, 0, 0, 0).sign(41, G, -135, "oak_wall_sign[facing=south]", "Here stood the", "Warden. His name", "has worn away.", "");
    }

    static void greatHall(B b) {
        int x1 = 0, x2 = 40, z1 = -200, z2 = -182, top = G + 12;
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) {
            boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
            for (int y = G; y <= top; y++) b.wset(x, y, z, edge ? WALL.pick(x, y, z) : B.s("air"));
            b.wset(x, G - 1, z, B.s((x + z) % 2 == 0 ? "polished_andesite" : "stone_bricks"));
        }
        // roof: dark slate gable along x
        B r = new B(b.w, 0, 0, 0, 0);
        Kit.gableX(r, x1, z1, x2, z2, top + 1, "deepslate_tile_stairs", "deepslate_tile_slab", WALL, true);
        // tall windows and doors
        for (int x = x1 + 3; x <= x2 - 3; x += 5) for (int y = G + 3; y <= G + 8; y++) { b.wset(x, y, z2, B.s("glass_pane")); b.wset(x, y, z1, B.s("glass_pane")); }
        for (int x = 19; x <= 21; x++) for (int y = G; y <= G + 3; y++) b.wset(x, y, z2, B.s(y == G + 3 ? (x == 20 ? "chiseled_stone_bricks" : "stone_brick_stairs[facing=" + (x == 19 ? "east" : "west") + ",half=top]") : "air"));
        // long tables, benches, the high seat
        for (int x = 6; x <= 34; x++) for (int z : new int[]{-193, -189}) {
            b.wset(x, G, z, B.s("dark_oak_planks".replace("dark_oak_planks", x % 7 == 0 ? "dark_oak_fence" : "spruce_planks")));
            b.wset(x, G, z - 1, B.s("spruce_stairs[facing=south]")); b.wset(x, G, z + 1, B.s("spruce_stairs[facing=north]"));
            if (x % 5 == 0) b.wset(x, G + 1, z, B.s("candle[candles=" + (1 + x % 3) + ",lit=false]"));
        }
        b.wset(20, G, -198, B.s("dark_oak_stairs[facing=north]")); b.wset(19, G, -198, B.s("dark_oak_trapdoor[facing=west,open=true]")); b.wset(21, G, -198, B.s("dark_oak_trapdoor[facing=east,open=true]"));
        b.wset(20, G + 1, -199, B.s("dark_oak_planks")); b.wset(20, G + 2, -199, B.s("dark_oak_planks"));
        for (int x = 15; x <= 25; x += 5) b.wset(x, G + 5, z1 + 1, B.s("blue_wall_banner[facing=south]"));
        for (int x = 8; x <= 32; x += 8) Kit.hangLantern(new B(b.w, 0, 0, 0, 0), x, top - 1, -191, 3, false);
        // fireplace on the east wall
        for (int z = -194; z <= -188; z++) for (int y = G; y <= G + 5; y++) b.wset(x2 - 1, y, z, B.s("bricks"));
        b.wset(x2 - 1, G, -191, B.s("campfire[lit=false]")); b.wset(x2 - 1, G + 1, -191, B.s("air")); b.wset(x2 - 1, G, -192, B.s("air")); b.wset(x2 - 1, G, -190, B.s("air"));
        b.wset(x2 - 2, G + 3, -191, B.s("player_head".replace("player_head", "skeleton_skull[rotation=4]")));
        // cobwebs and dust
        for (int i = 0; i < 18; i++) { int x = 2 + (int) (Noise.hash(i, 3, 1) * 37), z = -199 + (int) (Noise.hash(i, 4, 1) * 16); int y = G + 8 + (int) (Noise.hash(i, 5, 1) * 3); if (b.wget(x, y, z).isAir()) b.wset(x, y, z, B.s("cobweb")); }
    }

    static void study(B b) {
        int x1 = -27, x2 = -5, z1 = -201, z2 = -183;
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) {
            boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
            for (int y = G; y <= G + 10; y++) b.wset(x, y, z, edge ? WALL.pick(x, y, z) : (y == G + 5 ? B.s("dark_oak_planks") : B.s("air")));
            b.wset(x, G - 1, z, B.s("dark_oak_planks"));
        }
        B r = new B(b.w, 0, 0, 0, 0);
        Kit.gableX(r, x1, z1, x2, z2, G + 11, "deepslate_tile_stairs", "deepslate_tile_slab", WALL, true);
        for (int y = G; y <= G + 1; y++) b.wset(-16, y, z2, B.s("air"));
        Kit.door(r, -16, G, z2, SOUTH, "dark_oak_door", false);
        for (int x = x1 + 3; x <= x2 - 3; x += 4) for (int y = G + 1; y <= G + 3; y++) b.wset(x, y, z2, B.s("glass_pane"));
        for (int x = x1 + 3; x <= x2 - 3; x += 4) for (int y = G + 7; y <= G + 8; y++) b.wset(x, y, z1, B.s("glass_pane"));
        // study: shelves, desk, the warden's log
        for (int x = x1 + 1; x <= x2 - 1; x++) for (int y = G; y <= G + 3; y++) if (x % 3 != 0 || y > G + 1) b.wset(x, y, z1 + 1, B.s(y == G + 3 ? "dark_oak_slab[type=top]" : "bookshelf"));
        r.lectern(-16, G, -195, SOUTH, "warden_journal");
        b.wset(-18, G, -196, B.s("dark_oak_planks")); b.wset(-18, G + 1, -196, B.s("candle[candles=3,lit=false]"));
        b.wset(-14, G, -196, B.s("dark_oak_planks")); b.wset(-14, G + 1, -196, B.s("flower_pot"));
        r.frame(-16, G + 2, -199, SOUTH, "minecraft:map"); r.frame(-17, G + 2, -199, SOUTH, "minecraft:compass"); r.frame(-15, G + 2, -199, SOUTH, "minecraft:spyglass");
        Kit.carpet(r, -20, -194, -12, -188, G, "blue");
        // the warden's bed, and a child's little bed with a carved gull
        Kit.bed(r, -25, G, -197, WEST, "blue"); b.wset(-26, G, -199, B.s("chest[facing=east]"));
        Kit.bed(r, -8, G, -197, EAST, "white"); b.wset(-7, G, -199, B.s("barrel[facing=up]"));
        r.frame(-6, G + 2, -197, WEST, "minecraft:feather");
        r.sign(-8, G + 1, -199, "dark_oak_wall_sign[facing=south]", "E's drawing:", "Papa, Mama", "and me, and", "all the gulls");
        r.frame(-9, G + 2, -199, SOUTH, "minecraft:paper");
        b.wset(-6, G, -192, B.s("cobweb")); b.wset(-26, G + 4, -184, B.s("cobweb"));
        // stair to the upper floor (empty, dusty)
        for (int i = 0; i < 5; i++) b.wset(-26, G + i, -186 - i, B.s("dark_oak_stairs[facing=north]"));
        for (int i = 0; i < 4; i++) b.wset(-26, G + 5, -186 - i, B.s("air"));
    }

    static void sunCourt(B b) {
        int x1 = MX - 3, x2 = MX + 11, z1 = MZ - 3, z2 = MZ + 9;
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) {
            boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
            for (int y = G; y <= G + 8; y++) b.wset(x, y, z, edge ? WALL.pick(x, y, z) : B.s("air"));
            b.wset(x, G - 1, z, B.s((x + z) % 2 == 0 ? "polished_diorite" : "polished_andesite"));
        }
        // glass roof on dark beams
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) b.wset(x, G + 9, z, B.s((x - x1) % 4 == 0 ? "dark_oak_planks" : "glass"));
        // grid: pillars, mirrors (set separately), chalk lines on the floor
        for (int z = 0; z < PuzzleLayouts.MIRROR_GRID.length; z++) for (int x = 0; x < PuzzleLayouts.MIRROR_GRID[z].length(); x++) {
            char c = PuzzleLayouts.MIRROR_GRID[z].charAt(x);
            int wx = MX + x, wz = MZ + z;
            if (c == '#') for (int y = G; y <= G + 8; y++) b.wset(wx, y, wz, B.s(y == G + 8 ? "chiseled_stone_bricks" : "stone_bricks"));
            else b.wset(wx, G - 1, wz, B.s("smooth_quartz"));
        }
        // the sun lens (light source) in the west wall and the crystal target in the north wall
        b.wset(MX - 3, G, MZ + PuzzleLayouts.MIRROR_ROW, B.s("glowstone"));
        b.wset(MX - 2, G, MZ + PuzzleLayouts.MIRROR_ROW, B.s("air"));
        b.wset(MX - 3, G + 1, MZ + PuzzleLayouts.MIRROR_ROW, B.s("yellow_stained_glass"));
        b.wset(MX + PuzzleLayouts.MIRROR_TARGET_COL, G, MZ - 3, B.s("amethyst_block"));
        b.wset(MX + PuzzleLayouts.MIRROR_TARGET_COL, G, MZ - 2, B.s("air"));
        b.wset(MX + PuzzleLayouts.MIRROR_TARGET_COL, G + 1, MZ - 3, B.s("chiseled_stone_bricks"));
        Puzzles.mirrorTarget = new BlockPos(MX + PuzzleLayouts.MIRROR_TARGET_COL, G, MZ - 3);
        new B(b.w, 0, 0, 0, 0).sign(MX + 4, G + 2, z2 - 1, "dark_oak_wall_sign[facing=north]", "Turn the mirrors.", "Lead the sun", "to the crystal", "and the tower wakes");
        // doorway from the courtyard (south wall)
        for (int x = MX + 3; x <= MX + 5; x++) for (int y = G; y <= G + 2; y++) b.wset(x, y, z2, B.s("air"));
    }

    static void armory(B b) {
        int x1 = 57, x2 = 69, z1 = -160, z2 = -141;
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) {
            boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
            for (int y = G; y <= G + 6; y++) b.wset(x, y, z, edge ? WALL.pick(x, y, z) : B.s("air"));
            b.wset(x, G - 1, z, B.s("stone_bricks"));
            b.wset(x, G + 7, z, B.s("stone_brick_slab[type=bottom]"));
        }
        for (int y = G; y <= G + 2; y++) for (int z = -152; z <= -150; z++) b.wset(x1, y, z, B.s("air"));
        B r = new B(b.w, 0, 0, 0, 0);
        // weapon racks and armour stands
        for (int z = -158; z <= -144; z += 3) r.stand(68.5, G, z + 0.5, 90, "minecraft:chainmail_helmet", "minecraft:chainmail_chestplate", "", "", "minecraft:iron_sword".replace("iron", z % 2 == 0 ? "iron" : "stone"));
        for (int z = -158; z <= -143; z++) if (z % 3 == 0) r.frame(58, G + 2, z, EAST, z % 2 == 0 ? "minecraft:bow" : "minecraft:iron_axe");
        r.container(58, G, -144, "chest[facing=east]", "fi:keep_mail", "fi:keep_helm", "fi:keep_legs", "fi:keep_boots", "fi:healing");
        r.container(58, G, -147, "chest[facing=east]", "fi:keep_steel", "minecraft:arrow*24", "minecraft:bread*6");
        for (int z = -157; z <= -153; z++) b.wset(58, G, z, B.s("barrel[facing=east]"));
        b.wset(63, G, -143, B.s("grindstone[face=floor,facing=north]")); b.wset(62, G, -143, B.s("anvil[facing=north]".replace("anvil", "chipped_anvil")));
        Kit.hangLantern(r, 63, G + 6, -150, 1, false);
        // hidden vault behind the north bookshelves (button on the cask)
        for (int x = 65; x <= 68; x++) for (int z = -159; z <= -154; z++) for (int y = G; y <= G + 3; y++) b.wset(x, y, z, x == 65 || z == -159 || y == G + 3 ? WALL.pick(x, y, z) : B.s("air"));
        for (int x = 65; x <= 68; x++) for (int y = G; y <= G + 2; y++) b.wset(x, y, -153, B.s(x == 66 ? "bookshelf" : "stone_bricks"));
        b.wset(67, G, -153, B.s("bookshelf")); b.wset(67, G + 1, -153, B.s("bookshelf"));
        b.wset(66, G, -153, B.s("bookshelf")); b.wset(66, G + 1, -153, B.s("bookshelf"));
        b.wset(68, G, -151, B.s("barrel[facing=up]")); b.wset(68, G + 1, -151, B.s("oak_button[face=floor,facing=north]"));
        r.container(67, G, -157, "chest[facing=south]", "fi:warden_shield", "fi:golden_apple", "fi:healing");
        b.wset(66, G, -158, B.s("candle[candles=2,lit=false]"));
        r.stand(67.5, G, -155.5, 180, "minecraft:iron_helmet", "minecraft:iron_chestplate", "", "", "", "minecraft:shield");
    }

    static void barracks(B b) {
        int x1 = -27, x2 = -9, z1 = -150, z2 = -134;
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) {
            boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
            for (int y = G; y <= G + 6; y++) b.wset(x, y, z, edge ? WALL.pick(x, y, z) : B.s("air"));
            b.wset(x, G - 1, z, B.s("spruce_planks"));
        }
        B r = new B(b.w, 0, 0, 0, 0);
        Kit.gableZ(r, x1, z1, x2, z2, G + 7, "spruce_stairs", "spruce_slab", WALL, true);
        for (int y = G; y <= G + 1; y++) for (int x = -19; x <= -18; x++) b.wset(x, y, z1, B.s("air"));
        for (int z = -147; z <= -137; z += 3) { Kit.bed(r, -26, G, z, WEST, "gray"); Kit.bed(r, -10, G, z, EAST, "gray"); }
        r.container(-14, G, -148, "chest[facing=south]", "fi:sailcloth", "minecraft:white_wool*4");
        r.container(-22, G, -148, "chest[facing=south]", "minecraft:bread*6", "fi:arrows");
        Kit.tableTop(r, -18, G, -141, "spruce", "candle[candles=1,lit=false]");
        b.wset(-19, G, -141, B.s("spruce_stairs[facing=west]")); b.wset(-17, G, -141, B.s("spruce_stairs[facing=east]"));
        for (int z = -146; z <= -136; z += 5) for (int y = G + 1; y <= G + 2; y++) { b.wset(x1, y, z, B.s("glass_pane")); }
        b.wset(-12, G + 3, -136, B.s("cobweb")); b.wset(-24, G + 3, -145, B.s("cobweb"));
    }

    static void lanternTower(B b) {
        int base = G - 8;
        for (int x = -TR; x <= TR; x++) for (int z = -TR; z <= TR; z++) {
            double d = Math.hypot(x, z);
            if (d > TR + 0.3) continue;
            boolean shell = d > TR - 1.4;
            for (int y = base; y <= TOWER_TOP - 1; y++) {
                int r = y > TOWER_TOP - 12 ? TR - 1 : TR;
                boolean sh = d > r - 1.4 && d <= r + 0.3;
                if (d > r + 0.3) b.wset(TX + x, y, TZ + z, B.s("air"));
                else b.wset(TX + x, y, TZ + z, sh ? (y % 12 < 1 ? B.s("polished_deepslate") : WALL.pick(TX + x, y, TZ + z)) : B.s("air"));
            }
            b.wset(TX + x, G - 1, TZ + z, B.s("stone_bricks"));
        }
        // walkable spiral of half-slabs (16 sectors per 8 blocks of height) around a central column
        for (int y = G; y < TOWER_TOP; y++) b.wset(TX, y, TZ, B.s("polished_deepslate"));
        for (int y = G - 1; y < TOWER_TOP; y++) for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            double d = Math.hypot(x, z); if (d < 1.2 || d > 4.6) continue;
            double ang = Math.atan2(z, x); if (ang < 0) ang += Math.PI * 2;
            int sector = (int) Math.floor(ang / (Math.PI * 2) * 16);
            for (int turn = 0; turn * 8 + G <= TOWER_TOP; turn++) {
                int half = turn * 16 + sector;                  // height in half blocks above G-1
                int sy = G - 1 + half / 2;
                if (sy > TOWER_TOP - 1) continue;
                if (y == sy) b.wset(TX + x, y, TZ + z, B.s(half % 2 == 0 ? "stone_brick_slab[type=bottom]" : "stone_brick_slab[type=top]"));
                else if (y > sy && y <= sy + 3 && !b.wget(TX + x, y, TZ + z).isOf(net.minecraft.block.Blocks.STONE_BRICK_SLAB)) b.wset(TX + x, y, TZ + z, B.s("air"));
            }
        }
        for (int y = G + 6; y < TOWER_TOP; y += 9) { int wx = TX + (int) Math.round(5.6 * Math.cos(y)), wz = TZ + (int) Math.round(5.6 * Math.sin(y)); b.wset(wx, y + 1, wz, B.s("glass_pane")); b.wset(wx, y + 2, wz, B.s("glass_pane")); }
        for (int y = G + 4; y < TOWER_TOP - 4; y += 12) b.wset(TX + 1, y, TZ, B.s("lantern"));
        // doorway at the base (gate) facing south into the bailey
        for (int x = TX - 1; x <= TX + 1; x++) for (int y = G; y <= G + 2; y++) for (int z = TZ + TR - 1; z <= TZ + TR; z++) b.wset(x, y, z, B.s("air"));
        // lantern room: floor, gallery, the great lens housing
        int L = TOWER_TOP;
        for (int x = -TR - 1; x <= TR + 1; x++) for (int z = -TR - 1; z <= TR + 1; z++) {
            double d = Math.hypot(x, z);
            if (d <= TR + 1.3 && !stairwell(x, z)) b.wset(TX + x, L - 1, TZ + z, B.s(d > TR - 0.7 ? "polished_deepslate" : "smooth_stone"));
            if (stairwell(x, z) && topStep(x, z) < L - 1) b.wset(TX + x, L - 1, TZ + z, B.s("air"));
            if (d > TR + 0.3 && d <= TR + 1.3) b.wset(TX + x, L, TZ + z, B.s("iron_bars"));
            for (int y = L; y <= L + 8; y++) if (d <= TR - 1.3) b.wset(TX + x, y, TZ + z, B.s("air"));
            if (d > TR - 1.3 && d <= TR - 0.3) for (int y = L; y <= L + 6; y++) b.wset(TX + x, y, TZ + z, B.s(y == L || y == L + 6 ? "polished_deepslate" : (x + z) % 3 == 0 ? "polished_deepslate_wall" : "glass"));
            if (d <= TR - 0.3) { b.wset(TX + x, L + 7, TZ + z, B.s("polished_deepslate")); }
        }
        // copper cupola
        for (int k = 0; k <= 4; k++) for (int x = -6 + k; x <= 6 - k; x++) for (int z = -6 + k; z <= 6 - k; z++) {
            double d = Math.hypot(x, z);
            if (d <= 6.3 - k && d > 5 - k) b.wset(TX + x, L + 8 + k, TZ + z, B.s("waxed_weathered_cut_copper"));
        }
        b.wset(TX, L + 13, TZ, B.s("lightning_rod".replace("lightning_rod", "waxed_weathered_cut_copper"))); b.wset(TX, L + 14, TZ, B.s("lightning_rod"));
        // the beacon pyramid and dark lens (lens top ring opens skyward through the cupola centre)
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) b.wset(TX + dx, L - 1, TZ + dz, B.s("gold_block"));
        for (int y = L + 1; y <= L + 13; y++) if (y != L + 6) b.wset(TX, y, TZ, B.s("air"));
        for (int k = 0; k <= 5; k++) b.wset(TX, L + 8 + k, TZ, B.s("air"));
        b.wset(TX, L + 13, TZ, B.s("air")); b.wset(TX, L + 14, TZ, B.s("air"));
        b.wset(TX + 2, L, TZ + 2, B.s("chest[facing=north]"));
        new B(b.w, 0, 0, 0, 0).container(TX + 2, L, TZ + 2, "chest[facing=north]", "fi:warden_key", "minecraft:paper");
        new B(b.w, 0, 0, 0, 0).sign(TX, L, TZ + 2, "dark_oak_sign[rotation=0]", "Here burned", "the Ember", "", "");
        // the stair column ends at the floor
        b.wset(TX, L - 1, TZ, B.s("crying_obsidian"));
        b.wset(TX, L - 2, TZ, B.s("iron_block"));
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (dx != 0 || dz != 0) b.wset(TX + dx, L - 2, TZ + dz, B.s("iron_block"));
        // move the core one up so the beacon sits on the gold base
        b.wset(TX, L - 1, TZ, B.s("gold_block"));
    }

    /** Highest spiral step (y) at an offset from the tower centre, or Integer.MIN_VALUE. */
    static int topStep(int x, int z) {
        double d = Math.hypot(x, z); if (d < 1.2 || d > 4.6) return Integer.MIN_VALUE;
        double ang = Math.atan2(z, x); if (ang < 0) ang += Math.PI * 2;
        int sector = (int) Math.floor(ang / (Math.PI * 2) * 16), best = Integer.MIN_VALUE;
        for (int turn = 0; turn * 8 + G <= TOWER_TOP; turn++) { int sy = G - 1 + (turn * 16 + sector) / 2; if (sy <= TOWER_TOP - 1) best = Math.max(best, sy); }
        return best;
    }
    static boolean stairwell(int x, int z) { int t = topStep(x, z); return t != Integer.MIN_VALUE && t >= TOWER_TOP - 5; }

    static void postern(B b) {
        // a small door in the west wall, a trail to the cliff and ledges down to a cave
        for (int y = G; y <= G + 1; y++) for (int x = X1; x <= X1 + 2; x++) b.wset(x, y, -165, B.s("air"));
        Kit.door(new B(b.w, 0, 0, 0, 0), X1 + 1, G, -165, net.minecraft.util.math.Direction.WEST, "spruce_door", false);
        Town.road(b, Pal.of("gravel:2", "coarse_dirt:2", "cobblestone:1"), 2, new int[]{X1 - 2, -165}, new int[]{-60, -164}, new int[]{-80, -165});
        // find the cliff edge going west and cut ledges down to y 75
        int edge = -80;
        for (int x = -60; x >= -110; x--) if (Terrain.height(x, -165) < 70) { edge = x + 1; break; }
        int y = G - 1;
        int x = edge;
        int z = -165;
        for (int i = 0; y > 75; i++) {
            b.wset(x, y, z, B.s("cobblestone_slab[type=top]".replace("_slab[type=top]", "")));
            for (int k = 1; k <= 3; k++) b.wset(x, y + k, z, B.s("air"));
            if (i % 2 == 0) z += 1; else { x -= 1; }
            y -= 1;
        }
        // cave in the cliff face
        for (int cx = -93; cx <= -85; cx++) for (int cz = -169; cz <= -161; cz++) for (int cy = 71; cy <= 78; cy++)
            if (Math.hypot(cx + 89, cz + 165) < 4.2 && cy < 77) b.wset(cx, cy, cz, B.s("air"));
        for (int cx = x - 1; cx <= -85; cx++) for (int cy = y + 1; cy <= y + 3; cy++) b.wset(cx, cy, z, B.s("air"));
        for (int cx = -93; cx <= -85; cx++) for (int cz = -169; cz <= -161; cz++) if (Math.hypot(cx + 89, cz + 165) < 4.2) b.wset(cx, 70, cz, B.s("stone"));
        new B(b.w, 0, 0, 0, 0).container(-90, 71, -166, "chest[facing=east]", "fi:star_chart", "minecraft:spyglass".replace("minecraft:spyglass", "minecraft:paper*3"), "fi:emeralds");
        b.wset(-89, 71, -168, B.s("lantern")); b.wset(-91, 71, -163, B.s("barrel[facing=up]"));
    }
}
