package com.lahvacek.freight_trains.client.screen;

import com.lahvacek.freight_trains.menu.StationRequesterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class StationRequesterScreen extends AbstractContainerScreen<StationRequesterMenu> {

    private boolean isUpgradeTab = false; // Výchozí stav: jsme na záložce Contract
    private Button contractButton;
    private Button upgradeButton;
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

        // Tlačítko Contract (levé)
        this.contractButton = this.addRenderableWidget(Button.builder(Component.literal("Contract"), button -> {
            this.isUpgradeTab = false; // Přepnutí stavu
            this.updateButtonStates(); // Aktualizace vzhledu tlačítek
        }).bounds(this.leftPos + 10, this.topPos + this.imageHeight - 25, 110, 20).build());

        // Tlačítko Upgrade (pravé)
        this.upgradeButton = this.addRenderableWidget(Button.builder(Component.literal("Upgrade"), button -> {
            this.isUpgradeTab = true; // Přepnutí stavu
            this.updateButtonStates(); // Aktualizace vzhledu tlačítek
        }).bounds(this.leftPos + 136, this.topPos + this.imageHeight - 25, 110, 20).build());

        // Zavoláme hned při otevření okna, aby se správné tlačítko deaktivovalo
        this.updateButtonStates();
    }

    // Pomocná metoda pro zašednutí aktivní záložky
    private void updateButtonStates() {
        this.contractButton.active = this.isUpgradeTab;  // Lze kliknout, pokud NEJSME na Contractech
        this.upgradeButton.active = !this.isUpgradeTab;  // Lze kliknout, pokud NEJSME na Upgradech
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!this.isUpgradeTab) {
             // Vykreslování textů (Souřadnice jsou relativní k levému hornímu rohu našeho okna)
        guiGraphics.drawString(this.font, "Contract: Level 1", 20, 10, 0x404040, false);
        guiGraphics.drawString(this.font, "Reward", 160, 10, 0x404040, false);

        // --- LEVÝ PANEL: Požadované itemy ---
        if (!this.menu.requestedItem.isEmpty()) {

            // Určíme si střed levého panelu.
            // Celá šířka okna je 256, levá polovina má 128, její střed je tedy na souřadnici X = 64.
            // (Můžeš si toto číslo mírně upravit, pokud to vůči tvé textuře nebude opticky sedět)
            int leftPanelCenter = 64;

            // 1. Vycentrování ikony Itemu (Item má 16x16 px, takže odečteme polovinu, tedy 8)
            guiGraphics.renderItem(this.menu.requestedItem, leftPanelCenter - 8, 40);

            // 2. Vycentrování názvu Itemu
            String itemName = this.menu.requestedItem.getHoverName().getString();
            int nameWidth = this.font.width(itemName); // Zjistí šířku textu v pixelech
            guiGraphics.drawString(this.font, itemName, leftPanelCenter - (nameWidth / 2), 60, 0x404040, false);

            // 3. Vycentrování číselného stavu (např. 0/513)
            String progress = this.menu.getCurrentAmount() + "/" + this.menu.getTargetAmount();
            int progressWidth = this.font.width(progress);
            guiGraphics.drawString(this.font, progress, leftPanelCenter - (progressWidth / 2), 70, 0x404040, false);
        }

        // --- PRAVÝ PANEL: Odměna ---
        if (!this.menu.rewardItem.isEmpty()) {
            guiGraphics.renderItem(this.menu.rewardItem, 170, 40);
            guiGraphics.drawString(this.font, this.menu.rewardItem.getCount() + "x", 190, 45, 0x404040, false);
        }
        } else {
            // --- JSME NA ZÁLOŽCE UPGRADE ---
            // Zde je zatím prázdno, přidáme jen zástupný text doprostřed
            String comingSoon = "Upgrade panel - Ve vývoji";
            int textWidth = this.font.width(comingSoon);
            guiGraphics.drawString(this.font, comingSoon, (this.imageWidth - textWidth) / 2, 40, 0x404040, false);
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

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    // Vycentrování okna na obrazovce
    int x = (this.width - this.imageWidth) / 2;
    int y = (this.height - this.imageHeight) / 2;

    // Vykreslení hlavní textury pozadí
    guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
}
}