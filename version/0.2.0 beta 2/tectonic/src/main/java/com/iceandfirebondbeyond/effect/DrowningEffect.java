package com.iceandfirebondbeyond.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Magical drowning does not depend on immersion or the victim's air-breathing AI. */
public final class DrowningEffect extends MobEffect {
    public DrowningEffect() {
        super(MobEffectCategory.HARMFUL, 0x237FA0);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "4fa760dd-5c4e-49a2-b0bf-5d4bda1a3c95",
                -0.25D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
    @Override public boolean isDurationEffectTick(int duration, int amplifier) { return true; }
    @Override public void applyEffectTick(LivingEntity victim, int amplifier) {
        if (!(victim.level() instanceof ServerLevel server)) return;
        if (victim.tickCount % 20 == 0) victim.hurt(victim.damageSources().drown(), 2.0F);
        if (victim.tickCount % 4 == 0) {
            int count = Math.min(48, 18 + (int) (victim.getBbWidth() * 4));
            server.sendParticles(ParticleTypes.SPLASH, victim.getX(), victim.getY() + victim.getBbHeight() * 0.65,
                    victim.getZ(), count, victim.getBbWidth() * 0.45, victim.getBbHeight() * 0.3,
                    victim.getBbWidth() * 0.45, 0.12);
            server.sendParticles(ParticleTypes.BUBBLE_POP, victim.getX(), victim.getEyeY(), victim.getZ(),
                    6, victim.getBbWidth() * 0.35, 0.15, victim.getBbWidth() * 0.35, 0.04);
        }
    }
}
