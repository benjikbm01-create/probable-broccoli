package dev.voidoutlaws.menu;

import dev.voidoutlaws.game.Roles;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** A server-side "button menu" drawn with items in a chest screen. */
public abstract class View {
    public abstract Text title();

    public abstract int rows();

    public abstract void render(ServerPlayerEntity p, Slots g);

    public void click(ServerPlayerEntity p, int slot, boolean right) {}

    public void tick(ServerPlayerEntity p) {}

    public void closed(ServerPlayerEntity p) {}

    public boolean valid(ServerPlayerEntity p) { return true; }

    /** Station role this view represents (or null). */
    public Roles.Role role() { return null; }
}
