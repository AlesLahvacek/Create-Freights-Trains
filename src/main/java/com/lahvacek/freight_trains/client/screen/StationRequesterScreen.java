package com.lahvacek.freight_trains.client.screen;

import com.lahvacek.freight_trains.menu.StationRequesterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class StationRequesterScreen extends AbstractContainerScreen<StationRequesterMenu> {

    // Cesta k textuře tvého okna (budeš si ji muset nakreslit, podobně jako u bloků)
    // Ve verzi 1.21+ se používá fromNamespaceAndPath
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath("createfreighttrains", "textures/gui/station_requester.png");

    public StationRequesterScreen(StationRequesterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        // Zvětšíme šířku a výšku okna, aby se nám tam vešel levý i pravý panel
        this.imageWidth = 256;
        this.imageHeight = 200;
    }

    @Override
    protected void init() {
        super.init();

        // --- PŘIDÁNÍ TLAČÍTEK PODLE TVÉHO NÁVRHU ---
        // Tlačítko Contract (levé)
        this.addRenderableWidget(Button.builder(Component.literal("Contract"), button -> {
            // Zatím nedělá nic, protože už na této záložce jsme
        }).bounds(this.leftPos + 10, this.topPos + this.imageHeight - 25, 110, 20).build());

        // Tlačítko Upgrade (pravé)
        this.addRenderableWidget(Button.builder(Component.literal("Upgrade"), button -> {
            // Později sem přidáme logiku na přepnutí "stránky" nebo zakrytí prvků
        }).bounds(this.leftPos + 136, this.topPos + this.imageHeight - 25, 110, 20).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Vycentrování okna na obrazovce
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Vykreslení hlavní textury pozadí
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Vykreslování textů (Souřadnice jsou relativní k levému hornímu rohu našeho okna)
        guiGraphics.drawString(this.font, "Contract: Level 1", 20, 10, 0x404040, false);
        guiGraphics.drawString(this.font, "Reward", 160, 10, 0x404040, false);

        // --- LEVÝ PANEL: Požadované itemy ---
        if (!this.menu.requestedItem.isEmpty()) {
            // Vykreslí "hologram" itemu
            guiGraphics.renderItem(this.menu.requestedItem, 30, 40);

            // Text: Název itemu a postup (např. 0/64)
            String itemName = this.menu.requestedItem.getHoverName().getString();
            guiGraphics.drawString(this.font, itemName, 30, 60, 0x404040, false);
            guiGraphics.drawString(this.font, this.menu.currentAmount + "/" + this.menu.targetAmount, 30, 70, 0x404040, false);
        }

        // --- PRAVÝ PANEL: Odměna ---
        if (!this.menu.rewardItem.isEmpty()) {
            guiGraphics.renderItem(this.menu.rewardItem, 170, 40);
            guiGraphics.drawString(this.font, this.menu.rewardItem.getCount() + "x", 190, 45, 0x404040, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Vykreslí ztmavené pozadí za oknem
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // Vykreslí tooltipy (když hráč najede myší na item)
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}