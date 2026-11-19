package com.eyad.cleanmirror.mixin;

import com.eyad.cleanmirror.CleanMirror;
import com.eyad.cleanmirror.config.CleanMirrorConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Toasts on the clean feed: Minecraft's own (advancements, recipes, tutorial, "now playing") always
 * show when toasts are on; toasts a mod made only show if you allow modded toasts. A toast counts as
 * Minecraft's own when its class lives in Minecraft's code.
 */
@Mixin(targets = "net.minecraft.client.toast.ToastManager$Entry")
public abstract class ToastEntryMixin {

    @WrapOperation(method = "draw",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/toast/Toast;draw(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;J)V"),
            require = 0)
    private void cleanmirror$filterToast(Toast toast, DrawContext context, TextRenderer text, long time, Operation<Void> original) {
        if (CleanMirror.renderingClean
                && !CleanMirrorConfig.get().showModdedToasts
                && !toast.getClass().getName().startsWith("net.minecraft.")) {
            return;
        }
        original.call(toast, context, text, time);
    }
}
