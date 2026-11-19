package com.eyad.cleanmirror.render;

import com.eyad.cleanmirror.config.CleanMirrorConfig;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.ReconfiguringScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.network.ClientCommonNetworkHandler;

/**
 * Which open menus appear on the clean feed.
 *
 * Minecraft's own screens (pause, options, inventory, chests, server prompts) show. A mod's own
 * screen doesn't unless you allow it - nobody watching needs to see you open a mod's config.
 *
 * "Minecraft's own" = the screen's class lives in Minecraft's code. In a real install those classes
 * are named like net.minecraft.class_419, so we only ever test the package, never a readable class
 * name - and connection screens are recognised by their actual type, not their name.
 */
public final class ScreenFilter {

    private ScreenFilter() {}

    public static boolean allow(Screen screen) {
        CleanMirrorConfig cfg = CleanMirrorConfig.get();
        if (!cfg.showScreens) return false;

        boolean vanilla = screen.getClass().getName().startsWith("net.minecraft.");
        if (!vanilla) return cfg.showModdedScreens;

        return cfg.showConnectionScreens || !isConnectionScreen(screen);
    }

    /** "Connecting...", world loading, disconnect messages, server resource-pack prompts. */
    private static boolean isConnectionScreen(Screen screen) {
        if (screen instanceof ConnectScreen
                || screen instanceof DisconnectedScreen
                || screen instanceof LevelLoadingScreen
                || screen instanceof ProgressScreen
                || screen instanceof ReconfiguringScreen
                || screen instanceof MessageScreen) {
            return true;
        }
        // The server resource-pack prompt is a private inner class of ClientCommonNetworkHandler.
        return screen instanceof ConfirmScreen && screen.getClass().getEnclosingClass() == ClientCommonNetworkHandler.class;
    }
}
