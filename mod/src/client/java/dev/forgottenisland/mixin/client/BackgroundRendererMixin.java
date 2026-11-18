package dev.forgottenisland.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.forgottenisland.client.ClientState;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The island's sea fog: thickest out on the water, thinner in the village, gone once the story allows. */
@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {
    @Shadow private static float red;
    @Shadow private static float green;
    @Shadow private static float blue;

    private static float fi$density(Camera camera) {
        if (ClientState.fog <= 0.001f || MinecraftClient.getInstance().player == null) return 0f;
        if (camera.getSubmersionType() != CameraSubmersionType.NONE) return 0f;
        double x = camera.getPos().x, z = camera.getPos().z, y = camera.getPos().y;
        if (y < 58) return 0f;                         // underground: caves have their own darkness
        double r = Math.sqrt(x * x + z * z);
        float d = (float) Math.min(1.0, Math.max(0.35, (r - 120) / 120.0));   // inland 0.35 .. open sea 1.0
        return d * ClientState.fog;
    }

    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void fi$fog(Camera camera, BackgroundRenderer.FogType type, float viewDistance, boolean thick, float tickDelta, CallbackInfo ci) {
        if (type != BackgroundRenderer.FogType.FOG_TERRAIN) return;
        float d = fi$density(camera);
        if (d <= 0f) return;
        float end = Math.min(viewDistance, viewDistance + (24f - viewDistance) * d * 0.9f + (1 - d) * 40f);
        end = Math.max(20f, Math.min(viewDistance, 110f - 86f * d));
        RenderSystem.setShaderFogStart(end * 0.05f);
        RenderSystem.setShaderFogEnd(end);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private static void fi$color(Camera camera, float tickDelta, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfo ci) {
        float d = fi$density(camera) * 0.75f;
        if (d <= 0f) return;
        float day = Math.max(0.15f, (red + green + blue) / 3f);
        red = red + (0.66f * day / 0.7f - red) * d;
        green = green + (0.71f * day / 0.7f - green) * d;
        blue = blue + (0.74f * day / 0.7f - blue) * d;
        RenderSystem.clearColor(red, green, blue, 0.0f);
    }
}
