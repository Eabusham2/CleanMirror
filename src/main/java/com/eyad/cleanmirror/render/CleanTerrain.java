package com.eyad.cleanmirror.render;

import com.eyad.cleanmirror.mixin.CameraAccessor;
import com.eyad.cleanmirror.mixin.WorldRendererAccessor;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.ChunkRenderingDataPreparer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;

/**
 * ===================== WHY THE CLEAN VIEW HAS ITS OWN TERRAIN LIST =====================
 *
 * Minecraft decides which chunks are visible with an "occlusion graph" built from the camera's
 * position, in the background, and only rebuilt when the camera moves. There is one per world
 * renderer. With a freecam, your camera and the clean camera are in different places every frame,
 * so a single graph keeps getting built for the wrong one - in testing, the stream lost most of the
 * distant ground while freecam was on.
 *
 * So REDRAW gives the clean view its own graph, plus its own "where was the camera last time"
 * bookkeeping, swapped in only for the clean render. Updates about chunks loading or finishing
 * building are passed to both graphs (see PreparerForwardMixin), so neither goes stale.
 *
 * The chunk WINDOW (which chunks are loaded around you) stays centred on your own camera - moving it
 * every frame would rebuild chunks constantly. Practical effect: with a freecam more than your
 * render distance away from your body, terrain around your body can be missing on the stream.
 */
public final class CleanTerrain {

    private CleanTerrain() {}

    private static WorldRenderer owner;
    private static ChunkRenderingDataPreparer main;
    private static ChunkRenderingDataPreparer clean;
    private static double cleanLastX = Double.MIN_VALUE, cleanLastY = Double.MIN_VALUE, cleanLastZ = Double.MIN_VALUE;
    private static boolean forwarding = false;

    /** What we change on the world renderer, to put back afterwards. */
    public record Saved(double lastX, double lastY, double lastZ, int chunkX, int chunkY, int chunkZ) {}

    public static Saved enter(WorldRenderer wr, Vec3d cleanCameraPos) {
        WorldRendererAccessor a = (WorldRendererAccessor) wr;
        if (a.cleanmirror$getChunks() == null) return null;

        ChunkRenderingDataPreparer current = a.cleanmirror$getPreparer();
        if (owner != wr || main != current || clean == null) {
            owner = wr;
            main = current;
            clean = new ChunkRenderingDataPreparer();
            clean.setStorage(a.cleanmirror$getChunks());
            cleanLastX = cleanLastY = cleanLastZ = Double.MIN_VALUE;
        }

        Saved saved = new Saved(a.cleanmirror$getLastX(), a.cleanmirror$getLastY(), a.cleanmirror$getLastZ(),
                a.cleanmirror$getChunkX(), a.cleanmirror$getChunkY(), a.cleanmirror$getChunkZ());

        a.cleanmirror$setPreparer(clean);
        a.cleanmirror$setLastX(cleanLastX);
        a.cleanmirror$setLastY(cleanLastY);
        a.cleanmirror$setLastZ(cleanLastZ);
        // Report the clean camera's section as "unchanged" so the chunk window isn't re-centred.
        a.cleanmirror$setChunkX(ChunkSectionPos.getSectionCoord(cleanCameraPos.getX()));
        a.cleanmirror$setChunkY(ChunkSectionPos.getSectionCoord(cleanCameraPos.getY()));
        a.cleanmirror$setChunkZ(ChunkSectionPos.getSectionCoord(cleanCameraPos.getZ()));
        // Force the visible-chunk list to be rebuilt for this camera's view.
        a.cleanmirror$setLastPitch(Double.NaN);
        return saved;
    }

    public static void exit(WorldRenderer wr, Saved saved, Vec3d mainCameraPos) {
        if (saved == null) return;
        WorldRendererAccessor a = (WorldRendererAccessor) wr;
        cleanLastX = a.cleanmirror$getLastX();
        cleanLastY = a.cleanmirror$getLastY();
        cleanLastZ = a.cleanmirror$getLastZ();

        a.cleanmirror$setPreparer(main);
        a.cleanmirror$setLastX(saved.lastX());
        a.cleanmirror$setLastY(saved.lastY());
        a.cleanmirror$setLastZ(saved.lastZ());
        a.cleanmirror$setChunkX(saved.chunkX());
        a.cleanmirror$setChunkY(saved.chunkY());
        a.cleanmirror$setChunkZ(saved.chunkZ());
        // The visible-chunk list now holds the clean view's; make your view rebuild its own next frame.
        a.cleanmirror$setLastPitch(Double.NaN);
        if (a.cleanmirror$getChunkBuilder() != null && mainCameraPos != null) {
            a.cleanmirror$getChunkBuilder().setCameraPosition(mainCameraPos);
        }
    }

    /** The other graph to keep in sync with `p`, or null. */
    public static ChunkRenderingDataPreparer partnerOf(ChunkRenderingDataPreparer p) {
        if (forwarding || clean == null || main == null) return null;
        if (p == main) return clean;
        if (p == clean) return main;
        return null;
    }

    public static boolean isMain(ChunkRenderingDataPreparer p) {
        return !forwarding && clean != null && p == main;
    }

    /** Run an update on the partner graph without it bouncing back. */
    public static void forward(Runnable r) {
        forwarding = true;
        try { r.run(); } finally { forwarding = false; }
    }

    public static ChunkRenderingDataPreparer clean() { return clean; }

    // ---- frozen camera positions for the background visibility jobs ----
    //
    // The visibility graph is rebuilt on a worker thread that reads the camera's block position
    // LATER, after we've already moved the camera back. So each graph gets its own frozen copy of
    // the position it was scheduled with. Copies are replaced, never edited, so a job that's still
    // running keeps seeing the position it started from.

    private static final Camera[] frozen = new Camera[2];

    public static Camera frozenFor(ChunkRenderingDataPreparer prep, Camera live) {
        if (clean == null || main == null) return live;
        int slot = prep == main ? 0 : prep == clean ? 1 : -1;
        if (slot < 0) return live;
        Vec3d pos = live.getCameraPos();
        Camera f = frozen[slot];
        if (f == null || !pos.equals(f.getCameraPos())) {
            f = new Camera();
            CameraAccessor a = (CameraAccessor) f;
            a.cleanmirror$setPosField(pos);
            a.cleanmirror$getBlockPos().set(pos.x, pos.y, pos.z);
            frozen[slot] = f;
        }
        return f;
    }

    /** Called when the clean window closes. */
    public static void release() {
        if (clean != null) {
            ChunkRenderingDataPreparer c = clean;
            clean = null;
            forward(() -> c.setStorage(null));
        }
        owner = null;
        main = null;
        frozen[0] = frozen[1] = null;
    }
}
