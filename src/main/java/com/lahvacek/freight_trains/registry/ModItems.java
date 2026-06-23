package com.lahvacek.freight_trains.registry;

import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItems {
    // 1. Definice registru ITEMS, kterou kompilátor nemohl najít
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("createfreighttrains");

    public static final DeferredItem<BlockItem> STATION_REQUESTER_ITEM = ITEMS.registerSimpleBlockItem(
        "station_requester",
        ModBlocks.STATION_REQUESTER
    );

}
