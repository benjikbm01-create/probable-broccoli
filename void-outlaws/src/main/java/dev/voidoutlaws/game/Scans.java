package dev.voidoutlaws.game;

import dev.voidoutlaws.reg.ModItems;
import dev.voidoutlaws.state.CrewState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;

import java.util.List;

/** Patrol scans: what gets found, and the choices the pilot can make. */
public final class Scans {
    private Scans() {}

    public enum Choice { COMPLY, BRIBE, RUN }

    public static void respond(ServerPlayerEntity p, Choice c) {
        JumpEvent e = Jumps.event;
        if (e == null || e.type != JumpEvent.Type.SCAN || e.done) return;
        CrewState s = Game.S();
        List<ServerPlayerEntity> aboard = Game.aboard();
        switch (c) {
            case COMPLY -> {
                Game.to(aboard, Game.name(p) + " answers: \"Scan away, officer!\" (big innocent smile)", Formatting.AQUA);
                scan(false);
                e.done = true;
            }
            case BRIBE -> {
                if (e.tutorial) {
                    Game.msg(p, "Save your credits for now: your cargo is hidden, so just COMPLY!", Formatting.YELLOW);
                    return;
                }
                int cost = Heat.bribeCost();
                if (s.credits(p.getUuid()) < cost) {
                    Game.msg(p, "You need " + cost + " credits in YOUR wallet to bribe. (You have " + s.credits(p.getUuid()) + ".)", Formatting.RED);
                    return;
                }
                s.addCredits(p.getUuid(), -cost);
                double chance = s.heat >= 80 ? 0.5 : 0.85;
                if (Game.R.nextDouble() < chance) {
                    Game.to(aboard, Game.name(p) + " slips the officer " + cost + " credits. \"Everything seems to be in order.\" *wink*", Formatting.GOLD);
                    Heat.add(-1, "bribe");
                    s.addRep("concordat", 1);
                } else {
                    Game.to(aboard, "This officer is HONEST?! How rude. They keep the money AND scan you.", Formatting.RED);
                    Heat.add(5, "attempted bribery");
                    scan(true);
                }
                e.done = true;
            }
            case RUN -> {
                if (e.tutorial) {
                    Game.msg(p, "Not during the tutorial! Your cargo is hidden, so just COMPLY.", Formatting.YELLOW);
                    return;
                }
                Game.to(aboard, Game.name(p) + " slams the throttle! \"Sorry officer, can't hear you, bad signal!\"", Formatting.RED);
                Heat.add(6, "ran from a scan");
                e.done = true;
                Jumps.queue(JumpEvent.Type.CHASE, 10);
            }
        }
    }

    /** Scan the ship. Returns how many contraband items were found. */
    public static int scan(boolean strict) {
        CrewState s = Game.S();
        List<ServerPlayerEntity> aboard = Game.aboard();
        boolean tutorial = Jumps.event != null && Jumps.event.tutorial;
        int found = 0;
        // cargo hold: always scanned
        for (int i = 0; i < s.cargo.size(); i++) {
            ItemStack st = s.cargo.getStack(i);
            if (ModItems.isContraband(st)) { found += st.getCount(); s.cargo.setStack(i, ItemStack.EMPTY); }
        }
        // pockets
        for (ServerPlayerEntity p : aboard) {
            if (Gear.disguised(p)) {
                Game.msg(p, "The scanner beeps at your fake mustache and moves on.", Formatting.GRAY);
                continue;
            }
            PlayerInventory inv = p.getInventory();
            boolean saved = false;
            for (int i = 0; i < inv.size(); i++) {
                ItemStack st = inv.getStack(i);
                if (!ModItems.isContraband(st)) continue;
                if (!saved && Companion.swallowChance(p)) {
                    saved = true;
                    Entity buddy = Companion.nearby(p);
                    String n = buddy != null && buddy.hasCustomName() ? buddy.getCustomName().getString() : "Your buddy";
                    Game.msg(p, n + " swallowed your " + st.getName().getString() + " to hide it! *gulp* (It'll give it back later. Probably.)", Formatting.LIGHT_PURPLE);
                    continue;
                }
                found += st.getCount();
                inv.setStack(i, ItemStack.EMPTY);
            }
        }
        // hidden compartment: only risky at high heat
        double chance = tutorial ? 0 : Math.max(0, (s.heat - 50) / 200.0) * (strict ? 2 : 1);
        if (Game.R.nextDouble() < chance) {
            int hiddenFound = 0;
            for (int i = 0; i < s.hidden.size(); i++) {
                ItemStack st = s.hidden.getStack(i);
                if (ModItems.isContraband(st)) { hiddenFound += st.getCount(); s.hidden.setStack(i, ItemStack.EMPTY); }
            }
            if (hiddenFound > 0) Game.to(aboard, "Uh oh. The scanner found the Hidden Compartment! (High Heat makes scans sharper.)", Formatting.DARK_RED);
            found += hiddenFound;
        }
        s.markDirty();
        if (found > 0) {
            int fine = Math.min(s.pot, 50);
            s.pot -= fine;
            Game.title(aboard, Game.t("CONTRABAND FOUND!", Formatting.RED), Game.t(found + " item(s) confiscated", Formatting.WHITE));
            Game.sound(aboard, SoundEvents.ENTITY_VILLAGER_NO, 1f, 0.8f);
            Game.to(aboard, "The patrol confiscates " + found + " contraband item(s)" + (fine > 0 ? " and fines the crew pot " + fine + " credits." : "."), Formatting.RED);
            s.addRep("concordat", -5);
            Heat.add(15, "contraband found");
        } else {
            Game.title(aboard, Game.t("SCAN CLEAR", Formatting.GREEN), Game.t("\"Have a lawful day, citizens.\"", Formatting.WHITE));
            Game.sound(aboard, SoundEvents.ENTITY_VILLAGER_YES, 1f, 1f);
            Heat.add(-2, "clean scan");
        }
        Tutorial.onScan(aboard, found);
        return found;
    }
}
