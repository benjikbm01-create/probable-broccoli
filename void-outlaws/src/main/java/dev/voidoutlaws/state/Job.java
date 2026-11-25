package dev.voidoutlaws.state;

import dev.voidoutlaws.world.Loc;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/** A crew job (only one active at a time, shared by everyone). */
public class Job {
    public enum Type { SMUGGLE, BOARDING, HEIST }

    public Type type = Type.SMUGGLE;
    public String title = "Job";
    public Item cargo;          // item to deliver (null for heists)
    public int qty;
    public Loc origin = Loc.DUSTREACH;
    public Loc dest = Loc.GILDED_MAW;
    public int payout;
    public int heat;
    public String faction = "freefolk";
    public boolean tutorial;
    public boolean story;
    /** Mission phase: heist 0=accepted 1=hacking 2=vault open 3=looted; boarding 0=sealed 1=bulkhead open. */
    public int phase;
    public int bonus;           // extra credits collected during the mission

    public NbtCompound toNbt() {
        NbtCompound n = new NbtCompound();
        n.putString("type", type.name());
        n.putString("title", title);
        if (cargo != null) n.putString("cargo", Registries.ITEM.getId(cargo).toString());
        n.putInt("qty", qty);
        n.putString("origin", origin.id);
        n.putString("dest", dest.id);
        n.putInt("payout", payout);
        n.putInt("heat", heat);
        n.putString("faction", faction);
        n.putBoolean("tutorial", tutorial);
        n.putBoolean("story", story);
        n.putInt("phase", phase);
        n.putInt("bonus", bonus);
        return n;
    }

    public static Job fromNbt(NbtCompound n) {
        Job j = new Job();
        try { j.type = Type.valueOf(n.getString("type")); } catch (Exception e) { j.type = Type.SMUGGLE; }
        j.title = n.getString("title");
        if (n.contains("cargo")) j.cargo = Registries.ITEM.get(Identifier.of(n.getString("cargo")));
        j.qty = n.getInt("qty");
        j.origin = Loc.byId(n.getString("origin"));
        j.dest = Loc.byId(n.getString("dest"));
        j.payout = n.getInt("payout");
        j.heat = n.getInt("heat");
        j.faction = n.getString("faction");
        j.tutorial = n.getBoolean("tutorial");
        j.story = n.getBoolean("story");
        j.phase = n.getInt("phase");
        j.bonus = n.getInt("bonus");
        return j;
    }

    public String cargoName() {
        return cargo == null ? "-" : cargo.getName().getString();
    }

    public String shortLine() {
        return switch (type) {
            case SMUGGLE -> cargoName() + " x" + qty + " -> " + dest.display;
            case BOARDING -> "Board the derelict, bring " + qty + " " + cargoName() + " -> " + dest.display;
            case HEIST -> "Heist: crack the vault on " + Loc.MERIDIAN.display;
        };
    }
}
