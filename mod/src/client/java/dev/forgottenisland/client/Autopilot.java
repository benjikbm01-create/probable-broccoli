package dev.forgottenisland.client;

import dev.forgottenisland.client.screen.*;
import dev.forgottenisland.npc.IslandNpcEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Scripted client driver used for automated end-to-end tests and screenshots.
 * Enabled only with -Dfi.autopilot=&lt;command file&gt;. The file is polled for new lines, so commands can
 * be appended while the game runs. Results go to autopilot.log in the game directory.
 */
public final class Autopilot {
    private Autopilot() {}
    private static Path file; private static long readPos; private static final Deque<String> queue = new ArrayDeque<>();
    private static int waitTicks; private static PrintWriter log; private static int walkTicks; private static String waitScreen;
    private static int passes, fails;

    public static void init() {
        String f = System.getProperty("fi.autopilot");
        if (f == null) return;
        file = Paths.get(f);
        try { log = new PrintWriter(new FileWriter("autopilot.log", false), true); } catch (IOException e) { throw new RuntimeException(e); }
        log("autopilot enabled, script " + file.toAbsolutePath());
    }

    static void log(String s) { if (log != null) log.println("[" + System.currentTimeMillis() + "] " + s); }
    static void check(boolean ok, String what) { if (ok) passes++; else fails++; log((ok ? "PASS " : "FAIL ") + what); }

