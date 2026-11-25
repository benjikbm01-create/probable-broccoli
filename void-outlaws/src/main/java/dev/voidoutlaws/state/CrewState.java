package dev.voidoutlaws.state;

import dev.voidoutlaws.world.Loc;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.PersistentState;

import java.util.*;

/** Everything the crew shares, saved with the world. */
public class CrewState extends PersistentState {
    public static final Type<CrewState> TYPE = new Type<>(CrewState::new, CrewState::read, null);

    public int heat = 0;
    public int hull = 100;
    public int pot = 0;
    public String transponder = "LL-" + (1000 + new Random().nextInt(9000));
    public Loc shipLoc = Loc.BURROW;
    public boolean jumping = false;
    public Loc jumpDest = Loc.DUSTREACH;
    public int story = 0;
    public int storyCounter = 0;
    public String ending = "";
    public int totalJobs = 0;
    public int jumpsSinceHunter = 5;
    public long heistCooldownUntil = 0;
    public boolean rulesSet = false;
    public Job job = null;

    public final Map<UUID, Integer> credits = new HashMap<>();
    public final Map<UUID, Integer> tutStep = new HashMap<>();
    public final Set<UUID> tutDone = new HashSet<>();
    public final Set<UUID> hintsOff = new HashSet<>();
    public final Set<UUID> known = new HashSet<>();
    public final Map<UUID, Integer> grudge = new HashMap<>();
    public final Map<UUID, Long> disguiseUntil = new HashMap<>();
    public final Map<UUID, UUID> companion = new HashMap<>();
    public final Map<String, Integer> rep = new HashMap<>();
    public final Map<String, Long> pads = new HashMap<>();
    public final Set<String> built = new HashSet<>();
    public final Set<Long> openedCrates = new HashSet<>();

    public final SimpleInventory cargo = new SimpleInventory(27);
    public final SimpleInventory hidden = new SimpleInventory(9);

    public CrewState() {
        cargo.addListener(i -> markDirty());
        hidden.addListener(i -> markDirty());
    }

