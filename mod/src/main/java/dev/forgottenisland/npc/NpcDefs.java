package dev.forgottenisland.npc;

import java.util.LinkedHashMap;
import java.util.Map;

/** The island's named residents. Appearance lives in assets/forgotten_island/textures/entity/npc/&lt;id&gt;.png */
public final class NpcDefs {
    public record Def(String id, String name, boolean slim, float scale, String mainHand, String offHand) {}
    public static final Map<String, Def> ALL = new LinkedHashMap<>();
    static {
        add("hester", "Hester Moss", true, 0.93f, "", "");
        add("tobias", "Tobias Wake", false, 0.95f, "", "");
        add("wendel", "Wendel Pike", false, 0.89f, "minecraft:stick", "");
        add("corin", "Corin Ashby", false, 0.99f, "minecraft:mace", "");
        add("wren", "Wren Ashdown", true, 0.94f, "minecraft:bow", "");
        add("ione", "Ione Calder", true, 0.92f, "minecraft:writable_book", "");
        add("sol", "Sol Reyes", true, 0.95f, "", "");
    }
    private static void add(String id, String name, boolean slim, float scale, String main, String off) {
        ALL.put(id, new Def(id, name, slim, scale, main, off));
    }
    private NpcDefs() {}
    public static Def get(String id) { return ALL.get(id); }
    public static String name(String id) { Def d = ALL.get(id); return d == null ? id : d.name(); }
}
