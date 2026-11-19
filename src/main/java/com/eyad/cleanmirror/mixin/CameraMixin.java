package com.eyad.cleanmirror.mixin;

import com.eyad.cleanmirror.render.CameraControl;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Some freecam mods force "third person" on, or report a different focused entity, by hooking these
 * two getters. While the clean feed is being drawn from your body, we answer them straight from the
 * camera's real fields - WrapMethod wraps the whole method, other mods' hooks included, so theirs
 * don't get a say for that one render.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow private boolean thirdPerson;
    @Shadow private Entity focusedEntity;

    @WrapMethod(method = "isThirdPerson")
    private boolean cleanmirror$thirdPerson(Operation<Boolean> original) {
        if (CameraControl.isPosedByUs((Camera) (Object) this)) return this.thirdPerson;
        return original.call();
    }

    @WrapMethod(method = "getFocusedEntity")
    private Entity cleanmirror$focused(Operation<Entity> original) {
        if (CameraControl.isPosedByUs((Camera) (Object) this)) return this.focusedEntity;
        return original.call();
    }
}
