package dev.forgottenisland.game;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.*;

/** Runtime for the three puzzles plus other block interactions (lantern, waystones, locks, levers). */
public final class Puzzles {
    private Puzzles() {}

    public interface Handler { boolean use(ServerPlayerEntity p, BlockPos pos, ItemStack held); }
    private static final Map<BlockPos, Handler> HANDLERS = new HashMap<>();
    public static void on(BlockPos pos, Handler h) { HANDLERS.put(pos.toImmutable(), h); }
    public static Map<BlockPos, Handler> handlers() { Places.init(); return HANDLERS; }

    // layout anchors (set by Layout)
    public static final Map<BlockPos, String> STONE_BUTTONS = new LinkedHashMap<>();
    public static final Map<String, BlockPos> STONE_TOPS = new LinkedHashMap<>();
    public static BlockPos mirrorOrigin, mirrorSource, mirrorTarget;
    public static BlockPos slideOrigin, slideReset;
    private static final List<String> sequence = new ArrayList<>();

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            BlockPos pos = hit.getBlockPos();
            Handler h = handlers().get(pos);
            if (h != null && h.use(sp, pos, player.getMainHandStack())) return ActionResult.SUCCESS;
            BlockState st = world.getBlockState(pos);
            if (st.isOf(ModBlocks.MIRROR)) { rotateMirror(sp, pos); return ActionResult.SUCCESS; }
            if (st.isOf(ModBlocks.RUNE_STONE)) { pushStone(sp, pos); return ActionResult.SUCCESS; }
            return ActionResult.PASS;
        });
    }

    // ------------------------------------------------------------ Puzzle 1
    /** Called by the stone button handlers registered in Layout. Returns false so the button still clicks. */
    public static boolean strikeStone(ServerPlayerEntity p, String stone) {
        var st = Game.state(p.getServer());
        ServerWorld w = p.getServerWorld();
        BlockPos top = STONE_TOPS.get(stone);
        if (st.flag("puzzle1_solved")) { chime(w, top, stone, 1f); return false; }
        if (!st.flag("stones_awake")) {
            w.playSound(null, top, SoundEvents.BLOCK_STONE_HIT, SoundCategory.BLOCKS, 1f, 0.6f);
            p.sendMessage(Text.literal("The stone is silent. Perhaps someone knows the song these stones once sang.").formatted(Formatting.GRAY, Formatting.ITALIC), true);
            return false;
        }
        int r = PuzzleLayouts.strike(sequence, stone);
        if (r < 0) {
            w.playSound(null, top, SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO.value(), SoundCategory.BLOCKS, 1.2f, 0.5f);
            for (BlockPos t : STONE_TOPS.values()) w.spawnParticles(ParticleTypes.SMOKE, t.getX() + 0.5, t.getY() + 1.2, t.getZ() + 0.5, 8, 0.2, 0.3, 0.2, 0.01);
            p.sendMessage(Text.literal("The stones fall silent. The song must begin again.").formatted(Formatting.GRAY, Formatting.ITALIC), true);
            st.addVar("stones_fails", 1);
            return false;
        }
        chime(w, top, stone, 0.6f + 0.15f * r);
        if (r == PuzzleLayouts.SONG.length) solveStones(p.getServer());
        return false;
    }

    private static void chime(ServerWorld w, BlockPos top, String stone, float pitch) {
        if (top == null) return;
        w.playSound(null, top, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.BLOCKS, 1.5f, pitch);
        w.playSound(null, top, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.5f, pitch);
        w.spawnParticles(ParticleTypes.NOTE, top.getX() + 0.5, top.getY() + 1.3, top.getZ() + 0.5, 3, 0.3, 0.2, 0.3, 1);
        w.spawnParticles(ParticleTypes.END_ROD, top.getX() + 0.5, top.getY() + 0.8, top.getZ() + 0.5, 10, 0.3, 0.6, 0.3, 0.02);
    }

    public static void solveStones(MinecraftServer s) {
        var st = Game.state(s);
        if (!st.set("puzzle1_solved")) return;
        sequence.clear();
        ServerWorld w = s.getOverworld();
        for (BlockPos t : STONE_TOPS.values()) w.spawnParticles(ParticleTypes.END_ROD, t.getX() + 0.5, t.getY() + 1, t.getZ() + 0.5, 30, 0.3, 1.0, 0.3, 0.05);
        Story.later(30, () -> Gates.open(s, "thornwall"));
        Story.later(30, () -> { for (var p : w.getPlayers()) p.sendMessage(Text.literal("Far up the road, something tears and gives way.").formatted(Formatting.DARK_GREEN, Formatting.ITALIC), true); });
    }

    public static void resetStones(MinecraftServer s) { sequence.clear(); Game.state(s).clear("puzzle1_solved"); Gates.close(s, "thornwall"); }

    // ------------------------------------------------------------ Puzzle 2
    public static boolean[] mirrorFlips(ServerWorld w) {
        List<int[]> cells = PuzzleLayouts.mirrorCells();
        boolean[] f = new boolean[cells.size()];
        for (int i = 0; i < cells.size(); i++) {
            BlockState b = w.getBlockState(mirrorOrigin.add(cells.get(i)[0], 0, cells.get(i)[1]));
            f[i] = b.isOf(ModBlocks.MIRROR) && b.get(ModBlocks.MirrorBlock.FLIP);
        }
        return f;
    }

    public static void setMirrors(ServerWorld w, boolean[] flips) {
        List<int[]> cells = PuzzleLayouts.mirrorCells();
        for (int i = 0; i < cells.size(); i++)
            w.setBlockState(mirrorOrigin.add(cells.get(i)[0], 0, cells.get(i)[1]), ModBlocks.MIRROR.getDefaultState().with(ModBlocks.MirrorBlock.FLIP, flips[i]), 2 | 16);
    }

    public static void rotateMirror(ServerPlayerEntity p, BlockPos pos) {
        var st = Game.state(p.getServer());
        ServerWorld w = p.getServerWorld();
        if (st.flag("puzzle2_solved")) { p.sendMessage(Text.literal("The mirror is locked in place by the light.").formatted(Formatting.GRAY, Formatting.ITALIC), true); return; }
        BlockState b = w.getBlockState(pos);
        w.setBlockState(pos, b.cycle(ModBlocks.MirrorBlock.FLIP), 2 | 16);
        w.playSound(null, pos, SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS, 0.5f, 1.6f);
        p.swingHand(Hand.MAIN_HAND, true);
        if (PuzzleLayouts.mirrorSolved(mirrorFlips(w))) solveMirrors(p.getServer());
    }

    public static void solveMirrors(MinecraftServer s) {
        var st = Game.state(s);
        if (!st.set("puzzle2_solved")) return;
        ServerWorld w = s.getOverworld();
        if (mirrorTarget != null) {
            w.playSound(null, mirrorTarget, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 2f, 1.2f);
            w.setBlockState(mirrorTarget, Blocks.GLOWSTONE.getDefaultState(), 2 | 16);
        }
        Story.later(40, () -> Gates.open(s, "tower_door"));
    }

    public static void resetMirrors(MinecraftServer s) {
        Game.state(s).clear("puzzle2_solved");
        ServerWorld w = s.getOverworld();
        setMirrors(w, PuzzleLayouts.MIRROR_START);
        if (mirrorTarget != null) w.setBlockState(mirrorTarget, Blocks.AMETHYST_BLOCK.getDefaultState(), 2 | 16);
        Gates.close(s, "tower_door");
    }

    private static void drawBeam(ServerWorld w) {
        if (mirrorOrigin == null) return;
        boolean any = false;
        for (var p : w.getPlayers()) if (p.squaredDistanceTo(Vec3d.ofCenter(mirrorOrigin)) < 34 * 34) any = true;
        if (!any) return;
        List<int[]> path = PuzzleLayouts.trace(mirrorFlips(w));
        DustParticleEffect gold = new DustParticleEffect(new Vector3f(1f, 0.85f, 0.35f), 1.1f);
        double y = mirrorOrigin.getY() + 0.55;
        double px = mirrorOrigin.getX() - 1 + 0.5, pz = mirrorOrigin.getZ() + PuzzleLayouts.MIRROR_ROW + 0.5;
        if (mirrorSource != null) { px = mirrorSource.getX() + 0.5; pz = mirrorSource.getZ() + 0.5; }
        for (int[] c : path) {
            double cx = mirrorOrigin.getX() + c[0] + 0.5, cz = mirrorOrigin.getZ() + c[1] + 0.5;
            boolean last = c == path.get(path.size() - 1);
            if (last && c.length == 3 && c[2] == 0) { cx = (px + cx) / 2; cz = (pz + cz) / 2; }   // stop at the obstacle face
            for (int k = 0; k < 4; k++) {
                double t = k / 4.0;
                w.spawnParticles(gold, px + (cx - px) * t, y, pz + (cz - pz) * t, 1, 0, 0, 0, 0);
            }
            px = cx; pz = cz;
        }
    }

    // ------------------------------------------------------------ Puzzle 3
    public static Set<Long> stones(ServerWorld w) {
        Set<Long> out = new HashSet<>();
        String[] g = PuzzleLayouts.SLIDE_GRID;
        for (int z = 0; z < g.length; z++) for (int x = 0; x < g[z].length(); x++)
            if (w.getBlockState(slideOrigin.add(x, 0, z)).isOf(ModBlocks.RUNE_STONE)) out.add(PuzzleLayouts.key(x, z));
        return out;
    }

    public static void pushStone(ServerPlayerEntity p, BlockPos pos) {
        var st = Game.state(p.getServer());
        if (slideOrigin == null || st.flag("puzzle3_solved")) return;
        ServerWorld w = p.getServerWorld();
        int x = pos.getX() - slideOrigin.getX(), z = pos.getZ() - slideOrigin.getZ();
        double ddx = pos.getX() + 0.5 - p.getX(), ddz = pos.getZ() + 0.5 - p.getZ();
        int dx = 0, dz = 0;
        if (Math.abs(ddx) > Math.abs(ddz)) dx = ddx > 0 ? 1 : -1; else dz = ddz > 0 ? 1 : -1;
        push(w, x, z, dx, dz);
        if (PuzzleLayouts.slideSolved(stones(w))) solveSlide(p.getServer());
    }

    /** Pushes the stone at grid (x,z); returns true if it moved. */
    public static boolean push(ServerWorld w, int x, int z, int dx, int dz) {
        Set<Long> s = stones(w);
        int[] to = PuzzleLayouts.slide(s, x, z, dx, dz);
        BlockPos from = slideOrigin.add(x, 0, z);
        if (to == null) { w.playSound(null, from, SoundEvents.BLOCK_DEEPSLATE_HIT, SoundCategory.BLOCKS, 1f, 0.5f); return false; }
        BlockPos dest = slideOrigin.add(to[0], 0, to[1]);
        w.setBlockState(from, Blocks.AIR.getDefaultState(), 2 | 16);
        w.setBlockState(dest, ModBlocks.RUNE_STONE.getDefaultState(), 2 | 16);
        w.playSound(null, dest, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.8f, 0.6f);
        for (int i = 0; i <= Math.abs(to[0] - x) + Math.abs(to[1] - z); i++)
            w.spawnParticles(ParticleTypes.CLOUD, from.getX() + 0.5 + dx * i, from.getY() + 0.1, from.getZ() + 0.5 + dz * i, 2, 0.2, 0.05, 0.2, 0.01);
        for (int[] so : PuzzleLayouts.slideSockets()) if (so[0] == to[0] && so[1] == to[1])
            w.playSound(null, dest, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 1f, 1.3f);
        return true;
    }

    public static void solveSlide(MinecraftServer s) {
        if (!Game.state(s).set("puzzle3_solved")) return;
        ServerWorld w = s.getOverworld();
        w.playSound(null, slideOrigin.add(4, 0, 4), SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.BLOCKS, 2f, 0.7f);
        Story.later(40, () -> Gates.open(s, "vault_door"));
    }

    public static void resetSlide(MinecraftServer s, boolean force) {
        var st = Game.state(s);
        if (st.flag("puzzle3_solved") && !force) return;
        st.clear("puzzle3_solved");
        ServerWorld w = s.getOverworld();
        String[] g = PuzzleLayouts.SLIDE_GRID;
        for (int z = 0; z < g.length; z++) for (int x = 0; x < g[z].length(); x++)
            if (g[z].charAt(x) != '#') w.setBlockState(slideOrigin.add(x, 0, z), Blocks.AIR.getDefaultState(), 2 | 16);
        for (int[] c : PuzzleLayouts.slideStart()) w.setBlockState(slideOrigin.add(c[0], 0, c[1]), ModBlocks.RUNE_STONE.getDefaultState(), 2 | 16);
        w.playSound(null, slideOrigin.add(4, 0, 4), SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 1f, 0.5f);
        if (force) Gates.close(s, "vault_door");
    }

    public static void tick(MinecraftServer s, long t) {
        if (t % 5 == 0) drawBeam(s.getOverworld());
    }
}
