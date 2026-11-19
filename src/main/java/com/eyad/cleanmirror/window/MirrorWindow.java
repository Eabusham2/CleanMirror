package com.eyad.cleanmirror.window;

import com.eyad.cleanmirror.CleanMirror;
import com.eyad.cleanmirror.config.CleanMirrorConfig;
import com.eyad.cleanmirror.render.CleanPass;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import static org.lwjgl.opengl.GL33C.*;

/**
 * The second window.
 *
 * Minecraft is built around one window. The rule here: the clean picture is drawn in the GAME's
 * graphics context like any other render, onto our own canvas. Only at the very end of the frame,
 * after the game has shown its own frame, do we switch to the second window, paste the picture in,
 * show it, and switch straight back - the switch back is in a finally block, because if it were
 * ever skipped every later draw call from the game would go nowhere.
 *
 * The second window's graphics context is created in the game's "share group", so it can read the
 * finished picture directly - and with exactly the settings Minecraft 1.21.11 uses for its own
 * (OpenGL 3.3 core, forward compatible), because mismatched settings can stop sharing from working.
 */
public final class MirrorWindow {

    private static long handle = 0L;
    private static GLCapabilities mainCaps;
    private static GLCapabilities mirrorCaps;
    private static Blitter blitter;
    private static CleanFramebuffer framebuffer;

    private static volatile boolean closeRequested = false;
    private static boolean frameReady = false;
    private static long lastBuildNanos = 0L;
    private static String appliedTitle = null;

    private MirrorWindow() {}

    public static boolean isOpen() { return handle != 0L; }
    public static long handle() { return handle; }
    public static CleanFramebuffer framebuffer() { return framebuffer; }

    // ------------------------------------------------------------------ open / close

    public static void open() {
        if (handle != 0L) return;
        CleanMirrorConfig cfg = CleanMirrorConfig.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        long mainHandle = mc.getWindow().getHandle();
        mainCaps = GL.getCapabilities();

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_CREATION_API, GLFW.GLFW_NATIVE_CONTEXT_API);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, cfg.borderless ? GLFW.GLFW_FALSE : GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_FLOATING, cfg.alwaysOnTop ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);

        String title = titleFromConfig();
        handle = GLFW.glfwCreateWindow(Math.max(320, cfg.windowWidth), Math.max(240, cfg.windowHeight), title, 0L, mainHandle);
        if (handle == 0L) {
            CleanMirror.LOG.error("The graphics driver refused to create the clean window. Your game is unaffected.");
            return;
        }
        appliedTitle = title;
        closeRequested = false;

        // Clicking X on the clean window closes only the clean window. We just note the request here
        // and close on the next tick - destroying a window from inside its own callback is unsafe.
        GLFW.glfwSetWindowCloseCallback(handle, w -> {
            GLFW.glfwSetWindowShouldClose(w, false);
            closeRequested = true;
        });

        long previous = GLFW.glfwGetCurrentContext();
        try {
            GLFW.glfwMakeContextCurrent(handle);
            mirrorCaps = GL.createCapabilities();
            GLFW.glfwSwapInterval(cfg.vsync ? 1 : 0);
            blitter = new Blitter();
            blitter.init();
        } catch (Throwable t) {
            CleanMirror.LOG.error("Setting up the clean window failed; closing it. Your game is unaffected.", t);
            blitter = null;
        } finally {
            GLFW.glfwMakeContextCurrent(previous);
            GL.setCapabilities(mainCaps);
        }

        if (blitter == null) {
            GLFW.glfwDestroyWindow(handle);
            handle = 0L;
            return;
        }

        framebuffer = new CleanFramebuffer();
        frameReady = false;
        lastBuildNanos = 0L;
        CleanMirror.LOG.info("Clean window open ({}x{}, {} mode).", cfg.windowWidth, cfg.windowHeight, cfg.mode);
    }

    public static void close() {
        if (handle == 0L) return;
        long previous = GLFW.glfwGetCurrentContext();
        try {
            GLFW.glfwMakeContextCurrent(handle);
            GL.setCapabilities(mirrorCaps);
            if (blitter != null) blitter.destroy();
        } catch (Throwable t) {
            CleanMirror.LOG.warn("Untidy shutdown of the clean window (harmless).", t);
        } finally {
            GLFW.glfwMakeContextCurrent(previous);
            if (mainCaps != null) GL.setCapabilities(mainCaps);
        }
        if (framebuffer != null) { framebuffer.destroy(); framebuffer = null; }
        CleanPass.release();
        GLFW.glfwDestroyWindow(handle);
        handle = 0L;
        blitter = null;
        appliedTitle = null;
        frameReady = false;
        CleanMirror.LOG.info("Clean window closed.");
    }

    /** Client tick: handle the close button and keep the title in sync with the setting. */
    public static void tick() {
        if (handle == 0L) return;
        if (closeRequested) { closeRequested = false; close(); return; }
        String want = titleFromConfig();
        if (!want.equals(appliedTitle)) {
            GLFW.glfwSetWindowTitle(handle, want);
            appliedTitle = want;
        }
    }

    private static String titleFromConfig() {
        String t = CleanMirrorConfig.get().windowTitle;
        return t == null || t.isBlank() ? "Clean Feed" : t;
    }

    // ------------------------------------------------------------------ frame limiting

    /**
     * Asked BEFORE any clean work is done. On a skipped frame nothing is copied or redrawn at all,
     * so at a 30 FPS cap you pay for the clean feed 30 times a second, not every game frame.
     */
    public static boolean shouldBuildFrame() {
        if (handle == 0L) return false;
        int cap = CleanMirrorConfig.get().fpsCap;
        long now = System.nanoTime();
        if (cap > 0) {
            long interval = 1_000_000_000L / cap;
            // small tolerance so a 60 cap on a 60 Hz game doesn't drop to 30 from timer jitter
            if (now - lastBuildNanos < interval - interval / 8) return false;
        }
        lastBuildNanos = now;
        return true;
    }

    public static void markFrameReady() { frameReady = true; }

    // ------------------------------------------------------------------ present

    /** End of the game's frame: show the clean picture in the second window, if a new one was built. */
    public static void present() {
        if (handle == 0L || framebuffer == null || blitter == null || !frameReady) return;
        frameReady = false;

        int texture = framebuffer.colorGlId();
        if (texture == 0) return;
        int srcW = framebuffer.width(), srcH = framebuffer.height();
        CleanMirrorConfig cfg = CleanMirrorConfig.get();

        // Make sure the GPU has finished drawing the clean picture before the other window reads it.
        long fence = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        glFlush();

        boolean failed = false;
        long previous = GLFW.glfwGetCurrentContext();
        try {
            GLFW.glfwMakeContextCurrent(handle);
            GL.setCapabilities(mirrorCaps);
            if (fence != 0L) glWaitSync(fence, 0, GL_TIMEOUT_IGNORED);

            int[] w = new int[1], h = new int[1];
            GLFW.glfwGetFramebufferSize(handle, w, h);
            if (w[0] > 0 && h[0] > 0) {
                blitter.blit(texture, srcW, srcH, w[0], h[0], cfg.scaling);
                GLFW.glfwSwapBuffers(handle);
            }
        } catch (Throwable t) {
            CleanMirror.LOG.error("The clean window hit an error and has been closed. Your game is fine.", t);
            failed = true;
        } finally {
            GLFW.glfwMakeContextCurrent(previous);
            if (mainCaps != null) GL.setCapabilities(mainCaps);
            if (fence != 0L) glDeleteSync(fence);
        }
        if (failed) close();
    }
}
