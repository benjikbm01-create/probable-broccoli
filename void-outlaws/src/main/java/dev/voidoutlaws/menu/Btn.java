package dev.voidoutlaws.menu;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Unit;

import java.util.ArrayList;
import java.util.List;

/** Builds the item "buttons" used in menus. */
public final class Btn {
    private Btn() {}

    public static ItemStack of(Item item, String name, Formatting color, String... lore) {
        return of(item, Text.literal(name).setStyle(Style.EMPTY.withItalic(false).withColor(color)), lore);
    }

    public static ItemStack of(Item item, Text name, String... lore) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponentTypes.CUSTOM_NAME, name.copy().setStyle(name.getStyle().withItalic(false)));
        if (lore.length > 0) {
            List<Text> lines = new ArrayList<>();
            for (String l : lore) {
                for (String part : l.split("\n")) lines.add(Text.literal(part).setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY)));
            }
            s.set(DataComponentTypes.LORE, new LoreComponent(lines));
        }
        s.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        return s;
    }

    public static ItemStack pane(Item glass) {
        return of(glass, " ", Formatting.GRAY);
    }

    public static ItemStack blank() {
        return pane(Items.GRAY_STAINED_GLASS_PANE);
    }

    public static ItemStack glint(ItemStack s) {
        s.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return s;
    }

    public static ItemStack count(ItemStack s, int n) {
        s.setCount(Math.max(1, Math.min(64, n)));
        return s;
    }

    public static ItemStack close() {
        return of(Items.BARRIER, "Close", Formatting.RED);
    }
}
