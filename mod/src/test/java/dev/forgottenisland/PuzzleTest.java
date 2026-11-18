package dev.forgottenisland;

import dev.forgottenisland.game.PuzzleLayouts;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PuzzleTest {
    @Test void stonesRequireTheSongOrder() {
        List<String> seq = new ArrayList<>();
        assertEquals(1, PuzzleLayouts.strike(seq, "heron"));
        assertEquals(-1, PuzzleLayouts.strike(seq, "owl"));      // wrong: resets
        assertTrue(seq.isEmpty());
        int r = 0;
        for (String s : PuzzleLayouts.SONG) r = PuzzleLayouts.strike(seq, s);
        assertEquals(4, r);
        assertEquals(Set.of(PuzzleLayouts.STONES), Set.of(PuzzleLayouts.SONG), "every stone appears in the song");
        assertNotEquals(Arrays.asList(PuzzleLayouts.STONES), Arrays.asList(PuzzleLayouts.SONG), "circle order must not give the answer away");
    }

    @Test void mirrorsHaveExactlyOneSolutionAndStartUnsolved() {
        int n = PuzzleLayouts.mirrorCells().size();
        assertEquals(PuzzleLayouts.MIRROR_START.length, n);
        int solutions = 0;
        for (int mask = 0; mask < (1 << n); mask++) {
            boolean[] f = new boolean[n];
            for (int i = 0; i < n; i++) f[i] = (mask >> i & 1) == 1;
            if (PuzzleLayouts.mirrorSolved(f)) solutions++;
        }
        assertEquals(1, solutions, "unique mirror solution");
        assertFalse(PuzzleLayouts.mirrorSolved(PuzzleLayouts.MIRROR_START));
    }

    @Test void glidingStonesAreSolvableInElevenPushes() {
        assertEquals(PuzzleLayouts.slideStart().size(), PuzzleLayouts.slideSockets().size());
        Set<Long> start = new HashSet<>();
        for (int[] s : PuzzleLayouts.slideStart()) start.add(PuzzleLayouts.key(s[0], s[1]));
        assertFalse(PuzzleLayouts.slideSolved(start));
        assertEquals(11, PuzzleLayouts.slideMinMoves());
    }

    @Test void stonesStopAtWallsAndCannotBePushedFromWalls() {
        Set<Long> s = new HashSet<>();
        s.add(PuzzleLayouts.key(2, 3));
        int[] to = PuzzleLayouts.slide(s, 2, 3, 1, 0);       // east along row 3 until the wall at x=8
        assertArrayEquals(new int[]{7, 3}, to);
        assertNull(PuzzleLayouts.slide(s, 1, 1, 1, 0), "a stone against the west wall can't be pushed east (nowhere to stand)");
    }
}
