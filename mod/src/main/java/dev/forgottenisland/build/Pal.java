package dev.forgottenisland.build;

import net.minecraft.block.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Weighted block palette. Picks deterministically by world position so rebuilds are identical. */
public final class Pal {
    private final List<BlockState> states = new ArrayList<>();
    private final List<Integer> cum = new ArrayList<>();
    private int total;
    private int salt;

    /** e.g. Pal.of("stone_bricks:6", "mossy_stone_bricks:2", "cracked_stone_bricks:1") */
    public static Pal of(String... specs) {
        Pal p = new Pal();
        for (String s : specs) {
            int w = 1; String k = s;
            int i = s.lastIndexOf(':');
            if (i > 0 && s.substring(i + 1).matches("\\d+")) { w = Integer.parseInt(s.substring(i + 1)); k = s.substring(0, i); }
            p.total += w; p.states.add(B.s(k)); p.cum.add(p.total);
        }
        p.salt = specs.length > 0 ? specs[0].hashCode() : 0;
        return p;
    }

    public BlockState pick(int x, int y, int z) {
        if (states.size() == 1) return states.get(0);
        int r = (int) (Noise.hash(x * 31 + y, z * 17 - y, salt) * total);
        for (int i = 0; i < cum.size(); i++) if (r < cum.get(i)) return states.get(i);
        return states.get(states.size() - 1);
    }
}
