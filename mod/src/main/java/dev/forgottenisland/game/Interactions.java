package dev.forgottenisland.game;

import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** World interactions that aren't puzzles: waystone lamps, keyed locks, shortcut levers. */
public final class Interactions {
    private Interactions() {}
    public static final List<BlockPos> WAYSTONES = new ArrayList<>();

    public static void waystone(BlockPos pos) {
        int idx = WAYSTONES.size();
        WAYSTONES.add(pos.toImmutable());
        Puzzles.on(pos, (p, at, held) -> {
            var st = Game.state(p.getServer());
            ServerWorld w = p.getServerWorld();
            if (st.flag("waystone." + idx)) { p.sendMessage(msg("The lamp burns steadily."), true); return true; }
            if (!"tinderbox".equals(KeyItems.idOf(held))) { p.sendMessage(msg("An old waystone lamp, cold for years. It needs a flame."), true); return true; }
            st.set("waystone." + idx);
            BlockState b = w.getBlockState(at);
            if (b.contains(CampfireBlock.LIT)) w.setBlockState(at, b.with(CampfireBlock.LIT, true), 2 | 16);
            w.playSound(null, at, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1f, 1f);
            w.playSound(null, at, SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS, 1f, 1f);
            int lit = 0;
            for (int i = 0; i < WAYSTONES.size(); i++) if (st.flag("waystone." + i)) lit++;
            st.setVar("waystones_lit", lit);
            p.sendMessage(msg("The waystone lamp flickers to life (" + lit + "/" + WAYSTONES.size() + ")."), true);
            return true;
        });
    }

    public static void resetWaystones(MinecraftServer s) {
        ServerWorld w = s.getOverworld();
        var st = Game.state(s);
        for (int i = 0; i < WAYSTONES.size(); i++) {
            st.clear("waystone." + i);
            BlockState b = w.getBlockState(WAYSTONES.get(i));
            if (b.contains(CampfireBlock.LIT)) w.setBlockState(WAYSTONES.get(i), b.with(CampfireBlock.LIT, false), 2 | 16);
        }
        st.setVar("waystones_lit", 0);
    }

    /** A gate that opens when the player uses it while holding a key item. */
    public static void lock(BlockPos pos, String gate, String key, boolean consume, String lockedMsg) {
        Puzzles.on(pos, (p, at, held) -> {
            if (Gates.isOpen(p.getServer(), gate)) return false;
            if (key != null && !key.equals(KeyItems.idOf(held))) { p.sendMessage(msg(lockedMsg), true); return true; }
            if (key != null && consume) KeyItems.take(p, key, 1);
            Gates.open(p.getServer(), gate);
            return true;
        });
    }

    /** A lever/wheel that opens a shortcut gate from one side. */
    public static void lever(BlockPos pos, String gate, String flag) {
        Puzzles.on(pos, (p, at, held) -> {
            if (!Gates.isOpen(p.getServer(), gate)) {
                Gates.open(p.getServer(), gate);
                if (flag != null) Game.state(p.getServer()).set(flag);
                p.sendMessage(msg("Somewhere nearby, old mechanisms grind open."), true);
            }
            return false;   // let the lever visibly flip
        });
    }

    static Text msg(String s) { return Text.literal(s).formatted(Formatting.GRAY, Formatting.ITALIC); }
}
