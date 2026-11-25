package dev.voidoutlaws.game;

import dev.voidoutlaws.reg.ModItems;
import dev.voidoutlaws.state.CrewState;
import dev.voidoutlaws.state.Job;
import dev.voidoutlaws.world.Loc;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;

import java.util.*;

/** The first-mission tutorial. Each player has their own step; steps are lenient so friends can join anytime. */
public final class Tutorial {
    private Tutorial() {}

    public static final int WELCOME = 0, CONSOLE = 1, JUMP = 2, HATCH = 3, TAKE_JOB = 4, HIDE = 5, SCAN = 6, HEAT = 7,
            ROLES = 8, DELIVER = 9, SPLIT = 10, DONE = 11;

    record Step(String title, String text, String reminder) {}

    static final Step[] STEPS = {
            new Step("Welcome, outlaw!",
                    "Welcome aboard the Lucky Larceny! You and your friends are its crew. This short tutorial shows you everything, step by step. (Type /tutorial anytime to see your current step, or /tutorial skip to skip it.)",
                    "Welcome aboard!"),
            new Step("Step 1: The Ship Console",
                    "Walk to the FRONT of the ship (the bridge) and right-click the glowing SHIP CONSOLE.",
                    "Right-click the SHIP CONSOLE at the front of the ship"),
            new Step("Step 2: Jump!",
                    "This is the star map. Click DUSTREACH to jump there (any place works). A jump takes about 20 seconds - watch the bar at the top of your screen.",
                    "Pick a destination on the Ship Console (try Dustreach)"),
            new Step("Step 3: Step outside",
                    "You've arrived! Go through the HATCH (the striped door in the cargo bay, middle of the ship). To come back later, right-click the DOCKING BEACON on the landing pad.",
                    "Leave the ship through the HATCH in the cargo bay"),
            new Step("Step 4: Take a job",
                    "Find a JOB BOARD and take a job. There's one inside the cantina on Dustreach (and another on the Gilded Maw). Jobs are shared by the whole crew.",
                    "Take a job at a JOB BOARD (Dustreach cantina)"),
            new Step("Step 5: Hide the cargo",
                    "Your job gave you CONTRABAND. Patrol scans find it in your pockets and in the Cargo Hold... but NOT in the HIDDEN COMPARTMENT! Go back to the ship and put the cargo into the Hidden Compartment (the loose floor plate in the cargo bay).",
                    "Put the contraband in the HIDDEN COMPARTMENT (cargo bay floor)"),
            new Step("Step 6: Pass a scan",
                    "Now use the Ship Console to jump to the job's destination (type /crew to check where). A patrol will scan you on the way. When it hails, open the Ship Console and click COMPLY - your cargo is hidden, so you'll pass!",
                    "Jump to the job's destination, then COMPLY with the scan"),
            new Step("Heat",
                    "That was a scan! If they find contraband, they take it and your crew's HEAT goes up. Heat is your wanted level (see the sidebar). High Heat means more scans, patrols, and even bounty hunters. Lower it by lying low at The Burrow, paying fines, bribing, or swapping your transponder.",
                    "HEAT = your wanted level. Keep it low!"),
            new Step("Step 7: Teamwork!",
                    "PIRATES! GUNNER: use the Gunnery Station and click the red enemy ships. PILOT: use the Ship Console and click when the star is inside the green zone to dodge. A 3rd friend can be the MECHANIC at the Engineering Panel and click sparks to fix the hull.",
                    "Gunner: click red ships. Pilot: click in the green zone."),
            new Step("Step 8: Deliver",
                    "Nice flying! When you dock at the destination, use its JOB BOARD (or the DEAD DROP on Meridian Prime) and click DELIVER. Cargo is taken from your pockets and the ship's holds automatically.",
                    "Deliver the cargo at the destination's Job Board"),
            new Step("Step 9: Payday",
                    "The money went into the CREW POT. Open the LOOT LOCKER on the ship (common room) to split it. Split it fairly... or skim a little and hope nobody notices!",
                    "Split the Crew Pot at the LOOT LOCKER on the ship"),
            new Step("Tutorial complete!",
                    "You're a real outlaw now! Use /guide for controls, /crew for status, and talk to people you meet for bigger work. Have fun, and don't get caught!",
                    "")
    };

