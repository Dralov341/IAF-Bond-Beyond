package com.iceandfirebondbeyond.client.renderer.layer;

import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.iceandfire.entity.DragonType;
import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.github.alexthe666.iceandfire.enums.EnumDragonTextures;
import com.github.alexthe666.iceandfire.item.ItemDragonArmor;
import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import com.iceandfirebondbeyond.item.NetheriteDragonArmorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

/**
 * Port of CE's LayerDragonArmor: render each equipped part on the animated parent
 * model. IAF's species have different skull/muzzle UVs; lightning also has
 * different body details. Keep native textures for every native material,
 * including Dragonsteel, and select fitted Netherite textures where needed.
 * See META-INF/BOND-BEYOND-CE-NOTICE.txt for upstream attribution and changes.
 */
public final class DragonArmorLayer extends RenderLayer<
        EntityDragonBase, AdvancedEntityModel<EntityDragonBase>> {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final Map<EquipmentSlot, ResourceLocation> NETHERITE_TEXTURES =
            createNetheriteTextures();
    private static final ResourceLocation ICE_NETHERITE_HEAD =
            netheriteTexture("head", "_icedragon");
    private static final ResourceLocation LIGHTNING_NETHERITE_HEAD =
            netheriteTexture("head", "_lightningdragon");
    private static final ResourceLocation LIGHTNING_NETHERITE_BODY =
            netheriteTexture("body", "_lightningdragon");

    public DragonArmorLayer(RenderLayerParent<EntityDragonBase,
            AdvancedEntityModel<EntityDragonBase>> parent) {
        super(parent);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer,
            int packedLight, @NotNull EntityDragonBase dragon, float limbSwing,
            float limbSwingAmount, float partialTick, float ageInTicks,
            float netHeadYaw, float headPitch) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = dragon.getItemBySlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemDragonArmor)) continue;
            ResourceLocation texture = stack.getItem() instanceof NetheriteDragonArmorItem
                    ? netheriteTexture(dragon, slot) : nativeTexture(dragon, slot);
            getParentModel().renderToBuffer(poseStack,
                    buffer.getBuffer(RenderType.entityCutoutNoCull(texture)),
                    packedLight, OverlayTexture.NO_OVERLAY,
                    stack.getItem() instanceof com.iceandfirebondbeyond.item.SeaSteelDragonArmorItem ? .30F : 1.0F,
                    stack.getItem() instanceof com.iceandfirebondbeyond.item.SeaSteelDragonArmorItem ? .64F : 1.0F,
                    stack.getItem() instanceof com.iceandfirebondbeyond.item.SeaSteelDragonArmorItem ? .68F : 1.0F, 1.0F);
        }
    }

    private static ResourceLocation nativeTexture(EntityDragonBase dragon, EquipmentSlot slot) {
        EnumDragonTextures.Armor armor = EnumDragonTextures.Armor.getArmorForDragon(dragon, slot);
        if (dragon.dragonType == DragonType.FIRE) return armor.FIRETEXTURE;
        if (dragon.dragonType == DragonType.ICE) return armor.ICETEXTURE;
        return armor.LIGHTNINGTEXTURE;
    }

    private static ResourceLocation netheriteTexture(EntityDragonBase dragon, EquipmentSlot slot) {
        if (slot == EquipmentSlot.HEAD) {
            if (dragon.dragonType == DragonType.ICE) return ICE_NETHERITE_HEAD;
            if (dragon.dragonType == DragonType.LIGHTNING) return LIGHTNING_NETHERITE_HEAD;
        }
        if (slot == EquipmentSlot.LEGS && dragon.dragonType == DragonType.LIGHTNING) {
            return LIGHTNING_NETHERITE_BODY;
        }
        return NETHERITE_TEXTURES.get(slot);
    }

    private static ResourceLocation netheriteTexture(String part, String speciesSuffix) {
        return new ResourceLocation(IceAndFireBondBeyond.MOD_ID,
                "textures/models/dragon_armor/armor_" + part + "_netherite"
                        + speciesSuffix + ".png");
    }

    private static Map<EquipmentSlot, ResourceLocation> createNetheriteTextures() {
        EnumMap<EquipmentSlot, ResourceLocation> textures = new EnumMap<>(EquipmentSlot.class);
        String[] parts = {"head", "neck", "body", "tail"};
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            textures.put(ARMOR_SLOTS[i], netheriteTexture(parts[i], ""));
        }
        return textures;
    }
}
