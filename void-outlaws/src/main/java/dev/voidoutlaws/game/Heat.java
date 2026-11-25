package dev.voidoutlaws.game;

import dev.voidoutlaws.state.CrewState;
import dev.voidoutlaws.world.Loc;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Formatting;

/** Shared crew wanted level, 0..100. */
public final class Heat {
    private Heat() {}

    private static int decayTimer = 0;

    public static String level(int heat) {
        if (heat >= 80) return "MOST WANTED";
        if (heat >= 60) return "Hunted";
        if (heat >= 40) return "Wanted";
        if (heat >= 20) return "Noticed";
        return "Clean";
    }

    public static Formatting color(int heat) {
        if (heat >= 80) return Formatting.DARK_RED;
        if (heat >= 60) return Formatting.RED;
        if (heat >= 40) return Formatting.GOLD;
        if (heat >= 20) return Formatting.YELLOW;
        return Formatting.GREEN;
    }

    public static String stars(int heat) {
        int n = Math.min(5, heat / 20 + (heat > 0 ? 1 : 0));
        if (heat == 0) n = 0;
        return "★".repeat(n) + "☆".repeat(5 - n);
    }

    public static void add(int amount, String reason) {
        CrewState s = Game.S();
        int before = s.heat;
        s.heat = Math.max(0, Math.min(100, s.heat + amount));
        s.markDirty();
        if (s.heat == before) return;
        String sign = amount > 0 ? "+" : "";
        Game.crew((amount > 0 ? "▲ " : "▼ ") + sign + (s.heat - before) + " Heat (" + reason + ") - now " + s.heat + " " + level(s.heat),
                amount > 0 ? Formatting.RED : Formatting.GREEN);
        if (!level(before).equals(level(s.heat))) {
            String extra = switch (level(s.heat)) {
                case "Noticed" -> "Patrols are starting to recognise your ship.";
                case "Wanted" -> "More scans, and troopers on lawful worlds!";
                case "Hunted" -> "Bounty hunters are now looking for you!";
                case "MOST WANTED" -> "Everyone is after you. Time to lie low!";
                default -> "You're squeaky clean. Suspiciously clean.";
            };
            Game.title(Game.players(), Game.t("Heat: " + level(s.heat), color(s.heat)), Game.t(extra, Formatting.WHITE));
        }
    }

    public static int bribeCost() {
        CrewState s = Game.S();
        int base = 40 + s.heat * 3;
        double mult = 1.0 - Math.max(-50, Math.min(50, s.rep("concordat"))) / 200.0;
        return Math.max(20, (int) Math.round(base * mult));
    }

    public static int fineCost() {
        CrewState s = Game.S();
        return Math.max(30, 30 + s.heat * 4);
    }

    static void tickSecond(MinecraftServer server) {
        CrewState s = Game.S();
        if (s.heat <= 0 || Game.players().isEmpty() || s.jumping) return;
        decayTimer++;
        int every = s.shipLoc == Loc.BURROW ? 15 : (s.shipLoc.lawful ? 0 : 120);
        if (every > 0 && decayTimer >= every) {
            decayTimer = 0;
            s.heat = Math.max(0, s.heat - 1);
            s.markDirty();
            if (s.shipLoc == Loc.BURROW && s.heat % 10 == 0) Game.crew("Lying low at The Burrow... Heat is now " + s.heat + ".", Formatting.GREEN);
        }
    }
}
