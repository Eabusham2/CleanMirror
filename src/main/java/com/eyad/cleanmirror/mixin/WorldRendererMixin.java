package com.eyad.cleanmirror.mixin;

import com.eyad.cleanmirror.render.CleanTerrain;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.ChunkRenderingDataPreparer;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** Give each visibility graph a frozen copy of its camera's position (see CleanTerrain). */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @WrapOperation(method = "updateCamera",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/ChunkRenderingDataPreparer;updateSectionOcclusionGraph(ZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;Ljava/util/List;Lit/unimi/dsi/fastutil/longs/LongOpenHashSet;)V"))
    private void cleanmirror$frozenCamera(ChunkRenderingDataPreparer prep, boolean cull, Camera camera, Frustum frustum,
                                          List<ChunkBuilder.BuiltChunk> chunks, LongOpenHashSet active, Operation<Void> original) {
        original.call(prep, cull, CleanTerrain.frozenFor(prep, camera), frustum, chunks, active);
    }
}
