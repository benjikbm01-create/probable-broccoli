package dev.voidoutlaws.reg;

import dev.voidoutlaws.VoidOutlaws;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;

import java.util.LinkedHashSet;
import java.util.Set;

public final class ModItems {
    private ModItems() {}

    public static final Set<Item> CONTRABAND = new LinkedHashSet<>();

    public static Item VOID_DUST, GLOWMOSS, STOLEN_TECH, HUM_CRYSTAL, GRUMBLEBACK_EGG;
    public static Item BLASTER, JETPACK, GRAPPLING_HOOK, DISGUISE_KIT, TRANSPONDER_CHIP, HULL_PATCH,
            COMPANION_WHISTLE, SPACE_TOMATO, HANDBOOK, LUMEN_LEDGER, NEBULA_NOODLES;

    private static Item reg(String id, Item item) {
        return Registry.register(Registries.ITEM, VoidOutlaws.id(id), item);
    }

    private static Item contraband(String id, String tip) {
        Item i = reg(id, new GearItem(GearItem.Kind.CONTRABAND, new Item.Settings().rarity(Rarity.UNCOMMON), tip));
        CONTRABAND.add(i);
        return i;
    }

    private static Item gear(String id, GearItem.Kind kind, Item.Settings s, String tip) {
        return reg(id, new GearItem(kind, s, tip));
    }

    public static void init() {
        VOID_DUST = contraband("void_dust", "Sparkly, illegal, and very sneezy.");
        GLOWMOSS = contraband("glowmoss", "Glows in the dark. Also illegal in the dark.");
        STOLEN_TECH = contraband("stolen_tech", "Definitely fell off the back of a cruiser.");
        HUM_CRYSTAL = contraband("hum_crystal", "Hums show tunes when nobody is listening.");
        GRUMBLEBACK_EGG = contraband("grumbleback_egg", "Warm. Occasionally grumbles.");

        BLASTER = gear("blaster", GearItem.Kind.BLASTER, new Item.Settings().maxCount(1),
                "Right-click to fire. Stuns crewmates instead of hurting them.");
        JETPACK = gear("jetpack", GearItem.Kind.JETPACK, new Item.Settings().maxCount(1),
                "Right-click for a burst of thrust.");
        GRAPPLING_HOOK = gear("grappling_hook", GearItem.Kind.GRAPPLE, new Item.Settings().maxCount(1),
                "Right-click a block up to 32 blocks away to zip to it.");
        DISGUISE_KIT = gear("disguise_kit", GearItem.Kind.DISGUISE, new Item.Settings().maxCount(16),
                "Right-click: 5 minutes of fake mustache. Troopers ignore you, pocket scans skip you.");
        TRANSPONDER_CHIP = gear("transponder_chip", GearItem.Kind.TRANSPONDER, new Item.Settings().maxCount(16),
                "Use at the Engineering Panel to swap the ship's ID: big Heat drop.");
        HULL_PATCH = gear("hull_patch", GearItem.Kind.HULL_PATCH, new Item.Settings().maxCount(16),
                "Right-click aboard the ship to repair 30% hull.");
        COMPANION_WHISTLE = gear("companion_whistle", GearItem.Kind.WHISTLE, new Item.Settings().maxCount(1),
                "Right-click to call your little alien buddy.");
        SPACE_TOMATO = gear("space_tomato", GearItem.Kind.TOMATO, new Item.Settings().maxCount(16),
                "Throw it at a backstabber. Harmless. Satisfying.");
        HANDBOOK = gear("handbook", GearItem.Kind.HANDBOOK, new Item.Settings().maxCount(1),
                "Right-click to read. Controls and tips for new outlaws.");
        LUMEN_LEDGER = gear("lumen_ledger", GearItem.Kind.LEDGER, new Item.Settings().maxCount(1).rarity(Rarity.EPIC),
                "Right-click to read.");
        NEBULA_NOODLES = reg("nebula_noodles", new Item(new Item.Settings()
                .food(new FoodComponent.Builder().nutrition(7).saturationModifier(0.7f).alwaysEdible().build())));

        Registry.register(Registries.ITEM_GROUP, VoidOutlaws.id("main"), FabricItemGroup.builder()
                .icon(() -> new ItemStack(BLASTER))
                .displayName(Text.translatable("itemGroup.voidoutlaws.main"))
                .entries((ctx, e) -> {
                    e.add(HANDBOOK);
                    for (Item i : CONTRABAND) e.add(i);
                    e.add(BLASTER); e.add(JETPACK); e.add(GRAPPLING_HOOK); e.add(DISGUISE_KIT);
                    e.add(TRANSPONDER_CHIP); e.add(HULL_PATCH); e.add(COMPANION_WHISTLE); e.add(SPACE_TOMATO);
                    e.add(NEBULA_NOODLES);
                    for (OutlawBlock.Kind k : OutlawBlock.Kind.values()) e.add(ModBlocks.get(k));
                })
                .build());
    }

    public static boolean isContraband(ItemStack s) {
        return !s.isEmpty() && CONTRABAND.contains(s.getItem());
    }
}
