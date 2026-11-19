package com.eyad.cleanmirror.mixin;

import com.eyad.cleanmirror.render.NametagRules;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Shadow @Final protected EntityRenderManager dispatcher;

    /**
     * Runs AFTER the whole render state is built - every subclass and every other mod included -
     * then puts the nametag back to what vanilla would show.
     */
    @WrapMethod(method = "getAndUpdateRenderState")
    private EntityRenderState cleanmirror$cleanNametag(Entity entity, float tickProgress, Operation<EntityRenderState> original) {
        EntityRenderState state = original.call(entity, tickProgress);
        if (state != null && NametagRules.active()) {
            NametagRules.sanitize(entity, state, this.dispatcher.targetedEntity, this.dispatcher.camera != null, tickProgress);
        }
        return state;
    }

    /** Strict nametags: draw the label exactly as vanilla does, skipping anything other mods add here. */
    @WrapMethod(method = "renderLabelIfPresent")
    private void cleanmirror$strictLabel(EntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue,
                                         CameraRenderState camera, Operation<Void> original) {
        if (!NametagRules.strict()) {
            original.call(state, matrices, queue, camera);
            return;
        }
        if (state.displayName != null) {
            queue.submitLabel(matrices, state.nameLabelPos, 0, state.displayName, !state.sneaking,
                    state.light, state.squaredDistanceToCamera, camera);
        }
    }
}
