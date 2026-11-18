package dev.forgottenisland.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayDeque;
import java.util.Deque;

/** Latest UI state pushed by the server. */
public final class ClientState {
    private ClientState() {}
    public static String objTitle = "", objText = "";
    public static boolean dev, hudAllowed = true, hudVisible = true, introDone;
    public static float fogTarget = 0, fog = 0;          // 0..1
    public static String fadeText; public static long fadeStart;
    public record Toast(String title, String text, long start, long dur) {}
    public static final Deque<Toast> toasts = new ArrayDeque<>();
    /** Set by tests (autopilot) to observe what the UI shows. */
    public static String lastUiKind = "";

    public static void hud(String json) {
        JsonObject j = JsonParser.parseString(json).getAsJsonObject();
        objTitle = j.get("title").getAsString();
        objText = j.get("text").getAsString();
        fogTarget = j.get("fog").getAsInt() / 100f;
        dev = j.get("dev").getAsBoolean();
        hudAllowed = j.get("hud").getAsBoolean();
        introDone = j.get("intro").getAsBoolean();
    }

    public static void toast(String title, String text, long dur) {
        toasts.addLast(new Toast(title, text, -1, dur));
    }

    public static void fade(String text) { fadeText = text; fadeStart = System.currentTimeMillis(); }

    public static void tickFog() {
        float d = fogTarget - fog;
        fog += Math.signum(d) * Math.min(Math.abs(d), 0.004f);   // ~12 s to clear
    }
}
