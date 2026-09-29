package com.iceandfirebondbeyond.event;

import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import com.iceandfirebondbeyond.util.DragonSteelLightning;
import com.iceandfirebondbeyond.util.ElementalCombat;
import com.iceandfirebondbeyond.util.LightningWeapons;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Observe real hits, including addon weapons that never call IAF's item methods. */
@Mod.EventBusSubscriber(modid = IceAndFireBondBeyond.MOD_ID)
public final class LightningWeaponEvents {
    private static final String PROJECTILE_WEAPON = "BondBeyondLightningWeapon";

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void replaceWeaponBolt(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof net.minecraft.world.entity.LightningBolt bolt
                && DragonSteelLightning.replacesWeaponBolt(bolt)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void rememberProjectile(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()
                || !(event.getEntity() instanceof Projectile projectile)
                || !(projectile.getOwner() instanceof LivingEntity owner)) return;
        ItemStack weapon = LightningWeapons.carriedWeapon(projectile);
        if (weapon.isEmpty()) {
            // Prefer the item actually being used, even when it is NOT lightning.
            // A lightning sword in the other hand must not electrify ordinary arrows.
            weapon = rangedWeapon(owner.getUseItem());
            if (weapon.isEmpty()) weapon = rangedWeapon(owner.getMainHandItem());
            if (weapon.isEmpty()) weapon = rangedWeapon(owner.getOffhandItem());
        }
        if (LightningWeapons.isLightning(weapon))
            projectile.getPersistentData().put(PROJECTILE_WEAPON, weapon.copy().save(new net.minecraft.nbt.CompoundTag()));
    }

    private static ItemStack rangedWeapon(ItemStack stack) {
        return stack.getItem() instanceof ProjectileWeaponItem
                ? stack : ItemStack.EMPTY;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void successfulHit(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide || event.getAmount() <= 0
                || DragonSteelLightning.isProcessing()
                || !(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        Entity direct = event.getSource().getDirectEntity();
        ItemStack weapon = ItemStack.EMPTY;
        if (direct == attacker && ElementalCombat.isPhysicalMelee(event.getSource())) {
            weapon = attacker.getMainHandItem();
        } else if (direct instanceof Projectile projectile) {
            weapon = projectile.getPersistentData().contains(PROJECTILE_WEAPON, Tag.TAG_COMPOUND)
                    ? ItemStack.of(projectile.getPersistentData().getCompound(PROJECTILE_WEAPON))
                    : LightningWeapons.carriedWeapon(projectile);
        }
        if (LightningWeapons.isLightning(weapon)) DragonSteelLightning.strike(weapon, event.getEntity(), attacker);
    }

    @SubscribeEvent
    public static void finishHits(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level)
            DragonSteelLightning.flush(level);
    }

    private LightningWeaponEvents() {}
}
