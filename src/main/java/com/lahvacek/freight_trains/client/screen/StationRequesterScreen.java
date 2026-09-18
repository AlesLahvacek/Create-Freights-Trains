package com.lahvacek.freight_trains.client.screen; // Uprav podle svého balíčku

import com.lahvacek.freight_trains.menu.StationRequesterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class StationRequesterScreen extends AbstractContainerScreen<StationRequesterMenu> {

    // Cesta k textuře tvého okna
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath("createfreighttrains", "textures/gui/station_requester.png");

    private boolean isUpgradeTab = false;
    private Button contractButton;
    private Button upgradeButton;

    public StationRequesterScreen(StationRequesterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 200;
    }

    @Override
    protected void init() {
        super.init();

        // Tlačítko Contract (levé)
        this.contractButton = this.addRenderableWidget(Button.builder(Component.literal("Contract"), button -> {
            this.isUpgradeTab = false;
            this.updateButtonStates();
        }).bounds(this.leftPos + 10, this.topPos + this.imageHeight - 25, 110, 20).build());

        // Tlačítko Upgrade (pravé)
        this.upgradeButton = this.addRenderableWidget(Button.builder(Component.literal("Upgrade"), button -> {
            this.isUpgradeTab = true;
            this.updateButtonStates();
        }).bounds(this.leftPos + 136, this.topPos + this.imageHeight - 25, 110, 20).build());

        this.updateButtonStates();
    }

    private void updateButtonStates() {
        this.contractButton.active = this.isUpgradeTab;
        this.upgradeButton.active = !this.isUpgradeTab;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {

        if (!this.isUpgradeTab) {
            // --- JSME NA ZÁLOŽCE CONTRACT ---
            guiGraphics.drawString(this.font, "Contract", 20, 10, 0x404040, false);
            guiGraphics.drawString(this.font, "Reward", 160, 10, 0x404040, false);

            // --- LEVÝ PANEL: Mřížka požadovaných itemů (max 4) ---
            int startX = 35; // Střed levého sloupce
            int startY = 35; // Výchozí Y souřadnice pro první řádek
            int colSpacing = 60; // Mezera mezi sloupci
            int rowSpacing = 45; // Mezera mezi řádky

            for (int i = 0; i < this.menu.requestedItems.size(); i++) {
                ItemStack item = this.menu.requestedItems.get(i);
                if (item.isEmpty()) continue;

                // Matematika pro určení sloupce (0 nebo 1) a řádku (0 nebo 1)
                int col = i % 2;
                int row = i / 2;

                // Výpočet přesného středu pro tento konkrétní item
                int centerX = startX + (col * colSpacing);
                int currentY = startY + (row * rowSpacing);

                // 1. Vykreslení ikony (-8 posouvá ikonu na střed, protože má 16x16)
                guiGraphics.renderItem(item, centerX - 8, currentY);

                // 2. Název Itemu zarovnaný na střed
                String itemName = item.getHoverName().getString();
                int nameWidth = this.font.width(itemName);
                guiGraphics.drawString(this.font, itemName, centerX - (nameWidth / 2), currentY + 20, 0x404040, false);

                // 3. Postup (např. 0/512)
                String progress = this.menu.getCurrentAmount(i) + "/" + this.menu.getTargetAmount(i);
                int progressWidth = this.font.width(progress);
                guiGraphics.drawString(this.font, progress, centerX - (progressWidth / 2), currentY + 30, 0x404040, false);
            }

            // --- PRAVÝ PANEL: Odměna ---
            if (!this.menu.rewardItem.isEmpty()) {
                guiGraphics.renderItem(this.menu.rewardItem, 170, 40);
                guiGraphics.drawString(this.font, this.menu.rewardItem.getCount() + "x", 190, 45, 0x404040, false);
            }

        } else {
            // --- JSME NA ZÁLOŽCE UPGRADE ---
            String comingSoon = "Upgrade panel - Ve vývoji";
            int textWidth = this.font.width(comingSoon);
            guiGraphics.drawString(this.font, comingSoon, (this.imageWidth - textWidth) / 2, 40, 0x404040, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}