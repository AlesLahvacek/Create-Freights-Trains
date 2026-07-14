package com.lahvacek.freight_trains.registry;

import com.lahvacek.freight_trains.util.CargoPoolManager;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;


public class ModCapabilities {
    
    public static void registerCapabilities(RegisterCapabilitiesEvent event){
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlocks.STATION_REQUESTER_BE.get(), (blockEntity, side) -> blockEntity.getItemHandler());
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(CargoPoolManager.INSTANCE);
    }
    
}
