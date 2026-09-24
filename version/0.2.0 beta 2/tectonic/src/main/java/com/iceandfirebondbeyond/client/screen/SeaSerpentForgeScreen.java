package com.iceandfirebondbeyond.client.screen;

import com.iceandfirebondbeyond.inventory.SeaSerpentForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class SeaSerpentForgeScreen extends AbstractContainerScreen<SeaSerpentForgeMenu> {
    public SeaSerpentForgeScreen(SeaSerpentForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageHeight = 185; inventoryLabelY = 91;
    }
    private void slot(GuiGraphics g, int x, int y) {
        g.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, 0xFF17272E);
        g.fill(leftPos + x, topPos + y, leftPos + x + 17, topPos + y + 17, 0xFFE6EFEC);
        g.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, 0xFF66818A);
    }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF172C37);
        g.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFFB7CACE);
        g.fill(leftPos + 5, topPos + 21, leftPos + imageWidth - 5, topPos + 87, 0xFF8FAAAF);
        slot(g, 44, 35); slot(g, 44, 62); slot(g, 116, 45);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) slot(g, 8 + col * 18, 103 + row * 18);
        for (int col = 0; col < 9; col++) slot(g, 8 + col * 18, 161);
        g.fill(leftPos + 73, topPos + 48, leftPos + 104, topPos + 57, 0xFF38505A);
        int length = 30 * menu.progress() / menu.duration();
        g.fill(leftPos + 73, topPos + 49, leftPos + 73 + length, topPos + 56, 0xFF42D5CE);
        g.drawString(font, Component.translatable("gui.iceandfire_bond_beyond.forge." +
                (!menu.assembled() ? "incomplete" : menu.powered() ? "working" : "waiting")), leftPos + 8, topPos + 24, 0x173847, false);
    }
    @Override public void render(GuiGraphics g, int x, int y, float partial) {
        renderBackground(g); super.render(g, x, y, partial); renderTooltip(g, x, y);
    }
}
