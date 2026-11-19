package com.eyad.cleanmirror;

import com.eyad.cleanmirror.config.CleanMirrorConfig;
import com.eyad.cleanmirror.render.HudFilter;
import com.eyad.cleanmirror.window.MirrorWindow;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TreeSet;

public class CleanMirror implements ClientModInitializer {

    public static final String MOD_ID = "cleanmirror";
    public static final Logger LOG = LoggerFactory.getLogger("CleanMirror");

    /**
     * True only while the clean feed is being built. Every filter in this mod checks it, and every
     * place that sets it puts it back in a finally block. If it ever got stuck on, your own window
     * would start losing things too.
     */
    public static volatile boolean renderingClean = false;

    private static KeyBinding toggleKey;
    private static KeyBinding dumpKey;
    private static boolean openedOnLaunch = false;

    @Override
    public void onInitializeClient() {
        CleanMirrorConfig.get();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cleanmirror.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F6, category));
        dumpKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cleanmirror.dump", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F7, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!openedOnLaunch) {
                openedOnLaunch = true;
                if (CleanMirrorConfig.get().openOnLaunch) MirrorWindow.open();
            }
            while (toggleKey.wasPressed()) {
                if (MirrorWindow.isOpen()) MirrorWindow.close();
                else MirrorWindow.open();
            }
            while (dumpKey.wasPressed()) dumpDiscovered(client);
            MirrorWindow.tick();
        });

        LOG.info("CleanMirror loaded. F6 = open/close the clean window, F7 = list HUD pieces in chat.");
    }

    /** F7: print every HUD piece the game has drawn this session, and whether the feed shows it. */
    private static void dumpDiscovered(MinecraftClient client) {
        if (client.player == null) return;
        var all = new TreeSet<>(HudFilter.discovered());
        if (all.isEmpty()) {
            client.player.sendMessage(Text.literal("[CleanMirror] No HUD pieces seen yet - play for a moment, then press F7 again.")
                    .formatted(Formatting.YELLOW), false);
            return;
        }
        client.player.sendMessage(Text.literal("[CleanMirror] HUD pieces seen this session:").formatted(Formatting.AQUA), false);
        for (String id : all) {
            boolean shown = HudFilter.wouldAllow(id);
            client.player.sendMessage(
                    Text.literal(shown ? "  SHOWN  " : "  HIDDEN ").formatted(shown ? Formatting.GREEN : Formatting.GRAY)
                            .append(Text.literal(id).formatted(Formatting.WHITE)),
                    false);
        }
        LOG.info("HUD pieces seen: {}", all);
    }
}
