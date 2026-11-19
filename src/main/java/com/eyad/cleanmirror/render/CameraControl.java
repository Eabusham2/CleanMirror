package com.eyad.cleanmirror.render;

import com.eyad.cleanmirror.mixin.CameraAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * ===================== KEEPING FREECAM OFF THE STREAM =====================
 *
 * In 1.21.11 the camera is positioned once per frame BEFORE the world is drawn, and the world
 * renderer just reads it. So for the clean redraw we:
 *
 *   1. Take a full snapshot of the game's camera (position, angles, rotation, who it follows).
 *   2. Re-pose it the way VANILLA would - from your body, or from the mob you're spectating -
 *      using vanilla's own maths, written straight into the camera's fields. Freecam mods hook the
 *      camera's methods; they can't hook a field write, so they can't move it.
 *   3. Draw the clean world.
 *   4. Put every field back exactly as it was, so your own view never knows this happened.
 *
 * "Vanilla camera entity" means: the entity you're spectating if it's a real entity in the world,
 * otherwise you. Freecam mods that swap in a fake camera entity fail the "real entity in the world"
 * test, so the feed falls back to your body. Real spectating (spectator mode, clicking a mob) still
 * mirrors, because that IS vanilla.
 */
public final class CameraControl {

    private CameraControl() {}

    private static final Vector3f HORIZONTAL = new Vector3f(0.0F, 0.0F, -1.0F);
    private static final Vector3f VERTICAL = new Vector3f(0.0F, 1.0F, 0.0F);
    private static final Vector3f DIAGONAL = new Vector3f(-1.0F, 0.0F, 0.0F);

    private static Camera posed = null;

    public static boolean isPosedByUs(Camera camera) {
        return posed != null && posed == camera;
    }

    /** Everything we change, so it can be put back bit-for-bit. */
    public static final class Snapshot {
        private final Camera camera;
        private final Vec3d pos;
        private final float yaw, pitch;
        private final Quaternionf rotation;
        private final Vector3f horizontal, vertical, diagonal;
        private final Entity focused;
        private final boolean thirdPerson;

        private Snapshot(Camera camera) {
            CameraAccessor a = (CameraAccessor) camera;
            this.camera = camera;
            this.pos = a.cleanmirror$getPos();
            this.yaw = a.cleanmirror$getYaw();
            this.pitch = a.cleanmirror$getPitch();
            this.rotation = new Quaternionf(camera.getRotation());
            this.horizontal = new Vector3f(a.cleanmirror$horizontal());
            this.vertical = new Vector3f(a.cleanmirror$vertical());
            this.diagonal = new Vector3f(a.cleanmirror$diagonal());
            this.focused = a.cleanmirror$getFocused();
            this.thirdPerson = a.cleanmirror$getThirdPerson();
        }

        public void restore() {
            CameraAccessor a = (CameraAccessor) camera;
            a.cleanmirror$setPosField(pos);
            if (pos != null) a.cleanmirror$getBlockPos().set(pos.x, pos.y, pos.z);
            a.cleanmirror$setYawField(yaw);
            a.cleanmirror$setPitchField(pitch);
            camera.getRotation().set(rotation);
            a.cleanmirror$horizontal().set(horizontal);
            a.cleanmirror$vertical().set(vertical);
            a.cleanmirror$diagonal().set(diagonal);
            a.cleanmirror$setFocused(focused);
            a.cleanmirror$setThirdPerson(thirdPerson);
            posed = null;
        }
    }

    /** The entity vanilla's camera would follow: a real spectated entity, otherwise the player. */
    public static Entity legitCameraEntity(MinecraftClient mc) {
        Entity cam = mc.getCameraEntity();
        if (cam == null || mc.world == null) return mc.player;
        if (cam == mc.player) return cam;
        Entity inWorld = mc.world.getEntityById(cam.getId());
        return inWorld == cam ? cam : mc.player;
    }

