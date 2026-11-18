package dev.forgottenisland.build;

import dev.forgottenisland.ForgottenIsland;
import dev.forgottenisland.game.Game;
import dev.forgottenisland.game.Places;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Island master plan: registers every place/zone/gate/puzzle anchor and runs the world build. */
public final class Layout {
    private Layout() {}

    /** Called once (lazily) by Places.init(). Order matters: terrain pads first. */
    public static void registerPlaces() {
        for (Part p : parts()) p.places();
    }

    public interface Part { String name(); void places(); void build(ServerWorld w); }

    static List<Part> PARTS;
    static List<Part> parts() {
        if (PARTS == null) {
            PARTS = new ArrayList<>();
            PARTS.add(new Part() {
                public String name() { return "terrain"; }
                public void places() {}
                public void build(ServerWorld w) { TerrainBuilder.build(w, TerrainBuilder.MIN, TerrainBuilder.MIN, TerrainBuilder.MAX, TerrainBuilder.MAX); }
            });
            PARTS.addAll(Structures.parts());
        }
        return PARTS;
    }

    static void say(ServerCommandSource src, String msg) {
        ForgottenIsland.LOG.info("[build] {}", msg);
        src.sendFeedback(() -> Text.literal("[build] " + msg), false);
    }

    public static void buildAll(ServerCommandSource src) {
        Places.init();
        long t0 = System.currentTimeMillis();
        for (Part p : parts()) runPart(src, p);
        finish(src);
        say(src, "Island complete in " + (System.currentTimeMillis() - t0) / 1000 + " s, " + B.placed + " blocks.");
    }

    public static void build(ServerCommandSource src, String name) {
        Places.init();
        if (name.equals("finish")) { finish(src); return; }
        for (Part p : parts()) if (p.name().equals(name)) { runPart(src, p); return; }
        say(src, "Unknown part. Parts: " + parts().stream().map(Part::name).toList() + ", finish");
    }

    static void runPart(ServerCommandSource src, Part p) {
        long t = System.currentTimeMillis();
        say(src, "building " + p.name() + "...");
        p.build(src.getServer().getOverworld());
        say(src, p.name() + " done (" + (System.currentTimeMillis() - t) / 1000.0 + " s)");
    }

    /** Game rules, spawn, world border, cleanup of stray mobs; marks the world as built. */
    static void finish(ServerCommandSource src) {
        var s = src.getServer();
        ServerWorld w = s.getOverworld();
        GameRules r = w.getGameRules();
        r.get(GameRules.KEEP_INVENTORY).set(true, s);
        r.get(GameRules.DO_MOB_SPAWNING).set(false, s);
        r.get(GameRules.DO_MOB_GRIEFING).set(false, s);
        r.get(GameRules.DO_FIRE_TICK).set(false, s);
        r.get(GameRules.DO_INSOMNIA).set(false, s);
        r.get(GameRules.DO_PATROL_SPAWNING).set(false, s);
        r.get(GameRules.DO_TRADER_SPAWNING).set(false, s);
        r.get(GameRules.DO_WARDEN_SPAWNING).set(false, s);
        r.get(GameRules.SPAWN_RADIUS).set(0, s);
        r.get(GameRules.ANNOUNCE_ADVANCEMENTS).set(false, s);
        r.get(GameRules.DISABLE_RAIDS).set(true, s);
        r.get(GameRules.SNOW_ACCUMULATION_HEIGHT).set(0, s);
        r.get(GameRules.RANDOM_TICK_SPEED).set(1, s);
        Places.P sp = Places.get("cp_dock") != null ? Places.get("cp_dock") : new Places.P(100, 65, 160, 180);
        w.setSpawnPos(BlockPos.ofFloored(sp.x(), sp.y(), sp.z()), sp.yaw());
        w.getWorldBorder().setCenter(5, -5);
        w.getWorldBorder().setSize(580);
        int removed = 0;
        for (Entity e : new ArrayList<Entity>(toList(w.iterateEntities()))) {
            if (e instanceof PlayerEntity) continue;
            if (e.getCommandTags().contains("fi_deco") || e.getCommandTags().contains("fi_ambient")) continue;
            if (e instanceof net.minecraft.entity.decoration.ItemFrameEntity || e instanceof net.minecraft.entity.decoration.ArmorStandEntity) continue;
            e.discard(); removed++;
        }
        var st = Game.state(s);
        st.set("world_built");
        s.setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        say(src, "finish: rules set, " + removed + " stray entities removed, world marked built.");
        s.saveAll(false, true, true);
    }

    static List<Entity> toList(Iterable<Entity> it) { List<Entity> l = new ArrayList<>(); it.forEach(l::add); return l; }

    /** Writes a shaded top-down map of the island to run-dir/island_map.png. */
    public static void renderMap(ServerCommandSource src) {
        ServerWorld w = src.getServer().getOverworld();
        int S = 270, scale = 1;
        BufferedImage img = new BufferedImage(2 * S, 2 * S, BufferedImage.TYPE_INT_RGB);
        int[] prev = new int[2 * S];
        for (int z = -S; z < S; z++) {
            for (int x = -S; x < S; x++) {
                int top = w.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1;
                BlockPos p = new BlockPos(x, top, z);
                BlockState st = w.getBlockState(p);
                MapColor mc = st.getMapColor(w, p);
                int rgb = mc == MapColor.CLEAR ? 0 : mc.color;
                double sh = 1.0 + (top - prev[x + S]) * 0.08;
                if (!st.getFluidState().isEmpty()) sh = 0.85 + (top - w.getTopY(Heightmap.Type.OCEAN_FLOOR, x, z)) * -0.01;
                sh = Math.max(0.5, Math.min(1.3, sh));
                int r = (int) Math.min(255, ((rgb >> 16) & 255) * sh), g = (int) Math.min(255, ((rgb >> 8) & 255) * sh), b = (int) Math.min(255, (rgb & 255) * sh);
                img.setRGB(x + S, z + S, (r << 16) | (g << 8) | b);
                prev[x + S] = top;
            }
        }
        try {
            File f = new File("island_map.png");
            ImageIO.write(img, "png", f);
            say(src, "map written to " + f.getAbsolutePath() + " (" + scale + ")");
        } catch (Exception e) { say(src, "map failed: " + e); }
    }
}
