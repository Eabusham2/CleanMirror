package com.eyad.cleanmirror.test.mixin;

import com.eyad.cleanmirror.test.FakeMods;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws GREEN by injecting into InGameHud.render(), the way hacked clients hook their HUD. */
@Mixin(InGameHud.class)
public abstract class FakeMeteorHudMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void fake$meteorHud(DrawContext ctx, RenderTickCounter tick, CallbackInfo ci) {
        ctx.fill(100, 0, 140, 30, FakeMods.GREEN);
    }
}
