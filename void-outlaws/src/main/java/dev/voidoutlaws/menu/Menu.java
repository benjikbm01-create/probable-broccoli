package dev.voidoutlaws.menu;

import dev.voidoutlaws.VoidOutlaws;
import dev.voidoutlaws.game.Game;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** Chest-style screen whose slots are buttons. Works with an unmodified client screen. */
public class Menu extends GenericContainerScreenHandler {
    public static final Set<Menu> OPEN = new HashSet<>();

    public final View view;
    private final SimpleInventory inv;
    private final ServerPlayerEntity owner;
    private final int size;

    private Menu(int syncId, PlayerInventory pinv, View view, ServerPlayerEntity owner, SimpleInventory inv) {
        super(type(view.rows()), syncId, pinv, inv, view.rows());
        this.view = view;
        this.inv = inv;
        this.owner = owner;
        this.size = view.rows() * 9;
        render();
        OPEN.add(this);
    }

    private static ScreenHandlerType<GenericContainerScreenHandler> type(int rows) {
        return switch (rows) {
            case 1 -> ScreenHandlerType.GENERIC_9X1;
            case 2 -> ScreenHandlerType.GENERIC_9X2;
            case 4 -> ScreenHandlerType.GENERIC_9X4;
            case 5 -> ScreenHandlerType.GENERIC_9X5;
            case 6 -> ScreenHandlerType.GENERIC_9X6;
            default -> ScreenHandlerType.GENERIC_9X3;
        };
    }

    public static void open(ServerPlayerEntity p, View v) {
        p.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (id, pinv, pl) -> new Menu(id, pinv, v, p, new SimpleInventory(v.rows() * 9)), v.title()));
    }

    /** Opens on the next tick (safe to call from inside a click). */
    public static void openLater(ServerPlayerEntity p, View v) {
        dev.voidoutlaws.game.Sched.later(1, () -> { if (!p.isRemoved()) open(p, v); });
    }

    public void render() {
        Slots g = new Slots(size);
        try {
            view.render(owner, g);
        } catch (Exception e) {
            VoidOutlaws.LOG.error("Menu render failed", e);
        }
        g.apply(inv);
    }

    public static void tickAll() {
        for (Menu m : new ArrayList<>(OPEN)) {
            if (m.owner.isRemoved() || m.owner.currentScreenHandler != m) {
                OPEN.remove(m);
                continue;
            }
            try {
                m.view.tick(m.owner);
                if (!m.view.valid(m.owner)) {
                    m.owner.closeHandledScreen();
                    continue;
                }
            } catch (Exception e) {
                VoidOutlaws.LOG.error("Menu tick failed", e);
            }
            if (Game.tick % 2 == 0) m.render();
        }
    }

    public static void refreshAll() {
        for (Menu m : new ArrayList<>(OPEN)) m.render();
    }

    @Override
    public void onSlotClick(int slot, int button, SlotActionType action, PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity sp)) return;
        if (slot >= 0 && slot < size && (action == SlotActionType.PICKUP || action == SlotActionType.QUICK_MOVE)) {
            try {
                view.click(sp, slot, button == 1);
            } catch (Exception e) {
                VoidOutlaws.LOG.error("Menu click failed", e);
            }
            if (sp.currentScreenHandler == this) render();
        }
        // Every other click is ignored on purpose: menu items can never be taken.
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        OPEN.remove(this);
        if (player instanceof ServerPlayerEntity sp) {
            try { view.closed(sp); } catch (Exception e) { VoidOutlaws.LOG.error("Menu close failed", e); }
        }
    }
}
