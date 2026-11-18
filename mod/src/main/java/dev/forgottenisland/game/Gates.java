package dev.forgottenisland.game;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * Barriers that open as the story progresses. Each gate remembers its closed blocks so it can be
 * re-closed by a reset. State flag: "gate.&lt;id&gt;" (set = open).
 */
public final class Gates {
    private Gates() {}
    public record Gate(String id, Map<BlockPos, BlockState> closed, SoundEvent sound, boolean defaultOpen) {}
    private static final Map<String, Gate> ALL = new LinkedHashMap<>();

    public static Gate add(String id, SoundEvent sound) {
        Gate g = new Gate(id, new LinkedHashMap<>(), sound, false);
        ALL.put(id, g);
        return g;
    }
    /** A gate that is open unless something (a boss fight) shuts it. */
    public static Gate addOpen(String id, SoundEvent sound) {
        Gate g = new Gate(id, new LinkedHashMap<>(), sound, true);
        ALL.put(id, g);
        return g;
    }
    public static Map<String, Gate> all() { Places.init(); return ALL; }
    public static boolean isOpen(MinecraftServer s, String id) {
        Gate g = all().get(id);
        if (g != null && g.defaultOpen()) return !Game.state(s).flag("gate_shut." + id);
        return Game.state(s).flag("gate." + id);
    }
    private static void mark(MinecraftServer s, Gate g, boolean open) {
        if (g.defaultOpen()) { if (open) Game.state(s).clear("gate_shut." + g.id()); else Game.state(s).set("gate_shut." + g.id()); }
        else { if (open) Game.state(s).set("gate." + g.id()); else Game.state(s).clear("gate." + g.id()); }
    }

    public static void open(MinecraftServer s, String id) {
        Gate g = all().get(id);
        if (g == null) return;
        mark(s, g, true);
        ServerWorld w = s.getOverworld();
        BlockPos center = null;
        for (BlockPos p : g.closed().keySet()) {
            w.setBlockState(p, Blocks.AIR.getDefaultState(), 2 | 16);
            w.spawnParticles(ParticleTypes.CLOUD, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.01);
            center = p;
        }
        if (center != null && g.sound() != null) w.playSound(null, center, g.sound(), SoundCategory.BLOCKS, 1.2f, 0.8f);
    }

    public static void close(MinecraftServer s, String id) {
        Gate g = all().get(id);
        if (g == null) return;
        mark(s, g, false);
        ServerWorld w = s.getOverworld();
        g.closed().forEach((p, st) -> w.setBlockState(p, st, 2 | 16));
    }

    /** Checks that every gate's blocks match its saved state (used by the self-test). */
    public static boolean consistent(MinecraftServer s, String id) {
        Gate g = all().get(id);
        ServerWorld w = s.getOverworld();
        boolean open = isOpen(s, id);
        for (var e : g.closed().entrySet()) {
            BlockState now = w.getBlockState(e.getKey());
            if (open ? !now.isAir() : !now.equals(e.getValue())) return false;
        }
        return true;
    }
}
