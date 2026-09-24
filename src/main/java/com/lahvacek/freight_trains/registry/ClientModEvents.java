package com.lahvacek.freight_trains.registry;

import com.lahvacek.freight_trains.client.screen.StationRequesterScreen;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

// @EventBusSubscriber zajistí, že si toho NeoForge všimne při startu
// value = Dist.CLIENT je extrémně důležité - říká, že tento kód se nemá spouštět na dedikovaném serveru!
@EventBusSubscriber(modid = "createfreighttrains", value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        // Zde řekneme hře: "Když server pošle STATION_REQUESTER_MENU, otevři na monitoru StationRequesterScreen"
        event.register(ModMenuTypes.STATION_REQUESTER_MENU.get(), StationRequesterScreen::new);
    }

    @SubscribeEvent
public static void onClientExtensions(RegisterClientExtensionsEvent event) {
    event.registerFluidType(new IClientFluidTypeExtensions() {
        // Půjčíme si textury obyčejné vody
        private static final ResourceLocation STILL = ResourceLocation.parse("minecraft:block/water_still");
        private static final ResourceLocation FLOWING = ResourceLocation.parse("minecraft:block/water_flow");

        @Override
        public ResourceLocation getStillTexture() {
            return STILL;
        }

        @Override
        public ResourceLocation getFlowingTexture() {
            return FLOWING;
        }

        @Override
        public int getTintColor() {
            // Hexadecimální kód pro tmavě oranžovo-hnědou barvu guláše (AARRGGBB)
            // FF na začátku znamená plnou neprůhlednost
            return 0xFF993300;
        }
    }, ModFluids.GOULASH_TYPE.get());
}
}