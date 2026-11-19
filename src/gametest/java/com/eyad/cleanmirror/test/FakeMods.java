package com.eyad.cleanmirror.test;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.util.Identifier;

/**
 * Test-only stand-ins for the kinds of mod drawing the clean feed must keep out.
 * Each draws a solid block of one colour in the top-left, so a screenshot shows instantly whether it leaked.
 *
 *   RED    (x 0-40)    a mod HUD layer registered the modern way, with a name  -> must NOT be on the feed
 *   BLUE   (x 50-90)   Fabric's old nameless HUD callback                       -> must NOT be on the feed
 *   GREEN  (x 100-140) a "Meteor-style" injection into InGameHud.render()       -> must NOT be on the feed
 *   YELLOW (x 150-190) a named layer from a mod we then WHITELIST ("voicechat")  -> MUST be on the feed
 */
public class FakeMods implements ClientModInitializer {

    public static final int RED = 0xFFFF0000, BLUE = 0xFF0000FF, GREEN = 0xFF00FF00, YELLOW = 0xFFFFFF00;
    public static volatile boolean freecam = false;
    public static volatile double freecamShiftX = 0.0;

    @Override
    public void onInitializeClient() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Identifier.of("fakehud", "overlay"),
                (ctx, tick) -> ctx.fill(0, 0, 40, 30, RED));
        HudElementRegistry.attachElementBefore(VanillaHudElements.STATUS_EFFECTS, Identifier.of("voicechat", "hud"),
                (ctx, tick) -> ctx.fill(150, 0, 190, 30, YELLOW));
        HudRenderCallback.EVENT.register((ctx, tick) -> ctx.fill(50, 0, 90, 30, BLUE));
        if (System.getProperty("cleanmirror.autopilot") != null) Autopilot.install();
    }
}
