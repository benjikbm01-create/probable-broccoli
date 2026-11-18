package dev.forgottenisland.build;

/** Deterministic 2D value-gradient noise + fBm (no Minecraft dependency). */
public final class Noise {
    private final int[] perm = new int[512];

    public Noise(long seed) {
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;
        java.util.Random r = new java.util.Random(seed);
        for (int i = 255; i > 0; i--) { int j = r.nextInt(i + 1); int t = p[i]; p[i] = p[j]; p[j] = t; }
        for (int i = 0; i < 512; i++) perm[i] = p[i & 255];
    }

    private static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private static double grad(int h, double x, double y) {
        switch (h & 7) {
            case 0: return x + y; case 1: return -x + y; case 2: return x - y; case 3: return -x - y;
            case 4: return x; case 5: return -x; case 6: return y; default: return -y;
        }
    }

    /** Perlin noise in roughly [-1, 1]. */
    public double at(double x, double y) {
        int X = (int) Math.floor(x) & 255, Y = (int) Math.floor(y) & 255;
        double xf = x - Math.floor(x), yf = y - Math.floor(y);
        double u = fade(xf), v = fade(yf);
        int aa = perm[perm[X] + Y], ab = perm[perm[X] + Y + 1], ba = perm[perm[X + 1] + Y], bb = perm[perm[X + 1] + Y + 1];
        double x1 = lerp(u, grad(aa, xf, yf), grad(ba, xf - 1, yf));
        double x2 = lerp(u, grad(ab, xf, yf - 1), grad(bb, xf - 1, yf - 1));
        return lerp(v, x1, x2) * 0.95;
    }

    public double fbm(double x, double y, int octaves) {
        double sum = 0, amp = 1, f = 1, norm = 0;
        for (int i = 0; i < octaves; i++) { sum += at(x * f, y * f) * amp; norm += amp; amp *= 0.5; f *= 2.03; }
        return sum / norm;
    }

    /** Deterministic hash in [0,1) for scattering decorations. */
    public static double hash(int x, int z, int salt) {
        long h = x * 374761393L + z * 668265263L + salt * 2147483647L;
        h = (h ^ (h >> 13)) * 1274126177L;
        h ^= (h >> 16);
        return (h & 0xffffff) / (double) 0x1000000;
    }

    static double lerp(double t, double a, double b) { return a + t * (b - a); }
    public static double clamp(double v, double lo, double hi) { return v < lo ? lo : v > hi ? hi : v; }
    public static double smooth(double e0, double e1, double x) { double t = clamp((x - e0) / (e1 - e0), 0, 1); return t * t * (3 - 2 * t); }
}
