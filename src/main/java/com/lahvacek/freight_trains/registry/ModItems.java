package com.lahvacek.freight_trains.registry;

import com.lahvacek.freight_trains.item.DestinationCardItem;
import com.lahvacek.freight_trains.item.WayBillItem;

import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.item.Item;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("createfreighttrains");

    // --- BLOCK ITEMS ---
    public static final DeferredItem<BlockItem> STATION_REQUESTER_ITEM = ITEMS.registerSimpleBlockItem(
        "station_requester",
        ModBlocks.STATION_REQUESTER
    );
    public static final DeferredItem<BlockItem> CARGO_INSPECTOR_ITEM = ITEMS.registerSimpleBlockItem(
        "cargo_inspector", 
        ModBlocks.CARGO_INSPECTOR
    );
    public static final DeferredItem<BlockItem> DEPOT_TERMINAL = ITEMS.registerSimpleBlockItem(
        "depot_terminal",
        ModBlocks.DEPOT_TERMINAL);

    // --- ITEMS ---
    public static final DeferredItem<Item> DESTINATION_CARD = ITEMS.register("destination_card", 
    () -> new DestinationCardItem(new Item.Properties()
            .stacksTo(1)));
    public static final DeferredItem<Item> WAYBILL = ITEMS.register("waybill", 
    () -> new WayBillItem(new Item.Properties()
            .stacksTo(1)));

}
