package com.eyad.cleanmirror.test.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Behaves like a health-nametag mod: appends "[HP n]" to every living entity's display name. */
@Mixin(Entity.class)
public abstract class FakeHealthTagMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void fake$health(CallbackInfoReturnable<Text> cir) {
        if ((Object) this instanceof LivingEntity living) {
            cir.setReturnValue(cir.getReturnValue().copy()
                    .append(Text.literal(" [HP " + (int) living.getHealth() + "]").formatted(Formatting.RED)));
        }
    }
}
