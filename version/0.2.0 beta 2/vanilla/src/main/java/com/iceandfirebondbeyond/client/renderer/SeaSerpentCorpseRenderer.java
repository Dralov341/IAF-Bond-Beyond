package com.iceandfirebondbeyond.client.renderer;

import com.iceandfirebondbeyond.client.model.SeaSerpentRemainsModel;
import com.iceandfirebondbeyond.entity.SeaSerpentCorpseEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class SeaSerpentCorpseRenderer extends EntityRenderer<SeaSerpentCorpseEntity> {
    private static final String[] COLORS = {"blue", "bronze", "darkblue", "green", "purple", "red", "teal"};
    private static final ResourceLocation BONES = new ResourceLocation("iceandfire_bond_beyond:textures/entity/sea_serpent_skeleton.png");
    private static final ResourceLocation SKULL = new ResourceLocation("iceandfire_bond_beyond:textures/entity/sea_serpent_skull.png");
    private final SeaSerpentRemainsModel flesh = new SeaSerpentRemainsModel(SeaSerpentRemainsModel.createFleshLayer().bakeRoot());
    private final SeaSerpentRemainsModel skeleton = new SeaSerpentRemainsModel(SeaSerpentRemainsModel.createSkeletonLayer().bakeRoot());
    private final ModelPart skull = SeaSerpentRemainsModel.createSkullLayer().bakeRoot();
    public SeaSerpentCorpseRenderer(EntityRendererProvider.Context context) { super(context); }
    public static ResourceLocation fleshTexture(int variant) {
        return new ResourceLocation("iceandfire:textures/models/seaserpent/seaserpent_"
                + COLORS[Math.max(0, Math.min(COLORS.length - 1, variant))] + "_blink.png");
    }
    @Override public ResourceLocation getTextureLocation(SeaSerpentCorpseEntity entity) {
        return fleshTexture(entity.getVariant());
    }
    @Override public void render(SeaSerpentCorpseEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        float scale = entity.getScale();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        pose.scale(-scale, -scale, scale);
        if (entity.isSkull()) {
            // Center the full snout inside the clickable box and put the jaw
            // directly on the block instead of suspending it .22 * scale up.
            pose.translate(0, -.5 / 16, 6.0 / 16);
            skull.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(SKULL)), light, OverlayTexture.NO_OVERLAY);
        } else {
            pose.translate(0, -1.501, 0);
            SeaSerpentRemainsModel model = entity.isSkeleton() ? skeleton : flesh;
            model.pose(entity.getRemainsPose(partial));
            model.root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(entity.isSkeleton() ? BONES : getTextureLocation(entity))),
                    light, OverlayTexture.NO_OVERLAY);
            if (entity.isSkeleton()) {
                pose.pushPose(); model.translateToHead(pose);
                skull.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(SKULL)), light, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
        }
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }
}
