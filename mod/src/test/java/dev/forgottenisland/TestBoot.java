package dev.forgottenisland;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;

/** Boots Minecraft + this mod once per test JVM, in the same order the game does. */
public final class TestBoot {
    private static boolean done;
    private TestBoot() {}
    public static synchronized void boot() {
        if (done) return;
        done = true;
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
        try {
            FabricLoader.getInstance().invokeEntrypoints("main", ModInitializer.class, ModInitializer::onInitialize);
        } catch (RuntimeException e) {
            Throwable t = e; while (t.getCause() != null) t = t.getCause();
            System.err.println("[TestBoot] entrypoint failure root cause: " + t);
            throw e;
        }
    }
}
