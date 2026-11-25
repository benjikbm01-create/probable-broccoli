package dev.voidoutlaws.menu;

import dev.voidoutlaws.game.Game;
import dev.voidoutlaws.game.Loot;
import dev.voidoutlaws.state.CrewState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Comet Cards: a simple higher-or-lower card game against the house. */
public class CardsView extends View {
    private static final int[] BETS = {10, 25, 50, 100};
    private static final String[] SUITS = {"Comets", "Moons", "Nebulas", "Rockets"};

    private enum Phase { BET, PLAY, RESULT }

    private Phase phase = Phase.BET;
    private int bet, card, suit, streak, winnings;
    private String result = "";

    @Override public Text title() { return Text.literal("Comet Cards"); }
    @Override public int rows() { return 3; }

    private static String face(int v) {
        return switch (v) { case 1 -> "Ace"; case 11 -> "Jack"; case 12 -> "Queen"; case 13 -> "King"; default -> String.valueOf(v); };
    }

    private ItemStack cardItem() {
        return Btn.count(Btn.of(Items.PAPER, face(card) + " of " + SUITS[suit], Formatting.WHITE, "Value: " + card + " (Ace low, King high)"), card);
    }

    @Override
    public void render(ServerPlayerEntity p, Slots g) {
        CrewState s = Game.S();
        g.fill(Btn.pane(Items.GREEN_STAINED_GLASS_PANE));
        g.set(18, Btn.of(Items.GOLD_NUGGET, "Wallet: " + s.credits(p.getUuid()) + " cr", Formatting.GOLD));
        switch (phase) {
            case BET -> {
                g.set(4, Btn.of(Items.BOOK, "How to play", Formatting.YELLOW,
                        "Place a bet. A card is dealt.", "Guess if the NEXT card is HIGHER or LOWER.",
                        "Each right guess: winnings x1.5.", "Ties are free. Cash out anytime!", "5 in a row cashes out automatically."));
                for (int i = 0; i < BETS.length; i++)
                    g.set(10 + i * 2, Btn.count(Btn.of(Items.GOLD_INGOT, "Bet " + BETS[i] + " cr", Formatting.GOLD), Math.min(64, BETS[i] / 5)));
                g.set(22, Btn.of(Items.MUSHROOM_STEW, "Buy the crew a round (30 cr)", Formatting.GREEN,
                        "Everyone online gets Nebula Noodles.", "Clears YOUR backstabber grudge."));
                g.set(26, Btn.close());
            }
            case PLAY -> {
                g.set(4, Btn.of(Items.CLOCK, "Streak: " + streak + "   Winnings: " + winnings + " cr", Formatting.YELLOW, "Bet: " + bet + " cr"));
                g.set(13, cardItem());
                g.set(11, Btn.of(Items.LIME_CONCRETE, "HIGHER", Formatting.GREEN));
                g.set(15, Btn.of(Items.RED_CONCRETE, "LOWER", Formatting.RED));
                if (streak > 0) g.set(22, Btn.glint(Btn.of(Items.EMERALD, "CASH OUT " + winnings + " cr", Formatting.GREEN)));
            }
            case RESULT -> {
                g.set(4, Btn.of(Items.BELL, result, Formatting.YELLOW, "Click anywhere to play again."));
                g.set(13, cardItem());
            }
        }
    }

    @Override
    public void click(ServerPlayerEntity p, int slot, boolean right) {
        CrewState s = Game.S();
        switch (phase) {
            case BET -> {
                if (slot == 26) { p.closeHandledScreen(); return; }
                if (slot == 22) { Loot.buyRound(p); return; }
                for (int i = 0; i < BETS.length; i++) {
                    if (slot == 10 + i * 2) {
                        if (s.credits(p.getUuid()) < BETS[i]) { Game.bar(p, "Not enough credits for that bet.", Formatting.RED); return; }
                        s.addCredits(p.getUuid(), -BETS[i]);
                        bet = BETS[i];
                        winnings = bet;
                        streak = 0;
                        deal();
                        phase = Phase.PLAY;
                        Game.sound(p, SoundEvents.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                    }
                }
            }
            case PLAY -> {
                if (slot == 22 && streak > 0) { cashOut(p); return; }
                if (slot != 11 && slot != 15) return;
                boolean higher = slot == 11;
                int old = card;
                deal();
                if (card == old) {
                    Game.bar(p, "Tie! Free redraw.", Formatting.YELLOW);
                    return;
                }
                if ((card > old) == higher) {
                    streak++;
                    winnings = (int) Math.round(winnings * 1.5);
                    Game.sound(p, SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1f, 1f + streak * 0.1f);
                    if (streak >= 5) cashOut(p);
                } else {
                    result = "Bust! You lost " + bet + " cr.";
                    Game.sound(p, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1f, 0.5f);
                    bet = 0; streak = 0; winnings = 0;
                    phase = Phase.RESULT;
                }
            }
            case RESULT -> phase = Phase.BET;
        }
    }

    private void deal() {
        card = 1 + Game.R.nextInt(13);
        suit = Game.R.nextInt(SUITS.length);
    }

    private void cashOut(ServerPlayerEntity p) {
        CrewState s = Game.S();
        s.addCredits(p.getUuid(), winnings);
        result = "Cashed out " + winnings + " cr! (streak " + streak + ")";
        Game.sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        if (winnings >= 200) Game.crew(Game.name(p) + " just won " + winnings + " cr at Comet Cards!", Formatting.GOLD);
        bet = 0; streak = 0; winnings = 0;
        phase = Phase.RESULT;
    }

    @Override
    public void closed(ServerPlayerEntity p) {
        if (phase == Phase.PLAY) {
            CrewState s = Game.S();
            if (streak > 0) s.addCredits(p.getUuid(), winnings);
            else s.addCredits(p.getUuid(), bet);
            phase = Phase.BET;
        }
    }
}
