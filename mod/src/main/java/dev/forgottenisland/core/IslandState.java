package dev.forgottenisland.core;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

/** World-saved story state (data/forgotten_island.dat in the world folder). */
public class IslandState extends PersistentState {
    public static final String ID = "forgotten_island";
    public final Live data = new Live();
    /** Incremented on every change; consumers compare to detect updates cheaply. */
    public int revision;

    public final class Live extends StateData {
        @Override protected void changed() { revision++; markDirty(); }
    }

    public static final Type<IslandState> TYPE = new Type<>(IslandState::new, IslandState::fromNbt, null);

    public static IslandState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, ID);
    }

    public static IslandState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        IslandState s = new IslandState();
        NbtList flags = nbt.getList("flags", 8);
        for (int i = 0; i < flags.size(); i++) s.data.flags().add(flags.getString(i));
        NbtCompound vars = nbt.getCompound("vars");
        for (String k : vars.getKeys()) s.data.vars().put(k, vars.getInt(k));
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtList flags = new NbtList();
        for (String f : data.flags()) flags.add(NbtString.of(f));
        nbt.put("flags", flags);
        NbtCompound vars = new NbtCompound();
        data.vars().forEach(vars::putInt);
        nbt.put("vars", vars);
        nbt.putInt("format", 1);
        return nbt;
    }
}
