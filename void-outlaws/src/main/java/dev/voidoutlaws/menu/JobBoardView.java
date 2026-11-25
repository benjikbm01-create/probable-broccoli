package dev.voidoutlaws.menu;

import dev.voidoutlaws.game.Game;
import dev.voidoutlaws.game.Jobs;
import dev.voidoutlaws.reg.ModItems;
import dev.voidoutlaws.state.CrewState;
import dev.voidoutlaws.state.Job;
import dev.voidoutlaws.world.Loc;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** Job board (offers + deliveries) or dead drop (deliveries only). */
public class JobBoardView extends View {
    private final Loc here;
    private final boolean offers;
    private List<Job> shown = new ArrayList<>();

    public JobBoardView(Loc here, boolean offers) {
        this.here = here;
        this.offers = offers;
    }

    @Override public Text title() { return Text.literal((offers ? "Job Board - " : "Dead Drop - ") + here.display); }
    @Override public int rows() { return 3; }

    private static Item icon(Job j) {
        return switch (j.type) {
            case SMUGGLE -> j.cargo == null ? Items.CHEST : j.cargo;
            case BOARDING -> ModItems.GRUMBLEBACK_EGG;
            case HEIST -> Items.GOLD_INGOT;
        };
    }

    @Override
    public void render(ServerPlayerEntity p, Slots g) {
        CrewState s = Game.S();
        g.fill(Btn.pane(Items.BROWN_STAINED_GLASS_PANE));
        Job cur = s.job;
        if (cur == null) g.set(4, Btn.of(Items.PAPER, "Your crew has no job", Formatting.GRAY, offers ? "Pick one below!" : "Job Boards are on Dustreach and the Gilded Maw."));
        else g.set(4, Btn.of(Items.WRITABLE_BOOK, "Current job: " + cur.title, Formatting.GOLD, Jobs.describe(cur).toArray(new String[0])));
        if (offers) {
            shown = Jobs.offers(here, p);
            for (int i = 0; i < shown.size() && i < 7; i++) {
                Job j = shown.get(i);
                List<String> lore = new ArrayList<>(Jobs.describe(j));
                lore.add(cur == null ? "§aClick to accept!" : "§7(Finish your current job first)");
                g.set(10 + i, Btn.of(icon(j), j.title, j.tutorial ? Formatting.GREEN : j.story ? Formatting.LIGHT_PURPLE : Formatting.YELLOW, lore.toArray(new String[0])));
            }
        } else {
            g.set(13, Btn.of(Items.HOPPER, "Dead Drop", Formatting.GRAY, "Deliveries for " + here.display + " go here.", "No questions asked. No receipts given."));
        }
        if (Jobs.canDeliverHere(here)) {
            g.set(20, Btn.glint(Btn.of(Items.EMERALD, "DELIVER " + cur.qty + "x " + cur.cargoName(), Formatting.GREEN,
                    "Crew has: " + Jobs.count(p, cur.cargo) + "/" + cur.qty,
                    "(Counts your pockets, the Cargo Hold", "and the Hidden Compartment.)")));
        } else {
            String why = cur == null ? "No active job." : cur.type == Job.Type.HEIST ? "Heists finish when you escape Meridian Prime." : "This job delivers to " + cur.dest.display + ".";
            g.set(20, Btn.of(Items.GRAY_DYE, "Nothing to deliver here", Formatting.GRAY, why));
        }
        if (cur != null && !cur.story) g.set(24, Btn.of(Items.FLINT_AND_STEEL, "Abandon job", Formatting.RED, "RIGHT-click to confirm.", "The client will be a bit upset."));
        g.set(26, Btn.close());
    }

    @Override
    public void click(ServerPlayerEntity p, int slot, boolean right) {
        if (slot >= 10 && slot < 17 && offers) {
            int i = slot - 10;
            if (i < shown.size()) {
                if (Jobs.accept(p, shown.get(i), here)) p.closeHandledScreen();
            }
            return;
        }
        switch (slot) {
            case 20 -> Jobs.deliver(p, here);
            case 24 -> { if (right) Jobs.abandon(p); else Game.bar(p, "Right-click to confirm abandoning the job.", Formatting.YELLOW); }
            case 26 -> p.closeHandledScreen();
            default -> {}
        }
    }
}
