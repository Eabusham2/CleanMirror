package com.eyad.cleanmirror.mixin;

import com.eyad.cleanmirror.render.NametagRules;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Players draw their label through their own override, which is also where Simple Voice Chat adds
 * its speaker icon. Normal mode leaves this alone (cleaned text, icon still shows). Strict mode draws
 * exactly what vanilla 1.21.11 draws and nothing else.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @WrapMethod(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V")
    private void cleanmirror$strictPlayerLabel(PlayerEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue,
                                               CameraRenderState camera, Operation<Void> original) {
        if (!NametagRules.strict()) {
            original.call(state, matrices, queue, camera);
            return;
        }
        matrices.push();
        int offset = state.extraEars ? -10 : 0;
        if (state.playerName != null) {
            queue.submitLabel(matrices, state.nameLabelPos, offset, state.playerName, !state.sneaking,
                    state.light, state.squaredDistanceToCamera, camera);
            matrices.translate(0.0F, 9.0F * 1.15F * 0.025F, 0.0F);
        }
        if (state.displayName != null) {
            queue.submitLabel(matrices, state.nameLabelPos, offset, state.displayName, !state.sneaking,
                    state.light, state.squaredDistanceToCamera, camera);
        }
        matrices.pop();
    }
}
