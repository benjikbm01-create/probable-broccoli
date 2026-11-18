package dev.forgottenisland.game;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;

import java.util.*;
import java.util.function.Consumer;

/** Named trigger volumes. Entering sets flag "entered.&lt;name&gt;" and runs the zone's handler. */
public final class Zones {
    private Zones() {}
    public record Zone(String name, Box box, Consumer<ServerPlayerEntity> onEnter, boolean repeat) {}
    private static final Map<String, Zone> ALL = new LinkedHashMap<>();
    private static final Set<String> inside = new HashSet<>();

    public static void add(String name, double x1, double y1, double z1, double x2, double y2, double z2, Consumer<ServerPlayerEntity> onEnter) {
        add(name, x1, y1, z1, x2, y2, z2, onEnter, false);
    }
    public static void add(String name, double x1, double y1, double z1, double x2, double y2, double z2, Consumer<ServerPlayerEntity> onEnter, boolean repeat) {
        ALL.put(name, new Zone(name, new Box(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, Math.max(z1, z2) + 1), onEnter, repeat));
    }
    public static Map<String, Zone> all() { Places.init(); return ALL; }
    public static Zone get(String name) { Places.init(); return ALL.get(name); }

    public static boolean in(ServerPlayerEntity p, String zone) {
        Zone z = get(zone);
        return z != null && z.box().contains(p.getPos());
    }

    public static void tick(ServerPlayerEntity p) {
        var st = Game.state(p.getServer());
        if (!st.flag("intro_done")) return;
        for (Zone z : all().values()) {
            boolean now = z.box().contains(p.getPos());
            boolean was = inside.contains(z.name());
            if (now && !was) {
                inside.add(z.name());
                boolean first = st.set("entered." + z.name());
                if ((first || z.repeat()) && z.onEnter() != null) z.onEnter().accept(p);
            } else if (!now && was) inside.remove(z.name());
        }
    }

    public static void resetInside() { inside.clear(); }
}
