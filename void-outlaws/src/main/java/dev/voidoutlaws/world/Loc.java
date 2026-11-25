package dev.voidoutlaws.world;

import dev.voidoutlaws.VoidOutlaws;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

/** Every place the ship can dock. The ship interior itself lives in {@link #SHIP}. */
public enum Loc {
    MERIDIAN("meridian_prime", "Meridian Prime", Formatting.AQUA, Items.QUARTZ_BLOCK, true,
            "A spotless lawful core world.", "Lots of rules. Lots of scanners.", "Danger: patrols & scans"),
    DUSTREACH("dustreach", "Dustreach", Formatting.GOLD, Items.RED_SAND, false,
            "A dusty frontier planet.", "Nobody asks questions here.", "Job board, cantina, black market"),
    GILDED_MAW("gilded_maw", "The Gilded Maw", Formatting.LIGHT_PURPLE, Items.GOLD_BLOCK, false,
            "A glittering crime-lord space station.", "Big jobs for brave crews.", "Job board, black market"),
    BURROW("burrow", "The Burrow", Formatting.GREEN, Items.TUFF, false,
            "Your crew's secret asteroid hideout.", "Lie low here to cool off Heat.", "Safe: Heat drops fast, free repairs"),
    DERELICT("derelict", "Derelict Freighter", Formatting.DARK_GREEN, Items.OXIDIZED_COPPER, false,
            "A drifting wreck in deep space.", "Only reachable with a boarding job.", "Danger: scavengers");

    public static final RegistryKey<World> SHIP = RegistryKey.of(RegistryKeys.WORLD, VoidOutlaws.id("ship"));

    public final String id;
    public final String display;
    public final Formatting color;
    public final Item icon;
    public final boolean lawful;
    public final String[] lore;
    public final RegistryKey<World> key;

    Loc(String id, String display, Formatting color, Item icon, boolean lawful, String... lore) {
        this.id = id;
        this.display = display;
        this.color = color;
        this.icon = icon;
        this.lawful = lawful;
        this.lore = lore;
        this.key = RegistryKey.of(RegistryKeys.WORLD, VoidOutlaws.id(id));
    }

    public static Loc byId(String id) {
        for (Loc l : values()) if (l.id.equals(id)) return l;
        return BURROW;
    }

    public static Loc fromWorld(RegistryKey<World> key) {
        for (Loc l : values()) if (l.key.equals(key)) return l;
        return null;
    }

    public static boolean isModWorld(RegistryKey<World> key) {
        return key.equals(SHIP) || fromWorld(key) != null;
    }
}