    /** Snapshot the camera, then pose it like vanilla would from `target`. Returns null if not possible. */
    public static Snapshot poseFrom(MinecraftClient mc, Camera camera, Entity target, float tickProgress) {
        if (target == null || !camera.isReady()) return null;
        CameraAccessor a = (CameraAccessor) camera;
        if (a.cleanmirror$getArea() == null) return null;

        Snapshot snap = new Snapshot(camera);

        Perspective perspective = mc.options.getPerspective();
        boolean thirdPerson = !perspective.isFirstPerson();

        // Eye height: vanilla smooths it on the camera. Use that smoothing when the camera was
        // already following this entity; otherwise use the entity's eye height directly.
        float eye = snap.focused == target
                ? MathHelper.lerp(tickProgress, a.cleanmirror$getLastCameraY(), a.cleanmirror$getCameraY())
                : target.getStandingEyeHeight();

        a.cleanmirror$setFocused(target);
        a.cleanmirror$setThirdPerson(thirdPerson);
        posed = camera;

        setRotation(camera, target.getYaw(tickProgress), target.getPitch(tickProgress));
        setPos(camera, new Vec3d(
                MathHelper.lerp(tickProgress, target.lastX, target.getX()),
                MathHelper.lerp(tickProgress, target.lastY, target.getY()) + eye,
                MathHelper.lerp(tickProgress, target.lastZ, target.getZ())));

        if (thirdPerson) {
            if (perspective.isFrontView()) {
                setRotation(camera, a.cleanmirror$getYaw() + 180.0F, -a.cleanmirror$getPitch());
            }
            float dist = 4.0F;
            float scale = 1.0F;
            if (target instanceof LivingEntity living) {
                scale = living.getScale();
                dist = (float) living.getAttributeValue(EntityAttributes.CAMERA_DISTANCE);
            }
            float vScale = scale;
            float vDist = dist;
            if (target.hasVehicle() && target.getVehicle() instanceof LivingEntity vehicle) {
                vScale = vehicle.getScale();
                vDist = (float) vehicle.getAttributeValue(EntityAttributes.CAMERA_DISTANCE);
            }
            moveBy(camera, -clipToSpace(camera, Math.max(scale * dist, vScale * vDist)), 0.0F, 0.0F);
        } else if (target instanceof LivingEntity living && living.isSleeping()) {
            Direction dir = living.getSleepingDirection();
            setRotation(camera, dir != null ? dir.getPositiveHorizontalDegrees() - 180.0F : 0.0F, 0.0F);
            moveBy(camera, 0.0F, 0.3F, 0.0F);
        }
        return snap;
    }

    // ---- vanilla Camera maths, written into fields so nothing hooked on the methods can interfere ----

    private static void setRotation(Camera camera, float yaw, float pitch) {
        CameraAccessor a = (CameraAccessor) camera;
        a.cleanmirror$setPitchField(pitch);
        a.cleanmirror$setYawField(yaw);
        Quaternionf q = camera.getRotation();
        q.rotationYXZ((float) Math.PI - yaw * ((float) Math.PI / 180.0F), -pitch * ((float) Math.PI / 180.0F), 0.0F);
        HORIZONTAL.rotate(q, a.cleanmirror$horizontal());
        VERTICAL.rotate(q, a.cleanmirror$vertical());
        DIAGONAL.rotate(q, a.cleanmirror$diagonal());
    }

    private static void setPos(Camera camera, Vec3d pos) {
        CameraAccessor a = (CameraAccessor) camera;
        a.cleanmirror$setPosField(pos);
        a.cleanmirror$getBlockPos().set(pos.x, pos.y, pos.z);
    }

    private static void moveBy(Camera camera, float surge, float heave, float sway) {
        Vector3f v = new Vector3f(sway, heave, -surge).rotate(camera.getRotation());
        Vec3d p = ((CameraAccessor) camera).cleanmirror$getPos();
        setPos(camera, new Vec3d(p.x + v.x, p.y + v.y, p.z + v.z));
    }

    private static float clipToSpace(Camera camera, float distance) {
        CameraAccessor a = (CameraAccessor) camera;
        World area = a.cleanmirror$getArea();
        Vec3d pos = a.cleanmirror$getPos();
        Vector3f horizontal = a.cleanmirror$horizontal();
        for (int i = 0; i < 8; i++) {
            float g = (i & 1) * 2 - 1;
            float h = (i >> 1 & 1) * 2 - 1;
            float j = (i >> 2 & 1) * 2 - 1;
            Vec3d from = pos.add(g * 0.1F, h * 0.1F, j * 0.1F);
            Vec3d to = from.add(new Vec3d(horizontal).multiply(-distance));
            HitResult hit = area.raycast(new RaycastContext(from, to,
                    RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, a.cleanmirror$getFocused()));
            if (hit.getType() != HitResult.Type.MISS) {
                float d = (float) hit.getPos().squaredDistanceTo(pos);
                if (d < MathHelper.square(distance)) distance = MathHelper.sqrt(d);
            }
        }
        return distance;
    }
}
