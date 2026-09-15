package com.lahvacek.freight_trains.menu;

import com.lahvacek.freight_trains.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class StationRequesterMenu extends AbstractContainerMenu {

    // Tyto proměnné budou držet data pro vykreslení na obrazovce
    public final ItemStack requestedItem;
    public final int currentAmount;
    public final int targetAmount;
    public final ItemStack rewardItem; // Do budoucna pro pravý panel

    // Konstruktor volaný na KLIENTOVI (Když mu server pošle data)
    public StationRequesterMenu(int windowId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        super(ModMenuTypes.STATION_REQUESTER_MENU.get(), windowId);

        // Přečteme data, která nám poslal server při otevírání okna
        this.requestedItem = ItemStack.STREAM_CODEC.decode(data);
        this.currentAmount = data.readInt();
        this.targetAmount = data.readInt();
        this.rewardItem = ItemStack.STREAM_CODEC.decode(data);
    }

    // Konstruktor volaný na SERVERU (Při otevírání okna z bloku)
    public StationRequesterMenu(int windowId, Inventory playerInventory, ItemStack reqItem, int curAmt, int tarAmt, ItemStack rewItem) {
        super(ModMenuTypes.STATION_REQUESTER_MENU.get(), windowId);
        this.requestedItem = reqItem;
        this.currentAmount = curAmt;
        this.targetAmount = tarAmt;
        this.rewardItem = rewItem;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // Nemáme sloty, takže tu nic neděláme
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // Okno je platné, dokud ho hráč nezavře
    }
}