package dev.voidoutlaws.menu;

import dev.voidoutlaws.game.Game;
import dev.voidoutlaws.game.Loot;
import dev.voidoutlaws.state.CrewState;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** The Loot Locker: split the Crew Pot (or betray everyone, in a funny way). */
public class LockerView extends View {
    @Override public Text title() { return Text.literal("Loot Locker"); }
    @Override public int rows() { return 3; }

    @Override
    public void render(ServerPlayerEntity p, Slots g) {
        CrewState s = Game.S();
        g.fill(Btn.pane(Items.YELLOW_STAINED_GLASS_PANE));
        List<ServerPlayerEntity> crew = Game.players();
        List<String> names = new ArrayList<>();
        for (ServerPlayerEntity c : crew) names.add(Game.name(c) + (s.grudge.getOrDefault(c.getUuid(), 0) > 0 ? " §c(backstabber)" : ""));
        int share = crew.isEmpty() ? 0 : s.pot / crew.size();
        List<String> lore = new ArrayList<>();
        lore.add("Online crew (" + crew.size() + "):");
        lore.addAll(names);
        lore.add("Fair share each: " + share + " cr");
        g.set(4, Btn.of(Items.GOLD_BLOCK, "Crew Pot: " + s.pot + " cr", Formatting.GOLD, lore.toArray(new String[0])));
        g.set(11, Btn.of(Items.LIME_CONCRETE, "Split evenly", Formatting.GREEN, "Everyone online gets an equal share.", "The honest choice. Boring, but honest."));
        g.set(13, Btn.of(Items.ORANGE_CONCRETE, "Skim off the top", Formatting.GOLD, "Secretly take an extra 20% first,", "then split the rest evenly.", "50% chance your crew notices!"));
        g.set(15, Btn.of(Items.RED_CONCRETE, "Grab it all and RUN", Formatting.RED, "Take 60% of the pot. Everyone will know.",
                "You'll glow, pay a grudge tax, and your", "crew gets tomatoes to throw at you.", "§cRIGHT-click to confirm."));
        g.set(22, Btn.of(Items.GOLD_NUGGET, "Your wallet: " + s.credits(p.getUuid()) + " cr", Formatting.YELLOW, "Pay a crewmate: /pay <name> <amount>"));
        g.set(26, Btn.close());
    }

    @Override
    public void click(ServerPlayerEntity p, int slot, boolean right) {
        switch (slot) {
            case 11 -> Loot.split(p, Loot.Mode.EVEN);
            case 13 -> Loot.split(p, Loot.Mode.SKIM);
            case 15 -> {
                if (right) Loot.split(p, Loot.Mode.GRAB);
                else Game.bar(p, "Right-click to confirm. Are you SURE you want to betray your crew?", Formatting.RED);
            }
            case 26 -> p.closeHandledScreen();
            default -> {}
        }
    }
}
