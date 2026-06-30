package com.lahvacek.freight_trains.block;

import java.util.*;

import com.lahvacek.freight_trains.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.util.RandomSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.stream.Collectors;

public class CargoInspectorBlockEntity extends BlockEntity {

    public CargoInspectorBlockEntity(BlockPos pos, BlockState state) {
        // Zde si později doplníme správný odkaz na registraci
        super(ModBlocks.CARGO_INSPECTOR_BE.get(), pos, state);
    }

    // 1. Záznam pro základní definici předmětů v poolu
    private record BaseCargo(Item item, int baseMin, int baseMax, int unlockLevel) {}

    // List of possible cargo options
    private static final List<BaseCargo> CARGO_POOL = List.of(
        new BaseCargo(Items.OAK_LOG, 100, 300, 1),
        new BaseCargo(Items.STONE, 200, 500, 1),
        new BaseCargo(Items.IRON_INGOT, 150, 400, 1),
        new BaseCargo(Items.COPPER_INGOT, 200, 500, 3),
        new BaseCargo(Items.REDSTONE, 300, 800, 5),
        new BaseCargo(Items.GOLD_INGOT, 50, 150, 5),
        new BaseCargo(Items.DIAMOND, 10, 30, 10)
    );
    private int stationLevel = 1;
    
    // Map containing current values for delivery
    private final Map<Item, Integer> activeManifest = new HashMap<>();

    public void generateNewManifest() {
        RandomSource random = (this.level != null) ? this.level.random : RandomSource.create();
        this.activeManifest.clear();

        // level based filter
        List<BaseCargo> available = CARGO_POOL.stream()
            .filter(c -> this.stationLevel >= c.unlockLevel())
            .collect(Collectors.toList());

        Collections.shuffle(available);

        
        // current limiting is set to min of unlocked resources
        int typesToRequest = random.nextInt(5) + 2; // 2 to 6
        typesToRequest = Math.min(typesToRequest, available.size());

        for (int i = 0; i < typesToRequest; i++) {
            BaseCargo chosen = available.get(i);

            double multiplier = 1.0 + ((this.stationLevel - chosen.unlockLevel()) * 0.15);
            
            int min = (int) (chosen.baseMin() * multiplier);
            int max = (int) (chosen.baseMax() * multiplier);
            
            int finalAmount = (max <= min) ? min : random.nextInt(max - min + 1) + min;

            this.activeManifest.put(chosen.item(), finalAmount);
        }
        
        setChanged();
        
        System.out.println("Generated new manifest for level: " + stationLevel + " with " + typesToRequest + " items!");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("StationLevel", this.stationLevel);

        // Manifest saving
        ListTag manifestList = new ListTag();
        for (Map.Entry<Item, Integer> entry : this.activeManifest.entrySet()) {
            CompoundTag itemTag = new CompoundTag();

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(entry.getKey());
            itemTag.putString("Item", itemId.toString());
            itemTag.putInt("Amount", entry.getValue());
            
            manifestList.add(itemTag);
        }
        tag.put("Manifest", manifestList);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("StationLevel")) {
            this.stationLevel = tag.getInt("StationLevel");
        }

        // Nmanifest loading
        this.activeManifest.clear();
        if (tag.contains("Manifest", Tag.TAG_LIST)) {
            ListTag manifestList = tag.getList("Manifest", Tag.TAG_COMPOUND);
            for (int i = 0; i < manifestList.size(); i++) {
                CompoundTag itemTag = manifestList.getCompound(i);

                ResourceLocation itemId = ResourceLocation.parse(itemTag.getString("Item"));
                Item item = BuiltInRegistries.ITEM.get(itemId);
                int amount = itemTag.getInt("Amount");
                
                this.activeManifest.put(item, amount);
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide() && this.activeManifest.isEmpty()) {
            generateNewManifest();
        }
    }
}
