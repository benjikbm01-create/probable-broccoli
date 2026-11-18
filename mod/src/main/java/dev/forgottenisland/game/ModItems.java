package dev.forgottenisland.game;

import dev.forgottenisland.ForgottenIsland;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class ModItems {
    private ModItems() {}

    /** The adventurer's journal: right-click (or press J) to read objectives, people and hints. */
    public static final Item JOURNAL = register("journal", new Item(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)) {
        @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            if (user instanceof ServerPlayerEntity sp) Journal.open(sp);
            return TypedActionResult.success(user.getStackInHand(hand), world.isClient);
        }
    });

    /** A glowing story relic. */
    public static final Item EMBER = register("lantern_ember", new Item(new Item.Settings().maxCount(1).rarity(Rarity.EPIC).fireproof()
            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)));

    private static Item register(String id, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(ForgottenIsland.MOD_ID, id), item);
    }

    public static void init() {}
}
