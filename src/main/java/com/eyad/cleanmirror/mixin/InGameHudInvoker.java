package com.eyad.cleanmirror.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Direct access to each vanilla HUD piece, so the clean feed can call them one by one instead of
 * calling InGameHud.render() - which is exactly where clients like Meteor hook their own HUD in.
 */
@Mixin(InGameHud.class)
public interface InGameHudInvoker {
    @Invoker("renderMiscOverlays") void cleanmirror$miscOverlays(DrawContext c, RenderTickCounter t);
    @Invoker("renderCrosshair") void cleanmirror$crosshair(DrawContext c, RenderTickCounter t);
    @Invoker("renderMainHud") void cleanmirror$mainHud(DrawContext c, RenderTickCounter t);
    @Invoker("renderStatusEffectOverlay") void cleanmirror$statusEffects(DrawContext c, RenderTickCounter t);
    @Invoker("renderBossBarHud") void cleanmirror$bossBar(DrawContext c, RenderTickCounter t);
    @Invoker("renderSleepOverlay") void cleanmirror$sleep(DrawContext c, RenderTickCounter t);
    @Invoker("renderDemoTimer") void cleanmirror$demoTimer(DrawContext c, RenderTickCounter t);
    @Invoker("renderScoreboardSidebar") void cleanmirror$scoreboard(DrawContext c, RenderTickCounter t);
    @Invoker("renderOverlayMessage") void cleanmirror$overlayMessage(DrawContext c, RenderTickCounter t);
    @Invoker("renderTitleAndSubtitle") void cleanmirror$title(DrawContext c, RenderTickCounter t);
    @Invoker("renderChat") void cleanmirror$chat(DrawContext c, RenderTickCounter t);
    @Invoker("renderPlayerList") void cleanmirror$playerList(DrawContext c, RenderTickCounter t);
    @Invoker("renderSubtitlesHud") void cleanmirror$subtitles(DrawContext c, boolean defer);
}
