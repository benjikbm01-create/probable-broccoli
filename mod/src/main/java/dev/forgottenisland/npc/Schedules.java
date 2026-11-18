package dev.forgottenisland.npc;

import dev.forgottenisland.content.Content;
import dev.forgottenisland.core.Cond;
import dev.forgottenisland.core.StateView;

/** Resolves where a character should be at a given time, honouring story overrides. Pure logic (unit tested). */
public final class Schedules {
    private Schedules() {}

    public static boolean inRange(Content.ScheduleEntry e, long timeOfDay) {
        long t = Math.floorMod(timeOfDay, 24000L);
        return e.from <= e.to ? (t >= e.from && t < e.to) : (t >= e.from || t < e.to);
    }

    public static Content.ScheduleEntry resolve(Content.Schedule s, StateView state, long timeOfDay) {
        if (s == null) return null;
        for (Content.ScheduleOverride o : s.overrides) {
            if (!Cond.test(o.if_, state)) continue;
            for (Content.ScheduleEntry e : o.entries) if (inRange(e, timeOfDay)) return e;
        }
        for (Content.ScheduleEntry e : s.entries) if (inRange(e, timeOfDay)) return e;
        return s.entries.isEmpty() ? null : s.entries.get(0);
    }
}
