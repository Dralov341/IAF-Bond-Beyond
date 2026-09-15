package com.iceandfirebondbeyond.client.screen;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.iceandfirebondbeyond.inventory.SeaSerpentMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

public final class SeaSerpentScreen
        extends AbstractContainerScreen<SeaSerpentMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("iceandfire", "textures/gui/dragon.png");
    private static final ItemStack EMPTY_ARMOR_ICON = new ItemStack(Items.SADDLE);

    public SeaSerpentScreen(
            SeaSerpentMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title);
        imageHeight = 214;
    }

    @Override
    public void render(
            @NotNull GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(
            @NotNull GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        // Profile labels are rendered in screen coordinates in renderBg.
    }

    @Override
    protected void renderBg(
            @NotNull GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int left = (width - imageWidth) / 2;
        int top = (height - imageHeight) / 2;
        graphics.blit(TEXTURE, left, top, 0, 0, imageWidth, imageHeight);
        // The IAF dragon panel has five equipment frames. The integrated
        // armor-and-saddle design uses only the first frame.
        graphics.fill(left + 7, top + 35, left + 25, top + 72, 0xFFC6C6C6);
        graphics.fill(left + 151, top + 16, left + 171, top + 56, 0xFFC6C6C6);
        // Erase the dragon-helmet silhouette baked into IAF's panel.
        graphics.fill(left + 8, top + 18, left + 24, top + 34, 0xFF8B8B8B);
        if (!menu.getSlot(0).hasItem()) {
            graphics.renderItem(EMPTY_ARMOR_ICON, left + 8, top + 18);
        }

        EntitySeaSerpent serpent = menu.getSerpent();
        if (serpent == null) {
            return;
        }

        float inverseScale = 1.0F / Math.max(0.0001F, serpent.getSeaSerpentScale());
        Quaternionf rotation = new Quaternionf()
                .rotateY((float) Mth.lerp((float) mouseX / width, 0.0D, Math.PI))
                .rotateZ((float) Mth.lerp(
                        (float) mouseY / width,
                        Math.PI,
                        Math.PI + 0.2D
                ));
        InventoryScreen.renderEntityInInventory(
                graphics,
                left + 88,
                top + 55,
                Math.max(2, (int) (inverseScale * 23.0F)),
                rotation,
                null,
                serpent
        );

        Component displayName = serpent.hasCustomName()
                ? Component.translatable(
                "gui.iceandfire_bond_beyond.sea_serpent.name",
                serpent.getCustomName()
        )
                : Component.translatable(
                "gui.iceandfire_bond_beyond.sea_serpent.unnamed"
        );
        drawCentered(graphics, displayName, left, top + 75);
        drawCentered(
                graphics,
                Component.translatable(
                        "gui.iceandfire_bond_beyond.sea_serpent.health",
                        Mth.floor(Math.min(serpent.getHealth(), serpent.getMaxHealth())),
                        Mth.floor(serpent.getMaxHealth())
                ),
                left,
                top + 84
        );
        drawCentered(
                graphics,
                Component.translatable(
                        "gui.iceandfire_bond_beyond.sea_serpent.gender",
                        Component.translatable(
                                menu.isMale()
                                        ? "gui.iceandfire_bond_beyond.gender.male"
                                        : "gui.iceandfire_bond_beyond.gender.female"
                        )
                ),
                left,
                top + 93
        );
        drawCentered(
                graphics,
                Component.translatable(
                        "gui.iceandfire_bond_beyond.sea_serpent.hunger",
                        menu.getHunger(),
                        menu.getMaxHunger()
                ),
                left,
                top + 102
        );
        drawCentered(
                graphics,
                Component.translatable(
                        "gui.iceandfire_bond_beyond.sea_serpent.stage",
                        menu.getStage(),
                        menu.getDays(),
                        menu.isAgingDisabled()
                                ? Component.translatable(
                                "gui.iceandfire_bond_beyond.sea_serpent.frozen"
                        )
                                : Component.empty()
                ),
                left,
                top + 111
        );
        drawCentered(
                graphics,
                Component.translatable(
                        "gui.iceandfire_bond_beyond.sea_serpent.owner",
                        menu.getOwnerName()
                ),
                left,
                top + 120
        );
    }

    private void drawCentered(
            GuiGraphics graphics,
            Component text,
            int left,
            int y
    ) {
        graphics.drawString(
                font,
                text,
                left + imageWidth / 2 - font.width(text) / 2,
                y,
                0xFFFFFF,
                false
        );
    }
}
