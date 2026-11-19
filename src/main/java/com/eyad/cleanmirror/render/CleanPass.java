package com.eyad.cleanmirror.render;

import com.eyad.cleanmirror.CleanMirror;
import com.eyad.cleanmirror.config.CleanMirrorConfig;
import com.eyad.cleanmirror.config.CleanMirrorConfig.MirrorMode;
import com.eyad.cleanmirror.mixin.GameRendererAccessor;
import com.eyad.cleanmirror.mixin.MinecraftClientAccessor;
import com.eyad.cleanmirror.window.CleanFramebuffer;
import com.eyad.cleanmirror.window.MirrorWindow;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.BannerResultGuiElementRenderer;
import net.minecraft.client.gui.render.BookModelGuiElementRenderer;
import net.minecraft.client.gui.render.EntityGuiElementRenderer;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.PlayerSkinGuiElementRenderer;
import net.minecraft.client.gui.render.ProfilerChartGuiElementRenderer;
import net.minecraft.client.gui.render.SignGuiElementRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screen.DebugOptionsScreen;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;

import java.util.List;

/**
 * Builds one frame of the clean feed onto its own canvas. Runs once per (non-skipped) frame, in the
 * middle of Minecraft's own frame, right after your world is finished and before your GUI starts.
 *
 * Everything that gets swapped (framebuffer, camera, camera entity, crosshair target) is put back in
 * a finally block. Everything is wrapped in its own profiler section, "cleanmirror", so the F3 pie
 * chart's vanilla slices - block entities included - read exactly as they would without this mod,
 * and the clean feed's cost shows up as its own labelled slice instead of hiding inside them.
 */
public final class CleanPass {

    private CleanPass() {}

    private static GuiRenderState cleanGuiState;
    private static GuiRenderer cleanGuiRenderer;
    private static boolean loggedWorld = false, loggedHud = false, loggedRenderer = false;

