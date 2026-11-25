package dev.voidoutlaws.game;

import dev.voidoutlaws.menu.Menu;
import dev.voidoutlaws.state.CrewState;
import dev.voidoutlaws.world.Loc;
import dev.voidoutlaws.world.Structures;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameRules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/** Central hub: server reference, ticking, and broadcast helpers. */
public final class Game {
    private Game() {}

    public static MinecraftServer server;
    public static long tick;
    public static final Random R = new Random();
    public static final String SHIP_NAME = "Lucky Larceny";

    public static CrewState S() {
        return CrewState.get(server);
    }

    public static void onStart(MinecraftServer s) {
        server = s;
        tick = 0;
        CrewState st = S();
        Structures.ensureShip(s, st);
        Structures.ensureBuilt(s, st, st.shipLoc);
        if (!st.rulesSet) {
            s.getGameRules().get(GameRules.KEEP_INVENTORY).set(true, s);
            st.rulesSet = true;
            st.markDirty();
        }
        if (st.jumping) {
            // A jump was interrupted by a restart: just finish it.
            st.jumping = false;
            st.shipLoc = st.jumpDest;
            Structures.ensureBuilt(s, st, st.shipLoc);
            st.markDirty();
        }
        Sidebar.init(s);
    }

    public static void onStop() {
        Jumps.reset();
        Mobs.reset();
        Missions.reset();
        Jobs.reset();
        Sched.clear();
        Menu.OPEN.clear();
        Roles.clear();
        server = null;
    }

    public static void tick(MinecraftServer s) {
        if (server == null) return;
        tick++;
        Sched.tick();
        Jumps.tick(s);
        Menu.tickAll();
        if (tick % 20 == 0) {
            Sidebar.update(s);
            Heat.tickSecond(s);
            Missions.tickSecond(s);
            Mobs.tickSecond(s);
            Tutorial.tickSecond(s);
            Gear.tickSecond(s);
            Jumps.tickSecond(s);
        }
        if (tick % 100 == 0) Npcs.tick(s);
    }

    // ------------------------------------------------------------------ players
    public static List<ServerPlayerEntity> players() {
        return new ArrayList<>(server.getPlayerManager().getPlayerList());
    }

    public static List<ServerPlayerEntity> aboard() {
        List<ServerPlayerEntity> out = new ArrayList<>();
        for (ServerPlayerEntity p : players()) if (p.getServerWorld().getRegistryKey().equals(Loc.SHIP)) out.add(p);
        return out;
    }

    public static List<ServerPlayerEntity> in(Loc l) {
        List<ServerPlayerEntity> out = new ArrayList<>();
        for (ServerPlayerEntity p : players()) if (p.getServerWorld().getRegistryKey().equals(l.key)) out.add(p);
        return out;
    }

    public static boolean isAboard(ServerPlayerEntity p) {
        return p.getServerWorld().getRegistryKey().equals(Loc.SHIP);
    }

    public static Loc locOf(ServerPlayerEntity p) {
        return Loc.fromWorld(p.getServerWorld().getRegistryKey());
    }

    // ------------------------------------------------------------------ messages
    public static MutableText prefix() {
        return Text.literal("[" + SHIP_NAME + "] ").formatted(Formatting.DARK_AQUA);
    }

    public static MutableText t(String s, Formatting f) {
        return Text.literal(s).formatted(f);
    }

    public static void crew(Text msg) {
        for (ServerPlayerEntity p : players()) p.sendMessage(prefix().append(msg));
    }

    public static void crew(String msg, Formatting f) {
        crew(t(msg, f));
    }

    public static void msg(ServerPlayerEntity p, String msg, Formatting f) {
        p.sendMessage(prefix().append(t(msg, f)));
    }

    public static void to(Collection<ServerPlayerEntity> ps, String msg, Formatting f) {
        for (ServerPlayerEntity p : ps) msg(p, msg, f);
    }

    public static void bar(ServerPlayerEntity p, String msg, Formatting f) {
        p.sendMessage(t(msg, f), true);
    }

    public static void title(Collection<ServerPlayerEntity> ps, Text title, Text sub) {
        for (ServerPlayerEntity p : ps) {
            p.networkHandler.sendPacket(new TitleFadeS2CPacket(8, 50, 15));
            p.networkHandler.sendPacket(new SubtitleS2CPacket(sub == null ? Text.empty() : sub));
            p.networkHandler.sendPacket(new TitleS2CPacket(title));
        }
    }

    public static void sound(Collection<ServerPlayerEntity> ps, SoundEvent e, float vol, float pitch) {
        for (ServerPlayerEntity p : ps) p.playSoundToPlayer(e, SoundCategory.MASTER, vol, pitch);
    }

    public static void sound(ServerPlayerEntity p, SoundEvent e, float vol, float pitch) {
        p.playSoundToPlayer(e, SoundCategory.MASTER, vol, pitch);
    }

    public static boolean hasItem(ServerPlayerEntity p, net.minecraft.item.Item item) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.size(); i++) if (inv.getStack(i).isOf(item)) return true;
        return false;
    }

    public static String name(ServerPlayerEntity p) {
        return p.getGameProfile().getName();
    }
}
