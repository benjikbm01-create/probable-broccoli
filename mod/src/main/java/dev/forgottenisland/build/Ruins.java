package dev.forgottenisland.build;

import dev.forgottenisland.dialogue.DialogueManager;
import dev.forgottenisland.game.*;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

/** The Undercroft: the ancient halls beneath Kestrel Keep, down to the Wellspring. */
public final class Ruins implements Layout.Part {
    public String name() { return "ruins"; }
    static final T W = new T(0, 0, 0, 0);
    static final int CY = 57;                     // upper cistern standing level
    static final int HY = 45;                     // hall of echoes standing level
    static final int AY = 28;                     // wellspring standing level
    static final int AX = -10, AZ = -58, AR = 18; // arena centre/radius
    static final int SX = 16, SZ = -101;          // gliding stones grid origin
    static final Pal OLD = Pal.of("deepslate_bricks:4", "cracked_deepslate_bricks:2", "deepslate_tiles:2", "polished_deepslate:1", "cobbled_deepslate:1");
    static final Pal LIGHT_STONE = Pal.of("stone_bricks:4", "mossy_stone_bricks:3", "cracked_stone_bricks:2");
    static final Pal TRIM = Pal.of("polished_blackstone_bricks:3", "chiseled_polished_blackstone:1");

    public void places() {
        W.place("cp_undercroft", 20, CY, -164, 180);
        W.place("cp_echoes", 20, HY, -132, 0);
        W.place("cp_vault", 20, HY, -88, 0);
        W.place("cp_wellspring", AX, AY, AZ - AR - 6, 0);
        W.place("echo_hall", 20, HY, -120, 0);
        W.place("boss_center", AX, AY, AZ, 180);
        Story.bossCenter = new BlockPos(AX, AY, AZ);
        W.zone("undercroft", 4, CY - 2, -177, 36, CY + 10, -145, p -> Checkpoints.set(p, "cp_undercroft"));
        W.zone("echo_hall", -9, HY - 2, -134, 49, HY + 12, -104, p -> { Checkpoints.set(p, "cp_echoes"); Encounters.trigger(p, "echo_hall"); });
        int[][] pools = {{0, -126}, {40, -126}, {20, -108}};
        for (int i = 0; i < 3; i++) {
            int idx = i + 1;
            W.zoneRepeat("echo_" + idx, pools[i][0] - 2, HY - 1, pools[i][1] - 2, pools[i][0] + 2, HY + 3, pools[i][1] + 2, p -> {
                var st = Game.state(p.getServer());
                if (!st.flag("echo_" + idx)) DialogueManager.start(p, "echo", null);
            });
            W.place("echo_pool_" + idx, pools[i][0], HY, pools[i][1], 0);
        }
        W.zone("vault", SX, HY - 2, -91, SX + 8, HY + 6, -82, p -> Checkpoints.set(p, "cp_vault"));
        W.zone("arena_door", AX - 4, AY - 1, AZ - AR - 8, AX + 4, AY + 4, AZ - AR - 3, p -> Checkpoints.set(p, "cp_wellspring"));
        W.zoneRepeat("wellspring", AX - AR + 2, AY - 3, AZ - AR + 2, AX + AR - 2, AY + 12, AZ + AR - 2, Story::startBoss);
        W.zoneRepeat("warden_rest", AX - 4, AY - 2, AZ - 4, AX + 4, AY + 5, AZ + 4, p -> {
            var st = Game.state(p.getServer());
            if (st.flag("boss_defeated") && !st.flag("warden_rest") && DialogueManager.session(p) == null) DialogueManager.start(p, "warden", null);
        });
        W.zone("secret_flooded_chamber", -26, HY - 1, -122, -20, HY + 4, -116, p -> Secrets.discover(p, "flooded_chamber"));
        W.zone("secret_kings_mural", 54, HY - 1, -114, 60, HY + 5, -108, p -> Secrets.discover(p, "kings_mural"));
        W.zone("sea_cave", 94, 55, -168, 112, 70, -148, null);
        // encounters
        var hall = Encounters.add("echo_hall", "true");
        hall.spawns().add(new Encounters.Spawn(EntityType.DROWNED, 6, HY, -116, Encounters.gear(Items.AIR, null)));
        hall.spawns().add(new Encounters.Spawn(EntityType.DROWNED, 34, HY, -116, Encounters.gear(Items.AIR, null)));
        hall.spawns().add(new Encounters.Spawn(EntityType.ZOMBIE, 20, HY, -112, Encounters.gear(Items.STONE_SWORD, Items.CHAINMAIL_HELMET)));
        hall.spawns().add(new Encounters.Spawn(EntityType.SKELETON, 44, HY, -110, Encounters.gear(Items.BOW, Items.CHAINMAIL_HELMET)));
        var cist = Encounters.add("cistern", "true");
        W.zone("cistern_mid", 6, CY - 2, -158, 34, CY + 8, -147, p -> Encounters.trigger(p, "cistern"));
        cist.spawns().add(new Encounters.Spawn(EntityType.ZOMBIE, 10, CY, -150, Encounters.gear(Items.IRON_SHOVEL, Items.CHAINMAIL_HELMET)));
        cist.spawns().add(new Encounters.Spawn(EntityType.ZOMBIE, 30, CY, -152, Encounters.gear(null, Items.CHAINMAIL_HELMET)));
        cist.spawns().add(new Encounters.Spawn(EntityType.SPIDER, 20, CY, -149, null));
        // puzzle anchors + gates
        Puzzles.slideOrigin = new BlockPos(SX, HY - 1 + 1 - 1 + 0, SZ).up(0);
        Puzzles.slideOrigin = new BlockPos(SX, HY, SZ);
        Puzzles.slideReset = new BlockPos(SX + 3, HY + 1, SZ - 2);
        Puzzles.on(Puzzles.slideReset, (p, pos, held) -> { Puzzles.resetSlide(p.getServer(), false); return false; });
        var echoDoor = Gates.add("echo_door", SoundEvents.BLOCK_IRON_DOOR_OPEN);
        for (int x = SX + 3; x <= SX + 5; x++) for (int y = HY; y <= HY + 2; y++) echoDoor.closed().put(new BlockPos(x, y, SZ - 3), B.s("iron_bars"));
        var vault = Gates.add("vault_door", SoundEvents.BLOCK_PISTON_CONTRACT);
        for (int x = SX + 3; x <= SX + 5; x++) for (int y = HY; y <= HY + 2; y++) vault.closed().put(new BlockPos(x, y, SZ + 9), B.s("chiseled_polished_blackstone"));
        var arena = Gates.add("arena", SoundEvents.BLOCK_IRON_DOOR_CLOSE);
        for (int x = AX - 2; x <= AX + 2; x++) for (int y = AY; y <= AY + 3; y++) arena.closed().put(new BlockPos(x, y, AZ - AR - 1), B.s("iron_bars"));
        var sink = Gates.add("sinkhole", SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN);
        for (int x = -40; x <= -38; x++) for (int z = -64; z <= -62; z++) sink.closed().put(new BlockPos(x, Terrain.stand(-39, -63) - 1, z), B.s("iron_bars"));
        var sea = Gates.add("sea_gate", SoundEvents.BLOCK_IRON_DOOR_OPEN);
        for (int z = -160; z <= -158; z++) for (int y = CY; y <= CY + 2; y++) sea.closed().put(new BlockPos(90, y, z), B.s("iron_bars"));
        Interactions.lever(new BlockPos(88, CY + 1, -161), "sea_gate", "sea_gate_found");
    }

