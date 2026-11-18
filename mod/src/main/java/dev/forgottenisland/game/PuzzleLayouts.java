package dev.forgottenisland.game;

import java.util.*;

/**
 * Pure puzzle logic + layouts (shared by the world builder, the runtime and the unit tests).
 * Grid x maps to world +X (east) and grid z to world +Z (south).
 */
public final class PuzzleLayouts {
    private PuzzleLayouts() {}

    // ------------------------------------------------------------ Puzzle 1: the Singing Stones
    /** Stone carvings in the order they stand around the circle (clockwise from the road). */
    public static final String[] STONES = {"owl", "heron", "hare", "stag"};
    /** The order sung in the lullaby. */
    public static final String[] SONG = {"heron", "stag", "owl", "hare"};

    /** Feeds one strike; returns -1 wrong (reset), 0..3 progress, 4 = solved. */
    public static int strike(List<String> sequence, String stone) {
        sequence.add(stone);
        for (int i = 0; i < sequence.size(); i++) if (!sequence.get(i).equals(SONG[i])) { sequence.clear(); return -1; }
        return sequence.size();
    }

    // ------------------------------------------------------------ Puzzle 2: the Sun Court mirrors
    /** '#' pillar, 'M' mirror, '.' floor. Light enters from the west edge on row MIRROR_ROW heading east;
     *  it must leave through the north edge at column MIRROR_TARGET_COL. */
    public static final String[] MIRROR_GRID = {
            "......#..",
            "....MM...",
            "..#......",
            "........M",
            "..#....#.",
            "...#..#..",
            "....M...M"};
    public static final int MIRROR_ROW = 3, MIRROR_TARGET_COL = 5;
    /** Initial mirror orientations (true = '\'), in row-major mirror order. Unsolved by design. */
    public static final boolean[] MIRROR_START = {true, true, false, false, true};

    public static List<int[]> mirrorCells() {
        List<int[]> out = new ArrayList<>();
        for (int z = 0; z < MIRROR_GRID.length; z++) for (int x = 0; x < MIRROR_GRID[z].length(); x++) if (MIRROR_GRID[z].charAt(x) == 'M') out.add(new int[]{x, z});
        return out;
    }

    /** Traces the beam. Returns the visited cells; last element {x,z,1} if it reached the target. */
    public static List<int[]> trace(boolean[] flips) {
        List<int[]> mirrors = mirrorCells();
        List<int[]> path = new ArrayList<>();
        int x = -1, z = MIRROR_ROW, dx = 1, dz = 0, W = MIRROR_GRID[0].length(), H = MIRROR_GRID.length;
        for (int steps = 0; steps < 200; steps++) {
            x += dx; z += dz;
            if (z == -1 && x == MIRROR_TARGET_COL && dz == -1) { path.add(new int[]{x, z, 1}); return path; }
            if (x < 0 || z < 0 || x >= W || z >= H) { path.add(new int[]{x, z, 0}); return path; }
            if (MIRROR_GRID[z].charAt(x) == '#') { path.add(new int[]{x, z, 0}); return path; }
            path.add(new int[]{x, z, 0});
            for (int i = 0; i < mirrors.size(); i++) {
                if (mirrors.get(i)[0] == x && mirrors.get(i)[1] == z) {
                    int ndx, ndz;
                    if (!flips[i]) { ndx = -dz; ndz = -dx; }     // '/'  E->N, N->E, W->S, S->W
                    else { ndx = dz; ndz = dx; }                  // '\'  E->S, S->E, W->N, N->W
                    dx = ndx; dz = ndz;
                }
            }
        }
        return path;
    }
    public static boolean mirrorSolved(boolean[] flips) {
        List<int[]> p = trace(flips);
        return !p.isEmpty() && p.get(p.size() - 1).length == 3 && p.get(p.size() - 1)[2] == 1;
    }

    // ------------------------------------------------------------ Puzzle 3: the Gliding Stones
    /** '#' wall, 'S' rune stone start, 'o' socket. Stones glide until they hit a wall or another stone.
     *  A stone can only be pushed from a side the player can stand on (not a wall). */
    public static final String[] SLIDE_GRID = {
            "#########",
            "#......##",
            "#...S#..#",
            "#.S.....#",
            "#....#..#",
            "#.......#",
            "##.S..o##",
            "#..#o.o##",
            "#########"};

    public static boolean slideWall(int x, int z) {
        return z < 0 || z >= SLIDE_GRID.length || x < 0 || x >= SLIDE_GRID[z].length() || SLIDE_GRID[z].charAt(x) == '#';
    }
    public static List<int[]> slideStart() { return cells(SLIDE_GRID, 'S'); }
    public static List<int[]> slideSockets() { return cells(SLIDE_GRID, 'o'); }
    private static List<int[]> cells(String[] g, char c) {
        List<int[]> out = new ArrayList<>();
        for (int z = 0; z < g.length; z++) for (int x = 0; x < g[z].length(); x++) if (g[z].charAt(x) == c) out.add(new int[]{x, z});
        return out;
    }

    /** Where a stone at (x,z) ends up when pushed in (dx,dz); returns null if it cannot move. */
    public static int[] slide(Set<Long> stones, int x, int z, int dx, int dz) {
        if (slideWall(x - dx, z - dz)) return null;          // nowhere to stand to push
        int nx = x, nz = z;
        while (!slideWall(nx + dx, nz + dz) && !stones.contains(key(nx + dx, nz + dz))) { nx += dx; nz += dz; }
        return (nx == x && nz == z) ? null : new int[]{nx, nz};
    }
    public static long key(int x, int z) { return ((long) x << 32) | (z & 0xffffffffL); }

    public static boolean slideSolved(Set<Long> stones) {
        for (int[] s : slideSockets()) if (!stones.contains(key(s[0], s[1]))) return false;
        return true;
    }

    /** Breadth-first solver: minimum number of pushes from the start layout (-1 = unsolvable). */
    public static int slideMinMoves() {
        Set<Long> start = new HashSet<>();
        for (int[] s : slideStart()) start.add(key(s[0], s[1]));
        Deque<Set<Long>> q = new ArrayDeque<>(); Map<Set<Long>, Integer> dist = new HashMap<>();
        q.add(start); dist.put(start, 0);
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!q.isEmpty()) {
            Set<Long> s = q.poll();
            if (slideSolved(s)) return dist.get(s);
            for (long k : s) {
                int x = (int) (k >> 32), z = (int) k;
                for (int[] d : dirs) {
                    int[] to = slide(s, x, z, d[0], d[1]);
                    if (to == null) continue;
                    Set<Long> n = new HashSet<>(s); n.remove(k); n.add(key(to[0], to[1]));
                    if (!dist.containsKey(n)) { dist.put(n, dist.get(s) + 1); q.add(n); }
                }
            }
        }
        return -1;
    }
}
