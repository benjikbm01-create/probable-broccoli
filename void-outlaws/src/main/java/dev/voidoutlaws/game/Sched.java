package dev.voidoutlaws.game;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Run something a few ticks later on the server thread. */
public final class Sched {
    private Sched() {}

    private record Task(long due, Runnable r) {}

    private static final List<Task> TASKS = new ArrayList<>();

    public static void later(int ticks, Runnable r) {
        TASKS.add(new Task(Game.tick + Math.max(1, ticks), r));
    }

    static void tick() {
        if (TASKS.isEmpty()) return;
        List<Task> due = new ArrayList<>();
        for (Iterator<Task> it = TASKS.iterator(); it.hasNext(); ) {
            Task t = it.next();
            if (t.due <= Game.tick) { due.add(t); it.remove(); }
        }
        for (Task t : due) {
            try { t.r.run(); } catch (Exception e) { dev.voidoutlaws.VoidOutlaws.LOG.error("Scheduled task failed", e); }
        }
    }

    static void clear() { TASKS.clear(); }
}
