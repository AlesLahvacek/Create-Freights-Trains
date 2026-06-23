package com.lahvacek.freight_trains.registry;

import com.lahvacek.freight_trains.block.StationRequesterBlock;
import com.lahvacek.freight_trains.block.StationRequesterEntity;

// import com.lahvacek.freight_trains.block.StationAcceptorBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredBlock;

public class ModBlocks {
    
    public static final String MODID = "createfreighttrains";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);

    public static final DeferredBlock<Block> STATION_REQUESTER = BLOCKS.register("station_requester", () -> new StationRequesterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StationRequesterEntity>> STATION_REQUESTER_BE = BLOCK_ENTITIES.register("station_requester_be",
         () -> BlockEntityType.Builder.of(StationRequesterEntity::new, STATION_REQUESTER.get()).build(null));

}
