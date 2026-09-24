package com.iceandfirebondbeyond.event;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.github.alexthe666.iceandfire.entity.props.EntityDataProvider;
import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import com.iceandfirebondbeyond.item.ElementalShieldItem;
import com.iceandfirebondbeyond.registry.SeaSteelContent;
import com.iceandfirebondbeyond.util.DragonSteelLightning;
import com.iceandfirebondbeyond.util.ElementalCombat;
import com.iceandfirebondbeyond.util.SeaSerpentCombat;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IceAndFireBondBeyond.MOD_ID)
public final class ElementalCombatEvents {
    private static final ThreadLocal<Boolean> RETALIATING = ThreadLocal.withInitial(() -> false);
    private static final String NEXT_RETALIATION = "BondBeyondNextShieldRetaliation";

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void serpentWeakness(LivingHurtEvent event) {
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof EntitySeaSerpent) {
            event.setAmount(event.getAmount() * ElementalCombat.incomingMultiplier(event.getEntity(), event.getSource()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void shieldBlock(ShieldBlockEvent event) {
        LivingEntity defender = event.getEntity();
        if (defender.level().isClientSide || RETALIATING.get() || event.getBlockedDamage() <= 0
                || !(defender.getUseItem().getItem() instanceof ElementalShieldItem shield)) return;
        Entity source = event.getDamageSource().getEntity();
        Entity root = source == null ? null : SeaSerpentCombat.damageTarget(source);
        if (!(root instanceof LivingEntity attacker) || !DragonSteelLightning.canStrike(attacker, defender)
                || defender.distanceToSqr(attacker) > 100 || !defender.hasLineOfSight(attacker)) return;
        long now = defender.level().getGameTime();
        // No recursive shield-vs-shield arcs; sustained breath cannot proc every tick.
        if (defender.getPersistentData().getLong(NEXT_RETALIATION) > now) return;
        defender.getPersistentData().putLong(NEXT_RETALIATION, now + 10);
        RETALIATING.set(true);
        try {
            switch (shield.element()) {
                case WATER -> attacker.addEffect(new MobEffectInstance(SeaSteelContent.DROWNING.get(), 120, 0), defender);
                case FIRE -> { if (IafConfig.dragonWeaponFireAbility) attacker.setSecondsOnFire(5); }
                case ICE -> {
                    if (IafConfig.dragonWeaponIceAbility) {
                        EntityDataProvider.getCapability(attacker).ifPresent(data -> data.frozenData.setFrozen(attacker, 100));
                        attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2), defender);
                    }
                }
                case LIGHTNING -> DragonSteelLightning.strike(5.0F, attacker, defender);
            }
        } finally {
            RETALIATING.remove();
        }
    }

    private ElementalCombatEvents() {}
}
