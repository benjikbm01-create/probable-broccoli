package dev.forgottenisland.build;

import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.Chunk;

/** Writes the Terrain model into the world: rock strata, soils, sea floor, the stream and its waterfall, biomes. */
public final class TerrainBuilder {
    private TerrainBuilder() {}
    public static final int MIN = -270, MAX = 270;

    static final BlockState WATER = B.s("water"), AIR = B.s("air"), FALL = B.s("water[level=8]");

    /** Rock that shows on cliff faces: banded strata with noise. */
    static BlockState strata(int x, int y, int z) {
        double n = Terrain.N4.at(x / 14.0, z / 14.0) * 3 + Terrain.N2.at(x / 5.0 + y / 9.0, z / 5.0) * 1.2;
        double band = y + n;
        double h = Noise.hash(x, z, y);
        if (band < 56) return h < 0.3 ? B.s("deepslate") : B.s("tuff");
        int k = (int) Math.floor(band / 2.6);
        if (h < 0.04) return B.s("cobblestone");
        if (h < 0.06) return B.s("mossy_cobblestone");
        return switch (Math.floorMod(k, 9)) {
            case 0, 4 -> B.s("stone"); case 1, 6 -> B.s("andesite"); case 2 -> h < 0.5 ? B.s("tuff") : B.s("stone");
            case 3 -> B.s("stone"); case 5 -> h < 0.6 ? B.s("diorite") : B.s("calcite"); case 7 -> B.s("tuff"); default -> h < 0.3 ? B.s("granite") : B.s("andesite");
        };
    }

