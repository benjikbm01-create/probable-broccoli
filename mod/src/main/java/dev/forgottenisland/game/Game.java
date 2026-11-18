package dev.forgottenisland.game;

import com.google.gson.JsonObject;
import dev.forgottenisland.ForgottenIsland;
import dev.forgottenisland.content.Content;
import dev.forgottenisland.core.GameCtx;
import dev.forgottenisland.core.IslandState;
import dev.forgottenisland.core.StateData;
import dev.forgottenisland.dialogue.DialogueManager;
import dev.forgottenisland.net.Net;
import dev.forgottenisland.npc.NpcDirector;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.ItemEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

/** Central access to game state plus the per-tick systems (HUD sync, objective notices, item safety). */
public final class Game {
    private Game() {}
    private static MinecraftServer server;
    private static final Map<String, Integer> lastStatus = new HashMap<>();
    private static boolean statusPrimed;
    private static String lastHud = "";
    private static long tickCount;

    public static MinecraftServer server() { return server; }
    public static StateData state(MinecraftServer s) { return IslandState.get(s).data; }
    public static boolean devMode(MinecraftServer s) { return state(s).flag("dev_mode"); }

    public static void onServerStart(MinecraftServer s) {
        server = s; lastStatus.clear(); statusPrimed = false; lastHud = ""; tickCount = 0;
        NpcDirector.reset();
    }
    public static void onServerStop() { server = null; NpcDirector.reset(); }

    public static void toast(ServerPlayerEntity p, String title, String text) {
        ServerPlayNetworking.send(p, new Net.Ui("toast", title + "\n" + text));
    }

    public static void tick(MinecraftServer s) {
        tickCount++;
        if (tickCount % 10 == 0) {
            NpcDirector.tick(s);
            for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
                Zones.tick(p);
                syncHud(p, false);
            }
            objectiveNotices(s);
        }
        if (tickCount % 20 == 0) DialogueManager.tick(s);
        if (tickCount % 100 == 0) for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) protectKeyItems(p);
        Puzzles.tick(s, tickCount);
        Encounters.tick(s, tickCount);
        Story.tick(s, tickCount);
    }

    /** HUD: tracked objective + atmosphere settings. */
    public static void syncHud(ServerPlayerEntity p, boolean force) {
        StateData st = state(p.getServer());
        GameCtx c = new GameCtx(st, p);
        Content.Objective o = Journal.tracked(c);
        JsonObject j = new JsonObject();
        boolean dev = devMode(p.getServer());
        j.addProperty("title", o == null ? "" : dev ? o.id : o.title);
        j.addProperty("text", o == null ? "" : dev ? "(objective text hidden in dev mode)" : o.text);
        j.addProperty("fog", st.flag("lantern_lit") ? 0 : 100);
        j.addProperty("dev", dev);
        j.addProperty("hud", !st.flag("hud_off"));
        j.addProperty("intro", st.flag("intro_done"));
        String s = j.toString();
        if (force || !s.equals(lastHud)) { lastHud = s; ServerPlayNetworking.send(p, new Net.Ui("hud", s)); }
    }

    private static void objectiveNotices(MinecraftServer s) {
        List<ServerPlayerEntity> ps = s.getPlayerManager().getPlayerList();
        if (ps.isEmpty()) return;
        ServerPlayerEntity p = ps.get(0);
        StateData st = state(s);
        if (!st.flag("intro_done")) return;
        GameCtx c = new GameCtx(st, p);
        for (Content.Objective o : ContentHolder.get().objectives) {
            int now = Journal.done(o, c) ? 2 : Journal.active(o, c) ? 1 : 0;
            Integer before = lastStatus.put(o.id, now);
            if (!statusPrimed || before == null || before == now) continue;
            boolean dev = devMode(s);
            String title = dev ? o.id : o.title;
            if (now == 1 && before == 0) {
                toast(p, o.isMain() ? "New objective" : "personal".equals(o.kind) ? "Personal quest" : "Side quest", title);
                p.getServerWorld().playSound(null, p.getBlockPos(), SoundEvents.UI_LOOM_TAKE_RESULT, SoundCategory.PLAYERS, 0.8f, 1.2f);
            } else if (now == 2) {
                toast(p, "Completed", title);
                p.getServerWorld().playSound(null, p.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.5f, 1.0f);
            }
        }
        statusPrimed = true;
    }

    /** Quest items are never lost: dropped copies vanish and the item returns to the pack. */
    private static void protectKeyItems(ServerPlayerEntity p) {
        StateData st = state(p.getServer());
        if (devMode(p.getServer()) && st.flag("dev_no_restore")) return;
        for (ItemEntity ie : p.getServerWorld().getEntitiesByClass(ItemEntity.class, p.getBoundingBox().expand(64), e -> true)) {
            String id = KeyItems.idOf(ie.getStack());
            if (id != null && KeyItems.isQuest(id) && ie.getItemAge() > 60) ie.discard();
        }
        for (String id : KeyItems.ids()) {
            if (!KeyItems.isQuest(id) || !st.flag("got." + id) || st.flag("used." + id)) continue;
            if (KeyItems.count(p, id) > 0 || !p.currentScreenHandler.getCursorStack().isEmpty() && id.equals(KeyItems.idOf(p.currentScreenHandler.getCursorStack()))) continue;
            KeyItems.give(p, id, -1);
            p.sendMessage(Text.literal("(" + KeyItems.def(id).name() + " has been returned to your pack.)").formatted(Formatting.GRAY, Formatting.ITALIC), false);
        }
        // remember any key items the player has picked up (from chests etc.)
        var inv = p.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            String id = KeyItems.idOf(inv.getStack(i));
            if (id != null && KeyItems.isQuest(id)) st.set("got." + id);
        }
    }

    public static void log(String msg, Object... args) { ForgottenIsland.LOG.info(msg, args); }
}
