package com.iceandfirebondbeyond.event;

import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import com.iceandfirebondbeyond.util.SeaSerpentOwnerAssist;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Capture owner intent before vanilla loses multipart/indirect attack information. */
@Mod.EventBusSubscriber(modid = IceAndFireBondBeyond.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OwnerCombatEvents {
    private OwnerCombatEvents() {}
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void ownerMelee(AttackEntityEvent event) {
        SeaSerpentOwnerAssist.record(event.getEntity(), event.getTarget(), false);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void ownerDamage(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide || event.getAmount() <= 0) return;
        Entity source = event.getSource().getEntity();
        if (source == null && event.getSource().getDirectEntity() instanceof Projectile projectile)
            source = projectile.getOwner();
        if (source instanceof Player owner) SeaSerpentOwnerAssist.record(owner, event.getEntity(), false);
        if (event.getEntity() instanceof Player owner && source != null)
            SeaSerpentOwnerAssist.record(owner, source, true);
    }
}
