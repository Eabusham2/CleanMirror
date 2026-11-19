package com.eyad.cleanmirror.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.util.memory.ObjectPool;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    /** Vanilla's spectator post-shader (creeper / spider / enderman view). Mirrored on REDRAW. */
    @Accessor("postProcessorId") Identifier cleanmirror$postId();
    @Accessor("postProcessorEnabled") boolean cleanmirror$postEnabled();
    @Accessor("pool") ObjectPool cleanmirror$pool();
    @Accessor("fogRenderer") FogRenderer cleanmirror$fog();
    /** Re-records the camera for the rest of the frame after we put the real camera back. */
    @Invoker("updateCameraState") void cleanmirror$updateCameraState(float tickProgress);
}
