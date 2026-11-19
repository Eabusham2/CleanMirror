package com.eyad.cleanmirror.mixin;

import com.eyad.cleanmirror.CleanMirror;
import com.eyad.cleanmirror.config.CleanMirrorConfig;
import com.eyad.cleanmirror.render.CleanPass;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Where the clean feed is built, read straight off the 1.21.11 frame:
 *
 *     updateCamera()          camera placed (freecam mods act here)
 *     renderWorld()           world, entities, nametags, hand
 *     post shaders / outlines
 *  >> CleanPass.run() <<      main framebuffer holds the finished world, no GUI yet
 *     guiState.clear()        ...then your own HUD/menus get built and drawn
 *
 * Hooking right before guiState.clear() means the world picture is complete (including spectator
 * shaders and glowing outlines) and not a single pixel of GUI has touched it yet.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;clear()V"))
    private void cleanmirror$buildCleanFeed(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        if (CleanMirror.renderingClean) return;   // never recurse
        CleanPass.run((GameRenderer) (Object) this, tickCounter, tick);
    }

    /** REDRAW: the clean feed's own FOV, scaled so sprinting / speed / underwater effects still apply. */
    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float cleanmirror$fov(float fov, Camera camera, float tickProgress, boolean changingFov) {
        if (!CleanMirror.renderingClean || !changingFov) return fov;
        double override = CleanMirrorConfig.get().fovOverride;
        if (override <= 0) return fov;
        double base = MinecraftClient.getInstance().options.getFov().getValue();
        return base > 0 ? (float) (fov * (override / base)) : fov;
    }

    /** REDRAW: keep your held item off the clean feed. */
    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void cleanmirror$hand(float tickProgress, boolean sleeping, Matrix4f positionMatrix, CallbackInfo ci) {
        if (CleanMirror.renderingClean && CleanMirrorConfig.get().hideHand) ci.cancel();
    }
}
