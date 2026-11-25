package dev.voidoutlaws.menu;

import dev.voidoutlaws.game.*;
import dev.voidoutlaws.state.CrewState;
import dev.voidoutlaws.state.Job;
import dev.voidoutlaws.world.Loc;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** Pilot station: star map while docked, steering / hail answers during jumps. */
public class ConsoleView extends View {
    private static final Loc[] ORDER = {Loc.MERIDIAN, Loc.DUSTREACH, Loc.GILDED_MAW, Loc.BURROW, Loc.DERELICT};

    @Override public Text title() { return Text.literal("Ship Console (Pilot)"); }
    @Override public int rows() { return 3; }
    @Override public Roles.Role role() { return Roles.Role.PILOT; }

    @Override
    public void render(ServerPlayerEntity p, Slots g) {
        CrewState s = Game.S();
        g.fill(Btn.pane(Items.BLACK_STAINED_GLASS_PANE));
        JumpEvent e = Jumps.event;
        if (!s.jumping) {
            g.set(4, Btn.of(Items.COMPASS, "Star Map", Formatting.AQUA,
                    "Docked at: " + s.shipLoc.display, "Click a destination to JUMP.",
                    "Crewmates outside the ship stay behind", "(they can shuttle back later)."));
            for (int i = 0; i < ORDER.length; i++) {
                Loc l = ORDER[i];
                if (!Jumps.destAvailable(l)) {
                    g.set(11 + i, Btn.of(Items.GRAY_DYE, "??? Unknown signal", Formatting.DARK_GRAY, "Needs a boarding job to unlock."));
                    continue;
                }
                List<String> lore = new ArrayList<>(List.of(l.lore));
                if (l == s.shipLoc) lore.add("§aYou are here");
                else lore.add("§eClick to jump!");
                Job j = s.job;
                if (j != null && (j.dest == l || (j.type == Job.Type.HEIST && l == Loc.MERIDIAN) || (j.type == Job.Type.BOARDING && l == Loc.DERELICT)))
                    lore.add("§6★ Your job goes here");
                ItemStack st = Btn.of(l.icon, l.display, l.color, lore.toArray(new String[0]));
                if (l == s.shipLoc) Btn.glint(st);
                g.set(11 + i, st);
            }
            g.set(18, status(s));
            g.set(26, Btn.close());
            return;
        }
        if (e == null) {
            int eta = Math.max(0, (Jumps.jumpTotal - Jumps.jumpTicks) / 20);
            g.set(4, Btn.of(Items.ENDER_EYE, "In hyperspace -> " + s.jumpDest.display, Formatting.AQUA,
                    "ETA: " + eta + "s", "Stay alert! Things can happen during a jump."));
            g.row(1, Btn.pane(Items.LIGHT_BLUE_STAINED_GLASS_PANE));
            g.set(18, status(s));
            g.set(26, Btn.close());
            return;
        }
        if (e.type == JumpEvent.Type.SCAN) {
            g.set(4, Btn.of(Items.SPYGLASS, "Patrol hail! (" + e.secondsLeft() + "s to answer)", Formatting.AQUA,
                    "\"Cut your engines and prepare for a cargo scan.\""));
            g.set(11, Btn.of(Items.LIME_CONCRETE, "COMPLY", Formatting.GREEN,
                    "Let them scan the ship.", "They search the Cargo Hold and pockets.", "The Hidden Compartment is (usually) safe."));
            g.set(13, Btn.of(Items.GOLD_INGOT, "BRIBE (" + Heat.bribeCost() + " cr)", Formatting.GOLD,
                    "Pay from YOUR wallet. Usually works.", "Your wallet: " + s.credits(p.getUuid()) + " cr", "Cheaper with good Concordat rep."));
            g.set(15, Btn.of(Items.RED_CONCRETE, "RUN!", Formatting.RED,
                    "Floor it! Starts a POLICE CHASE.", "+Heat, but escape = no scan."));
            return;
        }
        // timing bar for pirates / chase / salvage
        String head = switch (e.type) {
            case PIRATES -> "EVADE! Dodges ready: " + e.dodge;
            case CHASE -> "ESCAPE! Progress: " + fmt(e.escape) + "/" + e.escapeNeed;
            default -> "Catch the salvage!";
        };
        List<String> lore = new ArrayList<>();
        lore.add("Click when the ★ is inside the GREEN zone!");
        if (e.flashTicks > 0) lore.add("§e" + e.flash);
        lore.add(e.secondsLeft() + "s left");
        g.set(4, Btn.of(Items.NETHER_STAR, head, Formatting.GOLD, lore.toArray(new String[0])));
        for (int i = 0; i < 9; i++) {
            boolean inZone = i >= e.zone && i < e.zone + e.zoneW;
            ItemStack st;
            if (i == e.cursor) st = Btn.of(Items.NETHER_STAR, inZone ? "★ NOW!" : "★", inZone ? Formatting.GREEN : Formatting.YELLOW);
            else if (inZone) st = Btn.of(Items.LIME_STAINED_GLASS_PANE, "green zone", Formatting.GREEN);
            else st = Btn.pane(Items.GRAY_STAINED_GLASS_PANE);
            if (i == e.cursor && inZone) Btn.glint(st);
            g.set(9 + i, st);
        }
        ItemStack steer = e.pilotCooldown > 0
                ? Btn.of(Items.ORANGE_STAINED_GLASS_PANE, "steadying...", Formatting.GOLD)
                : Btn.of(Items.LEVER, "STEER! (click)", Formatting.RED, "Click anywhere in the bottom two rows.");
        g.row(2, steer);
    }

    private static String fmt(double d) {
        return d == Math.floor(d) ? String.valueOf((int) d) : String.format("%.1f", d);
    }

    static ItemStack status(CrewState s) {
        return Btn.of(Items.PAPER, "Ship status", Formatting.WHITE,
                "Heat: " + s.heat + " (" + Heat.level(s.heat) + ")",
                "Hull: " + s.hull + "%",
                "Transponder ID: " + s.transponder,
                "Crew Pot: " + s.pot + " cr",
                "Job: " + (s.job == null ? "none" : s.job.shortLine()));
    }

    @Override
    public void click(ServerPlayerEntity p, int slot, boolean right) {
        CrewState s = Game.S();
        if (slot == 26 && (!s.jumping || Jumps.event == null)) { p.closeHandledScreen(); return; }
        if (!s.jumping) {
            if (slot >= 11 && slot < 11 + ORDER.length) {
                Loc l = ORDER[slot - 11];
                if (l != s.shipLoc && Jumps.destAvailable(l)) Jumps.start(p, l);
            }
            return;
        }
        JumpEvent e = Jumps.event;
        if (e == null) return;
        if (e.type == JumpEvent.Type.SCAN) {
            if (slot == 11) Scans.respond(p, Scans.Choice.COMPLY);
            else if (slot == 13) Scans.respond(p, Scans.Choice.BRIBE);
            else if (slot == 15) Scans.respond(p, Scans.Choice.RUN);
            return;
        }
        if (slot >= 9) Jumps.pilotClick(p);
    }

    @Override
    public void closed(ServerPlayerEntity p) {
        Roles.release(p, Roles.Role.PILOT);
    }
}