    public static void run(GameRenderer gr, RenderTickCounter tick, boolean tickWorld) {
        if (!MirrorWindow.isOpen() || !MirrorWindow.shouldBuildFrame()) return;
        CleanFramebuffer target = MirrorWindow.framebuffer();
        if (target == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        Framebuffer main = mc.getFramebuffer();
        target.matchSizeOf(main);

        Profiler profiler = Profilers.get();
        profiler.push("cleanmirror");
        try {
            boolean worldThisFrame = mc.isFinishedLoading() && tickWorld && mc.world != null;
            CleanMirrorConfig cfg = CleanMirrorConfig.get();

            if (cfg.mode == MirrorMode.REDRAW && worldThisFrame) {
                profiler.push("world");
                redrawWorld(mc, gr, tick, target, main);
                profiler.pop();
            } else {
                target.copyColorFrom(main);
            }
            target.clearDepth();

            profiler.push("gui");
            drawGui(mc, gr, tick, tickWorld, target, main);
            profiler.pop();

            MirrorWindow.markFrameReady();
        } finally {
            profiler.pop();
        }
    }

    // ------------------------------------------------------------------ REDRAW: world again, clean

    private static void redrawWorld(MinecraftClient mc, GameRenderer gr, RenderTickCounter tick,
                                    CleanFramebuffer target, Framebuffer main) {
        CleanMirrorConfig cfg = CleanMirrorConfig.get();
        MinecraftClientAccessor mca = (MinecraftClientAccessor) mc;
        float tickProgress = tick.getTickProgress(true);

        // renderWorld() recomputes what you're aiming at. Save it, so drawing the clean view can never
        // change what your clicks hit this frame.
        HitResult savedHit = mc.crosshairTarget;
        Entity savedTargeted = mc.targetedEntity;
        Entity savedCameraEntity = mc.getCameraEntity();
        CameraControl.Snapshot camSnap = null;
        CleanTerrain.Saved terrain = null;

        CleanMirror.renderingClean = true;
        try {
            mca.cleanmirror$setFramebuffer(target.get());
            target.clearAll();

            if (cfg.freecamProof) {
                Entity legit = CameraControl.legitCameraEntity(mc);
                if (legit != null) {
                    mca.cleanmirror$setCameraEntityRaw(legit);
                    camSnap = CameraControl.poseFrom(mc, gr.getCamera(), legit, tickProgress);
                }
            }

            // The clean view gets its own visible-chunk graph (see CleanTerrain).
            terrain = CleanTerrain.enter(mc.worldRenderer, gr.getCamera().getCameraPos());

            // Terrain is positioned on the GPU using a camera position the game uploads once per frame,
            // before the world is drawn. Upload the clean camera's position for this render...
            uploadGlobals(mc, gr, tick);

            gr.renderWorld(tick);
            mc.worldRenderer.drawEntityOutlinesFramebuffer();

            GameRendererAccessor gra = (GameRendererAccessor) gr;
            Identifier postId = gra.cleanmirror$postId();
            if (postId != null && gra.cleanmirror$postEnabled()) {
                PostEffectProcessor post = mc.getShaderLoader().loadPostEffect(postId, DefaultFramebufferSet.MAIN_ONLY);
                if (post != null) post.render(target.get(), gra.cleanmirror$pool());
            }
        } catch (Throwable t) {
            if (!loggedWorld) {
                loggedWorld = true;
                CleanMirror.LOG.error("REDRAW failed while drawing the world. The clean feed will show "
                        + "a plain copy instead. Switching Mode to REUSE avoids this code entirely.", t);
            }
            try { target.copyColorFrom(main); } catch (Throwable ignored) { }
        } finally {
            if (camSnap != null) camSnap.restore();
            try { CleanTerrain.exit(mc.worldRenderer, terrain, gr.getCamera().getCameraPos()); } catch (Throwable ignored) { }
            // ...and put yours back for the rest of the frame.
            if (camSnap != null) {
                try { uploadGlobals(mc, gr, tick); } catch (Throwable ignored) { }
            }
            mca.cleanmirror$setCameraEntityRaw(savedCameraEntity);
            mc.crosshairTarget = savedHit;
            mc.targetedEntity = savedTargeted;
            mca.cleanmirror$setFramebuffer(main);
            CleanMirror.renderingClean = false;
            try {
                ((GameRendererAccessor) gr).cleanmirror$updateCameraState(tickProgress);
                // renderWorld switched lighting to world mode; your GUI items expect item lighting.
                gr.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ITEMS_3D);
            } catch (Throwable ignored) { }
        }
    }

    /** Same call GameRenderer.render() makes once per frame, for whatever the camera currently is. */
    private static void uploadGlobals(MinecraftClient mc, GameRenderer gr, RenderTickCounter tick) {
        gr.getGlobalSettings().set(
                mc.getWindow().getFramebufferWidth(),
                mc.getWindow().getFramebufferHeight(),
                mc.options.getGlintStrength().getValue(),
                mc.world == null ? 0L : mc.world.getTime(),
                tick,
                mc.options.getMenuBackgroundBlurrinessValue(),
                gr.getCamera(),
                mc.options.getTextureFiltering().getValue() == net.minecraft.client.option.TextureFilteringMode.RGSS);
    }

    // ------------------------------------------------------------------ the clean GUI

