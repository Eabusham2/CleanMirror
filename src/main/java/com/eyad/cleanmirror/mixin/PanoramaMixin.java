package com.eyad.cleanmirror.mixin;

import com.eyad.cleanmirror.CleanMirror;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * The menu panorama turns a little every time it's drawn. The clean feed draws it too, so without
 * this your own title screen would spin at double speed. The clean copy just doesn't advance it.
 */
@Mixin(RotatingCubeMapRenderer.class)
public abstract class PanoramaMixin {

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, require = 0)
    private boolean cleanmirror$noDoubleSpin(boolean rotate) {
        return rotate && !CleanMirror.renderingClean;
    }
}
