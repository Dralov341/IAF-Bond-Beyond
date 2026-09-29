package com.iceandfirebondbeyond.client.renderer;

import com.iceandfirebondbeyond.entity.SeaSteelArrowEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class SeaSteelArrowRenderer extends ArrowRenderer<SeaSteelArrowEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("iceandfire_bond_beyond:textures/entity/sea_serpent_steel_arrow.png");
    public SeaSteelArrowRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(SeaSteelArrowEntity arrow) { return TEXTURE; }
}
