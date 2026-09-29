package com.iceandfirebondbeyond.item;

import com.github.alexthe666.iceandfire.item.DragonsteelArmorMaterial;
import com.github.alexthe666.iceandfire.item.ItemDragonsteelArmor;
import com.iceandfirebondbeyond.client.model.SeaSteelArmorModel;
import com.iceandfirebondbeyond.registry.SeaSteelContent;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import java.util.function.Consumer;

public final class SeaSteelArmorItem extends ItemDragonsteelArmor {
    private static final DragonsteelArmorMaterial MATERIAL = new DragonsteelArmorMaterial(
            "iceandfire_bond_beyond:sea_serpent_steel", 160, new int[]{6, 9, 12, 7}, 30,
            SoundEvents.ARMOR_EQUIP_DIAMOND, 6.0F) {
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(SeaSteelContent.INGOT.get()); }
        @Override public float getKnockbackResistance() { return 0.1F; }
    };
    public SeaSteelArmorItem(Type type) { super(MATERIAL, type.getSlot().getIndex(), type); }
    @Override public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level level,
            java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        lines.add(net.minecraft.network.chat.Component.translatable("tooltip.iceandfire_bond_beyond.steel.armor")
                .withStyle(net.minecraft.ChatFormatting.AQUA));
    }
    @Override public boolean isFireResistant() { return true; }
    @Override public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "iceandfire_bond_beyond:textures/models/armor/sea_serpent_steel" + (slot == EquipmentSlot.LEGS ? "_legs.png" : ".png");
    }
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private SeaSteelArmorModel inner, outer;
            @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                    EquipmentSlot slot, HumanoidModel<?> original) {
                if (slot == EquipmentSlot.LEGS) {
                    if (inner == null) inner = new SeaSteelArmorModel(true);
                    return inner;
                }
                if (outer == null) outer = new SeaSteelArmorModel(false);
                return outer;
            }
        });
    }
}
