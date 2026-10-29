package com.lahvacek.freight_trains.util; // uprav package podle sebe

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import com.lahvacek.freight_trains.mixin.StructureTemplatePoolAccessor;
import com.lahvacek.freight_trains.CreateFreightTrains;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = "createfreighttrains")
public class StructureInjectionHandler {

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        RegistryAccess registryAccess = event.getServer().registryAccess();
        Registry<StructureTemplatePool> templatePoolRegistry = registryAccess.registryOrThrow(Registries.TEMPLATE_POOL);

        // Meadow vesnice používají plains pool, proto stačí registrovat vanilla varianty vesnic.
        for (String biome : List.of("plains", "desert", "savanna", "snowy", "taiga")) {
            injectIntoVillage(templatePoolRegistry, biome);
        }
    }

    private static void injectIntoVillage(Registry<StructureTemplatePool> registry, String biome) {
        ResourceLocation poolId = ResourceLocation.parse("minecraft:village/" + biome + "/houses");
        StructureTemplatePool pool = registry.get(poolId);
        if (pool == null) {
            CreateFreightTrains.LOGGER.warn("Could not find village template pool {}", poolId);
            return;
        }

        addPieceToPool(pool, "createfreighttrains:train_station", 50);
        addPieceToPool(pool, "createfreighttrains:plot_empty", 50);
        CreateFreightTrains.LOGGER.info("Injected train station into village pool {}", poolId);
    }

    private static void addPieceToPool(StructureTemplatePool pool, String location, int weight) {
        StructurePoolElement piece = StructurePoolElement.legacy(location)
                .apply(StructureTemplatePool.Projection.RIGID);

        // Přetypujeme pool na náš Accessor – tím obejdeme chybu "not visible"!
        StructureTemplatePoolAccessor accessor =
                (com.lahvacek.freight_trains.mixin.StructureTemplatePoolAccessor) pool;

        List<Pair<StructurePoolElement, Integer>> rawTemplates = new ArrayList<>(accessor.getRawTemplates());
        rawTemplates.add(Pair.of(piece, weight));
        accessor.setRawTemplates(rawTemplates);

        it.unimi.dsi.fastutil.objects.ObjectArrayList<StructurePoolElement> templates =
                new it.unimi.dsi.fastutil.objects.ObjectArrayList<>(accessor.getTemplates());
        for (int i = 0; i < weight; i++) {
            templates.add(piece);
        }
        accessor.setTemplates(templates);
    }
}