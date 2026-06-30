package com.lahvacek.freight_trains.block;

import com.lahvacek.freight_trains.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CargoInspectorBlockEntity extends BlockEntity {

    public CargoInspectorBlockEntity(BlockPos pos, BlockState state) {
        // Zde si později doplníme správný odkaz na registraci
        super(ModBlocks.CARGO_INSPECTOR_BE.get(), pos, state);
    }
    
}