    private static final Map<UUID, Integer> SINCE = new HashMap<>();
    private static final Map<UUID, Integer> AUTO = new HashMap<>();

    /** Current step, or -1 when the player isn't doing the tutorial. */
    public static int step(ServerPlayerEntity p) {
        Integer s = Game.S().tutStep.get(p.getUuid());
        return s == null ? -1 : s;
    }

    public static void start(ServerPlayerEntity p) {
        CrewState s = Game.S();
        s.tutDone.remove(p.getUuid());
        s.markDirty();
        if (!Game.hasItem(p, ModItems.HANDBOOK)) p.getInventory().offerOrDrop(Books.handbook());
        set(p, WELCOME);
    }

    public static void skip(ServerPlayerEntity p) {
        CrewState s = Game.S();
        s.tutStep.remove(p.getUuid());
        s.tutDone.add(p.getUuid());
        s.markDirty();
        Game.msg(p, "Tutorial skipped. Type /tutorial restart to replay it, or /guide for a quick reminder.", Formatting.GRAY);
        Story.onTutorialDone();
    }

    public static void set(ServerPlayerEntity p, int n) {
        CrewState s = Game.S();
        if (n >= DONE) {
            s.tutStep.remove(p.getUuid());
            boolean first = s.tutDone.add(p.getUuid());
            s.markDirty();
            show(p, DONE);
            if (first) {
                s.addCredits(p.getUuid(), 50);
                Game.msg(p, "+50 credits tutorial bonus in your wallet.", Formatting.GOLD);
            }
            Game.sound(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            Story.onTutorialDone();
            return;
        }
        s.tutStep.put(p.getUuid(), n);
        s.markDirty();
        AUTO.remove(p.getUuid());
        show(p, n);
    }

    private static void advance(ServerPlayerEntity p, int from) {
        if (step(p) == from) set(p, from + 1);
    }

    public static void show(ServerPlayerEntity p, int n) {
        if (n < 0 || n >= STEPS.length) return;
        Step st = STEPS[n];
        SINCE.put(p.getUuid(), 0);
        Game.title(List.of(p), Game.t(st.title, Formatting.GOLD), Game.t(st.reminder, Formatting.WHITE));
        p.sendMessage(Game.t("[Tutorial] ", Formatting.GOLD).append(Game.t(st.text, Formatting.YELLOW)));
        Game.sound(p, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 1f, 1.2f);
    }

    public static void showCurrent(ServerPlayerEntity p) {
        int n = step(p);
        if (n < 0) {
            Game.msg(p, "You've finished the tutorial. /tutorial restart replays it. /guide shows the controls.", Formatting.GRAY);
            return;
        }
        Step st = STEPS[n];
        p.sendMessage(Game.t("[Tutorial " + (n + 1) + "/" + DONE + "] ", Formatting.GOLD).append(Game.t(st.text, Formatting.YELLOW)));
    }

    // ------------------------------------------------------------------ hooks
    static void tickSecond(MinecraftServer server) {
        CrewState s = Game.S();
        for (ServerPlayerEntity p : Game.players()) {
            int n = step(p);
            if (n < 0) continue;
            int auto = AUTO.merge(p.getUuid(), 1, Integer::sum);
            Job j = s.job;
            if (n == WELCOME && auto >= 6) { set(p, CONSOLE); continue; }
            if (n == HEAT && auto >= 12) { set(p, ROLES); continue; }
            if (n == TAKE_JOB && j != null) {
                Game.msg(p, "Your crew has a job: " + j.shortLine(), Formatting.GREEN);
                set(p, j.type == Job.Type.SMUGGLE ? HIDE : DELIVER);
                continue;
            }
            if (n >= HIDE && n <= DELIVER && j == null) {
                Game.msg(p, "Your crew has no job right now - grab one from a Job Board.", Formatting.GRAY);
                set(p, TAKE_JOB);
                continue;
            }
            if (n == HIDE && hiddenHasContraband()) { set(p, SCAN); continue; }
            if (n == SPLIT && s.pot <= 0) { set(p, DONE); continue; }
            if (!s.hintsOff.contains(p.getUuid())) {
                int since = SINCE.merge(p.getUuid(), 1, Integer::sum);
                if (since % 30 == 0 && !STEPS[n].reminder.isEmpty()) Game.bar(p, "Tutorial: " + STEPS[n].reminder, Formatting.GOLD);
            }
        }
    }

    static boolean hiddenHasContraband() {
        CrewState s = Game.S();
        for (int i = 0; i < s.hidden.size(); i++) if (ModItems.isContraband(s.hidden.getStack(i))) return true;
        return false;
    }

    public static void onConsoleOpen(ServerPlayerEntity p) {
        advance(p, CONSOLE);
    }

    static void onJumpStart(List<ServerPlayerEntity> aboard) {
        for (ServerPlayerEntity p : aboard) {
            int n = step(p);
            if (n == CONSOLE || n == JUMP) set(p, JUMP);
        }
    }

    static void onArrive(List<ServerPlayerEntity> aboard, Loc dest) {
        for (ServerPlayerEntity p : aboard) {
            int n = step(p);
            if (n == JUMP) set(p, HATCH);
            if (n == HEAT || n == ROLES) {
                Game.msg(p, "You'll practice the ship roles the next time pirates show up!", Formatting.GRAY);
                set(p, DELIVER);
            }
        }
    }

    public static void onHatch(ServerPlayerEntity p) {
        advance(p, HATCH);
    }

    static void onJobTaken() {
        Job j = Game.S().job;
        for (ServerPlayerEntity p : Game.players()) {
            if (step(p) == TAKE_JOB) set(p, j != null && j.type == Job.Type.SMUGGLE ? HIDE : DELIVER);
        }
    }

    public static void onHiddenClosed(ServerPlayerEntity p) {
        if (hiddenHasContraband()) {
            for (ServerPlayerEntity pl : Game.players()) if (step(pl) == HIDE) set(pl, SCAN);
        }
    }

    static void onScan(List<ServerPlayerEntity> aboard, int found) {
        for (ServerPlayerEntity p : aboard) {
            if (step(p) != SCAN) continue;
            if (found > 0) Game.msg(p, "Oops - they found some contraband! That's what happens when it isn't hidden. Heat went up. Lesson learned!", Formatting.YELLOW);
            else Game.msg(p, "You passed! The Hidden Compartment kept your cargo safe.", Formatting.GREEN);
            set(p, HEAT);
        }
    }

    static void onEventStart(List<ServerPlayerEntity> aboard, JumpEvent e) {
        if (e.type != JumpEvent.Type.PIRATES) return;
        for (ServerPlayerEntity p : aboard) {
            int n = step(p);
            if (n == HEAT || n == ROLES) set(p, ROLES);
        }
    }

    static void onEventEnd(List<ServerPlayerEntity> aboard, JumpEvent e) {
        if (e.type != JumpEvent.Type.PIRATES) return;
        for (ServerPlayerEntity p : aboard) if (step(p) == ROLES) set(p, DELIVER);
    }

    static void onJobComplete() {
        for (ServerPlayerEntity p : Game.players()) {
            int n = step(p);
            if (n >= HIDE && n <= DELIVER) set(p, SPLIT);
        }
    }

    public static void onSplit() {
        for (ServerPlayerEntity p : Game.players()) if (step(p) == SPLIT) set(p, DONE);
    }
}
