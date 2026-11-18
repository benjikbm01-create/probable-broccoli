package dev.forgottenisland.build;

import java.util.ArrayList;
import java.util.List;

/**
 * The island's landform as a pure function of (x, z). Coordinates: +X east, +Z south.
 * height() returns the y of the top solid block (players stand at height()+1). Sea surface is y=62.
 *
 * Layout: Gullhaven harbour village in the south-east around a sheltered bay; the Mosswood covering the
 * centre and west with Cairn Hill rising in the middle; a sheer northern plateau carrying Kestrel Keep,
 * reachable only through one gorge; Gull Rock, a sea stack off the south-east coast.
 */
public final class Terrain {
    private Terrain() {}
    public static final int SEA = 62;
    static final Noise N1 = new Noise(1001), N2 = new Noise(2002), N3 = new Noise(3003), N4 = new Noise(4004);

    public enum Region { SEA, BEACH, VILLAGE, FOREST, DEEP_FOREST, PLATEAU, CLIFF, GULL_ROCK }

    /** Flattened area: inside the rectangle the ground is exactly y; it blends back over 'fall' blocks. */
    public record Pad(int x1, int z1, int x2, int z2, int y, int fall) {
        double weight(double x, double z) {
            double dx = Math.max(Math.max(x1 - x, x - x2), 0), dz = Math.max(Math.max(z1 - z, z - z2), 0);
            double d = Math.sqrt(dx * dx + dz * dz);
            return fall <= 0 ? (d == 0 ? 1 : 0) : 1 - Noise.smooth(0, fall, d);
        }
    }
    public static final List<Pad> PADS = new ArrayList<>();
    public static void pad(int x1, int z1, int x2, int z2, int y, int fall) { PADS.add(new Pad(Math.min(x1, x2), Math.min(z1, z2), Math.max(x1, x2), Math.max(z1, z2), y, fall)); }

    /** Stream: polyline with water level per point; width in blocks. */
    public record StreamPt(double x, double z, double level, double width) {}
    public static final List<StreamPt> STREAM = List.of(
            new StreamPt(-74, -12, 85, 1.6), new StreamPt(-84, 2, 83, 1.8), new StreamPt(-96, 18, 81, 2.0),
            new StreamPt(-104, 30, 81, 2.2), new StreamPt(-108, 37, 81, 2.2),
            new StreamPt(-111, 44, 67, 2.6), new StreamPt(-120, 52, 66, 2.6), new StreamPt(-138, 60, 64, 2.8),
            new StreamPt(-160, 66, 63, 3.0), new StreamPt(-182, 70, 62, 3.4), new StreamPt(-205, 74, 62, 3.6));
    /** Index of the stream segment where the waterfall drops. */
    public static final int FALLS_SEGMENT = 4;

    static double ell(double x, double z, double cx, double cz, double rx, double rz) {
        double a = (x - cx) / rx, b = (z - cz) / rz;
        return Math.sqrt(a * a + b * b);
    }

    /** > 0 on land, < 0 at sea. */
    public static double land(double x, double z) {
        double a = 1 - ell(x, z, -30, -10, 178, 166);
        double b = 1 - ell(x, z, 12, -158, 102, 74);
        double c = 1 - ell(x, z, 106, 104, 84, 86);
        double L = Math.max(a, Math.max(b, c)) + 0.09 * N1.fbm(x / 70, z / 70, 3) + 0.05 * N4.fbm(x / 30, z / 30, 2);
        double bay = Math.min(ell(x, z, 106, 192, 40, 42), ell(x, z, 106, 230, 30, 60));
        if (bay < 1) L = Math.min(L, (bay - 1) * 0.6 - 0.01);
        double rock = 1 - ell(x, z, 186, 218, 9, 8);
        return Math.max(L, rock * 3);
    }

    public static double ridgeZ(double x) { return -103 + 3.5 * N1.at(x / 26.0, 7.3); }
    public static boolean inGorge(double x, double z) { return Math.abs(x + 25) <= 4.5 && z <= -92 && z >= -131; }

    static double cairn(double x, double z) {
        double dx = x + 55, dz = z + 35;
        return 38 * Math.exp(-(dx * dx + dz * dz) / (2 * 27.0 * 27.0));
    }

