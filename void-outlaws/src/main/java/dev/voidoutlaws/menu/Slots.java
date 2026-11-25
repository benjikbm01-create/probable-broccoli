package dev.voidoutlaws.menu;

import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;

public class Slots {
    private final ItemStack[] next;

    Slots(int size) {
        next = new ItemStack[size];
        java.util.Arrays.fill(next, ItemStack.EMPTY);
    }

    public int size() { return next.length; }

    public void set(int slot, ItemStack s) {
        if (slot >= 0 && slot < next.length) next[slot] = s;
    }

    public void fill(ItemStack s) {
        for (int i = 0; i < next.length; i++) next[i] = s.copy();
    }

    public void row(int r, ItemStack s) {
        for (int i = 0; i < 9; i++) set(r * 9 + i, s.copy());
    }

    void apply(SimpleInventory inv) {
        for (int i = 0; i < next.length; i++) {
            if (!ItemStack.areEqual(inv.getStack(i), next[i])) inv.setStack(i, next[i]);
        }
    }
}
