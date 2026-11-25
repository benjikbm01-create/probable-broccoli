package dev.voidoutlaws.game;

import dev.voidoutlaws.menu.Menu;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/** Who is sitting at which ship station right now (not saved: drop-in/drop-out safe). */
public final class Roles {
    private Roles() {}

    public enum Role {
        PILOT("Pilot", Formatting.AQUA), GUNNER("Gunner", Formatting.RED), MECHANIC("Mechanic", Formatting.GREEN);
        public final String display;
        public final Formatting color;

        Role(String d, Formatting c) { display = d; color = c; }
    }

    private static final Map<Role, UUID> HOLDERS = new EnumMap<>(Role.class);

    public static ServerPlayerEntity holder(Role r) {
        UUID u = HOLDERS.get(r);
        if (u == null || Game.server == null) return null;
        ServerPlayerEntity p = Game.server.getPlayerManager().getPlayer(u);
        if (p == null || !(p.currentScreenHandler instanceof Menu m) || m.view.role() != r) {
            HOLDERS.remove(r);
            return null;
        }
        return p;
    }

    public static boolean manned(Role r) {
        return holder(r) != null;
    }

    /** Try to take a station. Returns false (and tells the player) if a crewmate is using it. */
    public static boolean claim(ServerPlayerEntity p, Role r) {
        ServerPlayerEntity cur = holder(r);
        if (cur != null && cur != p) {
            Game.msg(p, Game.name(cur) + " is already the " + r.display + ". Try another station!", Formatting.YELLOW);
            return false;
        }
        HOLDERS.put(r, p.getUuid());
        Game.bar(p, "You are the " + r.display.toUpperCase() + "!", r.color);
        return true;
    }

    public static void release(ServerPlayerEntity p, Role r) {
        if (p.getUuid().equals(HOLDERS.get(r))) HOLDERS.remove(r);
    }

    public static void releaseAll(ServerPlayerEntity p) {
        HOLDERS.values().removeIf(u -> u.equals(p.getUuid()));
    }

    static void clear() { HOLDERS.clear(); }
}
