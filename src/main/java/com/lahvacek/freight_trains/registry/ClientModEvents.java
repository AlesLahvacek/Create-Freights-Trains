package com.lahvacek.freight_trains.registry;

import com.lahvacek.freight_trains.client.screen.StationRequesterScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

// @EventBusSubscriber zajistí, že si toho NeoForge všimne při startu
// value = Dist.CLIENT je extrémně důležité - říká, že tento kód se nemá spouštět na dedikovaném serveru!
@EventBusSubscriber(modid = "createfreighttrains", value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        // Zde řekneme hře: "Když server pošle STATION_REQUESTER_MENU, otevři na monitoru StationRequesterScreen"
        event.register(ModMenuTypes.STATION_REQUESTER_MENU.get(), StationRequesterScreen::new);
    }
}