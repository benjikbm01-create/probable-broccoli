package dev.voidoutlaws.game;

import dev.voidoutlaws.reg.ModItems;
import dev.voidoutlaws.state.CrewState;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;

import java.util.*;

/** Splitting the Crew Pot - fairly, or not. */
public final class Loot {
    private Loot() {}

    public enum Mode { EVEN, SKIM, GRAB }

    public static void split(ServerPlayerEntity p, Mode mode) {
        CrewState s = Game.S();
        List<ServerPlayerEntity> crew = Game.players();
        int pot = s.pot;
        if (pot <= 0) { Game.msg(p, "The Crew Pot is empty. Go do crimes!", Formatting.GRAY); return; }
        if (crew.size() <= 1 && mode != Mode.EVEN) {
            Game.msg(p, "There's nobody here to betray. You just took your own money. Congratulations?", Formatting.GRAY);
            mode = Mode.EVEN;
        }
        s.pot = 0;
        s.markDirty();
        switch (mode) {
            case EVEN -> {
                distribute(pot, crew);
                Game.crew(Game.name(p) + " split the pot (" + pot + " cr) evenly. How wholesome!", Formatting.GREEN);
            }
            case SKIM -> {
                int skim = Math.max(1, pot / 5);
                s.addCredits(p.getUuid(), skim);
                distribute(pot - skim, crew);
                if (Game.R.nextBoolean()) {
                    Game.crew(Game.name(p) + " split the pot... but their pockets are jingling suspiciously. (" + skim + " cr skimmed!)", Formatting.GOLD);
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 1200, 0));
                } else {
                    Game.crew(Game.name(p) + " split the pot (" + (pot - skim) + " cr). Seems fair. Totally fair.", Formatting.GREEN);
                    Game.msg(p, "Nobody noticed you skimming " + skim + " cr. You sneaky space-weasel.", Formatting.LIGHT_PURPLE);
                }
            }
            case GRAB -> {
                int grab = (int) Math.round(pot * 0.6);
                s.addCredits(p.getUuid(), grab);
                List<ServerPlayerEntity> others = new ArrayList<>(crew);
                others.remove(p);
                distribute(pot - grab, others);
                s.grudge.put(p.getUuid(), 2);
                s.markDirty();
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 3600, 0));
                Game.title(crew, Game.t("BACKSTABBER!", Formatting.RED), Game.t(Game.name(p) + " grabbed the loot and ran!", Formatting.WHITE));
                Game.sound(crew, SoundEvents.ENTITY_VILLAGER_NO, 1f, 0.6f);
                Game.crew(Game.name(p) + " grabbed " + grab + " cr and RAN! They'll glow for 3 minutes, and their next 2 shares get a 25% grudge tax. Revenge tomatoes have been issued.", Formatting.RED);
                for (ServerPlayerEntity o : others) o.getInventory().offerOrDrop(new ItemStack(ModItems.SPACE_TOMATO, 4));
                Game.msg(p, "Want forgiveness? Buy the crew a round at a card table.", Formatting.GRAY);
            }
        }
        Tutorial.onSplit();
    }

    /** Even split with the backstabber "grudge tax". */
    private static void distribute(int amount, List<ServerPlayerEntity> who) {
        if (who.isEmpty() || amount <= 0) return;
        CrewState s = Game.S();
        int share = amount / who.size();
        int rest = amount - share * who.size();
        int taxPool = 0;
        Map<ServerPlayerEntity, Integer> got = new LinkedHashMap<>();
        List<ServerPlayerEntity> honest = new ArrayList<>();
        for (ServerPlayerEntity p : who) {
            int mine = share + (rest-- > 0 ? 1 : 0);
            int g = s.grudge.getOrDefault(p.getUuid(), 0);
            if (g > 0) {
                int tax = mine / 4;
                taxPool += tax;
                mine -= tax;
                if (g - 1 <= 0) s.grudge.remove(p.getUuid()); else s.grudge.put(p.getUuid(), g - 1);
                Game.msg(p, "Grudge tax: " + tax + " cr of your share went to your crewmates.", Formatting.GOLD);
            } else honest.add(p);
            got.put(p, mine);
        }
        if (taxPool > 0) {
            List<ServerPlayerEntity> to = honest.isEmpty() ? who : honest;
            int each = taxPool / to.size();
            for (ServerPlayerEntity p : to) got.merge(p, each, Integer::sum);
        }
        for (Map.Entry<ServerPlayerEntity, Integer> e : got.entrySet()) {
            s.addCredits(e.getKey().getUuid(), e.getValue());
            Game.msg(e.getKey(), "You got " + e.getValue() + " credits. Wallet: " + s.credits(e.getKey().getUuid()) + " cr.", Formatting.GOLD);
            Game.sound(e.getKey(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        }
        s.markDirty();
    }

    public static void buyRound(ServerPlayerEntity p) {
        CrewState s = Game.S();
        if (s.credits(p.getUuid()) < 30) { Game.msg(p, "A round costs 30 credits.", Formatting.RED); return; }
        s.addCredits(p.getUuid(), -30);
        s.grudge.remove(p.getUuid());
        p.removeStatusEffect(StatusEffects.GLOWING);
        for (ServerPlayerEntity o : Game.players()) o.getInventory().offerOrDrop(new ItemStack(ModItems.NEBULA_NOODLES));
        Game.crew(Game.name(p) + " bought everyone a round of Nebula Noodles! All is forgiven. (Mostly.)", Formatting.GREEN);
    }
}
