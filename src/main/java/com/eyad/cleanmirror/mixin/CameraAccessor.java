package com.eyad.cleanmirror.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Raw access to the camera's fields. We write them directly rather than calling the camera's own
 * methods, because freecam mods hook those methods - writing the fields means nothing can move the
 * clean feed's camera except us.
 */
@Mixin(Camera.class)
public interface CameraAccessor {
    @Accessor("area") World cleanmirror$getArea();
    @Accessor("focusedEntity") Entity cleanmirror$getFocused();
    @Accessor("focusedEntity") void cleanmirror$setFocused(Entity e);
    @Accessor("thirdPerson") boolean cleanmirror$getThirdPerson();
    @Accessor("thirdPerson") void cleanmirror$setThirdPerson(boolean b);
    @Accessor("pos") Vec3d cleanmirror$getPos();
    @Accessor("pos") void cleanmirror$setPosField(Vec3d v);
    @Accessor("blockPos") BlockPos.Mutable cleanmirror$getBlockPos();
    @Accessor("yaw") float cleanmirror$getYaw();
    @Accessor("yaw") void cleanmirror$setYawField(float f);
    @Accessor("pitch") float cleanmirror$getPitch();
    @Accessor("pitch") void cleanmirror$setPitchField(float f);
    @Accessor("horizontalPlane") Vector3f cleanmirror$horizontal();
    @Accessor("verticalPlane") Vector3f cleanmirror$vertical();
    @Accessor("diagonalPlane") Vector3f cleanmirror$diagonal();
    @Accessor("cameraY") float cleanmirror$getCameraY();
    @Accessor("lastCameraY") float cleanmirror$getLastCameraY();
}
