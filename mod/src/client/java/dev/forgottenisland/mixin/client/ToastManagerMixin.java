package dev.forgottenisland.mixin.client;

import net.minecraft.client.toast.AdvancementToast;
import net.minecraft.client.toast.RecipeToast;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps vanilla "recipe unlocked"/"advancement" pop-ups out of the story (they break immersion at key moments). */
@Mixin(ToastManager.class)
public abstract class ToastManagerMixin {
    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void fi$quiet(Toast toast, CallbackInfo ci) {
        if (toast instanceof RecipeToast || toast instanceof AdvancementToast) ci.cancel();
    }
}
