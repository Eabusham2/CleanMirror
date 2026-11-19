package com.eyad.cleanmirror.test;

import com.eyad.cleanmirror.config.CleanMirrorConfig;
import com.eyad.cleanmirror.config.CleanMirrorConfig.MirrorMode;
import com.eyad.cleanmirror.config.CleanMirrorConfig.NametagMode;
import com.eyad.cleanmirror.render.HudFilter;
import com.eyad.cleanmirror.window.MirrorWindow;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * Drives the in-world scenario on a normal game launch (no test-framework lockstep), when run with
 * -Dcleanmirror.autopilot. Captures, per scene:
 *   NN_name_clean.png   the clean canvas read back from the GPU
 *   NN_name_screen.png  the whole virtual display: your window on top, the clean window below
 */
public final class Autopilot {

    private static final Path OUT = Path.of("cleanmirror-test-out");
    private static int worldTicks = -1;

    private Autopilot() {}

    public static void install() {
        ClientTickEvents.END_CLIENT_TICK.register(Autopilot::tick);
    }

    private static int menuTicks = 0;
    private static boolean createRequested = false, createPressed = false;

    private static void tick(MinecraftClient c) {
        if (c.world == null || c.player == null) {
            createWorld(c);
            return;
        }
        worldTicks++;
        CleanMirrorConfig cfg = CleanMirrorConfig.get();

        switch (worldTicks) {
            case 0 -> {
                try { Files.createDirectories(OUT); } catch (IOException e) { throw new UncheckedIOException(e); }
                cfg.fpsCap = 0;
                cfg.mode = MirrorMode.REUSE;
                cfg.nametags = NametagMode.VANILLA;
                cfg.allowedMods = new LinkedHashSet<>(List.of("voicechat"));
                cfg.windowWidth = 854;
                cfg.windowHeight = 480;
                log("in world");
            }
            case 60 -> {
                double x = c.player.getX(), y = c.player.getY(), z = c.player.getZ();
                command(c, "tp @a " + f(x) + " " + f(y) + " " + f(z) + " 0 0");
                command(c, "summon husk " + f(x) + " " + f(y) + " " + f(z + 3.5)
                        + " {CustomName:\"Bob\",CustomNameVisible:1b,NoAI:1b,PersistenceRequired:1b,Silent:1b,Rotation:[180f,0f]}");
                command(c, "time set noon");
                command(c, "gamerule doDaylightCycle false");
                GLFW.glfwSetWindowPos(c.getWindow().getHandle(), 0, 0);
                MirrorWindow.open();
                GLFW.glfwSetWindowPos(MirrorWindow.handle(), 0, 560);
            }
            case 110 -> {
                SystemToast.add(c.getToastManager(), SystemToast.Type.WORLD_BACKUP,
                        Text.literal("Vanilla toast"), Text.literal("should show"));
                c.getToastManager().add(new FakeModToast());
            }
            case 125 -> c.world.getEntitiesByClass(net.minecraft.entity.mob.HuskEntity.class,
                    c.player.getBoundingBox().expand(8), z -> true).forEach(z -> log("mob:"
                    + " customName=" + z.getCustomName()
                    + " nameVisible=" + z.isCustomNameVisible()
                    + " shouldRenderName=" + z.shouldRenderName()
                    + " targeted=" + (c.getEntityRenderDispatcher().targetedEntity == z)
                    + " clientTargeted=" + (c.targetedEntity == z)
                    + " cameraKnown=" + (c.getEntityRenderDispatcher().camera != null)
                    + " invisible=" + z.isInvisibleTo(c.player)
                    + " team=" + z.getScoreboardTeam()
                    + " passengers=" + z.hasPassengers()
                    + " hudEnabled=" + MinecraftClient.isHudEnabled()
                    + " isCamera=" + (z == c.getCameraEntity())));
            case 130 -> capture(c, "02_reuse");
            case 140 -> cfg.mode = MirrorMode.REDRAW;
            case 160 -> capture(c, "03_redraw");
            case 170 -> FakeMods.freecam = true;
            case 190 -> capture(c, "04_redraw_freecam");
            case 195 -> FakeMods.freecamShiftX = 20.0;
            case 205 -> capture(c, "04b_redraw_freecam_moved");
            case 207 -> { FakeMods.freecam = false; FakeMods.freecamShiftX = 0.0; }
            case 210 -> {
                c.debugHudEntryList.setF3Enabled(true);
                c.getDebugHud().toggleRenderingChart();
            }
            case 250 -> capture(c, "05_f3_pie");
            case 260 -> {
                c.getDebugHud().toggleRenderingChart();
                c.debugHudEntryList.setF3Enabled(false);
                c.setScreen(new GameMenuScreen(true));
            }
            case 280 -> capture(c, "06_pause");
            case 290 -> c.setScreen(null);
            case 300 -> cfg.nametags = NametagMode.AS_IS;
            case 320 -> capture(c, "07_redraw_tags_asis");
            case 330 -> {
                cfg.nametags = NametagMode.VANILLA;
                cfg.mode = MirrorMode.REUSE;
                log("HUD pieces seen: " + new TreeSet<>(HudFilter.discovered()));
            }
            case 360 -> {
                log("done");
                MirrorWindow.close();
                c.scheduleStop();
            }
            default -> { }
        }
    }