    public void build(ServerWorld w) {
        B b = new B(w, W);
        cistern(b);
        seaTunnel(b);
        descent(b, 20, CY, -146, HY, true);
        echoHall(b);
        slideChamber(b);
        vaultAndStair(b);
        wellspring(b);
        sinkhole(b);
        for (String g : new String[]{"echo_door", "vault_door", "sinkhole", "sea_gate"}) Gates.close(w.getServer(), g);
        Puzzles.resetSlide(w.getServer(), true);
    }

    static void room(B b, int x1, int y1, int z1, int x2, int y2, int z2, Pal wall, Pal floor) {
        for (int x = x1 - 1; x <= x2 + 1; x++) for (int z = z1 - 1; z <= z2 + 1; z++) for (int y = y1 - 1; y <= y2 + 1; y++) {
            boolean shell = x < x1 || x > x2 || z < z1 || z > z2 || y < y1 || y > y2;
            if (!shell) b.wset(x, y, z, B.s("air"));
            else if (y == y1 - 1) b.wset(x, y, z, floor.pick(x, y, z));
            else b.wset(x, y, z, wall.pick(x, y, z));
        }
    }

    static void cistern(B b) {
        // shaft from the courtyard grate
        for (int y = CY; y <= Keep.G - 2; y++) for (int x = 18; x <= 22; x++) for (int z = -162; z <= -158; z++) {
            boolean edge = x == 18 || x == 22 || z == -162 || z == -158;
            b.wset(x, y, z, edge ? LIGHT_STONE.pick(x, y, z) : B.s("air"));
        }
        for (int y = CY; y <= Keep.G - 2; y++) b.wset(20, y, -161, B.s("ladder[facing=south]"));
        for (int y = CY + 10; y <= Keep.G - 2; y += 9) b.wset(21, y, -159, B.s("soul_lantern"));
        // hall with water channels and arcades
        room(b, 5, CY, -176, 35, CY + 9, -146, LIGHT_STONE, Pal.of("stone_bricks:3", "mossy_stone_bricks:2", "cobblestone:1"));
        for (int x = 5; x <= 35; x++) for (int z = -176; z <= -146; z++) {
            boolean channel = (z >= -169 && z <= -167) || (z >= -153 && z <= -151);
            if (channel && x > 6 && x < 34) { b.wset(x, CY - 1, z, B.s("water")); b.wset(x, CY - 2, z, B.s("water")); }
        }
        for (int x = 9; x <= 31; x += 6) for (int z : new int[]{-172, -160, -148}) {
            for (int y = CY; y <= CY + 8; y++) { b.wset(x, y, z, B.s("stone_bricks")); b.wset(x + 1, y, z, B.s("stone_bricks")); }
            b.wset(x - 1, CY + 8, z, B.s("stone_brick_stairs[facing=east,half=top]")); b.wset(x + 2, CY + 8, z, B.s("stone_brick_stairs[facing=west,half=top]"));
            b.wset(x, CY + 3, z + 1, B.s("soul_wall_torch[facing=south]"));
        }
        for (int x = 6; x <= 34; x++) for (int z : new int[]{-168, -152}) if (x % 6 == 0) { b.wset(x, CY - 1, z, B.s("stone_brick_slab[type=top]")); }
        for (int x = 20; x <= 21; x++) for (int z : new int[]{-169, -168, -167}) b.wset(x, CY - 1, z, B.s("stone_bricks"));
        for (int x = 20; x <= 21; x++) for (int z : new int[]{-153, -152, -151}) b.wset(x, CY - 1, z, B.s("stone_bricks"));
        for (int i = 0; i < 30; i++) { int x = 6 + (int) (Noise.hash(i, 1, 7) * 29), z = -175 + (int) (Noise.hash(i, 2, 7) * 29); b.wset(x, CY + 9, z, B.s(i % 3 == 0 ? "cobweb" : "glow_lichen[down=true]".replace("down", "up"))); }
        b.wset(34, CY, -175, B.s("barrel[facing=up]")); b.wset(33, CY, -175, B.s("barrel[facing=up]"));
        new B(b.w, 0, 0, 0, 0).sign(20, CY + 1, -147, "stone_brick_wall".equals("") ? "" : "dark_oak_wall_sign[facing=north]", "These halls are", "older than the", "Keep. Older than", "remembering.");
    }

