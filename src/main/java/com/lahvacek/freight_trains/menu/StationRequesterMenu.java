package com.lahvacek.freight_trains.menu;

import com.lahvacek.freight_trains.block.StationRequesterEntity;
import java.util.ArrayList;
import java.util.List;

import com.lahvacek.freight_trains.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class StationRequesterMenu extends AbstractContainerMenu {

    public final List<ItemStack> requestedItems = new ArrayList<>();
    public final List<ItemStack> rewardItems = new ArrayList<>();
    private final ContainerData data;
    private final SimpleContainer contractItems = new SimpleContainer(7);
    private final StationRequesterEntity requester;

    // Konstruktor volaný na KLIENTOVI
    public StationRequesterMenu(int windowId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(ModMenuTypes.STATION_REQUESTER_MENU.get(), windowId);

        int itemCount = buf.readInt();
        for (int i = 0; i < itemCount; i++) {
            this.requestedItems.add(ItemStack.STREAM_CODEC.decode(buf));
        }

        int rewardCount = buf.readInt();
        for (int i = 0; i < rewardCount; i++) {
            this.rewardItems.add(ItemStack.STREAM_CODEC.decode(buf));
        }
        this.requester = null;
        syncContractItems();
        this.data = new SimpleContainerData(8);
        this.addDataSlots(this.data);
        addContractSlots();
    }

    // Konstruktor volaný na SERVERU
    public StationRequesterMenu(int windowId, Inventory playerInventory, List<ItemStack> reqItems,
                                List<ItemStack> rewItems, ContainerData data,
                                StationRequesterEntity requester) {
        super(ModMenuTypes.STATION_REQUESTER_MENU.get(), windowId);
        this.requestedItems.addAll(reqItems);
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
        return contractItems.getItem(index + 4);
    }

    // Pomocné metody pro Screen
    public int getCurrentAmount(int index) { return this.data.get(index); }
    public int getTargetAmount(int index) { return this.data.get(index + 4); }



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