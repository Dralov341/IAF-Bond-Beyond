package com.iceandfirebondbeyond.client.model;

import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import com.iceandfirebondbeyond.entity.SeaSerpentEggEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class SeaSerpentEggModel<T extends SeaSerpentEggEntity>
        extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(IceAndFireBondBeyond.MOD_ID, "sea_serpent_egg"),
            "main"
    );

    private final ModelPart egg1;

    public SeaSerpentEggModel(ModelPart root) {
        this.egg1 = root.getChild("Egg1");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        PartDefinition egg1 = root.addOrReplaceChild(
                "Egg1",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(
                                -3.0F, -2.8F, -3.0F,
                                6.0F, 6.0F, 6.0F,
                                new CubeDeformation(0.0F)
                        ),
                PartPose.offset(0.0F, 19.6F, 0.0F)
        );

        PartDefinition egg2 = egg1.addOrReplaceChild(
                "Egg2",
                CubeListBuilder.create().texOffs(0, 12)
                        .addBox(
                                -2.5F, -0.6F, -2.5F,
                                5.0F, 5.0F, 5.0F,
                                new CubeDeformation(0.0F)
                        ),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        PartDefinition egg3 = egg2.addOrReplaceChild(
                "Egg3",
                CubeListBuilder.create().texOffs(20, 12)
                        .addBox(
                                -2.5F, 5.4F, -2.5F,
                                5.0F, 5.0F, 5.0F,
                                new CubeDeformation(0.0F)
                        ),
                PartPose.offset(0.0F, -10.0F, 0.0F)
        );

        egg3.addOrReplaceChild(
                "Egg4",
                CubeListBuilder.create().texOffs(24, 0)
                        .addBox(
                                -2.0F, 5.2F, -2.0F,
                                4.0F, 4.0F, 4.0F,
                                new CubeDeformation(0.0F)
                        ),
                PartPose.offset(0.0F, -0.9F, 0.0F)
        );

        // The native fin atlas paints only the local -X face. Mirror the
        // left cube so both painted faces sit at matching distances from
        // the shell, then bury their roots slightly to avoid a visible gap.
        egg1.addOrReplaceChild(
                "HeadFinL",
                CubeListBuilder.create().mirror().texOffs(14, 22)
                        .addBox(
                                8.5F, 25.0F, 5.0F,
                                1.0F, 6.0F, 6.0F,
                                new CubeDeformation(0.0F)
                        ),
                PartPose.offsetAndRotation(
                        1.15F, -8.0F, -28.6F,
                        1.5708F, -0.4189F, -3.1416F
                )
        );

        egg1.addOrReplaceChild(
                "HeadFinR",
                CubeListBuilder.create().texOffs(0, 22)
                        .addBox(
                                -9.5F, 25.0F, 5.0F,
                                1.0F, 6.0F, 6.0F,
                                new CubeDeformation(0.0F)
                        ),
                PartPose.offsetAndRotation(
                        -1.15F, -8.0F, -28.6F,
                        1.5708F, 0.4189F, 3.1416F
                )
        );

        return LayerDefinition.create(meshDefinition, 64, 64);
    }

    @Override
    public void setupAnim(
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        egg1.xRot = 0.0F;
        egg1.yRot = 0.0F;
        egg1.zRot = 0.0F;

        if (entity.isIncubating()) {
            egg1.xRot = -Mth.cos(ageInTicks * 0.3F + 1.0F) * 0.3F;
            egg1.zRot = Mth.cos(ageInTicks * 0.3F) * 0.3F;
        }
    }

    @Override
    public void renderToBuffer(
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        egg1.render(
                poseStack,
                vertexConsumer,
                packedLight,
                packedOverlay,
                red,
                green,
                blue,
                alpha
        );
    }
}