    static void seaTunnel(B b) {
        // tunnel east from the cistern to a cave opening on the sea
        for (int x = 35; x <= 92; x++) for (int z = -161; z <= -158; z++) for (int y = CY - 1; y <= CY + 3; y++) {
            boolean shell = z == -161 || z == -158 || y == CY - 1 || y == CY + 3;
            if (!shell) b.wset(x, y, z, B.s("air"));
            else if (y == CY - 1) b.wset(x, y, z, Pal.of("stone:2", "gravel:1", "cobblestone:1").pick(x, y, z));
            else if (!b.wget(x, y, z).isSolid()) b.wset(x, y, z, Pal.of("stone:3", "tuff:1").pick(x, y, z));
        }
        for (int x = 40; x <= 88; x += 12) b.wset(x, CY + 2, -160, B.s("lantern".replace("lantern", "soul_lantern[hanging=true]")));
        b.wset(88, CY, -161, B.s("stone_bricks")); b.wset(88, CY + 1, -161, B.s("air"));
        b.wset(88, CY + 1, -161, B.s("lever[face=wall,facing=south]"));
        b.wset(88, CY + 1, -162, B.s("stone_bricks"));
        // sea cave: grotto open to the water
        for (int x = 92; x <= 116; x++) for (int z = -168; z <= -150; z++) for (int y = 54; y <= 68; y++) {
            double d = Math.hypot((x - 102) / 11.0, (z - 159) / 8.5) + Math.abs(y - 61) / 10.0;
            if (d < 1.0) b.wset(x, y, z, y <= 62 ? B.s("water") : B.s("air"));
        }
        for (int x = 92; x <= 99; x++) for (int z = -161; z <= -157; z++) { b.wset(x, CY - 1, z, B.s("sand")); for (int y = CY; y <= CY + 3; y++) b.wset(x, y, z, B.s("air")); }
        for (int x = 99; x <= 104; x++) for (int z = -162; z <= -156; z++) b.wset(x, 62, z, B.s("sand"));
        b.boat(102, 63, -159, 90, net.minecraft.entity.vehicle.BoatEntity.Type.SPRUCE);
    }

