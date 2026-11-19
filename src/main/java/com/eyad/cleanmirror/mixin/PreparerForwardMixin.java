package com.eyad.cleanmirror.mixin;

import com.eyad.cleanmirror.render.CleanTerrain;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.ChunkRenderingDataPreparer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the clean view's visibility graph and yours in sync: whenever the game tells one of them a
 * chunk loaded, finished building, or needs re-checking, the other hears it too.
 */
@Mixin(ChunkRenderingDataPreparer.class)
public abstract class PreparerForwardMixin {

    @Inject(method = "addNeighbors", at = @At("HEAD"))
    private void cleanmirror$addNeighbors(ChunkPos pos, CallbackInfo ci) {
        ChunkRenderingDataPreparer other = CleanTerrain.partnerOf((ChunkRenderingDataPreparer) (Object) this);
        if (other != null) CleanTerrain.forward(() -> other.addNeighbors(pos));
    }

    @Inject(method = "schedulePropagationFrom", at = @At("HEAD"))
    private void cleanmirror$propagate(ChunkBuilder.BuiltChunk chunk, CallbackInfo ci) {
        ChunkRenderingDataPreparer other = CleanTerrain.partnerOf((ChunkRenderingDataPreparer) (Object) this);
        if (other != null) CleanTerrain.forward(() -> other.schedulePropagationFrom(chunk));
    }

    @Inject(method = "scheduleTerrainUpdate", at = @At("HEAD"))
    private void cleanmirror$schedule(CallbackInfo ci) {
        ChunkRenderingDataPreparer other = CleanTerrain.partnerOf((ChunkRenderingDataPreparer) (Object) this);
        if (other != null) CleanTerrain.forward(other::scheduleTerrainUpdate);
    }

    /** New world / resource reload: the clean graph follows your graph onto the new chunk storage. */
    @Inject(method = "setStorage", at = @At("HEAD"))
    private void cleanmirror$storage(BuiltChunkStorage storage, CallbackInfo ci) {
        if (CleanTerrain.isMain((ChunkRenderingDataPreparer) (Object) this)) {
            ChunkRenderingDataPreparer clean = CleanTerrain.clean();
            CleanTerrain.forward(() -> clean.setStorage(storage));
        }
    }
}