    public static CrewState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, "voidoutlaws_crew");
    }

    public int credits(UUID id) { return credits.getOrDefault(id, 0); }

    public void addCredits(UUID id, int amount) {
        credits.put(id, Math.max(0, credits(id) + amount));
        markDirty();
    }

    public int rep(String faction) { return rep.getOrDefault(faction, 0); }

    public void addRep(String faction, int amount) {
        rep.put(faction, Math.max(-100, Math.min(100, rep(faction) + amount)));
        markDirty();
    }

    // ---------------------------------------------------------------- NBT
    private static NbtCompound uuidIntMap(Map<UUID, Integer> m) {
        NbtCompound c = new NbtCompound();
        m.forEach((k, v) -> c.putInt(k.toString(), v));
        return c;
    }

    private static void readUuidIntMap(NbtCompound c, Map<UUID, Integer> m) {
        for (String k : c.getKeys()) {
            try { m.put(UUID.fromString(k), c.getInt(k)); } catch (IllegalArgumentException ignored) {}
        }
    }

    private static NbtList uuidSet(Set<UUID> s) {
        NbtList l = new NbtList();
        for (UUID u : s) l.add(NbtString.of(u.toString()));
        return l;
    }

    private static void readUuidSet(NbtList l, Set<UUID> s) {
        for (int i = 0; i < l.size(); i++) {
            try { s.add(UUID.fromString(l.getString(i))); } catch (IllegalArgumentException ignored) {}
        }
    }

    private static NbtCompound writeInv(SimpleInventory inv, RegistryWrapper.WrapperLookup reg) {
        NbtCompound c = new NbtCompound();
        DefaultedList<ItemStack> list = DefaultedList.ofSize(inv.size(), ItemStack.EMPTY);
        for (int i = 0; i < inv.size(); i++) list.set(i, inv.getStack(i));
        Inventories.writeNbt(c, list, reg);
        return c;
    }

    private static void readInv(NbtCompound c, SimpleInventory inv, RegistryWrapper.WrapperLookup reg) {
        DefaultedList<ItemStack> list = DefaultedList.ofSize(inv.size(), ItemStack.EMPTY);
        Inventories.readNbt(c, list, reg);
        for (int i = 0; i < inv.size(); i++) inv.setStack(i, list.get(i));
    }

    @Override
    public NbtCompound writeNbt(NbtCompound n, RegistryWrapper.WrapperLookup reg) {
        n.putInt("heat", heat);
        n.putInt("hull", hull);
        n.putInt("pot", pot);
        n.putString("transponder", transponder);
        n.putString("shipLoc", shipLoc.id);
        n.putBoolean("jumping", jumping);
        n.putString("jumpDest", jumpDest.id);
        n.putInt("story", story);
        n.putInt("storyCounter", storyCounter);
        n.putString("ending", ending);
        n.putInt("totalJobs", totalJobs);
        n.putInt("jumpsSinceHunter", jumpsSinceHunter);
        n.putLong("heistCooldownUntil", heistCooldownUntil);
        n.putBoolean("rulesSet", rulesSet);
        if (job != null) n.put("job", job.toNbt());
        n.put("credits", uuidIntMap(credits));
        n.put("tutStep", uuidIntMap(tutStep));
        n.put("grudge", uuidIntMap(grudge));
        n.put("tutDone", uuidSet(tutDone));
        n.put("hintsOff", uuidSet(hintsOff));
        n.put("known", uuidSet(known));
        NbtCompound dis = new NbtCompound();
        disguiseUntil.forEach((k, v) -> dis.putLong(k.toString(), v));
        n.put("disguise", dis);
        NbtCompound comp = new NbtCompound();
        companion.forEach((k, v) -> comp.putUuid(k.toString(), v));
        n.put("companion", comp);
        NbtCompound r = new NbtCompound();
        rep.forEach(r::putInt);
        n.put("rep", r);
        NbtCompound p = new NbtCompound();
        pads.forEach(p::putLong);
        n.put("pads", p);
        NbtList b = new NbtList();
        for (String s : built) b.add(NbtString.of(s));
        n.put("built", b);
        n.putLongArray("openedCrates", new ArrayList<>(openedCrates));
        n.put("cargo", writeInv(cargo, reg));
        n.put("hidden", writeInv(hidden, reg));
        return n;
    }

    public static CrewState read(NbtCompound n, RegistryWrapper.WrapperLookup reg) {
        CrewState s = new CrewState();
        s.heat = n.getInt("heat");
        s.hull = n.contains("hull") ? n.getInt("hull") : 100;
        s.pot = n.getInt("pot");
        if (n.contains("transponder")) s.transponder = n.getString("transponder");
        s.shipLoc = Loc.byId(n.getString("shipLoc"));
        s.jumping = n.getBoolean("jumping");
        s.jumpDest = Loc.byId(n.getString("jumpDest"));
        s.story = n.getInt("story");
        s.storyCounter = n.getInt("storyCounter");
        s.ending = n.getString("ending");
        s.totalJobs = n.getInt("totalJobs");
        s.jumpsSinceHunter = n.getInt("jumpsSinceHunter");
        s.heistCooldownUntil = n.getLong("heistCooldownUntil");
        s.rulesSet = n.getBoolean("rulesSet");
        if (n.contains("job")) s.job = Job.fromNbt(n.getCompound("job"));
        readUuidIntMap(n.getCompound("credits"), s.credits);
        readUuidIntMap(n.getCompound("tutStep"), s.tutStep);
        readUuidIntMap(n.getCompound("grudge"), s.grudge);
        readUuidSet(n.getList("tutDone", NbtElement.STRING_TYPE), s.tutDone);
        readUuidSet(n.getList("hintsOff", NbtElement.STRING_TYPE), s.hintsOff);
        readUuidSet(n.getList("known", NbtElement.STRING_TYPE), s.known);
        NbtCompound dis = n.getCompound("disguise");
        for (String k : dis.getKeys()) {
            try { s.disguiseUntil.put(UUID.fromString(k), dis.getLong(k)); } catch (IllegalArgumentException ignored) {}
        }
        NbtCompound comp = n.getCompound("companion");
        for (String k : comp.getKeys()) {
            try { s.companion.put(UUID.fromString(k), comp.getUuid(k)); } catch (Exception ignored) {}
        }
        NbtCompound r = n.getCompound("rep");
        for (String k : r.getKeys()) s.rep.put(k, r.getInt(k));
        NbtCompound p = n.getCompound("pads");
        for (String k : p.getKeys()) s.pads.put(k, p.getLong(k));
        NbtList b = n.getList("built", NbtElement.STRING_TYPE);
        for (int i = 0; i < b.size(); i++) s.built.add(b.getString(i));
        for (long l : n.getLongArray("openedCrates")) s.openedCrates.add(l);
        readInv(n.getCompound("cargo"), s.cargo, reg);
        readInv(n.getCompound("hidden"), s.hidden, reg);
        return s;
    }
}