    /** From the title screen: open Create World, pick Superflat, press create. */
    private static void createWorld(MinecraftClient c) {
        if (c.getOverlay() != null) return;   // still on the Mojang loading screen
        if (c.currentScreen instanceof net.minecraft.client.gui.screen.TitleScreen && menuTicks == 10) {
            CleanMirrorConfig.get().windowWidth = 854;
            CleanMirrorConfig.get().windowHeight = 480;
            try { Files.createDirectories(OUT); } catch (IOException e) { throw new UncheckedIOException(e); }
            CleanMirrorConfig.get().fpsCap = 0;
            GLFW.glfwSetWindowPos(c.getWindow().getHandle(), 0, 0);
            MirrorWindow.open();
            GLFW.glfwSetWindowPos(MirrorWindow.handle(), 0, 560);
        }
        if (c.currentScreen instanceof net.minecraft.client.gui.screen.TitleScreen && menuTicks == 40) capture(c, "01_title");
        if (c.currentScreen instanceof net.minecraft.client.gui.screen.TitleScreen && !createRequested && ++menuTicks > 50) {
            createRequested = true;
            log("opening Create World");
            net.minecraft.client.gui.screen.world.CreateWorldScreen.show(c, () -> c.setScreen(new net.minecraft.client.gui.screen.TitleScreen()));
        } else if (c.currentScreen instanceof net.minecraft.client.gui.screen.world.CreateWorldScreen screen && !createPressed) {
            createPressed = true;
            var creator = screen.getWorldCreator();
            creator.setWorldName("Autopilot");
            creator.getNormalWorldTypes().stream()
                    .filter(t -> t.preset().matchesKey(net.minecraft.world.gen.WorldPresets.FLAT))
                    .findFirst().ifPresent(creator::setWorldType);
            log("creating superflat world");
            ((com.eyad.cleanmirror.test.mixin.CreateWorldScreenInvoker) screen).autopilot$createLevel();
        }
    }

    private static void command(MinecraftClient c, String cmd) {
        IntegratedServer server = c.getServer();
        if (server == null) return;
        server.execute(() -> server.getCommandManager().parseAndExecute(server.getCommandSource(), cmd));
    }

    private static void capture(MinecraftClient c, String name) {
        log("capture " + name);
        if (MirrorWindow.framebuffer() != null && MirrorWindow.framebuffer().get() != null) {
            ScreenshotRecorder.takeScreenshot(MirrorWindow.framebuffer().get(), img -> {
                try (img) { img.writeTo(OUT.resolve(name + "_clean.png")); }
                catch (IOException e) { throw new UncheckedIOException(e); }
            });
        }
        try {
            new ProcessBuilder("import", "-window", "root", OUT.resolve(name + "_screen.png").toString())
                    .inheritIO().start().waitFor();
        } catch (Exception e) {
            log("screen capture failed: " + e);
        }
    }

    private static String f(double d) { return String.format(Locale.ROOT, "%.2f", d); }

    private static void log(String s) { System.out.println("[CleanMirrorAutopilot] " + s); }
}
