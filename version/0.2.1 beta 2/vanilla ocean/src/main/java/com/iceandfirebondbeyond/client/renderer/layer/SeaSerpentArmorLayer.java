package com.iceandfirebondbeyond.client.renderer.layer;

import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.TabulaModel;
import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import com.iceandfirebondbeyond.client.model.SeaSerpentArmorModel;
import com.iceandfirebondbeyond.item.SeaSerpentArmorItem;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

public final class SeaSerpentArmorLayer extends RenderLayer<
        EntitySeaSerpent,
        AdvancedEntityModel<EntitySeaSerpent>
        > {
    private static final Map<SeaSerpentArmorItem.ArmorTier, ResourceLocation>
            ARMOR_TEXTURES = createTextureMap();
    private static final Map<SeaSerpentArmorItem.ArmorTier, ResourceLocation> FIN_TEXTURES = createFinTextureMap();

    private final SeaSerpentArmorModel<EntitySeaSerpent> model;

    public SeaSerpentArmorLayer(
            RenderLayerParent<
                    EntitySeaSerpent,
                    AdvancedEntityModel<EntitySeaSerpent>
                    > parent,
            SeaSerpentArmorModel<EntitySeaSerpent> model
    ) {
        super(parent);
        this.model = model;
    }

    @Override
    public void render(
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource buffer,
            int packedLight,
            @NotNull EntitySeaSerpent serpent,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        SeaSerpentArmorItem.ArmorTier armor =
                SeaSerpentBondData.getArmorTier(serpent);
        Object parentModel = getParentModel();
        if (armor == null || serpent.isInvisible()
                || !(parentModel instanceof TabulaModel parent)) {
            return;
        }

        model.bindTo(parent);
        ResourceLocation texture = ARMOR_TEXTURES.get(armor);
        VertexConsumer vertexConsumer = buffer.getBuffer(
                RenderType.entityCutoutNoCull(texture)
        );
        model.renderToBuffer(
                poseStack,
                vertexConsumer,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );
        model.renderFinArmor(parent, poseStack, buffer.getBuffer(RenderType.entityCutoutNoCullZOffset(FIN_TEXTURES.get(armor))),
                packedLight, OverlayTexture.NO_OVERLAY);
    }

    private static Map<SeaSerpentArmorItem.ArmorTier, ResourceLocation> createFinTextureMap() {
        EnumMap<SeaSerpentArmorItem.ArmorTier, ResourceLocation> textures = new EnumMap<>(SeaSerpentArmorItem.ArmorTier.class);
        for (var tier : SeaSerpentArmorItem.ArmorTier.values()) textures.put(tier,
                new ResourceLocation(IceAndFireBondBeyond.MOD_ID, "textures/entity/sea_serpent_armor/sea_serpent_fin_armor_"
                        + tier.getTextureName() + ".png"));
        return textures;
    }

    private static Map<SeaSerpentArmorItem.ArmorTier, ResourceLocation>
    createTextureMap() {
        EnumMap<SeaSerpentArmorItem.ArmorTier, ResourceLocation> textures =
                new EnumMap<>(SeaSerpentArmorItem.ArmorTier.class);
        for (SeaSerpentArmorItem.ArmorTier tier
                : SeaSerpentArmorItem.ArmorTier.values()) {
            textures.put(tier, new ResourceLocation(
                    IceAndFireBondBeyond.MOD_ID,
                    "textures/entity/sea_serpent_armor/sea_serpent_armor_"
                            + tier.getTextureName()
                            + "_256x256.png"
            ));
        }
        return textures;
    }
}
