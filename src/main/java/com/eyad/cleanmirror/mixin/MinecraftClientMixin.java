package com.eyad.cleanmirror.mixin;

import com.eyad.cleanmirror.window.MirrorWindow;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Show the clean frame at the very end of the game's frame (after the game's own window has been
 * shown - the only safe moment to touch a second window), and tear the window down on quit.
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void cleanmirror$present(boolean tick, CallbackInfo ci) {
        MirrorWindow.present();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void cleanmirror$shutdown(CallbackInfo ci) {
        MirrorWindow.close();
    }
}
