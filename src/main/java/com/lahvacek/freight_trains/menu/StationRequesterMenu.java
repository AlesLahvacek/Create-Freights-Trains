package com.lahvacek.freight_trains.menu;

import com.lahvacek.freight_trains.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class StationRequesterMenu extends AbstractContainerMenu {

    public final ItemStack requestedItem;
    public final ItemStack rewardItem;
    private final ContainerData data; // Přidáno

    // Konstruktor volaný na KLIENTOVI
    public StationRequesterMenu(int windowId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(ModMenuTypes.STATION_REQUESTER_MENU.get(), windowId);

        this.requestedItem = ItemStack.STREAM_CODEC.decode(buf);
        this.rewardItem = ItemStack.STREAM_CODEC.decode(buf);

        // Klient si vytvoří prázdná data, která mu server bude automaticky přepisovat
        this.data = new SimpleContainerData(2);
        this.addDataSlots(this.data); // TOTO JE KLÍČOVÉ PRO ŽIVOU SYNCHRONIZACI
    }

    // Konstruktor volaný na SERVERU
    public StationRequesterMenu(int windowId, Inventory playerInventory, ItemStack reqItem, ItemStack rewItem, ContainerData data) {
        super(ModMenuTypes.STATION_REQUESTER_MENU.get(), windowId);
        this.requestedItem = reqItem;
        this.rewardItem = rewItem;
        this.data = data;

        this.addDataSlots(this.data); // TOTO JE KLÍČOVÉ
    }

    // Pomocné metody pro získání čísel v grafice
    public int getCurrentAmount() { return this.data.get(0); }
    public int getTargetAmount() { return this.data.get(1); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // Nemáme sloty, takže tu nic neděláme
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // Okno je platné, dokud ho hráč nezavře
    }
}