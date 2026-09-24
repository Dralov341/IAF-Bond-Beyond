package com.iceandfirebondbeyond.client.model;

import com.github.alexthe666.citadel.client.model.TabulaModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Only fin roots/rays are opaque in this atlas; native membranes show through. */
public final class SeaSerpentFinArmorModel {
    private static final String[] FINS = {"TailFinT4", "TailFinT3", "TailFinT2", "TailFinT1", "TailFin4", "TailFin3", "TailFin2", "TailFin1", "BodyFinLower", "NeckFin3", "HeadFinL", "HeadFinR", "NeckFin2", "NeckFin1", "BodyFinUpper"};
    private static final int[] PARENTS = {7, 7, 7, 6, 5, 4, 3, 2, 1, 10, 11, 11, 9, 8, 0};
    private final ModelPart[] pieces = new ModelPart[FINS.length];
    private final Matrix3f normal = new Matrix3f();
    public SeaSerpentFinArmorModel() {
        ModelPart root = createLayer().bakeRoot();
        for (int i = 0; i < FINS.length; i++) pieces[i] = root.getChild(FINS[i]);
    }
    public void render(TabulaModel parent, Matrix4f[] bones, PoseStack pose, VertexConsumer consumer, int light, int overlay) {
        for (int i = 0; i < FINS.length; i++) {
            var live = parent.getCube(FINS[i]);
            ModelPart fin = pieces[i];
            fin.setPos(live.rotationPointX, live.rotationPointY, live.rotationPointZ);
            fin.setRotation(live.rotateAngleX, live.rotateAngleY, live.rotateAngleZ);
            pose.pushPose();
            pose.last().pose().mul(bones[PARENTS[i]]);
            pose.last().normal().mul(normal.set(bones[PARENTS[i]]));
            fin.render(pose, consumer, light, overlay);
            pose.popPose();
        }
    }
    private static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("TailFinT4", CubeListBuilder.create().texOffs(200, 49).addBox(-0.000000F, -0.400000F, -2.000000F, 1.000000F, 6.000000F, 8.000000F, new CubeDeformation(0.0F)), PartPose.ZERO);
        root.addOrReplaceChild("TailFinT3", CubeListBuilder.create().texOffs(170, 55).mirror().addBox(-0.980000F, -1.000000F, -2.200000F, 1.000000F, 5.000000F, 5.000000F, new CubeDeformation(0.0F)), PartPose.ZERO);
        root.addOrReplaceChild("TailFinT2", CubeListBuilder.create().texOffs(185, 42).addBox(0.020000F, -1.000000F, -2.900000F, 1.000000F, 5.000000F, 6.000000F, new CubeDeformation(0.0F)), PartPose.ZERO);
        root.addOrReplaceChild("TailFinT1", CubeListBuilder.create().texOffs(19, 2).addBox(-0.000000F, -1.100000F, -1.700000F, 1.000000F, 5.000000F, 9.000000F, new CubeDeformation(0.0F)), PartPose.ZERO);
        root.addOrReplaceChild("TailFin4", CubeListBuilder.create().texOffs(173, 0).addBox(-0.000000F, -0.400000F, -6.200000F, 1.000000F, 5.000000F, 9.000000F, new CubeDeformation(0.0F)), PartPose.ZERO);
        root.addOrReplaceChild("TailFin3", CubeListBuilder.create().texOffs(173, 0).addBox(-0.000000F, -0.400000F, -5.200000F, 1.000000F, 5.000000F, 9.000000F, new CubeDeformation(0.0F)), PartPose.ZERO);
        root.addOrReplaceChild("TailFin2", CubeListBuilder.create().texOffs(154, 0).addBox(-0.000000F, -0.400000F, -4.000000F, 1.000000F, 5.000000F, 7.000000F, new CubeDeformation(0.0F)), PartPose.ZERO);
        root.addOrReplaceChild("TailFin1", CubeListBuilder.create().texOffs(139, 0).addBox(-0.000000F, -0.400000F, -3.800000F, 1.000000F, 6.000000F, 6.000000F, new CubeDeformation(0.0F)), PartPose.ZERO);
        root.addOrReplaceChild("BodyFinLower", CubeListBuilder.create().texOffs(120, 0).addBox(-0.000000F, -0.400000F, -3.500000F, 1.000000F, 5.000000F, 7.000000F, new CubeDeformation(0.0F)), PartPose.ZERO);
        root.addOrReplaceChild("NeckFin3", CubeListBuilder.create().texOffs(42, 2).addBox(-0.000000F, -0.400000F, -5.000000F, 1.000000F, 6.000000F, 5.000000F, new CubeDeformation(0.0F)), PartPose.ZERO);
        root.addOrReplaceChild("HeadFinL", CubeListBuilder.create().texOffs(200, 0).mirror().addBox(-0.500000F, 1.000000F, -3.000000F, 1.000000F, 6.000000F, 6.000000F, new CubeDeformation(0.0F)), PartPose.ZERO);
        root.addOrReplaceChild("HeadFinR", CubeListBuilder.create().texOffs(200, 0).addBox(-0.500000F, 1.000000F, -3.000000F, 1.000000F, 6.000000F, 6.000000F, new CubeDeformation(0.0F)), PartPose.ZERO);
        root.addOrReplaceChild("NeckFin2", CubeListBuilder.create().texOffs(62, 1).addBox(0.020000F, -0.400000F, -2.000000F, 1.000000F, 5.000000F, 9.000000F, new CubeDeformation(0.0F)), PartPose.ZERO);
        root.addOrReplaceChild("NeckFin1", CubeListBuilder.create().texOffs(80, 0).addBox(-0.000000F, 0.000000F, -2.000000F, 1.000000F, 5.000000F, 8.000000F, new CubeDeformation(0.0F)), PartPose.ZERO);
        root.addOrReplaceChild("BodyFinUpper", CubeListBuilder.create().texOffs(100, 0).addBox(-0.000000F, -0.400000F, -3.200000F, 1.000000F, 6.000000F, 7.000000F, new CubeDeformation(0.0F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 128);
    }
}
