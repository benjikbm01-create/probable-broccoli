package dev.voidoutlaws.menu;

import dev.voidoutlaws.game.Game;
import dev.voidoutlaws.game.Missions;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** Vault terminal hack: watch the light pattern, then repeat it. */
public class HackView extends View {
    private static final int[] PADS = {10, 12, 14, 16};
    private static final Item[] COLORS = {Items.RED_CONCRETE, Items.BLUE_CONCRETE, Items.YELLOW_CONCRETE, Items.LIME_CONCRETE};
    private static final String[] NAMES = {"Red", "Blue", "Yellow", "Green"};

    private final List<Integer> seq = new ArrayList<>();
    private boolean showing = true;
    private int t = 0, input = 0, lit = -1, flashBad = 0;

    public HackView() {
        newSequence();
    }

    private void newSequence() {
        seq.clear();
        int len = 3 + Math.min(2, Missions.hackRound);
        for (int i = 0; i < len; i++) seq.add(Game.R.nextInt(4));
        showing = true;
        t = -20;
        input = 0;
    }

    @Override public Text title() { return Text.literal("Vault Terminal - HACKING"); }
    @Override public int rows() { return 3; }

    @Override
    public boolean valid(ServerPlayerEntity p) {
        return Missions.hackRound < 3;
    }

    @Override
    public void tick(ServerPlayerEntity p) {
        if (flashBad > 0) flashBad--;
        if (!showing) { lit = -1; return; }
        t++;
        if (t < 0) { lit = -1; return; }
        int step = t / 16, within = t % 16;
        if (step >= seq.size()) { showing = false; lit = -1; return; }
        int prev = lit;
        lit = within < 11 ? seq.get(step) : -1;
        if (lit >= 0 && prev != lit) Game.sound(p, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 0.8f, 0.8f + lit * 0.25f);
    }

    @Override
    public void render(ServerPlayerEntity p, Slots g) {
        g.fill(Btn.pane(flashBad > 0 ? Items.RED_STAINED_GLASS_PANE : Items.BLACK_STAINED_GLASS_PANE));
        String head = "Lock " + (Missions.hackRound + 1) + "/3";
        String sub = showing ? "WATCH the pattern... (" + seq.size() + " lights)" : "REPEAT it! (" + input + "/" + seq.size() + ")";
        g.set(4, Btn.of(Items.OBSERVER, head + " - " + sub, Formatting.GREEN,
                "Remember the order the pads light up,", "then click them in the same order.",
                "Getting hit interrupts you - get your crew to guard you!"));
        for (int i = 0; i < 4; i++) {
            boolean on = lit == i;
            g.set(PADS[i], on ? Btn.glint(Btn.of(Items.SEA_LANTERN, NAMES[i] + "!", Formatting.WHITE))
                    : Btn.of(COLORS[i], NAMES[i], Formatting.GRAY));
        }
    }

    @Override
    public void click(ServerPlayerEntity p, int slot, boolean right) {
        if (showing) return;
        for (int i = 0; i < 4; i++) {
            if (slot != PADS[i]) continue;
            if (seq.get(input) == i) {
                input++;
                Game.sound(p, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 1f, 0.8f + i * 0.25f);
                if (input >= seq.size()) {
                    Missions.hackSolved(p);
                    if (Missions.hackRound < 3) newSequence();
                }
            } else {
                Game.sound(p, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1f, 0.5f);
                Game.bar(p, "ACCESS DENIED! Watch again...", Formatting.RED);
                flashBad = 15;
                newSequence();
            }
            return;
        }
    }
}