    /** Raw height before pads/streams. */
    static double rawHeight(double x, double z) {
        double L = land(x, z);
        if (L < 0) {
            double floor = 61 + L * 60 + 1.5 * N3.at(x / 18, z / 18);
            return Math.max(50, Math.min(60, floor));
        }
        if (ell(x, z, 186, 218, 9, 8) < 1.1) {   // Gull Rock
            double d = ell(x, z, 186, 218, 9, 8);
            return d < 0.85 ? 84 - d * 6 + 2 * N3.at(x / 3, z / 3) : 63 + (1 - d) * 40;
        }
        double ramp = Noise.smooth(0, 0.055, L);
        if (z < ridgeZ(x) && !inGorge(x, z)) {   // northern plateau with sheer sea cliffs
            if (L < 0.03) return 50;
            return 91 + 1.2 * N3.fbm(x / 40, z / 40, 2);
        }
        double forest = 67 + 9 * N2.fbm(x / 95, z / 95, 4) + 4 * N3.fbm(x / 28, z / 28, 2) + cairn(x, z);
        // keep the land just south of the ridge low so the cliff is always unclimbable
        double nearRidge = Noise.smooth(30, 6, z - ridgeZ(x));
        forest = forest * (1 - nearRidge * 0.25) + 70 * nearRidge * 0.25;
        double village = 63.5 + 7.5 * Noise.smooth(148, 62, z) + 0.8 * N3.fbm(x / 22, z / 22, 2);
        double vw = 1 - Noise.smooth(0.55, 1.0, ell(x, z, 102, 104, 72, 62));
        double base = forest + (village - forest) * vw;
        double h = SEA + (base - SEA) * ramp;
        if (inGorge(x, z)) {
            double t = Noise.clamp((-94 - z) / 34.0, 0, 1);
            h = 76 + 15 * t;
        }
        return h;
    }

    static double gorgeWalls(double x, double z, double h) {
        // vertical walls either side of the gorge (part of the plateau)
        double ax = Math.abs(x + 25);
        if (ax > 4.5 && ax < 11 && z < ridgeZ(x) + 1 && z > -132) return Math.max(h, 92 + 2 * N4.at(x / 6, z / 6));
        return h;
    }

    public static final class Column { public int top; public int water = Integer.MIN_VALUE; public boolean fall; public Region region; public boolean bed; }

    public static Column column(int x, int z) {
        Column c = new Column();
        double h = rawHeight(x + 0.5, z + 0.5);
        h = gorgeWalls(x + 0.5, z + 0.5, h);
        for (Pad p : PADS) { double w = p.weight(x + 0.5, z + 0.5); if (w > 0) h = h + (p.y - h) * w; }
        // stream
        double best = 1e9; double lvl = 0; double width = 0; int seg = -1; double segT = 0;
        for (int i = 0; i + 1 < STREAM.size(); i++) {
            StreamPt a = STREAM.get(i), b = STREAM.get(i + 1);
            double vx = b.x - a.x, vz = b.z - a.z, wx = x + 0.5 - a.x, wz = z + 0.5 - a.z;
            double t = Noise.clamp((vx * wx + vz * wz) / (vx * vx + vz * vz), 0, 1);
            double px = a.x + vx * t, pz = a.z + vz * t;
            double d = Math.hypot(x + 0.5 - px, z + 0.5 - pz);
            if (d < best) { best = d; lvl = a.level + (b.level - a.level) * t; width = a.width + (b.width - a.width) * t; seg = i; segT = t; }
        }
        if (seg == FALLS_SEGMENT) lvl = segT < 0.45 ? STREAM.get(seg).level : STREAM.get(seg + 1).level;
        int ilvl = (int) Math.round(lvl);
        if (land(x, z) < 0.01) { best = 1e9; }   // no stream channel or dikes out at sea
        boolean fallCol = seg == FALLS_SEGMENT && segT >= 0.38 && segT < 0.5;
        if (best < width) {
            c.top = Math.min((int) Math.round(h), ilvl - 1 - (best < width - 1 ? 1 : 0));
            c.water = ilvl; c.bed = true;
            if (fallCol) { c.fall = true; c.water = STREAM.get(seg).level > 0 ? (int) STREAM.get(seg).level : ilvl; c.top = (int) STREAM.get(seg + 1).level - 2; }
        } else if (best < width + 4) {
            double bank = ilvl + 0.5 + (best - width) * 0.9;
            c.top = (int) Math.round(Math.min(Math.max(h, ilvl), Math.max(bank, ilvl)));
            if (h < ilvl) c.top = ilvl;   // dike so the stream never spills
        } else c.top = (int) Math.round(h);
        if (c.top < SEA && c.water == Integer.MIN_VALUE) c.water = SEA;
        c.region = region(x, z, c.top);
        return c;
    }

    public static int height(int x, int z) { return column(x, z).top; }
    /** Standing y (feet) at a column. */
    public static int stand(int x, int z) { return height(x, z) + 1; }

    public static Region region(int x, int z, int top) {
        double L = land(x, z);
        if (ell(x, z, 186, 218, 9, 8) < 1.2) return Region.GULL_ROCK;
        if (top < SEA) return Region.SEA;
        if (z < ridgeZ(x) && !inGorge(x, z)) return Region.PLATEAU;
        if (Math.abs(x + 25) > 4.5 && Math.abs(x + 25) < 11 && z < ridgeZ(x) + 1 && z > -132) return Region.CLIFF;
        if (top <= 63 && L < 0.08) return Region.BEACH;
        if (ell(x, z, 102, 104, 72, 62) < 0.85) return Region.VILLAGE;
        if (ell(x, z, -140, 95, 55, 45) < 1 || ell(x, z, -150, -40, 50, 55) < 1) return Region.DEEP_FOREST;
        return Region.FOREST;
    }
}
