package com.lahvacek.freight_trains.block;

import java.util.*;

import com.lahvacek.freight_trains.registry.*;
import com.lahvacek.freight_trains.util.CargoEntry;
import com.lahvacek.freight_trains.util.CargoPoolManager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.stream.Collectors;

public class CargoInspectorBlockEntity extends BlockEntity {

    public CargoInspectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.CARGO_INSPECTOR_BE.get(), pos, state);
    }

    // 1. Záznam pro základní definici předmětů v poolu
    private record BaseCargo(Item item, int baseMin, int baseMax, int unlockLevel) {}
    private UUID stationId;
    private long cooldownUntil = 0;
    private boolean nextRewardPenalized = false;

    // List of possible cargo options
    // private static final List<BaseCargo> CARGO_POOL = List.of(
    //     new BaseCargo(Items.OAK_LOG, 100, 300, 1),
    //     new BaseCargo(Items.STONE, 200, 500, 1),
    //     new BaseCargo(Items.IRON_INGOT, 150, 400, 1),
    //     new BaseCargo(Items.COPPER_INGOT, 200, 500, 3),
    //     new BaseCargo(Items.REDSTONE, 300, 800, 5),
    //     new BaseCargo(Items.GOLD_INGOT, 50, 150, 5),
    //     new BaseCargo(Items.DIAMOND, 10, 30, 10)
    // );
    private int stationLevel = 1;
    
    // Map containing current values for delivery
    private final Map<Item, Integer> activeManifest = new HashMap<>();

    public void generateNewManifest() {
        if (this.level != null && this.level.isClientSide()) {
            return;
        }

        RandomSource random = (this.level != null) ? this.level.random : RandomSource.create();
        this.activeManifest.clear();

        if (!CargoPoolManager.INSTANCE.hasAnyEntries() && this.level != null) {
            CargoPoolManager.INSTANCE.loadFromResources(this.level.getServer().getResourceManager());
        }

        List<CargoEntry> availablePool = CargoPoolManager.INSTANCE.getPoolForLevel(this.stationLevel);

        if (availablePool.isEmpty()) {
            System.out.println("No cargo pool entries available for station level " + this.stationLevel + " yet.");
            return;
        }

        Collections.shuffle(availablePool);

        // current limiting is set to min of unlocked resources
        int numItemsRequested = random.nextInt(5) + 2; // 2 to 6
        numItemsRequested = Math.min(numItemsRequested, availablePool.size());

        for (int i = 0; i < numItemsRequested; i++) {
            CargoEntry entry = availablePool.get(this.level.random.nextInt(availablePool.size()));
            
            // Parsing String to minecraft item ID
            ResourceLocation itemId = ResourceLocation.parse(entry.itemId());
            Item item = BuiltInRegistries.ITEM.get(itemId);
            
            // Random amount of chosen Item (in min max boundaries) 
            int amount = entry.minAmount() + this.level.random.nextInt((entry.maxAmount() - entry.minAmount()) + 1);
            
            // Write to manifest (can accept same Item multiple times, in that case the values add up)
            this.activeManifest.put(item, this.activeManifest.getOrDefault(item, 0) + amount);
        }
        
        setChanged();
        
        System.out.println("Generated new manifest for level: " + stationLevel + " with " + numItemsRequested + " items!");
    }

    public void completeContract( boolean wasTooClose) {
        this.stationLevel++;
        
        if (this.level != null && !this.level.isClientSide()) {
            int emeraldCount = Math.min(this.stationLevel, 64); // Max 1 stack

            if (this.nextRewardPenalized) {
                emeraldCount = Math.max(1, emeraldCount / 2); 
                this.nextRewardPenalized = false;
            }

            

            ItemStack reward = new ItemStack(Items.EMERALD, emeraldCount);
            ItemEntity rewardEntity = new ItemEntity(this.level, 
                this.getBlockPos().getX() + 0.5, 
                this.getBlockPos().getY() + 1.2, 
                this.getBlockPos().getZ() + 0.5, 
                reward
            );
            rewardEntity.setDeltaMovement(0, 0.2, 0);
            this.level.addFreshEntity(rewardEntity);
            if (wasTooClose) {
                // 5 in-game days
                this.cooldownUntil = this.level.getGameTime() + (5 * 24000);
                this.nextRewardPenalized = true;
            }

            generateNewManifest();
            setChanged();
        }
    }

    public boolean isOnCooldown(long currentTime) {
        return currentTime < this.cooldownUntil;
    }

    public long getRemainingCooldownTicks(long currentTime) {
        return Math.max(0, this.cooldownUntil - currentTime);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("StationLevel", this.stationLevel);
        if (this.stationId != null) {
            tag.putUUID("StationId", this.stationId);
        }
        tag.putLong("CooldownUntil", this.cooldownUntil);
        tag.putBoolean("NextRewardPenalized", this.nextRewardPenalized);

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
        if (tag.hasUUID("StationId")) {
            this.stationId = tag.getUUID("StationId");
        }
        if (tag.contains("CooldownUntil")) {
            this.cooldownUntil = tag.getLong("CooldownUntil");
        }
        if (tag.contains("NextRewardPenalized")) {
            this.nextRewardPenalized = tag.getBoolean("NextRewardPenalized");
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

        if (this.stationId == null) {
            this.stationId = UUID.randomUUID();
            setChanged();
        }
    }

    // --- GETTERS ---
    public Map<Item, Integer> getActiveManifest() {
        return activeManifest;
    }

    public int getStationLevel() {
        return stationLevel;
    }

    public UUID getStationId() {
        return this.stationId;
    }
}
