package dev.voidoutlaws.menu;

import dev.voidoutlaws.game.Sched;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** A small "pick one" dialogue menu. */
public class ChoiceView extends View {
    public record Option(Item icon, String label, Formatting color, String desc, Runnable action) {}

    private final String title;
    private final List<Option> options;
    private boolean picked = false;

    public ChoiceView(String title, List<Option> options) {
        this.title = title;
        this.options = options;
    }

    private int slotFor(int i) {
        return switch (options.size()) {
            case 1 -> 13;
            case 2 -> i == 0 ? 11 : 15;
            default -> 11 + i * 2;
        };
    }

    @Override public Text title() { return Text.literal(title); }
    @Override public int rows() { return 3; }

    @Override
    public void render(ServerPlayerEntity p, Slots g) {
        g.fill(Btn.pane(Items.GRAY_STAINED_GLASS_PANE));
        for (int i = 0; i < options.size(); i++) {
            Option o = options.get(i);
            g.set(slotFor(i), Btn.of(o.icon, o.label, o.color, o.desc));
        }
    }

    @Override
    public void click(ServerPlayerEntity p, int slot, boolean right) {
        if (picked) return;
        for (int i = 0; i < options.size(); i++) {
            if (slotFor(i) == slot) {
                picked = true;
                Option o = options.get(i);
                p.closeHandledScreen();
                Sched.later(1, o.action);
                return;
            }
        }
    }
}
