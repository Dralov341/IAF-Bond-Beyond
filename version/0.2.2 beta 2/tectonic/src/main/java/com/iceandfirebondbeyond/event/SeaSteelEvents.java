package com.iceandfirebondbeyond.event;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.github.alexthe666.iceandfire.enums.EnumSkullType;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import com.github.alexthe666.iceandfire.item.ItemSeaSerpentScales;
import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import com.iceandfirebondbeyond.util.SeaSerpentDeath;
import com.iceandfirebondbeyond.util.ElementalCombat;
import com.iceandfirebondbeyond.entity.SeaSteelArrowEntity;
import com.iceandfirebondbeyond.item.SeaSteelArmorItem;
import com.iceandfirebondbeyond.item.SeaSteelBowItem;
import com.iceandfirebondbeyond.registry.SeaSteelContent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IceAndFireBondBeyond.MOD_ID)
public final class SeaSteelEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof EntitySeaSerpent serpent) || serpent.level().isClientSide
                || serpent.getPersistentData().getBoolean(SeaSerpentDeath.PENDING)) return;
        // Leave the original visible for its gradual fall. tickDeath transfers it
        // exactly once after the animation; vanilla death particles never run.
        serpent.getPersistentData().putBoolean(SeaSerpentDeath.PENDING, true);
        serpent.ejectPassengers();
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void drops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof EntitySeaSerpent serpent)
                || !serpent.getPersistentData().getBoolean(SeaSerpentDeath.PENDING)) return;
        // Equipment, rare eggs and other mods' unrelated drops stay on the death path.
        event.getDrops().removeIf(drop -> drop.getItem().getItem() instanceof ItemSeaSerpentScales
                || drop.getItem().is(IafItemRegistry.SERPENT_FANG.get())
                || drop.getItem().is(EnumSkullType.SEASERPENT.skull_item.get()));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void weaponHit(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide || event.getAmount() <= 0) return;
        var direct = event.getSource().getDirectEntity();
        boolean ranged = direct instanceof AbstractArrow arrow && (arrow instanceof SeaSteelArrowEntity
                || arrow.getPersistentData().getBoolean(SeaSteelBowItem.IMBUED));
        boolean melee = direct instanceof LivingEntity attacker && direct == event.getSource().getEntity()
                && ElementalCombat.isPhysicalMelee(event.getSource())
                && SeaSteelContent.isMelee(attacker.getMainHandItem().getItem());
        if (ranged || melee) event.getEntity().addEffect(new MobEffectInstance(SeaSteelContent.DROWNING.get(), 120, 0), event.getSource().getEntity());
    }
    @SubscribeEvent
    public static void armorTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || entity.tickCount % 20 != 0) return;
        int pieces = 0;
        for (var stack : entity.getArmorSlots()) if (stack.getItem() instanceof SeaSteelArmorItem) pieces++;
        if (pieces == 0) return;
        entity.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, false, false));
        if (entity.isInWaterOrRain()) entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, pieces - 1, false, false));
    }
    private SeaSteelEvents() {}
}
