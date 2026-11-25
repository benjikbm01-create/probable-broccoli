package dev.voidoutlaws.menu;

import dev.voidoutlaws.game.Game;
import dev.voidoutlaws.game.Missions;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Bulkhead panel: light every cell. Clicking a cell flips it and its neighbours. */
public class PowerView extends View {
    private boolean[] cells;
    private int start;

    public PowerView() {
        newPuzzle();
    }

    private void newPuzzle() {
        int n = Missions.powerRound == 0 ? 5 : 7;
        cells = new boolean[n];
        java.util.Arrays.fill(cells, true);
        start = 9 + (9 - n) / 2;
        do {
            int presses = 2 + Game.R.nextInt(3);
            for (int i = 0; i < presses; i++) press(Game.R.nextInt(n));
        } while (allOn());
    }

    private void press(int i) {
        for (int k = i - 1; k <= i + 1; k++) if (k >= 0 && k < cells.length) cells[k] = !cells[k];
    }

    private boolean allOn() {
        for (boolean c : cells) if (!c) return false;
        return true;
    }

    @Override public Text title() { return Text.literal("Bulkhead Panel - Reroute Power"); }
    @Override public int rows() { return 3; }

    @Override
    public boolean valid(ServerPlayerEntity p) {
        return Missions.powerRound < 2;
    }

    @Override
    public void render(ServerPlayerEntity p, Slots g) {
        g.fill(Btn.pane(Items.BLACK_STAINED_GLASS_PANE));
        g.set(4, Btn.of(Items.REDSTONE_TORCH, "Power relay " + (Missions.powerRound + 1) + "/2", Formatting.GREEN,
                "Light up ALL the cells!", "Clicking a cell flips it AND its neighbours.",
                "Getting hit interrupts you - get your crew to guard you!"));
        for (int i = 0; i < cells.length; i++)
            g.set(start + i, cells[i] ? Btn.of(Items.LIME_STAINED_GLASS, "ON", Formatting.GREEN) : Btn.of(Items.GRAY_STAINED_GLASS, "off", Formatting.DARK_GRAY));
        g.set(22, Btn.of(Items.LEVER, "Scramble (new puzzle)", Formatting.GRAY));
    }

    @Override
    public void click(ServerPlayerEntity p, int slot, boolean right) {
        if (slot == 22) { newPuzzle(); return; }
        int i = slot - start;
        if (i < 0 || i >= cells.length) return;
        press(i);
        Game.sound(p, SoundEvents.BLOCK_LEVER_CLICK, 1f, cells[i] ? 1.2f : 0.8f);
        if (allOn()) {
            Missions.powerSolved(p);
            if (Missions.powerRound < 2) newPuzzle();
        }
    }
}
