package dev.forgottenisland.game;

import dev.forgottenisland.content.Content;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/** Optional secrets: discovered by reaching hidden spots (zones named secret_&lt;id&gt;). */
public final class Secrets {
    private Secrets() {}

    public static void discover(ServerPlayerEntity p, String id) {
        var st = Game.state(p.getServer());
        if (!st.set("secret." + id)) return;
        String name = id;
        for (Content.Secret s : ContentHolder.get().secrets) if (s.id.equals(id)) name = s.name;
        int total = ContentHolder.get().secrets.size();
        Game.toast(p, "Secret found (" + st.var("secrets") + "/" + total + ")", Game.devMode(p.getServer()) ? id : name);
        p.getServerWorld().playSound(null, p.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 1f, 1.2f);
        p.getServerWorld().playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.4f, 1.6f);
    }
}
