package dev.forgottenisland.core;

import dev.forgottenisland.game.KeyItems;
import net.minecraft.server.network.ServerPlayerEntity;

/** StateView bound to a specific player (for has:item and time checks). */
public record GameCtx(StateData state, ServerPlayerEntity player) implements StateView {
    @Override public boolean flag(String name) {
        if (name.startsWith("at:")) return player != null && dev.forgottenisland.game.Zones.in(player, name.substring(3));
        return state.flag(name);
    }
    @Override public int var(String name) { return state.var(name); }
    @Override public boolean hasItem(String id) { return player != null && KeyItems.count(player, id) > 0; }
    @Override public long timeOfDay() { return player != null ? player.getServerWorld().getTimeOfDay() : 6000; }
}
