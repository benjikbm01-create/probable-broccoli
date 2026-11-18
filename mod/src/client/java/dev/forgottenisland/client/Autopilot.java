package dev.forgottenisland.client;

import net.minecraft.client.MinecraftClient;

/** Scripted client driver for end-to-end tests (enabled with -Dfi.autopilot=&lt;script&gt;). Implemented later. */
public final class Autopilot {
    private Autopilot() {}
    public static void init() {}
    public static void tick(MinecraftClient mc) {}
}
