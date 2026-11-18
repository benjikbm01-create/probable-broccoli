package dev.forgottenisland.game;

import dev.forgottenisland.ForgottenIsland;
import dev.forgottenisland.core.StateData;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Executes effect strings from content:
 * set:flag  clear:flag  var:name=N  add:name:N  rel:npc:N  give:item[:count]  take:item[:count]
 * event:name[:arg]  sound:namespace:path  toast:Title|Text  checkpoint:place  secret:id  book:id
 */
public final class Effects {
    private Effects() {}

    public static void run(ServerPlayerEntity p, List<String> effects) {
        for (String e : effects) run(p, e);
    }

    public static void run(ServerPlayerEntity p, String e) {
        StateData st = Game.state(p.getServer());
        int i = e.indexOf(':');
        String verb = i < 0 ? e : e.substring(0, i);
        String arg = i < 0 ? "" : e.substring(i + 1);
        try {
            switch (verb) {
                case "set" -> st.set(arg);
                case "clear" -> st.clear(arg);
                case "var" -> { String[] kv = arg.split("="); st.setVar(kv[0].trim(), Integer.parseInt(kv[1].trim())); }
                case "add" -> { String[] kv = arg.split(":"); st.addVar(kv[0], Integer.parseInt(kv[1])); }
                case "rel" -> { String[] kv = arg.split(":"); st.addVar("rel." + kv[0], Integer.parseInt(kv[1])); }
                case "give" -> { String[] a = arg.split(":"); KeyItems.give(p, a[0], a.length > 1 ? Integer.parseInt(a[1]) : -1); }
                case "take" -> { String[] a = arg.split(":"); KeyItems.take(p, a[0], a.length > 1 ? Integer.parseInt(a[1]) : 1); }
                case "event" -> { String[] a = arg.split(":", 2); Story.fire(p, a[0], a.length > 1 ? a[1] : ""); }
                case "sound" -> {
                    var snd = Registries.SOUND_EVENT.get(Identifier.of(arg));
                    if (snd != null) p.getServerWorld().playSound(null, p.getBlockPos(), snd, SoundCategory.PLAYERS, 1f, 1f);
                }
                case "toast" -> Game.toast(p, arg.contains("|") ? arg.substring(0, arg.indexOf('|')) : arg, arg.contains("|") ? arg.substring(arg.indexOf('|') + 1) : "");
                case "checkpoint" -> Checkpoints.set(p, arg);
                case "secret" -> Secrets.discover(p, arg);
                case "book" -> KeyItems.give(p, arg, 1);
                default -> ForgottenIsland.LOG.warn("Unknown effect {}", e);
            }
        } catch (RuntimeException ex) {
            ForgottenIsland.LOG.error("Effect '{}' failed", e, ex);
        }
    }
}
