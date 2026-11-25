package dev.voidoutlaws.game;

/** State of one event that happens during a jump. Menus render straight from this. */
public class JumpEvent {
    public enum Type {
        SCAN("Patrol Scan"), PIRATES("Pirate Attack"), CHASE("Police Chase"), SALVAGE("Drifting Salvage");
        public final String display;

        Type(String d) { display = d; }
    }

    public final Type type;
    public final boolean tutorial;
    public int age;
    public int duration;
    public boolean done;

    // gunner grid (45 slots); value = remaining life in ticks, 0 = empty
    public final int[] targets = new int[45];
    public int spawnEvery = 16, life = 40, maxTargets = 4;
    public int squadronHp;
    public int hits;

    // pilot timing bar
    public int cursor = 0, dir = 1, zone = 3, zoneW = 2, speed = 3, pilotCooldown = 0;
    public int dodge = 0;
    public double escape = 0;
    public int escapeNeed = 5;

    // mechanic
    public int mechReadyAt = 0;

    public boolean boardersSpawned = false;
    public String flash = "";
    public int flashTicks = 0;

    public JumpEvent(Type type, boolean tutorial) {
        this.type = type;
        this.tutorial = tutorial;
    }

    public void flash(String s) {
        flash = s;
        flashTicks = 40;
    }

    public int secondsLeft() {
        return Math.max(0, (duration - age) / 20);
    }

    public boolean cursorInZone() {
        return cursor >= zone && cursor < zone + zoneW;
    }

    public int targetCount() {
        int n = 0;
        for (int t : targets) if (t > 0) n++;
        return n;
    }

    public boolean usesTargets() {
        return type == Type.PIRATES || type == Type.CHASE;
    }

    public boolean usesBar() {
        return type == Type.PIRATES || type == Type.CHASE || type == Type.SALVAGE;
    }
}
