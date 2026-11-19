package com.eyad.cleanmirror.mixin;

import com.eyad.cleanmirror.render.HudFilter;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.impl.client.rendering.hud.HudLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The HUD filter. In 1.21.11 every HUD piece - vanilla ones and every mod piece registered with
 * Fabric - is drawn through one of Fabric's "root layers", one layer at a time. Each layer carries
 * its name. We step in right as a layer is about to draw and swap it for "draw nothing" if the clean
 * feed isn't allowed to show it.
 *
 * Your own window always passes (HudFilter.allow returns true outside the clean pass).
 */
@Mixin(targets = "net.fabricmc.fabric.impl.client.rendering.hud.HudElementRegistryImpl$RootLayer", remap = false)
public abstract class HudRootLayerMixin {

    private static final HudElement NOTHING = (context, tickCounter) -> { };

    @WrapOperation(
            method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/fabricmc/fabric/impl/client/rendering/hud/HudLayer;element(Lnet/fabricmc/fabric/api/client/rendering/v1/hud/HudElement;)Lnet/fabricmc/fabric/api/client/rendering/v1/hud/HudElement;"),
            require = 0)
    private HudElement cleanmirror$filter(HudLayer layer, HudElement vanilla, Operation<HudElement> original) {
        HudFilter.note(layer.id());
        if (!HudFilter.allow(layer.id())) return NOTHING;
        return original.call(layer, vanilla);
    }
}