    public static void build(ServerWorld w, int x1, int z1, int x2, int z2) {
        B b = new B(w, 0, 0, 0, 0);
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                Terrain.Column c = Terrain.column(x, z);
                double L = Terrain.land(x, z);
                if (L < -0.35 && c.top <= 50) continue;   // untouched flat ocean
                int top = c.top;
                // slope (for rocky faces)
                int slope = 0;
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) slope = Math.max(slope, top - Terrain.height(x + d[0], z + d[1]));
                // rock body
                for (int y = 48; y <= top; y++) b.wset(x, y, z, strata(x, y, z));
                // clear above
                int clearTo = Math.max(Terrain.SEA, top) + 2;
                for (int y = top + 1; y <= Math.min(clearTo, top + 60); y++) b.wset(x, y, z, AIR);
                surface(b, x, z, top, slope, c);
                // water
                if (c.water != Integer.MIN_VALUE && c.water > top) {
                    for (int y = top + 1; y <= c.water; y++) b.wset(x, y, z, c.fall ? FALL : WATER);
                    if (c.fall) b.wset(x, c.water, z, WATER);
                    if (c.region == Terrain.Region.SEA) seaLife(b, x, z, top, c.water);
                }
            }
        }
        roughenCliffs(b, x1, z1, x2, z2);
        biomes(w, x1, z1, x2, z2);
    }

    /** Breaks up sheer faces: rock buttresses and overhangs from 3D noise, plus moss and vines on the faces. */
    static void roughenCliffs(B b, int x1, int z1, int x2, int z2) {
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) {
            int top = Terrain.height(x, z);
            if (top < 60) continue;
            int hi = top;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) hi = Math.max(hi, Terrain.height(x + d[0], z + d[1]));
            for (int[] d : new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}}) hi = Math.max(hi, Terrain.height(x + d[0], z + d[1]));
            if (hi - top < 5) continue;                                   // only at the foot of tall faces
            if (Terrain.inGorge(x, z) && Math.abs(x + 25) < 4) continue;   // keep the gorge road clear
            for (int y = top + 1; y < Math.min(hi - 1, top + 4); y++) {
                double n = Terrain.N4.at(x / 5.0 + y / 7.0, z / 5.0 - y / 9.0) + Terrain.N2.at(y / 4.0, (x + z) / 6.0) * 0.5;
                boolean nearFace = false;
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) if (Terrain.height(x + d[0], z + d[1]) >= y) nearFace = true;
                if (!nearFace) continue;
                if (n > 0.28) b.wset(x, y, z, strata(x, y, z));
                else if (n > 0.18 && Noise.hash(x, y, z) < 0.4) b.wset(x, y, z, B.s(Noise.hash(x, z, y) < 0.5 ? "mossy_cobblestone" : "moss_block"));
            }
        }
        // vines and moss hanging on faces
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) {
            int top = Terrain.height(x, z);
            if (top < 60) continue;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nt = Terrain.height(x + d[0], z + d[1]);
                if (nt - top < 6 || Noise.hash(x, z, d[0] * 3 + d[1]) > 0.22) continue;
                // never on the plateau's southern face or the gorge: vines are climbable
                if (z > -138 && z < -84) continue;
                String face = d[0] == 1 ? "east" : d[0] == -1 ? "west" : d[1] == 1 ? "south" : "north";
                int len = 3 + (int) (Noise.hash(x, z, 17) * 9);
                for (int y = nt - 1; y > Math.max(top, nt - len); y--) {
                    var at = b.wget(x, y, z);
                    if (!at.isAir()) break;
                    if (!b.wget(x + d[0], y, z + d[1]).isSolid()) break;
                    b.wset(x, y, z, B.s("vine[" + face + "=true]"));
                }
            }
        }
    }

    static void surface(B b, int x, int z, int top, int slope, Terrain.Column c) {
        double h = Noise.hash(x, z, 7);
        double n = Terrain.N3.at(x / 9.0, z / 9.0);
        if (c.bed) { b.wset(x, top, z, h < 0.6 ? B.s("gravel") : h < 0.85 ? B.s("sand") : B.s("clay")); return; }
        switch (c.region) {
            case SEA -> {
                BlockState s = top < 54 ? (h < 0.5 ? B.s("gravel") : B.s("sand")) : (n > 0.35 ? B.s("gravel") : h < 0.08 ? B.s("clay") : B.s("sand"));
                b.wset(x, top, z, s); b.wset(x, top - 1, z, B.s("sand"));
            }
            case BEACH -> {
                b.wset(x, top, z, n > 0.45 ? B.s("gravel") : B.s("sand"));
                b.wset(x, top - 1, z, B.s("sand")); b.wset(x, top - 2, z, B.s("sandstone"));
            }
            case GULL_ROCK -> {
                if (slope < 2 && top > 80) b.wset(x, top, z, B.s("grass_block"));
            }
            case CLIFF -> {}
            default -> {
                if (slope >= 3) {                     // exposed rocky slope
                    if (slope < 5) b.wset(x, top, z, n > 0 ? B.s("stone") : B.s("grass_block"));
                    return;
                }
                boolean deep = c.region == Terrain.Region.DEEP_FOREST;
                BlockState top0;
                if (c.region == Terrain.Region.PLATEAU) {
                    top0 = n > 0.6 ? (h < 0.5 ? B.s("stone") : B.s("andesite")) : n > 0.5 ? B.s("gravel") : B.s("grass_block");
                } else if (deep) {
                    top0 = n > 0.25 ? B.s("podzol") : n < -0.35 ? B.s("moss_block") : B.s("grass_block");
                } else if (c.region == Terrain.Region.FOREST) {
                    top0 = n > 0.5 ? B.s("podzol") : n < -0.55 ? B.s("moss_block") : B.s("grass_block");
                } else {
                    top0 = B.s("grass_block");
                }
                b.wset(x, top, z, top0);
                b.wset(x, top - 1, z, B.s("dirt")); b.wset(x, top - 2, z, B.s("dirt"));
                if (h > 0.6) b.wset(x, top - 3, z, B.s("dirt"));
            }
        }
    }

    static void seaLife(B b, int x, int z, int top, int water) {
        double h = Noise.hash(x, z, 91);
        int depth = water - top;
        if (depth < 2) return;
        if (h < 0.05 && depth > 4) {           // kelp
            int len = 2 + (int) (Noise.hash(x, z, 92) * (depth - 2));
            for (int i = 1; i < len; i++) b.wset(x, top + i, z, B.s("kelp_plant"));
            b.wset(x, top + len, z, B.s("kelp[age=25]"));
        } else if (h < 0.16) {
            if (depth > 2 && h < 0.08) { b.wset(x, top + 1, z, B.s("tall_seagrass[half=lower]")); b.wset(x, top + 2, z, B.s("tall_seagrass[half=upper]")); }
            else b.wset(x, top + 1, z, B.s("seagrass"));
        }
    }

    static RegistryKey<Biome> biomeFor(int x, int z) {
        Terrain.Column c = Terrain.column(x, z);
        double L = Terrain.land(x, z);
        return switch (c.region) {
            case SEA -> L < -0.12 ? BiomeKeys.OCEAN : BiomeKeys.LUKEWARM_OCEAN;
            case BEACH -> BiomeKeys.BEACH;
            case GULL_ROCK -> BiomeKeys.STONY_SHORE;
            case VILLAGE -> BiomeKeys.PLAINS;
            case DEEP_FOREST -> BiomeKeys.DARK_FOREST;
            case PLATEAU, CLIFF -> BiomeKeys.MEADOW;
            default -> (x < -100 && z < 0) ? BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA : BiomeKeys.FOREST;
        };
    }

    static void biomes(ServerWorld w, int x1, int z1, int x2, int z2) {
        var reg = w.getRegistryManager().get(RegistryKeys.BIOME);
        java.util.Map<RegistryKey<Biome>, RegistryEntry<Biome>> entries = new java.util.HashMap<>();
        var sampler = w.getChunkManager().getNoiseConfig().getMultiNoiseSampler();
        for (int cx = x1 >> 4; cx <= x2 >> 4; cx++) for (int cz = z1 >> 4; cz <= z2 >> 4; cz++) {
            Chunk chunk = w.getChunk(cx, cz);
            chunk.populateBiomes((bx, by, bz, noise) -> {
                int x = bx << 2, z = bz << 2;
                RegistryKey<Biome> k = biomeFor(x + 2, z + 2);
                return entries.computeIfAbsent(k, kk -> reg.entryOf(kk));
            }, sampler);
            chunk.setNeedsSaving(true);
        }
    }

    public static ChunkPos chunk(int x, int z) { return new ChunkPos(x >> 4, z >> 4); }
}
