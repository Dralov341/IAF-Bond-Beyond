package com.iceandfirebondbeyond.client.renderer;

import com.iceandfirebondbeyond.entity.SeaSerpentRiderBubbleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** Render the actual projectile, using IAF's bubble texture at its synced hitbox size. */
public final class SeaSerpentBubbleRenderer extends EntityRenderer<SeaSerpentRiderBubbleEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("iceandfire", "textures/particles/sea_serpent_bubble.png");

    public SeaSerpentBubbleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(@NotNull SeaSerpentRiderBubbleEntity bubble, float yaw, float partialTick,
            @NotNull PoseStack stack, @NotNull MultiBufferSource buffers, int light) {
        float radius = bubble.getBubbleDiameter() * 0.5F;
        stack.pushPose();
        stack.translate(0, radius, 0);
        stack.mulPose(entityRenderDispatcher.cameraOrientation());
        stack.scale(radius, radius, radius);
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        vertex(vertices, stack.last(), -1, -1, 0, 1, light);
        vertex(vertices, stack.last(), 1, -1, 1, 1, light);
        vertex(vertices, stack.last(), 1, 1, 1, 0, light);
        vertex(vertices, stack.last(), -1, 1, 0, 0, light);
        stack.popPose();
        super.render(bubble, yaw, partialTick, stack, buffers, light);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose,
            float x, float y, float u, float v, int light) {
        vertices.vertex(pose.pose(), x, y, 0).color(255, 255, 255, 230)
                .uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose.normal(), 0, 0, 1).endVertex();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SeaSerpentRiderBubbleEntity bubble) {
        return TEXTURE;
    }
}
