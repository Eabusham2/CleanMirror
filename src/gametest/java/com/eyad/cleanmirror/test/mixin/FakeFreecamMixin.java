package com.eyad.cleanmirror.test.mixin;

import com.eyad.cleanmirror.test.FakeMods;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Behaves like a freecam mod: while on, lifts the camera 12 blocks up, looks straight down, and
 * forces third person (so the player's body would render) - via the camera's own methods.
 */
@Mixin(Camera.class)
public abstract class FakeFreecamMixin {
    @Shadow protected abstract void setPos(Vec3d pos);
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow public abstract Vec3d getCameraPos();
    @Shadow private boolean thirdPerson;

    @Inject(method = "update", at = @At("TAIL"))
    private void fake$freecam(World area, Entity focused, boolean third, boolean inverse, float tick, CallbackInfo ci) {
        if (!FakeMods.freecam) return;
        setRotation(0.0F, 90.0F);
        setPos(getCameraPos().add(FakeMods.freecamShiftX, 12.0, 0.0));
        this.thirdPerson = true;
    }
}
