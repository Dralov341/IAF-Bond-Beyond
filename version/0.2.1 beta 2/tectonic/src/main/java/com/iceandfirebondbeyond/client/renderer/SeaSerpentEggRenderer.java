package com.iceandfirebondbeyond.client.renderer;

import com.github.alexthe666.iceandfire.enums.EnumSeaSerpent;
import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import com.iceandfirebondbeyond.client.model.SeaSerpentEggModel;
import com.iceandfirebondbeyond.entity.SeaSerpentEggEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

public final class SeaSerpentEggRenderer extends LivingEntityRenderer<
        SeaSerpentEggEntity,
        SeaSerpentEggModel<SeaSerpentEggEntity>
        > {

    private static final Map<EnumSeaSerpent, ResourceLocation> TEXTURES =
            new EnumMap<>(EnumSeaSerpent.class);

    static {
        for (EnumSeaSerpent variant : EnumSeaSerpent.values()) {
            TEXTURES.put(
                    variant,
                    new ResourceLocation(
                            IceAndFireBondBeyond.MOD_ID,
                            "textures/entity/sea_serpent_egg/"
                                    + (variant == EnumSeaSerpent.DEEPBLUE ? "darkblue" : variant.resourceName)
                                    + ".png"
                    )
            );
        }
    }

    public SeaSerpentEggRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new SeaSerpentEggModel<>(
                        context.bakeLayer(SeaSerpentEggModel.LAYER_LOCATION)
                ),
                0.3F
        );
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(SeaSerpentEggEntity entity) {
        return TEXTURES.get(entity.getVariant());
    }

    @Override
    protected boolean shouldShowName(SeaSerpentEggEntity entity) {
        return entity.shouldShowName() && entity.hasCustomName();
    }
}
