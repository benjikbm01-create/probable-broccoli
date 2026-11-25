package dev.voidoutlaws.menu;

import dev.voidoutlaws.game.Game;
import dev.voidoutlaws.game.Heat;
import dev.voidoutlaws.state.CrewState;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Meridian Prime fine kiosk: pay credits to lower Heat. */
public class KioskView extends View {
    @Override public Text title() { return Text.literal("Fine Payment Kiosk"); }
    @Override public int rows() { return 3; }

    private int small() { return Heat.fineCost(); }
    private int big() { return (int) Math.round(Heat.fineCost() * 2.5); }

    @Override
    public void render(ServerPlayerEntity p, Slots g) {
        CrewState s = Game.S();
        g.fill(Btn.pane(Items.LIGHT_BLUE_STAINED_GLASS_PANE));
        g.set(4, Btn.of(Items.PAPER, "Concordat Fine Payment", Formatting.AQUA,
                "Pay your 'parking fines' and your", "Heat goes down. Totally normal. Very legal.",
                "Crew Heat: " + s.heat + " (" + Heat.level(s.heat) + ")"));
        g.set(11, Btn.of(Items.GOLD_NUGGET, "Pay small fine: " + small() + " cr", Formatting.YELLOW, "Heat -15", "Paid from YOUR wallet."));
        g.set(15, Btn.of(Items.GOLD_INGOT, "Pay big fine: " + big() + " cr", Formatting.GOLD, "Heat -40", "Paid from YOUR wallet."));
        g.set(22, Btn.of(Items.GOLD_NUGGET, "Your wallet: " + s.credits(p.getUuid()) + " cr", Formatting.YELLOW));
        g.set(26, Btn.close());
    }

    @Override
    public void click(ServerPlayerEntity p, int slot, boolean right) {
        CrewState s = Game.S();
        if (slot == 26) { p.closeHandledScreen(); return; }
        if (slot != 11 && slot != 15) return;
        if (s.heat <= 0) { Game.bar(p, "Your record is spotless. (For now.)", Formatting.GREEN); return; }
        int cost = slot == 11 ? small() : big();
        if (s.credits(p.getUuid()) < cost) { Game.bar(p, "Not enough credits (" + cost + " cr).", Formatting.RED); return; }
        s.addCredits(p.getUuid(), -cost);
        s.addRep("concordat", 2);
        Game.crew(Game.name(p) + " paid " + cost + " cr in 'fines'. What a responsible citizen.", Formatting.AQUA);
        Heat.add(slot == 11 ? -15 : -40, "fines paid");
    }
}
