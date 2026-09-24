package com.lahvacek.freight_trains.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModFluids {
    // Registr pro fyziku a vizuál tekutiny
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, "createfreighttrains");
    // Registr pro samotnou logiku tekutiny
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, "createfreighttrains");

    // Fyzikální vlastnosti guláše (hustší a pomalejší než voda)
    public static final DeferredHolder<FluidType, FluidType> GOULASH_TYPE = FLUID_TYPES.register("goulash",
        () -> new FluidType(FluidType.Properties.create().density(2000).viscosity(3000)));

    // Zdrojová a tekoucí varianta
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> GOULASH_SOURCE = FLUIDS.register("goulash",
        () -> new BaseFlowingFluid.Source(createProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> GOULASH_FLOWING = FLUIDS.register("flowing_goulash",
        () -> new BaseFlowingFluid.Flowing(createProperties()));

    // Propojení tekutiny s kbelíkem a blokem
    private static BaseFlowingFluid.Properties createProperties() {
        return new BaseFlowingFluid.Properties(GOULASH_TYPE, GOULASH_SOURCE, GOULASH_FLOWING)
                .bucket(ModItems.GOULASH_BUCKET)
                .block(ModBlocks.GOULASH_BLOCK);
    }
}