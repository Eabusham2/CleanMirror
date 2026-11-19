package com.eyad.cleanmirror.window;

import com.eyad.cleanmirror.compat.Fb;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Framebuffer;

/**
 * The clean feed's canvas: a real Minecraft framebuffer, always the same size as your game's.
 *
 * Same size matters: the HUD is laid out from your game window's size, so drawing it onto a
 * different-sized canvas would put the hotbar in the wrong place. The second window scales the
 * finished picture instead, which is free.
 */
public final class CleanFramebuffer {

    private Framebuffer fb;

    public Framebuffer get() { return fb; }
    public int width()  { return fb == null ? 0 : fb.textureWidth; }
    public int height() { return fb == null ? 0 : fb.textureHeight; }
    public int colorGlId() { return fb == null ? 0 : Fb.colorGlId(fb); }

    public void matchSizeOf(Framebuffer main) {
        int w = Math.max(1, main.textureWidth);
        int h = Math.max(1, main.textureHeight);
        if (fb == null) fb = Fb.create(w, h);
        else if (fb.textureWidth != w || fb.textureHeight != h) fb.resize(w, h);
    }

    /** REUSE: copy the finished world (no GUI on it yet) from your framebuffer. */
    public void copyColorFrom(Framebuffer main) {
        if (fb == null) return;
        int w = Math.min(fb.textureWidth, main.textureWidth);
        int h = Math.min(fb.textureHeight, main.textureHeight);
        encoder().copyTextureToTexture(main.getColorAttachment(), fb.getColorAttachment(), 0, 0, 0, 0, 0, w, h);
    }

    public void clearAll() {
        if (fb != null) encoder().clearColorAndDepthTextures(fb.getColorAttachment(), 0, fb.getDepthAttachment(), 1.0);
    }

    public void clearDepth() {
        if (fb != null && fb.getDepthAttachment() != null) encoder().clearDepthTexture(fb.getDepthAttachment(), 1.0);
    }

    public void destroy() {
        if (fb != null) { fb.delete(); fb = null; }
    }

    private static CommandEncoder encoder() {
        return RenderSystem.getDevice().createCommandEncoder();
    }
}
