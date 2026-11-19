package com.eyad.cleanmirror.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Minecraft draws everything onto "the" framebuffer and has no option to draw somewhere else. So
 * for the length of the clean pass we point that at our own canvas, then point it straight back.
 *
 * The camera entity is swapped the same way (as a raw field write - setCameraEntity() has side
 * effects like changing spectator shaders), so freecam mods that use a fake camera entity don't
 * leak into the clean redraw.
 */
@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Mutable
    @Accessor("framebuffer")
    void cleanmirror$setFramebuffer(Framebuffer framebuffer);

    @Accessor("cameraEntity")
    void cleanmirror$setCameraEntityRaw(Entity entity);
}
