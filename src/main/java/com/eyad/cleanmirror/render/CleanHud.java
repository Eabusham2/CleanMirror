package com.eyad.cleanmirror.render;

import com.eyad.cleanmirror.CleanMirror;
import com.eyad.cleanmirror.config.CleanMirrorConfig;
import com.eyad.cleanmirror.mixin.InGameHudInvoker;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.impl.client.rendering.hud.HudElementRegistryImpl;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

/**
 * The clean HUD: vanilla's InGameHud.render(), rebuilt step for step from the 1.21.11 source.
 *
 * Why rebuild it instead of calling it? Because InGameHud.render() is where clients like Meteor
 * inject their whole HUD. If the clean feed called it, their HUD would come along for the ride.
 * Rebuilding the same steps means anything hooked onto render() itself simply never runs here.
 *
 * Each top-level piece goes through Fabric's named root layer - the same thing Fabric does for your
 * own window - so mods attached to a piece (like Simple Voice Chat's mic icon, which attaches to
 * "status_effects") appear on the clean feed exactly when you whitelist them, and nowhere else.
 */
public final class CleanHud {

    private CleanHud() {}

    private static boolean warnedRegistry = false;

    public static void render(MinecraftClient mc, DrawContext ctx, RenderTickCounter tick) {
        InGameHudInvoker hud = (InGameHudInvoker) mc.inGameHud;
        boolean hidden = mc.options.hudHidden;

        if (mc.currentScreen instanceof LevelLoadingScreen) return;

        if (!hidden) {
            root(VanillaHudElements.MISC_OVERLAYS, ctx, tick, hud::cleanmirror$miscOverlays);
            root(VanillaHudElements.CROSSHAIR, ctx, tick, hud::cleanmirror$crosshair);
            ctx.createNewRootLayer();
            // renderMainHud already has Fabric's per-piece routing compiled into it
            // (hotbar, health, food, armour, air, mount, XP bar/level, held-item name, spectator).
            hud.cleanmirror$mainHud(ctx, tick);
            root(VanillaHudElements.STATUS_EFFECTS, ctx, tick, hud::cleanmirror$statusEffects);
            root(VanillaHudElements.BOSS_BAR, ctx, tick, hud::cleanmirror$bossBar);
        }

        root(VanillaHudElements.SLEEP, ctx, tick, hud::cleanmirror$sleep);

        if (!hidden) {
            root(VanillaHudElements.DEMO_TIMER, ctx, tick, hud::cleanmirror$demoTimer);
            root(VanillaHudElements.SCOREBOARD, ctx, tick, hud::cleanmirror$scoreboard);
            root(VanillaHudElements.OVERLAY_MESSAGE, ctx, tick, hud::cleanmirror$overlayMessage);
            root(VanillaHudElements.TITLE_AND_SUBTITLE, ctx, tick, hud::cleanmirror$title);
            root(VanillaHudElements.CHAT, ctx, tick, hud::cleanmirror$chat);
            root(VanillaHudElements.PLAYER_LIST, ctx, tick, hud::cleanmirror$playerList);
            boolean defer = mc.currentScreen == null || mc.currentScreen.deferSubtitles();
            root(VanillaHudElements.SUBTITLES, ctx, tick, (c, t) -> hud.cleanmirror$subtitles(c, defer));
        } else if (mc.currentScreen != null && mc.currentScreen.deferSubtitles()) {
            root(VanillaHudElements.SUBTITLES, ctx, tick, (c, t) -> hud.cleanmirror$subtitles(c, true));
        }

        // Fabric's old-style callback carries no names, so it's all-or-nothing.
        if (!CleanMirrorConfig.get().hideUnnamedDrawing) {
            HudRenderCallback.EVENT.invoker().onHudRender(ctx, tick);
        }
    }

    /** Draw one vanilla piece plus any mod pieces attached to it, through the whitelist. */
    private static void root(Identifier id, DrawContext ctx, RenderTickCounter tick, HudElement vanilla) {
        try {
            HudElementRegistryImpl.RootLayer layer = HudElementRegistryImpl.getRoot(id);
            if (layer != null) {
                layer.render(ctx, tick, vanilla);
                return;
            }
        } catch (Throwable t) {
            if (!warnedRegistry) {
                warnedRegistry = true;
                CleanMirror.LOG.warn("Fabric's HUD registry changed shape; drawing vanilla HUD pieces without mod layers.", t);
            }
        }
        // Fallback: vanilla piece only, still subject to the vanilla on/off switches.
        HudFilter.note(id);
        if (HudFilter.allow(id)) vanilla.render(ctx, tick);
    }
}
