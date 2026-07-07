package com.lahvacek.freight_trains.block;
import com.lahvacek.freight_trains.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DepotTerminalBlockEntity extends BlockEntity {
    
    private final Map<UUID, Integer> knownStations = new HashMap<>();

    public DepotTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.DEPOT_TERMINAL_BE.get(), pos, state);
    }

    public Map<UUID, Integer> getKnownStations() {
        return knownStations;
    }

    // update on new or higher station level
    public void updateStationLevel(UUID stationId, int level) {
        if (level > knownStations.getOrDefault(stationId, 0)) {
            knownStations.put(stationId, level);
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        
        CompoundTag stationsTag = new CompoundTag();
        for (Map.Entry<UUID, Integer> entry : knownStations.entrySet()) {
            stationsTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        tag.put("KnownStations", stationsTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        
        knownStations.clear();
        if (tag.contains("KnownStations")) {
            CompoundTag stationsTag = tag.getCompound("KnownStations");
            for (String key : stationsTag.getAllKeys()) {
                knownStations.put(UUID.fromString(key), stationsTag.getInt(key));
            }
        }
    }
}
