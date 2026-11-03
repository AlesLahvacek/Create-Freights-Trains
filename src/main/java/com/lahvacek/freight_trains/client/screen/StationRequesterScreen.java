package com.lahvacek.freight_trains.client.screen;

import com.lahvacek.freight_trains.block.StationRequesterEntity.BuildingType;
import com.lahvacek.freight_trains.block.StationRequesterEntity.PlotState;
import com.lahvacek.freight_trains.menu.StationRequesterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class StationRequesterScreen extends AbstractContainerScreen<StationRequesterMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("createfreighttrains",
                    "textures/gui/station_requester.png");

    private boolean isUpgradeTab;
    private BuildingType selectedBuilding = BuildingType.NONE;
    private Button contractButton;
    private Button upgradeButton;
    private Button selectMineButton;
    private Button selectFarmButton;
    private Button confirmBuildButton;

    public StationRequesterScreen(StationRequesterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 200;
    }

    @Override
    protected void init() {
        super.init();

        this.contractButton = this.addRenderableWidget(Button.builder(Component.literal("Contract"), button -> {
            this.isUpgradeTab = false;
            this.updateButtonStates();
        }).bounds(this.leftPos + 10, this.topPos + this.imageHeight - 25, 110, 20).build());

        this.upgradeButton = this.addRenderableWidget(Button.builder(Component.literal("Upgrade"), button -> {
            this.isUpgradeTab = true;
            this.updateButtonStates();
        }).bounds(this.leftPos + 136, this.topPos + this.imageHeight - 25, 110, 20).build());

        this.selectMineButton = this.addRenderableWidget(Button.builder(Component.literal("Mine"), button -> {
            this.selectedBuilding = BuildingType.MINE;
            this.updateButtonStates();
        }).bounds(this.leftPos + 23, this.topPos + 45, 90, 20).build());

        this.selectFarmButton = this.addRenderableWidget(Button.builder(Component.literal("Farm"), button -> {
            this.selectedBuilding = BuildingType.FARM;
            this.updateButtonStates();
        }).bounds(this.leftPos + 23, this.topPos + 75, 90, 20).build());

        this.confirmBuildButton = this.addRenderableWidget(Button.builder(Component.literal("Confirm Build"), button -> {
            if (this.selectedBuilding == BuildingType.NONE || this.minecraft == null
                    || this.minecraft.gameMode == null) {
                return;
            }
            int buttonId = this.selectedBuilding == BuildingType.MINE ? 1 : 2;
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
            this.menu.populateClientUpgradePreview(this.selectedBuilding);
            this.updateButtonStates();
        }).bounds(this.leftPos + 147, this.topPos + 130, 90, 20).build());

        this.updateButtonStates();
    }

    private void updateButtonStates() {
        this.contractButton.active = this.isUpgradeTab;
        this.upgradeButton.active = !this.isUpgradeTab;

        boolean showSelection = this.isUpgradeTab && this.menu.getPlotState() == PlotState.EMPTY;
        this.selectMineButton.visible = showSelection;
        this.selectFarmButton.visible = showSelection;
        this.confirmBuildButton.visible = showSelection;

        if (showSelection) {
            this.selectMineButton.active = this.selectedBuilding != BuildingType.MINE;
            this.selectFarmButton.active = this.selectedBuilding != BuildingType.FARM;
            this.confirmBuildButton.active = this.selectedBuilding != BuildingType.NONE;
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.menu.syncClientUpgradeItems();
        this.updateButtonStates();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int leftPanelCenter = 68;
        int rightPanelCenter = 192;

        if (!this.isUpgradeTab) {
            drawCenteredText(guiGraphics, "Contract", leftPanelCenter, 10, 0x404040);
            drawCenteredText(guiGraphics, "Reward", rightPanelCenter, 10, 0x404040);
            renderContractGrid(guiGraphics, this.menu.requestedItems, false);

            if (!this.menu.rewardItem.isEmpty()) {
                guiGraphics.renderItem(this.menu.rewardItem, rightPanelCenter - 8, 40);
                drawCenteredText(guiGraphics, this.menu.rewardItem.getCount() + "x",
                        rightPanelCenter, 60, 0x404040);
            }
            guiGraphics.drawString(this.font, "LEVEL: " + this.menu.getStationLevel(),
                    25, 160, 0x202020, false);
            return;
        }

        PlotState state = this.menu.getPlotState();
        BuildingType type = this.menu.getBuildingType();
        if (state == PlotState.EMPTY) {
            drawCenteredText(guiGraphics, "Select Upgrade", leftPanelCenter, 10, 0x404040);
            drawCenteredText(guiGraphics, "Details", rightPanelCenter, 10, 0x404040);
            guiGraphics.renderItem(new ItemStack(Items.IRON_PICKAXE), 5, 47);
            guiGraphics.renderItem(new ItemStack(Items.IRON_HOE), 5, 77);

            if (this.selectedBuilding == BuildingType.MINE) {
                drawCenteredText(guiGraphics, "Village Mine", rightPanelCenter, 35, 0x206020);
                drawCenteredText(guiGraphics, "Daily Output:", rightPanelCenter, 55, 0x404040);
                guiGraphics.renderItem(new ItemStack(Items.RAW_IRON), rightPanelCenter - 20, 70);
                guiGraphics.renderItem(new ItemStack(Items.COBBLESTONE), rightPanelCenter + 4, 70);
            } else if (this.selectedBuilding == BuildingType.FARM) {
                drawCenteredText(guiGraphics, "Village Farm", rightPanelCenter, 35, 0x206020);
                drawCenteredText(guiGraphics, "Daily Output:", rightPanelCenter, 55, 0x404040);
                guiGraphics.renderItem(new ItemStack(Items.WHEAT), rightPanelCenter - 20, 70);
                guiGraphics.renderItem(new ItemStack(Items.CARROT), rightPanelCenter + 4, 70);
            } else {
                drawCenteredText(guiGraphics, "Choose a building", rightPanelCenter, 65, 0x707070);
            }
            return;
        }

        String title = state == PlotState.CONSTRUCTING ? "Build Materials" : "Daily Upkeep";
        drawCenteredText(guiGraphics, title, leftPanelCenter, 10, 0x404040);
        drawCenteredText(guiGraphics, "Status", rightPanelCenter, 10, 0x404040);
        renderContractGrid(guiGraphics, this.menu.upgradeRequestedItems, true);

        String buildingName = type == BuildingType.MINE ? "Mine" : "Farm";
        drawCenteredText(guiGraphics, "Building: " + buildingName, rightPanelCenter, 35, 0x404040);
        if (state == PlotState.CONSTRUCTING) {
            drawCenteredText(guiGraphics, "CONSTRUCTING", rightPanelCenter, 55, 0xB8860B);
            drawCenteredText(guiGraphics, "Deliver materials", rightPanelCenter, 80, 0x404040);
            drawCenteredText(guiGraphics, "to finish build.", rightPanelCenter, 92, 0x404040);
        } else if (state == PlotState.ACTIVE) {
            drawCenteredText(guiGraphics, "ACTIVE", rightPanelCenter, 55, 0x208020);
            drawCenteredText(guiGraphics, "Produces resources", rightPanelCenter, 80, 0x404040);
            drawCenteredText(guiGraphics, "at end of day.", rightPanelCenter, 92, 0x404040);
        } else if (state == PlotState.HALTED) {
            drawCenteredText(guiGraphics, "HALTED!", rightPanelCenter, 55, 0xB02020);
            drawCenteredText(guiGraphics, "Upkeep missing!", rightPanelCenter, 80, 0xB02020);
            drawCenteredText(guiGraphics, "Supply items.", rightPanelCenter, 92, 0x404040);
        }
    }

    private void renderContractGrid(GuiGraphics guiGraphics, List<ItemStack> items, boolean upgrade) {
        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            if (item.isEmpty()) continue;

            int centerX = 40 + (i % 2) * 56;
            int currentY = 35 + (i / 2) * 45;
            guiGraphics.renderItem(item, centerX - 8, currentY);

            String itemName = item.getHoverName().getString();
            guiGraphics.pose().pushPose();
            float scale = 0.8f;
            guiGraphics.pose().scale(scale, scale, 1.0f);
            guiGraphics.drawString(this.font, itemName, (int) (centerX / scale)
                    - this.font.width(itemName) / 2, (int) ((currentY + 20) / scale),
                    0x404040, false);
            guiGraphics.pose().popPose();

            int current = upgrade ? this.menu.getUpgradeCurrentAmount(i) : this.menu.getCurrentAmount(i);
            int target = upgrade ? this.menu.getUpgradeTargetAmount(i) : this.menu.getTargetAmount(i);
            int color = current >= target && target > 0 ? 0x208020 : 0x404040;
            drawCenteredText(guiGraphics, current + "/" + target, centerX, currentY + 30, color);
        }
    }

    private void drawCenteredText(GuiGraphics guiGraphics, String text, int centerX, int y, int color) {
        guiGraphics.drawString(this.font, text, centerX - this.font.width(text) / 2, y, color, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
