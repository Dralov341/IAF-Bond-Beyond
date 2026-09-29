package com.iceandfirebondbeyond.mixin;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.github.alexthe666.iceandfire.entity.EntitySeaSerpentBubbles;
import com.iceandfirebondbeyond.entity.SeaSerpentRiderBubbleEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Route native AI shots through the same server collision path as mounted breath. */
@Mixin(EntitySeaSerpentBubbles.class)
public abstract class SeaSerpentBubbleMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void bondBeyond$useSizedBreath(CallbackInfo ci) {
        EntitySeaSerpentBubbles bubble = (EntitySeaSerpentBubbles) (Object) this;
        if (bubble.level().isClientSide || !(bubble.getOwner() instanceof EntitySeaSerpent serpent)) return;
        if (serpent.isAlive() && !bubble.isRemoved()) {
            Vec3 direction = bubble.getDeltaMovement();
            if (direction.lengthSqr() < 1.0E-8D) direction = new Vec3(bubble.xPower, bubble.yPower, bubble.zPower);
            if (direction.lengthSqr() < 1.0E-8D) direction = serpent.getLookAngle();
            // Legacy/saved or third-party native shots still enter the fixed path.
            // Ordinary wild AI now creates the custom entity directly.
            SeaSerpentRiderBubbleEntity.fire(serpent, bubble.position(), direction);
        }
        bubble.discard();
        ci.cancel();
    }
}
