package com.eyad.cleanmirror.compat;

import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.texture.GlTexture;

/**
 * The only place that reaches under Minecraft's graphics abstraction to the raw OpenGL texture id
 * (needed to show the picture in a second window). Verified against 1.21.11.
 */
public final class Fb {

    private Fb() {}

    public static int colorGlId(Framebuffer fb) {
        GpuTexture tex = fb.getColorAttachment();
        return tex instanceof GlTexture gl ? gl.getGlId() : 0;
    }

    public static Framebuffer create(int width, int height) {
        return new SimpleFramebuffer("CleanMirror", width, height, true);
    }
}
