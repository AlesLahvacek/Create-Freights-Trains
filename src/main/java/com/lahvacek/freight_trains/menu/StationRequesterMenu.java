package com.lahvacek.freight_trains.menu;

import java.util.ArrayList;
import java.util.List;

import com.lahvacek.freight_trains.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class StationRequesterMenu extends AbstractContainerMenu {

    public final List<ItemStack> requestedItems = new ArrayList<>();
    public final ItemStack rewardItem;
    private final ContainerData data;

    // Konstruktor volaný na KLIENTOVI
    public StationRequesterMenu(int windowId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(ModMenuTypes.STATION_REQUESTER_MENU.get(), windowId);

        int itemCount = buf.readInt();
        for (int i = 0; i < itemCount; i++) {
            this.requestedItems.add(ItemStack.STREAM_CODEC.decode(buf));
        }

        this.rewardItem = ItemStack.STREAM_CODEC.decode(buf);
        this.data = new SimpleContainerData(8);
        this.addDataSlots(this.data);
    }

    // Konstruktor volaný na SERVERU
    public StationRequesterMenu(int windowId, Inventory playerInventory, List<ItemStack> reqItems, ItemStack rewItem, ContainerData data) {
        super(ModMenuTypes.STATION_REQUESTER_MENU.get(), windowId);
        this.requestedItems.addAll(reqItems);
        this.rewardItem = rewItem;
        this.data = data;
        this.addDataSlots(this.data);
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
}