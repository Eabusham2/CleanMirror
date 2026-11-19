package com.eyad.cleanmirror.test.mixin;

import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Test-only: press "Create New World" programmatically. */
@Mixin(CreateWorldScreen.class)
public interface CreateWorldScreenInvoker {
    @Invoker("createLevel") void autopilot$createLevel();
}
