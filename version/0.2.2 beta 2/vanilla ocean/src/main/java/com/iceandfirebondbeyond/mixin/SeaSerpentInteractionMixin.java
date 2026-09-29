package com.iceandfirebondbeyond.mixin;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.iceandfirebondbeyond.event.CommonEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** IAF calls parent.interact directly for multipart entities. Catch that path too. */
@Mixin(Mob.class)
public abstract class SeaSerpentInteractionMixin {
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void bondBeyond$interact(Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        if ((Object) this instanceof EntitySeaSerpent serpent) {
            InteractionResult result = CommonEvents.interact(serpent, player, hand);
            if (result != InteractionResult.PASS) {
                cir.setReturnValue(result);
            }
        }
    }
}
