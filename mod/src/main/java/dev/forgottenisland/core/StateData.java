package dev.forgottenisland.core;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Plain, serialisable story state: flags and integer variables. Independent of Minecraft for unit tests. */
public class StateData implements StateView {
    public static final String[] ROMANCES = {"wren", "ione", "sol"};
    protected final Set<String> flags = new TreeSet<>();
    protected final Map<String, Integer> vars = new TreeMap<>();

    @Override public boolean flag(String name) { return flags.contains(name); }

    @Override public int var(String name) {
        if (name.equals("dating")) {
            int n = 0;
            for (String r : ROMANCES) if (flags.contains(r + "_dating")) n++;
            return n;
        }
        if (name.equals("secrets")) return (int) flags.stream().filter(f -> f.startsWith("secret.")).count();
        return vars.getOrDefault(name, 0);
    }

    public boolean set(String flag) { boolean c = flags.add(flag); if (c) changed(); return c; }
    public boolean clear(String flag) { boolean c = flags.remove(flag); if (c) changed(); return c; }
    public void setVar(String name, int value) {
        Integer old = vars.put(name, value);
        if (old == null || old != value) changed();
    }
    public void addVar(String name, int delta) {
        int v = var(name) + delta;
        if (name.startsWith("rel.")) v = Math.max(0, Math.min(10, v));
        setVar(name, v);
    }
    public Set<String> flags() { return flags; }
    public Map<String, Integer> vars() { return vars; }

    /** Clears everything except developer settings. */
    public void reset() {
        boolean dev = flags.contains("dev_mode");
        flags.clear(); vars.clear();
        if (dev) flags.add("dev_mode");
        changed();
    }

    public void copyFrom(StateData o) { flags.clear(); flags.addAll(o.flags); vars.clear(); vars.putAll(o.vars); changed(); }

    protected void changed() {}
}
