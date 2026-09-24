package com.iceandfirebondbeyond.entity;

import com.iceandfirebondbeyond.item.SeaSteelBowItem;
import com.iceandfirebondbeyond.registry.SeaSteelContent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public final class SeaSteelArrowEntity extends AbstractArrow {
    public SeaSteelArrowEntity(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level); imbue();
    }
    public SeaSteelArrowEntity(EntityType<? extends AbstractArrow> type, LivingEntity owner, Level level) {
        super(type, owner, level); imbue();
    }
    private void imbue() { setBaseDamage(6.0D); getPersistentData().putBoolean(SeaSteelBowItem.IMBUED, true); }
    @Override protected ItemStack getPickupItem() { return new ItemStack(SeaSteelContent.ARROW.get()); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