    private static void poll() {
        try {
            if (!Files.exists(file)) return;
            try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
                if (raf.length() < readPos) readPos = 0;
                raf.seek(readPos);
                String line;
                while ((line = raf.readLine()) != null) { if (!line.isBlank() && !line.startsWith("#")) queue.add(line.trim()); }
                readPos = raf.getFilePointer();
            }
        } catch (IOException e) { log("read error " + e); }
    }

    public static void tick(MinecraftClient mc) {
        if (file == null) return;
        if (mc.options != null) mc.options.pauseOnLostFocus = false;
        if (walkTicks > 0) { walkTicks--; mc.options.forwardKey.setPressed(walkTicks > 0); }
        if (waitScreen != null) {
            Screen s = mc.currentScreen;
            String n = s == null ? "none" : s.getClass().getSimpleName();
            if (!n.equals(waitScreen) && waitTicks-- > 0) return;
            check(n.equals(waitScreen), "screen is " + waitScreen + " (was " + n + ")");
            waitScreen = null; waitTicks = 0;
        }
        if (waitTicks > 0) { waitTicks--; return; }
        poll();
        int budget = 20;
        while (!queue.isEmpty() && waitTicks == 0 && waitScreen == null && budget-- > 0) {
            String cmd = queue.poll();
            try { run(mc, cmd); } catch (Exception e) { check(false, "command '" + cmd + "' threw " + e); }
        }
    }

    private static void run(MinecraftClient mc, String line) {
        log("> " + line);
        String[] a = line.split(" ", 2);
        String arg = a.length > 1 ? a[1] : "";
        switch (a[0]) {
            case "wait" -> waitTicks = Integer.parseInt(arg);
            case "chat" -> { if (mc.player != null) { if (arg.startsWith("/")) mc.player.networkHandler.sendChatCommand(arg.substring(1)); else mc.player.networkHandler.sendChatMessage(arg); } }
            case "shot" -> ScreenshotRecorder.saveScreenshot(mc.runDirectory, arg + ".png", mc.getFramebuffer(), t -> log("screenshot " + arg));
            case "hud" -> mc.options.hudHidden = arg.equals("off");
            case "look" -> { String[] p = arg.split(" "); mc.player.setYaw(Float.parseFloat(p[0])); mc.player.setPitch(Float.parseFloat(p[1])); }
            case "walk" -> { walkTicks = Integer.parseInt(arg); mc.options.forwardKey.setPressed(true); }
            case "screen" -> { waitScreen = arg.split(" ")[0]; waitTicks = arg.contains(" ") ? Integer.parseInt(arg.split(" ")[1]) : 100; }
            case "talk" -> {
                IslandNpcEntity n = nearestNpc(mc, arg);
                check(n != null, "found NPC " + arg);
                if (n != null) mc.interactionManager.interactEntity(mc.player, n, Hand.MAIN_HAND);
            }
            case "choose" -> {
                if (mc.currentScreen instanceof DialogueScreen ds) ds.choose(Integer.parseInt(arg) - 1);
                else check(false, "choose without dialogue open");
            }
            case "choosetext" -> {
                if (mc.currentScreen instanceof DialogueScreen ds) {
                    int idx = -1;
                    for (int i = 0; i < ds.options.size(); i++) if (ds.options.get(i).toLowerCase().contains(arg.toLowerCase())) { idx = i; break; }
                    check(idx >= 0, "option containing '" + arg + "'");
                    if (idx >= 0) ds.choose(idx);
                } else check(false, "choosetext without dialogue open");
            }
            case "expecttext" -> {
                boolean ok = mc.currentScreen instanceof DialogueScreen ds && (ds.text.toLowerCase().contains(arg.toLowerCase()) || ds.name.toLowerCase().contains(arg.toLowerCase()));
                check(ok, "dialogue text contains '" + arg + "'");
            }
            case "close" -> { if (mc.currentScreen != null) mc.currentScreen.close(); }
            case "intro" -> { if (mc.currentScreen instanceof IntroScreen s) { s.close(); check(true, "intro closed"); } else check(false, "intro screen open"); }
            case "journal" -> ClientNet.action("journal", 0);
            case "hint" -> ClientNet.action("hint", 0);
            case "use" -> {
                String[] p = arg.split(" ");
                BlockPos pos = new BlockPos(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                Direction side = p.length > 3 ? Direction.byName(p[3]) : Direction.UP;
                BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos).add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5), side, pos, false);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            }
            case "attack" -> {
                Entity best = null; double bd = 36;
                for (Entity e : mc.world.getEntities()) if (e instanceof HostileEntity && e.isAlive() && e.squaredDistanceTo(mc.player) < bd) { best = e; bd = e.squaredDistanceTo(mc.player); }
                if (best != null) { mc.player.lookAt(net.minecraft.command.argument.EntityAnchorArgumentType.EntityAnchor.EYES, best.getEyePos()); mc.interactionManager.attackEntity(mc.player, best); mc.player.swingHand(Hand.MAIN_HAND); }
            }
            case "slot" -> mc.player.getInventory().selectedSlot = Integer.parseInt(arg);
            case "expectui" -> check(ClientState.lastUiKind.equals(arg), "last UI message was " + arg + " (got " + ClientState.lastUiKind + ")");
            case "expecthud" -> check(ClientState.objTitle.toLowerCase().contains(arg.toLowerCase()) || (arg.equals("empty") && ClientState.objTitle.isEmpty()), "HUD objective contains '" + arg + "' (got '" + ClientState.objTitle + "')");
            case "expectnpc" -> { String[] p = arg.split(" "); IslandNpcEntity n = nearestNpc(mc, p[0]); check(n != null && (p.length < 2 || n.squaredDistanceTo(mc.player) < Double.parseDouble(p[1]) * Double.parseDouble(p[1])), "NPC " + arg + " nearby"); }
            case "respawn" -> { if (mc.player != null) mc.player.requestRespawn(); if (mc.currentScreen != null) mc.setScreen(null); }
            case "log" -> log(arg);
            case "summary" -> log("SUMMARY passes=" + passes + " fails=" + fails);
            case "quit" -> { log("SUMMARY passes=" + passes + " fails=" + fails); mc.scheduleStop(); }
            default -> check(false, "unknown command " + a[0]);
        }
    }

    static IslandNpcEntity nearestNpc(MinecraftClient mc, String id) {
        IslandNpcEntity best = null; double bd = 64 * 64;
        for (Entity e : mc.world.getEntities()) if (e instanceof IslandNpcEntity n && n.npcId().equals(id) && e.squaredDistanceTo(mc.player) < bd) { best = n; bd = e.squaredDistanceTo(mc.player); }
        return best;
    }
}
