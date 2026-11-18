package dev.forgottenisland.game;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/** Respawn checkpoints. The player always comes back at the latest one reached. */
public final class Checkpoints {
    private Checkpoints() {}
    public static final List<String> ORDER = List.of("cp_dock", "cp_inn", "cp_forest", "cp_stones", "cp_keep_gate", "cp_keep_court",
            "cp_undercroft", "cp_echoes", "cp_vault", "cp_wellspring", "cp_lantern", "cp_square");

    public static String current(net.minecraft.server.MinecraftServer s) {
        int i = Game.state(s).var("checkpoint");
        return ORDER.get(Math.max(0, Math.min(ORDER.size() - 1, i)));
    }

    public static void set(ServerPlayerEntity p, String name) {
        int idx = ORDER.indexOf(name);
        if (idx < 0) return;
        var st = Game.state(p.getServer());
        boolean changed = st.var("checkpoint") != idx || !st.flag("cp_set");
        st.setVar("checkpoint", idx);
        st.set("cp_set");
        apply(p);
        if (changed && st.flag("intro_done")) p.sendMessage(Text.literal("Checkpoint reached.").formatted(Formatting.DARK_AQUA, Formatting.ITALIC), true);
    }

    public static void apply(ServerPlayerEntity p) {
        Places.P cp = Places.get(current(p.getServer()));
        if (cp == null) return;
        p.setSpawnPoint(World.OVERWORLD, BlockPos.ofFloored(cp.x(), cp.y(), cp.z()), cp.yaw(), true, false);
    }

    /** After respawn, make sure the player stands exactly on the checkpoint. */
    public static void respawn(ServerPlayerEntity p) {
        Places.P cp = Places.get(current(p.getServer()));
        if (cp == null) return;
        p.teleport(p.getServer().getOverworld(), cp.x(), cp.y(), cp.z(), cp.yaw(), 0);
    }
}
