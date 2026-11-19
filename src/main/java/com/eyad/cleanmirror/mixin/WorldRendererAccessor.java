package com.eyad.cleanmirror.mixin;

import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.ChunkRenderingDataPreparer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The world renderer's per-camera terrain bookkeeping, so the clean view can have its own. */
@Mixin(WorldRenderer.class)
public interface WorldRendererAccessor {
    @Accessor("chunkRenderingDataPreparer") ChunkRenderingDataPreparer cleanmirror$getPreparer();
    @Mutable @Accessor("chunkRenderingDataPreparer") void cleanmirror$setPreparer(ChunkRenderingDataPreparer p);
    @Accessor("chunks") BuiltChunkStorage cleanmirror$getChunks();
    @Accessor("chunkBuilder") ChunkBuilder cleanmirror$getChunkBuilder();

    @Accessor("lastCameraX") double cleanmirror$getLastX();
    @Accessor("lastCameraX") void cleanmirror$setLastX(double v);
    @Accessor("lastCameraY") double cleanmirror$getLastY();
    @Accessor("lastCameraY") void cleanmirror$setLastY(double v);
    @Accessor("lastCameraZ") double cleanmirror$getLastZ();
    @Accessor("lastCameraZ") void cleanmirror$setLastZ(double v);
    @Accessor("lastCameraPitch") void cleanmirror$setLastPitch(double v);

    @Accessor("cameraChunkX") int cleanmirror$getChunkX();
    @Accessor("cameraChunkX") void cleanmirror$setChunkX(int v);
    @Accessor("cameraChunkY") int cleanmirror$getChunkY();
    @Accessor("cameraChunkY") void cleanmirror$setChunkY(int v);
    @Accessor("cameraChunkZ") int cleanmirror$getChunkZ();
    @Accessor("cameraChunkZ") void cleanmirror$setChunkZ(int v);
}
