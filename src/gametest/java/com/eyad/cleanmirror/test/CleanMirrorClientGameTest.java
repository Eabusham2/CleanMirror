package com.eyad.cleanmirror.test;

import com.eyad.cleanmirror.config.CleanMirrorConfig;
import com.eyad.cleanmirror.config.CleanMirrorConfig.MirrorMode;
import com.eyad.cleanmirror.config.CleanMirrorConfig.NametagMode;
import com.eyad.cleanmirror.window.MirrorWindow;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.WorldPresets;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Runs the real game with CleanMirror plus the fake mods, and captures three things per scene:
 *   *_main.png    - your window (Fabric's screenshot of the main framebuffer)
 *   *_clean.png   - the clean feed's canvas, read straight back from the GPU
 *   *_screen.png  - the whole virtual display, i.e. what the second window actually shows
 */
public class CleanMirrorClientGameTest implements FabricClientGameTest {

    private static final Path OUT = Path.of(System.getProperty("cleanmirror.testOut", "cleanmirror-test-out"));

    @Override
    public void runTest(ClientGameTestContext ctx) {
        try { Files.createDirectories(OUT); } catch (IOException e) { throw new UncheckedIOException(e); }

        CleanMirrorConfig cfg = CleanMirrorConfig.get();
        cfg.fpsCap = 0;
        cfg.mode = MirrorMode.REUSE;
        cfg.nametags = NametagMode.VANILLA;
        cfg.allowedMods = new LinkedHashSet<>(List.of("voicechat"));
        cfg.windowWidth = 854;
        cfg.windowHeight = 480;

        ctx.runOnClient(c -> {
            GLFW.glfwSetWindowPos(c.getWindow().getHandle(), 0, 0);
            MirrorWindow.open();
            GLFW.glfwSetWindowPos(MirrorWindow.handle(), 0, 560);
        });
        ctx.waitTicks(20);
        capture(ctx, "01_title");

        // The test machine has 2 CPU cores and no GPU: close the clean window while the world
        // generates (also exercises close + reopen) and keep the view distance small.
        ctx.runOnClient(c -> {
            MirrorWindow.close();
            c.options.getViewDistance().setValue(2);
            c.options.getSimulationDistance().setValue(5);
            c.options.getMaxFps().setValue(10);   // leave CPU for the world generator
        });

        try (TestSingleplayerContext sp = ctx.worldBuilder()
                .adjustSettings(creator -> creator.getNormalWorldTypes().stream()
                        .filter(t -> t.preset().matchesKey(WorldPresets.FLAT))
                        .findFirst().ifPresent(creator::setWorldType))
                .create()) {
            sp.getClientWorld().waitForChunksRender();
            ctx.runOnClient(c -> {
                c.options.getMaxFps().setValue(260);
                MirrorWindow.open();
                GLFW.glfwSetWindowPos(MirrorWindow.handle(), 0, 560);
            });

            Vec3d p = ctx.computeOnClient(c -> c.player.getEntityPos());
            String x = fmt(p.x), y = fmt(p.y), z = fmt(p.z + 3.5);
            sp.getServer().runCommand("tp @a " + fmt(p.x) + " " + fmt(p.y) + " " + fmt(p.z) + " 0 0");
            sp.getServer().runCommand("summon zombie " + x + " " + y + " " + z
                    + " {CustomName:\"Bob\",CustomNameVisible:1b,NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]}");
            sp.getServer().runCommand("time set noon");
            ctx.waitTicks(40);

            ctx.runOnClient(c -> {
                SystemToast.add(c.getToastManager(), SystemToast.Type.WORLD_BACKUP, Text.literal("Vanilla toast"), Text.literal("should show"));
                c.getToastManager().add(new FakeModToast());
            });
            ctx.waitTicks(10);
            capture(ctx, "02_reuse");

            cfg.mode = MirrorMode.REDRAW;
            ctx.waitTicks(5);
            capture(ctx, "03_redraw");

            FakeMods.freecam = true;
            ctx.waitTicks(5);
            capture(ctx, "04_redraw_freecam");
            FakeMods.freecam = false;

            ctx.runOnClient(c -> {
                c.debugHudEntryList.setF3Enabled(true);
                c.getDebugHud().toggleRenderingChart();
            });
            ctx.waitTicks(20);
            capture(ctx, "05_f3_pie");
            ctx.runOnClient(c -> {
                c.getDebugHud().toggleRenderingChart();
                c.debugHudEntryList.setF3Enabled(false);
            });

            ctx.setScreen(() -> new GameMenuScreen(true));
            ctx.waitTicks(10);
            capture(ctx, "06_pause");
            ctx.setScreen(() -> null);
            ctx.waitTicks(5);

            cfg.nametags = NametagMode.AS_IS;
            ctx.waitTicks(5);
            capture(ctx, "07_redraw_tags_asis");
            cfg.nametags = NametagMode.VANILLA;
        }

        ctx.runOnClient(c -> MirrorWindow.close());
    }

    private static String fmt(double d) { return String.format(java.util.Locale.ROOT, "%.2f", d); }

    private static void capture(ClientGameTestContext ctx, String name) {
        ctx.takeScreenshot(TestScreenshotOptions.of(name + "_main"));
        ctx.runOnClient(c -> ScreenshotRecorder.takeScreenshot(MirrorWindow.framebuffer().get(), img -> {
            try (img) { img.writeTo(OUT.resolve(name + "_clean.png")); }
            catch (IOException e) { throw new UncheckedIOException(e); }
        }));
        ctx.waitTicks(4);
        try {
            new ProcessBuilder("import", "-window", "root", OUT.resolve(name + "_screen.png").toString())
                    .inheritIO().start().waitFor();
        } catch (Exception e) {
            System.err.println("[CleanMirrorTest] screen capture failed: " + e);
        }
    }
}