    /** Broad stair descending south from (x, yTop, z) to yBottom. */
    static void descent(B b, int x, int yTop, int z, int yBottom, boolean grand) {
        int steps = yTop - yBottom;
        for (int i = 0; i <= steps + 3; i++) for (int dx = -2; dx <= 2; dx++) {
            int y = yTop - Math.min(i, steps), cz = z + i;
            for (int yy = y - 1; yy <= y + 4; yy++) b.wset(x + dx, yy, cz, B.s(yy == y - 1 ? "stone_bricks" : "air"));
            if (i < steps) b.wset(x + dx, y - 1, cz, B.s("stone_brick_stairs[facing=north]"));
            for (int side : new int[]{-3, 3}) for (int yy = y - 1; yy <= y + 5; yy++) b.wset(x + side, yy, cz, OLD.pick(x + side, yy, cz));
            b.wset(x + dx, y + 5, cz, OLD.pick(x + dx, y + 5, cz));
            if (i % 5 == 0) b.wset(x - 2, y + 2, cz, B.s("soul_wall_torch[facing=east]".replace("soul_wall_torch[facing=east]", "soul_lantern")));
        }
    }

    static void echoHall(B b) {
        room(b, -9, HY, -133, 49, HY + 12, -105, OLD, Pal.of("polished_deepslate:3", "deepslate_tiles:3", "cracked_deepslate_tiles:1"));
        // colonnade
        for (int x = -3; x <= 45; x += 8) for (int z : new int[]{-126, -112}) {
            for (int y = HY; y <= HY + 12; y++) for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++) b.wset(x + dx, y, z + dz, B.s(y == HY || y == HY + 11 ? "chiseled_deepslate" : "polished_deepslate"));
            b.wset(x, HY + 7, z - 1, B.s("soul_lantern[hanging=false]".replace("[hanging=false]", ""))); 
        }
        // the three memory pools
        int[][] pools = {{0, -126}, {40, -126}, {20, -108}};
        for (int[] p : pools) {
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                b.wset(p[0] + dx, HY - 1, p[1] + dz, B.s(rim ? "polished_blackstone_bricks" : "water"));
                if (!rim) { b.wset(p[0] + dx, HY - 2, p[1] + dz, B.s("sea_lantern")); }
                b.wset(p[0] + dx, HY, p[1] + dz, B.s("air"));
            }
            b.wset(p[0], HY - 1, p[1], B.s("water"));
            b.wset(p[0] + 2, HY, p[1] + 2, B.s("candle[candles=3,lit=true]"));
            b.wset(p[0] - 2, HY, p[1] - 2, B.s("candle[candles=2,lit=true]"));
        }
        // murals: block-art panels on the north wall with captions
        String[][] cap = {{"They raised a", "lantern of", "living flame", ""}, {"and the world", "remembered", "the isle", ""}, {"What the sea", "takes, the", "Wellspring keeps", ""}, {"Never carry the", "flame below.", "Never.", ""}};
        for (int i = 0; i < 4; i++) {
            int x0 = -6 + i * 14;
            for (int dx = 0; dx <= 6; dx++) for (int dy = 2; dy <= 8; dy++) {
                double d = Math.hypot(dx - 3, (dy - 5) * 1.2);
                String s = d < 1.2 ? "shroomlight" : d < 2.1 ? (i == 3 ? "red_terracotta" : "orange_terracotta") : d < 3.0 ? "yellow_terracotta" : "brown_terracotta";
                if (i == 2 && dy < 4) s = "light_blue_terracotta";
                b.wset(x0 + dx, HY + dy, -134, B.s(s));
            }
            new B(b.w, 0, 0, 0, 0).sign(x0 + 3, HY + 1, -133, "dark_oak_wall_sign[facing=south]", cap[i]);
        }
        // hidden: a flooded side chamber (west) and the King's Mural (east, behind a cracked wall gap)
        for (int x = -18; x <= -10; x++) for (int z = -121; z <= -117; z++) for (int y = HY - 1; y <= HY + 2; y++) b.wset(x, y, z, B.s("water"));
        for (int x = -10; x <= -10; x++) for (int z = -120; z <= -118; z++) for (int y = HY; y <= HY + 1; y++) b.wset(x, y, z, B.s("water"));
        room(b, -26, HY, -122, -19, HY + 4, -116, OLD, Pal.of("gravel"));
        for (int z = -121; z <= -117; z++) for (int y = HY; y <= HY + 1; y++) b.wset(-19, y, z, B.s("water"));
        for (int x = -18; x <= -11; x++) for (int z = -121; z <= -117; z++) b.wset(x, HY + 3, z, OLD.pick(x, HY + 3, z));
        new B(b.w, 0, 0, 0, 0).container(-24, HY, -119, "chest[facing=east]", "fi:ancient_coin", "fi:healing", "fi:emeralds", "minecraft:prismarine_crystals*6");
        b.wset(-25, HY + 3, -121, B.s("glow_lichen[up=true]")); b.wset(-21, HY, -121, B.s("sea_pickle[pickles=4,waterlogged=false]"));
        b.wset(-9, HY + 2, -119, B.s("glow_lichen[west=true]".replace("west", "east")));
        // King's mural
        room(b, 54, HY, -114, 60, HY + 5, -108, OLD, Pal.of("polished_deepslate"));
        for (int z = -112; z <= -111; z++) for (int y = HY; y <= HY + 1; y++) for (int x = 50; x <= 53; x++) b.wset(x, y, z, B.s("air"));
        b.wset(50, HY + 2, -112, B.s("cracked_deepslate_bricks")); b.wset(50, HY + 2, -111, B.s("cracked_deepslate_bricks"));
        for (int dz = 0; dz <= 5; dz++) for (int dy = 1; dy <= 5; dy++) {
            String s = (dz == 2 || dz == 3) && dy >= 3 ? "gold_block" : dy == 5 ? "orange_terracotta" : (dz + dy) % 2 == 0 ? "cyan_terracotta" : "light_blue_terracotta";
            b.wset(61, HY + dy - 1, -113 + dz, B.s(s));
        }
        new B(b.w, 0, 0, 0, 0).lectern(57, HY, -111, net.minecraft.util.math.Direction.EAST, "lore_book_kings");
        new B(b.w, 0, 0, 0, 0).container(55, HY, -113, "chest[facing=south]", "fi:golden_apple", "minecraft:gold_ingot*3");
        b.wset(59, HY + 4, -109, B.s("soul_lantern[hanging=true]"));
        // the way on: passage south to the gliding stones (closed by the echo door)
        for (int x = SX + 2; x <= SX + 6; x++) for (int z = -105; z <= SZ - 1; z++) for (int y = HY - 1; y <= HY + 4; y++) {
            boolean shell = x == SX + 2 || x == SX + 6 || y == HY - 1 || y == HY + 4;
            b.wset(x, y, z, shell ? OLD.pick(x, y, z) : B.s("air"));
        }
    }

    static void slideChamber(B b) {
        String[] g = PuzzleLayouts.SLIDE_GRID;
        for (int z = -1; z <= g.length; z++) for (int x = -1; x <= g[0].length(); x++) for (int y = HY - 1; y <= HY + 6; y++) {
            int wx = SX + x, wz = SZ + z;
            boolean outer = x < 0 || z < 0 || x >= g[0].length() || z >= g.length;
            if (y == HY - 1) { b.wset(wx, y, wz, B.s("polished_blackstone")); continue; }
            if (y == HY + 6 || outer) { b.wset(wx, y, wz, TRIM.pick(wx, y, wz)); continue; }
            char c = g[z].charAt(x);
            b.wset(wx, y, wz, c == '#' ? (y <= HY + 2 ? TRIM.pick(wx, y, wz) : B.s("air")) : B.s("air"));
        }
        for (int[] s : PuzzleLayouts.slideSockets()) { b.wset(SX + s[0], HY - 1, SZ + s[1], B.s("chiseled_tuff_bricks")); }
        for (int x = 0; x < g[0].length(); x++) for (int z = 0; z < g.length; z++) if (g[z].charAt(x) != '#' && (x + z) % 4 == 0) b.wset(SX + x, HY + 5, SZ + z, B.s("soul_lantern[hanging=true]"));
        b.wset(SX, HY + 5, SZ, B.s("polished_blackstone_bricks"));
        // doorway cells (north and south) are walls in the puzzle model; open them physically
        for (int y = HY; y <= HY + 2; y++) { b.wset(SX + 4, y, SZ, B.s("air")); b.wset(SX + 4, y, SZ + 8, B.s("air")); }
        for (int x = SX + 3; x <= SX + 5; x++) for (int z = SZ - 3; z <= SZ - 1; z++) for (int y = HY; y <= HY + 2; y++) b.wset(x, y, z, B.s("air"));
        b.wset(Puzzles.slideReset.getX(), Puzzles.slideReset.getY(), Puzzles.slideReset.getZ(), B.s("polished_blackstone_button[face=wall,facing=south]"));
        b.wset(Puzzles.slideReset.getX(), Puzzles.slideReset.getY(), Puzzles.slideReset.getZ() - 1, B.s("polished_blackstone_bricks"));
        new B(b.w, 0, 0, 0, 0).sign(SX + 5, HY + 1, SZ - 2, "dark_oak_wall_sign[facing=south]".replace("south", "south"), "Set the three", "stones upon the", "three seals.", "(button: reset)");
        b.wset(SX + 5, HY + 1, SZ - 1, B.s("polished_blackstone_bricks"));
    }

    static void vaultAndStair(B b) {
        // vault antechamber south of the puzzle, with the warden's last words
        room(b, SX, HY, SZ + 10, SX + 8, HY + 5, SZ + 17, OLD, Pal.of("polished_deepslate"));
        for (int x = SX + 3; x <= SX + 5; x++) for (int y = HY; y <= HY + 2; y++) b.wset(x, y, SZ + 9, B.s("air"));
        new B(b.w, 0, 0, 0, 0).lectern(SX + 4, HY, SZ + 13, net.minecraft.util.math.Direction.NORTH, "warden_journal");
        b.wset(SX + 1, HY, SZ + 11, B.s("candle[candles=4,lit=true]")); b.wset(SX + 7, HY, SZ + 11, B.s("candle[candles=4,lit=true]"));
        new B(b.w, 0, 0, 0, 0).container(SX + 7, HY, SZ + 16, "chest[facing=west]", "fi:healing", "fi:healing", "fi:golden_apple", "minecraft:cooked_beef*8");
        b.wset(SX + 1, HY, SZ + 16, B.s("water_cauldron[level=3]"));
        // long stair down to the Wellspring
        int x = AX, z = SZ + 18, y = HY;
        // first: a corridor west from the antechamber to above the arena entrance
        for (int cx = AX - 2; cx <= SX + 6; cx++) for (int cz = SZ + 16; cz <= SZ + 20; cz++) for (int cy = HY - 1; cy <= HY + 4; cy++) {
            boolean shell = cz == SZ + 16 || cz == SZ + 20 || cy == HY - 1 || cy == HY + 4 || cx == AX - 2;
            if (cz == SZ + 16 && cx >= SX + 1 && cx <= SX + 7 && cy >= HY && cy <= HY + 3) { b.wset(cx, cy, cz, B.s("air")); continue; }
            b.wset(cx, cy, cz, shell ? OLD.pick(cx, cy, cz) : B.s("air"));
        }
        for (int cx = AX; cx <= SX + 6; cx += 6) b.wset(cx, HY + 3, SZ + 18, B.s("soul_lantern[hanging=true]"));
        descent(b, AX, HY, SZ + 21, AY, true);
        for (int dx = -2; dx <= 2; dx++) for (int yy = HY; yy <= HY + 3; yy++) b.wset(AX + dx, yy, SZ + 20, B.s("air"));
    }

    static void wellspring(B b) {
        int floorY = AY - 1;
        for (int x = -AR - 2; x <= AR + 2; x++) for (int z = -AR - 2; z <= AR + 2; z++) {
            double d = Math.hypot(x, z);
            if (d > AR + 2) continue;
            int domeTop = AY + (int) Math.round(Math.sqrt(Math.max(0, (AR + 1.0) * (AR + 1.0) - d * d)) * 1.1);
            for (int y = floorY - 2; y <= domeTop + 2; y++) {
                int wx = AX + x, wz = AZ + z;
                boolean inside = d <= AR && y >= AY && y <= domeTop;
                if (inside) b.wset(wx, y, wz, B.s("air"));
                else if (y <= domeTop + 2) b.wset(wx, y, wz, Pal.of("deepslate:4", "cobbled_deepslate:2", "tuff:2", "deepslate_bricks:1").pick(wx, y, wz));
            }
            int wx = AX + x, wz = AZ + z;
            if (d <= AR) {
                boolean ring = d > 6.5 && d < 11.5;
                b.wset(wx, floorY, wz, B.s(ring ? "water" : d <= 6.5 ? "polished_deepslate" : "deepslate_tiles"));
                if (ring) b.wset(wx, floorY - 1, wz, B.s(Noise.hash(wx, wz, 2) < 0.2 ? "sea_lantern" : "prismarine_bricks"));
                if (ring && Noise.hash(wx, wz, 3) < 0.08) b.wset(wx, floorY - 1, wz, B.s("water"));
            }
        }
        // central dais and the memory pool
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) if (Math.hypot(x, z) < 3.3) b.wset(AX + x, AY - 1, AZ + z, B.s(Math.hypot(x, z) < 1.5 ? "water" : "chiseled_deepslate"));
        b.wset(AX, AY - 2, AZ, B.s("sea_lantern"));
        // causeways across the water ring
        for (int i = 7; i <= 11; i++) for (int w = -1; w <= 1; w++) { b.wset(AX + w, AY - 1, AZ - i, B.s("deepslate_tiles")); b.wset(AX + w, AY - 1, AZ + i, B.s("deepslate_tiles")); b.wset(AX - i, AY - 1, AZ + w, B.s("deepslate_tiles")); b.wset(AX + i, AY - 1, AZ + w, B.s("deepslate_tiles")); }
        // pillars of pale light and dripstone
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            int px = AX + (int) Math.round(Math.cos(a) * 14), pz = AZ + (int) Math.round(Math.sin(a) * 14);
            for (int y = AY; y <= AY + 7; y++) b.wset(px, y, pz, B.s(y == AY + 5 ? "sea_lantern" : "polished_deepslate"));
            b.wset(px, AY + 8, pz, B.s("pointed_dripstone[vertical_direction=up,thickness=tip]"));
        }
        for (int i = 0; i < 60; i++) {
            int x = AX + (int) ((Noise.hash(i, 9, 1) - 0.5) * AR * 1.8), z = AZ + (int) ((Noise.hash(i, 9, 2) - 0.5) * AR * 1.8);
            for (int y = AY + 25; y > AY + 4; y--) if (b.wget(x, y, z).isAir() && b.wget(x, y + 1, z).isSolid()) {
                b.wset(x, y, z, B.s(i % 4 == 0 ? "glow_lichen[up=true]" : "pointed_dripstone[vertical_direction=down,thickness=tip]"));
                break;
            }
        }
        // entrance from the north stair
        for (int x = AX - 2; x <= AX + 2; x++) for (int z = AZ - AR - 8; z <= AZ - AR + 1; z++) for (int y = AY - 1; y <= AY + 4; y++) b.wset(x, y, z, y == AY - 1 ? B.s("deepslate_tiles") : B.s("air"));
        for (int z = AZ - AR - 8; z <= AZ - AR; z++) for (int y = AY - 1; y <= AY + 5; y++) { b.wset(AX - 3, y, z, OLD.pick(AX - 3, y, z)); b.wset(AX + 3, y, z, OLD.pick(AX + 3, y, z)); }
        for (int x = AX - 3; x <= AX + 3; x++) for (int z = AZ - AR - 8; z <= AZ - AR; z++) b.wset(x, AY + 5, z, OLD.pick(x, AY + 5, z));
        b.wset(AX - 2, AY + 3, AZ - AR - 4, B.s("soul_lantern[hanging=true]"));
        new B(b.w, 0, 0, 0, 0).sign(AX + 2, AY + 1, AZ - AR - 6, "dark_oak_wall_sign[facing=west]", "THE WELLSPRING", "", "where the lost", "are kept");
    }

    static void sinkhole(B b) {
        // spiral stair from the arena's south-west edge up to a grate near the Singing Stones
        int cx = -39, cz = -63;
        int surface = Terrain.stand(cx, cz) - 1;
        for (int y = AY; y <= surface; y++) for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            double d = Math.hypot(x, z);
            if (d > 3.5) continue;
            int wx = cx + x, wz = cz + z;
            if (d > 2.5) { if (y < surface - 2) b.wset(wx, y, wz, Pal.of("deepslate:2", "stone:2", "mossy_cobblestone:1").pick(wx, y, wz)); continue; }
            if (d < 0.8) { b.wset(wx, y, wz, B.s("stone_bricks")); continue; }
            double ang = Math.atan2(z, x); if (ang < 0) ang += Math.PI * 2;
            int sector = (int) Math.floor(ang / (Math.PI * 2) * 16);
            int k = Math.floorMod(2 * (y - (AY - 1)) - sector, 16);
            b.wset(wx, y, wz, k == 0 ? B.s("cobblestone_slab[type=bottom]") : k == 15 ? B.s("cobblestone_slab[type=top]") : B.s("air"));
        }
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) b.wset(cx + x, surface, cz + z, B.s("air"));
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) if (Math.abs(x) == 2 || Math.abs(z) == 2) b.wset(cx + x, surface, cz + z, B.s("mossy_cobblestone"));
        // link tunnel from the arena edge to the stair foot
        int ax = AX - AR + 2, az = AZ;
        for (int t = 0; t <= 40; t++) {
            double f = t / 40.0;
            int x = (int) Math.round(ax + (cx - ax) * f), z = (int) Math.round(az + (cz - az) * f);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int y = AY; y <= AY + 3; y++) b.wset(x + dx, y, z + dz, B.s("air"));
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) b.wset(x + dx, AY - 1, z + dz, B.s("deepslate_tiles"));
        }
    }
}
