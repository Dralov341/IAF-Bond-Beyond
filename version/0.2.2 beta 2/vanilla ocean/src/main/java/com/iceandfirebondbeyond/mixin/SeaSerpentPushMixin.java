package com.iceandfirebondbeyond.mixin;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Intercept received impulses, including shoves routed through IAF's body parts. */
@Mixin(Entity.class)
public abstract class SeaSerpentPushMixin {
    @Inject(method = "push(DDD)V", at = @At("HEAD"), cancellable = true)
    private void bondBeyond$resistShove(double x, double y, double z, CallbackInfo ci) {
        if (!((Object) this instanceof EntitySeaSerpent serpent)) return;
        double multiplier = SeaSerpentBondData.getCollisionPushMultiplier(serpent);
        if (multiplier == 1.0D) return;
        if (multiplier > 0.0D) {
            // Scale only this incoming shove. Never scale existing swim velocity,
            // gravity, rider acceleration or the locked slam trajectory.
            serpent.setDeltaMovement(serpent.getDeltaMovement().add(
                    x * multiplier, y * multiplier, z * multiplier));
            serpent.hasImpulse = true;
        }
        ci.cancel();
    }
}
