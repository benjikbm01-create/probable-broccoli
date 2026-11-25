package dev.voidoutlaws.menu;

import dev.voidoutlaws.game.*;
import dev.voidoutlaws.reg.ModItems;
import dev.voidoutlaws.state.CrewState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Mechanic station: repairs, boosts and the transponder swap. */
public class EngineView extends View {
    @Override public Text title() { return Text.literal("Engineering Panel (Mechanic)"); }
    @Override public int rows() { return 3; }
    @Override public Roles.Role role() { return Roles.Role.MECHANIC; }

    private static int count(ServerPlayerEntity p, Item item) {
        PlayerInventory inv = p.getInventory();
        int n = 0;
        for (int i = 0; i < inv.size(); i++) if (inv.getStack(i).isOf(item)) n += inv.getStack(i).getCount();
        return n;
    }

    private static boolean consume(ServerPlayerEntity p, Item item) {
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isOf(item)) { inv.getStack(i).decrement(1); return true; }
        }
        return false;
    }

    @Override
    public void render(ServerPlayerEntity p, Slots g) {
        CrewState s = Game.S();
        g.fill(Btn.pane(Items.BLACK_STAINED_GLASS_PANE));
        int filled = Math.round(s.hull / 100f * 9);
        Item col = s.hull > 60 ? Items.LIME_STAINED_GLASS_PANE : s.hull > 30 ? Items.YELLOW_STAINED_GLASS_PANE : Items.RED_STAINED_GLASS_PANE;
        for (int i = 0; i < 9; i++) g.set(i, Btn.of(i < filled ? col : Items.GRAY_STAINED_GLASS_PANE, "Hull: " + s.hull + "%", Formatting.WHITE));
        for (int i = 0; i < 9; i++) {
            g.set(9 + i, Jumps.SPARKS[i]
                    ? Btn.of(Items.BLAZE_POWDER, "SPARK! Click to fix (+2% hull)", Formatting.GOLD, "Sparks slowly damage the hull.")
                    : Btn.of(Items.LIGHT_GRAY_STAINED_GLASS_PANE, "Systems OK", Formatting.GRAY));
        }
        g.set(18, Btn.of(Items.IRON_INGOT, "Apply Hull Patch (+30%)", Formatting.GREEN, "You have: " + count(p, ModItems.HULL_PATCH), "Buy patches at a Black Market."));
        g.set(20, Btn.of(Items.NAME_TAG, "Swap Transponder ID", Formatting.LIGHT_PURPLE,
                "Current ID: " + s.transponder, "Needs a Transponder Chip (Black Market).",
                "You have: " + count(p, ModItems.TRANSPONDER_CHIP), "Effect: Heat -35!"));
        JumpEvent e = Jumps.event;
        if (e != null && (e.type == JumpEvent.Type.PIRATES || e.type == JumpEvent.Type.CHASE)) {
            boolean ready = e.age >= e.mechReadyAt;
            String what = e.type == JumpEvent.Type.PIRATES ? "Boost Shields (+1 dodge)" : "Overcharge Engines (+1 escape)";
            g.set(22, Btn.of(ready ? Items.REDSTONE_BLOCK : Items.REDSTONE, what, ready ? Formatting.RED : Formatting.GRAY,
                    ready ? "READY - click!" : "Recharging... " + Math.max(0, (e.mechReadyAt - e.age) / 20) + "s"));
        } else {
            g.set(22, Btn.of(Items.GRAY_DYE, "Boost", Formatting.GRAY, "Only works during pirate attacks and chases."));
        }
        g.set(26, Btn.close());
    }

    @Override
    public void click(ServerPlayerEntity p, int slot, boolean right) {
        CrewState s = Game.S();
        if (slot >= 9 && slot <= 17) { Jumps.fixSpark(p, slot - 9); return; }
        switch (slot) {
            case 18 -> {
                if (s.hull >= 100) { Game.bar(p, "The hull is already at 100%.", Formatting.GRAY); return; }
                if (!consume(p, ModItems.HULL_PATCH)) { Game.bar(p, "You don't have a Hull Patch.", Formatting.RED); return; }
                s.hull = Math.min(100, s.hull + 30);
                s.markDirty();
                Game.bar(p, "Patched! Hull: " + s.hull + "%", Formatting.GREEN);
            }
            case 20 -> {
                if (!consume(p, ModItems.TRANSPONDER_CHIP)) { Game.bar(p, "You need a Transponder Chip (Black Market).", Formatting.RED); return; }
                String old = s.transponder;
                String[] pre = {"LL", "QX", "BRT", "MOO", "ZZ", "HV", "NOT-A-SHIP"};
                s.transponder = pre[Game.R.nextInt(pre.length)] + "-" + (1000 + Game.R.nextInt(9000));
                s.markDirty();
                Game.crew(Game.name(p) + " swapped the transponder: " + old + " is now " + s.transponder + ". Who? Never heard of them.", Formatting.LIGHT_PURPLE);
                Heat.add(-35, "new transponder ID");
            }
            case 22 -> Jumps.mechanicBoost(p);
            case 26 -> p.closeHandledScreen();
            default -> {}
        }
    }

    @Override
    public void closed(ServerPlayerEntity p) {
        Roles.release(p, Roles.Role.MECHANIC);
    }
}
