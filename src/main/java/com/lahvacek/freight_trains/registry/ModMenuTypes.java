package com.lahvacek.freight_trains.registry;

import com.lahvacek.freight_trains.menu.StationRequesterMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(BuiltInRegistries.MENU, "createfreighttrains");

    public static final DeferredHolder<MenuType<?>, MenuType<StationRequesterMenu>> STATION_REQUESTER_MENU =
        MENUS.register("station_requester_menu",
            () -> IMenuTypeExtension.create((windowId, inv, data) -> new StationRequesterMenu(windowId, inv, data)));
}