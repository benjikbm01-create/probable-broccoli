package dev.forgottenisland.client;

import dev.forgottenisland.client.render.NpcRenderer;
import dev.forgottenisland.client.render.WardenRenderer;
import dev.forgottenisland.game.ModBlocks;
import dev.forgottenisland.npc.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ForgottenIslandClient implements ClientModInitializer {
    public static KeyBinding JOURNAL, HINT, HUD;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.NPC, NpcRenderer::new);
        EntityRendererRegistry.register(ModEntities.WARDEN, WardenRenderer::new);
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.MIRROR, RenderLayer.getCutout());
        ClientNet.register();
        String cat = "key.categories.forgotten_island";
        JOURNAL = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.forgotten_island.journal", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, cat));
        HINT = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.forgotten_island.hint", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, cat));
        HUD = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.forgotten_island.hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, cat));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            ClientState.tickFog();
            while (JOURNAL.wasPressed()) ClientNet.action("journal", 0);
            while (HINT.wasPressed()) ClientNet.action("hint", 0);
            while (HUD.wasPressed()) ClientState.hudVisible = !ClientState.hudVisible;
            Autopilot.tick(mc);
        });
        HudRenderCallback.EVENT.register((ctx, counter) -> HudOverlay.render(ctx, counter.getTickDelta(true)));
        ClientPlayConnectionEvents.DISCONNECT.register((h, mc) -> { ClientState.objTitle = ""; ClientState.fog = 0; ClientState.fogTarget = 0; ClientState.toasts.clear(); });
        Autopilot.init();
    }
}
