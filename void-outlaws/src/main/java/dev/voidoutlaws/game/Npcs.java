package dev.voidoutlaws.game;

import dev.voidoutlaws.state.CrewState;
import dev.voidoutlaws.world.Loc;
import dev.voidoutlaws.world.Structures;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;

import java.util.List;

/** The few talking characters. They are vanilla villagers with no AI that we keep in place. */
public final class Npcs {
    private Npcs() {}

    public record Def(String id, String name, Formatting color, Loc loc, int x, int y, int z, float yaw,
                      VillagerType type, VillagerProfession prof) {}

    public static final List<Def> DEFS = List.of(
            new Def("mags", "Old Mags", Formatting.GOLD, Loc.DUSTREACH, 20, 0, -2, 90f, VillagerType.DESERT, VillagerProfession.BUTCHER),
            new Def("baroness", "Baroness Kessa Brightcoil", Formatting.LIGHT_PURPLE, Loc.GILDED_MAW, 0, 1, -12, 0f, VillagerType.SWAMP, VillagerProfession.CLERIC),
            new Def("pell", "Customs Officer Pell", Formatting.AQUA, Loc.MERIDIAN, -18, 0, -1, -90f, VillagerType.PLAINS, VillagerProfession.CARTOGRAPHER)
    );

    public static Def byId(String id) {
        for (Def d : DEFS) if (d.id.equals(id)) return d;
        return null;
    }

    public static Def of(net.minecraft.entity.Entity e) {
        for (String t : e.getCommandTags()) if (t.startsWith("vo_npc_")) return byId(t.substring(7));
        return null;
    }

    public static BlockPos pos(CrewState s, Def d) {
        return Structures.origin(s, d.loc).add(d.x, d.y, d.z);
    }

    static void tick(MinecraftServer server) {
        CrewState s = Game.S();
        for (Def d : DEFS) {
            if (!s.built.contains(d.loc.id + ":" + Structures.VERSION)) continue;
            ServerWorld w = server.getWorld(d.loc.key);
            if (w == null) continue;
            BlockPos p = pos(s, d);
            boolean near = false;
            for (ServerPlayerEntity pl : Game.in(d.loc)) if (pl.getBlockPos().getSquaredDistance(p) < 40 * 40) near = true;
            if (!near || !w.isChunkLoaded(p)) continue;
            List<VillagerEntity> found = w.getEntitiesByClass(VillagerEntity.class, new Box(p).expand(8),
                    e -> e.getCommandTags().contains("vo_npc_" + d.id));
            if (found.isEmpty()) {
                spawn(w, d, p);
            } else {
                for (int i = 1; i < found.size(); i++) found.get(i).discard();
                VillagerEntity v = found.get(0);
                if (v.squaredDistanceTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5) > 2) {
                    v.refreshPositionAndAngles(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, d.yaw, 0);
                }
            }
        }
    }

    private static void spawn(ServerWorld w, Def d, BlockPos p) {
        VillagerEntity v = EntityType.VILLAGER.create(w);
        if (v == null) return;
        v.setVillagerData(v.getVillagerData().withType(d.type).withProfession(d.prof).withLevel(5));
        v.refreshPositionAndAngles(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, d.yaw, 0);
        v.setHeadYaw(d.yaw);
        v.setBodyYaw(d.yaw);
        v.setAiDisabled(true);
        v.setInvulnerable(true);
        v.setPersistent();
        v.setCustomName(Text.literal(d.name).formatted(d.color));
        v.setCustomNameVisible(true);
        v.addCommandTag("vo_npc_" + d.id);
        w.spawnEntity(v);
    }
}