    private static void drawGui(MinecraftClient mc, GameRenderer gr, RenderTickCounter tick, boolean tickWorld,
                                CleanFramebuffer target, Framebuffer main) {
        GuiRenderer renderer = renderer(mc, gr);
        if (renderer == null) return;

        CleanMirrorConfig cfg = CleanMirrorConfig.get();
        MinecraftClientAccessor mca = (MinecraftClientAccessor) mc;

        CleanMirror.renderingClean = true;
        // Swapped for the whole GUI pass: some things (the title-screen panorama) draw straight onto
        // the current framebuffer while the GUI is still being assembled.
        mca.cleanmirror$setFramebuffer(target.get());
        try {
            cleanGuiState.clear();
            int mx = (int) mc.mouse.getScaledX(mc.getWindow());
            int my = (int) mc.mouse.getScaledY(mc.getWindow());
            DrawContext ctx = new DrawContext(mc, cleanGuiState, mx, my);

            boolean finished = mc.isFinishedLoading();
            boolean worldHud = finished && tickWorld && mc.world != null;
            float delta = tick.getDynamicDeltaTicks();

            if (worldHud) CleanHud.render(mc, ctx, tick);

            if (mc.getOverlay() != null) {
                if (cfg.showScreens) mc.getOverlay().render(ctx, mx, my, delta);
            } else if (finished && mc.currentScreen != null && ScreenFilter.allow(mc.currentScreen)) {
                mc.currentScreen.renderWithTooltip(ctx, mx, my, delta);
            }

            if (worldHud) mc.inGameHud.renderAutosaveIndicator(ctx, tick);
            if (finished && cfg.showToasts) mc.getToastManager().draw(ctx);
            if (cfg.showDebugScreen && !(mc.currentScreen instanceof DebugOptionsScreen)) {
                mc.inGameHud.renderDebugHud(ctx);
            }
            // Subtitles can be deferred to "later this frame". Run ours now, on our canvas, so they
            // can't spill into your window or into the next clean frame.
            mc.inGameHud.renderDeferredSubtitles();

            FogRenderer fog = ((GameRendererAccessor) gr).cleanmirror$fog();
            renderer.render(fog.getFogBuffer(FogRenderer.FogType.NONE));
            renderer.incrementFrame();
        } catch (Throwable t) {
            if (!loggedHud) {
                loggedHud = true;
                CleanMirror.LOG.error("The clean GUI failed to draw this frame. The world still shows. "
                        + "Your own window is unaffected.", t);
            }
            try { cleanGuiState.clear(); } catch (Throwable ignored) { }
        } finally {
            mca.cleanmirror$setFramebuffer(main);
            CleanMirror.renderingClean = false;
        }
    }

    /**
     * The clean feed gets its OWN GuiRenderer. Sharing yours would rotate its GPU buffers twice a
     * frame. The special-element list matches vanilla's exactly - including the profiler chart
     * renderer, which is what draws the F3 pie chart.
     */
    private static GuiRenderer renderer(MinecraftClient mc, GameRenderer gr) {
        if (cleanGuiRenderer != null) return cleanGuiRenderer;
        try {
            cleanGuiState = new GuiRenderState();
            VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
            cleanGuiRenderer = new GuiRenderer(
                    cleanGuiState,
                    immediate,
                    gr.getEntityRenderCommandQueue(),
                    gr.getEntityRenderDispatcher(),
                    List.of(
                            new EntityGuiElementRenderer(immediate, mc.getEntityRenderDispatcher()),
                            new PlayerSkinGuiElementRenderer(immediate),
                            new BookModelGuiElementRenderer(immediate),
                            new BannerResultGuiElementRenderer(immediate, mc.getAtlasManager()),
                            new SignGuiElementRenderer(immediate, mc.getAtlasManager()),
                            new ProfilerChartGuiElementRenderer(immediate)));
            // Fabric initialises modded GUI elements on the game's renderer through this hook;
            // ours needs the same call or modded items in menus could fail to draw.
            try {
                ((net.fabricmc.fabric.impl.client.rendering.GuiRendererExtensions) cleanGuiRenderer)
                        .fabric_onReady(gr.getEntityRenderCommandQueue());
            } catch (Throwable t) {
                CleanMirror.LOG.warn("Couldn't run Fabric's GUI-renderer setup on the clean renderer; "
                        + "modded GUI elements may not draw on the clean feed.", t);
            }
            return cleanGuiRenderer;
        } catch (Throwable t) {
            if (!loggedRenderer) {
                loggedRenderer = true;
                CleanMirror.LOG.error("Couldn't create the clean GUI renderer; the clean feed will show the world only.", t);
            }
            cleanGuiRenderer = null;
            cleanGuiState = null;
            return null;
        }
    }

    /** Called when the mirror window closes, to free GPU memory. */
    public static void release() {
        CleanTerrain.release();
        if (cleanGuiRenderer != null) {
            try { cleanGuiRenderer.close(); } catch (Throwable ignored) { }
        }
        cleanGuiRenderer = null;
        cleanGuiState = null;
    }
}
