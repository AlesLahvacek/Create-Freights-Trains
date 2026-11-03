package com.lahvacek.freight_trains.menu;

import com.lahvacek.freight_trains.block.StationRequesterEntity;
import com.lahvacek.freight_trains.block.StationRequesterEntity.BuildingType;
import com.lahvacek.freight_trains.block.StationRequesterEntity.PlotState;
import java.util.ArrayList;
import java.util.List;

import com.lahvacek.freight_trains.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class StationRequesterMenu extends AbstractContainerMenu {

    public final BlockPos blockPos;
    public final List<ItemStack> requestedItems = new ArrayList<>();
    public final List<ItemStack> upgradeRequestedItems = new ArrayList<>();
    public final List<ItemStack> rewardItems = new ArrayList<>();
    public final ItemStack rewardItem;
    private final ContainerData data;
    private final SimpleContainer contractItems = new SimpleContainer(7);
    private final StationRequesterEntity requester;
    private PlotState lastClientState = PlotState.EMPTY;
    // Konstruktor volaný na KLIENTOVI
    public StationRequesterMenu(int windowId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(ModMenuTypes.STATION_REQUESTER_MENU.get(), windowId);

        this.blockPos = buf.readBlockPos();

        int itemCount = buf.readInt();
        for (int i = 0; i < itemCount; i++) {
            this.requestedItems.add(ItemStack.STREAM_CODEC.decode(buf));
        }

        int upgradeItemCount = buf.readInt();
        for (int i = 0; i < upgradeItemCount; i++) {
            this.upgradeRequestedItems.add(ItemStack.STREAM_CODEC.decode(buf));
        }
        this.rewardItem = ItemStack.STREAM_CODEC.decode(buf);
        if (!this.rewardItem.isEmpty()) this.rewardItems.add(this.rewardItem);
        this.requester = null;
        syncContractItems();
        this.data = new SimpleContainerData(19);
        this.addDataSlots(this.data);
        addContractSlots();
    }

    // Konstruktor volaný na SERVERU
    public StationRequesterMenu(int windowId, Inventory playerInventory, BlockPos pos,
                                List<ItemStack> reqItems, List<ItemStack> upgradeItems,
                                ItemStack reward, ContainerData data) {
        this(windowId, playerInventory, pos, reqItems, upgradeItems,
                reward.isEmpty() ? List.of() : List.of(reward), data, null);
    }

    public StationRequesterMenu(int windowId, Inventory playerInventory, List<ItemStack> reqItems,
                                List<ItemStack> rewItems, ContainerData data,
                                StationRequesterEntity requester) {
        this(windowId, playerInventory, requester.getBlockPos(), reqItems, List.of(),
                rewItems, data, requester);
    }

    private StationRequesterMenu(int windowId, Inventory playerInventory, BlockPos pos,
                                 List<ItemStack> reqItems, List<ItemStack> upgradeItems,
                                 List<ItemStack> rewItems, ContainerData data,
                                 StationRequesterEntity requester) {
        super(ModMenuTypes.STATION_REQUESTER_MENU.get(), windowId);
        this.blockPos = pos;
        this.requestedItems.addAll(reqItems);
        this.upgradeRequestedItems.addAll(upgradeItems);
        this.rewardItem = rewItems.isEmpty() ? ItemStack.EMPTY : rewItems.get(0);
        this.rewardItems.addAll(rewItems);
        this.data = data;
        this.requester = requester;
        syncContractItems();
        this.addDataSlots(this.data);
        addContractSlots();
    }

    private void addContractSlots() {
        for (int i = 0; i < 7; i++) {
            this.addSlot(new ContractSlot(this.contractItems, i));
        }
    }

    private void syncContractItems() {
        for (int i = 0; i < 4; i++) {
            ItemStack item = i < requestedItems.size() ? requestedItems.get(i) : ItemStack.EMPTY;
            contractItems.setItem(i, item.copy());
        }
        for (int i = 0; i < 3; i++) {
            ItemStack item = i < rewardItems.size() ? rewardItems.get(i) : ItemStack.EMPTY;
            contractItems.setItem(i + 4, item.copy());
        }
    }

    private void syncContractItemsFromRequester() {
        requestedItems.clear();
        requestedItems.addAll(requester.getRequestedItems());
        upgradeRequestedItems.clear();
        upgradeRequestedItems.addAll(requester.getUpgradeRequestedItems());
        rewardItems.clear();
        rewardItems.addAll(requester.getRewardItems());
        syncContractItems();
    }

    @Override
    public void broadcastChanges() {
        if (requester != null) {
            syncContractItemsFromRequester();
        }
        super.broadcastChanges();
    }

    public ItemStack getRequestedItem(int index) {
        return contractItems.getItem(index);
    }

    public ItemStack getRewardItem(int index) {
        return index == 0 ? rewardItem : ItemStack.EMPTY;
    }

    // Pomocné metody pro Screen
    public int getCurrentAmount(int index) { return this.data.get(index); }
    public int getTargetAmount(int index) { return this.data.get(index + 4); }
    public int getStationLevel() { return this.data.get(8); }

    public ItemStack getUpgradeItem(int index) {
        return index < upgradeRequestedItems.size()
                ? upgradeRequestedItems.get(index) : ItemStack.EMPTY;
    }

    public int getUpgradeCurrentAmount(int index) { return this.data.get(index + 9); }
    public int getUpgradeTargetAmount(int index) { return this.data.get(index + 13); }

    public PlotState getPlotState() {
        int stateOrd = this.data.get(17);
        PlotState[] values = PlotState.values();
        return stateOrd >= 0 && stateOrd < values.length ? values[stateOrd] : PlotState.EMPTY;
    }

    public BuildingType getBuildingType() {
        int typeOrd = this.data.get(18);
        BuildingType[] values = BuildingType.values();
        return typeOrd >= 0 && typeOrd < values.length ? values[typeOrd] : BuildingType.NONE;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 1 || id == 2) {
            BuildingType selected = id == 1 ? BuildingType.MINE : BuildingType.FARM;

            if (!player.level().isClientSide()) {
                BlockEntity blockEntity = player.level().getBlockEntity(this.blockPos);
                if (blockEntity instanceof StationRequesterEntity requester) {
                    requester.startBuildingConstruction(selected);
                }
            } else {
                populateClientUpgradePreview(selected);
            }
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    public void populateClientUpgradePreview(BuildingType selected) {
        this.upgradeRequestedItems.clear();
        if (selected == BuildingType.MINE) {
            this.upgradeRequestedItems.add(new ItemStack(Items.COBBLESTONE));
            this.upgradeRequestedItems.add(new ItemStack(Items.OAK_PLANKS));
            this.upgradeRequestedItems.add(new ItemStack(Items.IRON_INGOT));
            this.upgradeRequestedItems.add(new ItemStack(Items.LANTERN));
        } else if (selected == BuildingType.FARM) {
            this.upgradeRequestedItems.add(new ItemStack(Items.OAK_LOG));
            this.upgradeRequestedItems.add(new ItemStack(Items.OAK_FENCE));
            this.upgradeRequestedItems.add(new ItemStack(Items.DIRT));
            this.upgradeRequestedItems.add(new ItemStack(Items.HAY_BLOCK));
        }
    }

    public void syncClientUpgradeItems() {
        PlotState currentState = getPlotState();
        BuildingType currentType = getBuildingType();

        if (currentState != this.lastClientState && currentType != BuildingType.NONE) {
            this.lastClientState = currentState;

            if (currentState == PlotState.CONSTRUCTING) {
                populateClientUpgradePreview(currentType);
            } else if (currentState == PlotState.ACTIVE || currentState == PlotState.HALTED) {
                this.upgradeRequestedItems.clear();
                if (currentType == BuildingType.MINE) {
                    this.upgradeRequestedItems.add(new ItemStack(Items.OAK_LOG));
                    this.upgradeRequestedItems.add(new ItemStack(Items.COAL));
                } else if (currentType == BuildingType.FARM) {
                    this.upgradeRequestedItems.add(new ItemStack(Items.BONE_MEAL));
                    this.upgradeRequestedItems.add(new ItemStack(Items.WHEAT_SEEDS));
                }
            }
        }
    }


    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // Nemáme sloty, takže tu nic neděláme
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // Okno je platné, dokud ho hráč nezavře
    }

    private static class ContractSlot extends Slot {
        private ContractSlot(SimpleContainer container, int slot) {
            super(container, slot, -100, -100);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}