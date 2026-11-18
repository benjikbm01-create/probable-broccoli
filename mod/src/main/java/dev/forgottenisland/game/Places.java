package dev.forgottenisland.game;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Named positions on the island (NPC schedule spots, checkpoints, triggers).
 * Pure Java so content can be validated without a running game. Points are registered by the
 * structure classes in dev.forgottenisland.build (each keeps its layout and its points together).
 */
public final class Places {
    /** A standing position (feet) and facing yaw. */
    public record P(double x, double y, double z, float yaw) {
        public P off(double dx, double dy, double dz) { return new P(x + dx, y + dy, z + dz, yaw); }
        public int bx() { return (int) Math.floor(x); }
        public int by() { return (int) Math.floor(y); }
        public int bz() { return (int) Math.floor(z); }
        public double dist2(double ox, double oy, double oz) { double a = x - ox, b = y - oy, c = z - oz; return a * a + b * b + c * c; }
    }

    private static final Map<String, P> ALL = new LinkedHashMap<>();
    private static boolean init;

    private Places() {}

    public static P add(String name, double x, double y, double z, float yaw) {
        P p = new P(x, y, z, yaw);
        if (ALL.put(name, p) != null) throw new IllegalStateException("Duplicate place " + name);
        return p;
    }

    public static synchronized void init() {
        if (init) return;
        init = true;
        dev.forgottenisland.build.Layout.registerPlaces();
    }

    public static P get(String name) { init(); return ALL.get(name); }
    public static P req(String name) {
        P p = get(name);
        if (p == null) throw new IllegalArgumentException("Unknown place " + name);
        return p;
    }
    public static Set<String> names() { init(); return ALL.keySet(); }
    public static Map<String, P> all() { init(); return ALL; }
}
