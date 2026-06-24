package com.lahvacek.freight_trains.block;

import com.lahvacek.freight_trains.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.tags.ItemTags;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class StationRequesterEntity extends BlockEntity {

    private ItemStack RequestItem = ItemStack.EMPTY;
    private int targetAmount = 0;
    private int currentAmount = 0;
    private int stationLevel = 1;

    // Record containing pool of input items
    private record ContractOption(net.minecraft.world.item.Item item, int minAmount, int maxAmount, int unlockLevel){};
    private static final List<ContractOption> ALL_CONTRACTS = List.of(
        new ContractOption(Items.OAK_LOG, 64, 128, 1),
        new ContractOption(Items.STONE, 48, 256, 1),
        new ContractOption(Items.COAL, 32, 128, 1),
        new ContractOption(Items.WHEAT, 64, 300, 1),
        new ContractOption(Items.COBBLESTONE, 128, 512, 1),
        // LVL 5+
        new ContractOption(Items.IRON_INGOT, 48, 128, 5),
        new ContractOption(Items.COPPER_INGOT, 48, 192, 5),
        new ContractOption(Items.COAL_BLOCK, 12, 32, 5),
        new ContractOption(Items.BRICKS, 48, 128, 5),
        new ContractOption(Items.GOLD_INGOT, 16, 64, 5),
        // LVL 10+
        new ContractOption(Items.IRON_BLOCK, 12, 48, 10),
        new ContractOption(Items.GOLD_BLOCK, 6, 32, 10),
        new ContractOption(Items.DIAMOND, 12, 48 ,10 ),
        new ContractOption(Items.REDSTONE_BLOCK, 16, 64, 10),
        new ContractOption(Items.QUARTZ_BLOCK, 32, 128, 10)
    );

    public StationRequesterEntity(BlockPos pos, BlockState state){
        super(ModBlocks.STATION_REQUESTER_BE.get(), pos, state);

    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide() && this.RequestItem.isEmpty()) {
            generateNewContract();
        }
    }

    private final IItemHandler itemHandler = new IItemHandler() {
        @Override
        public int getSlots(){return 1;}

        @Override
        public @NotNull ItemStack getStackInSlot(int slot){return ItemStack.EMPTY;}

        // --- Block logic ---
        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {

        // check for item similarity
        if (RequestItem.isEmpty()) {
            return stack;
        }

        boolean isExactMatch = ItemStack.isSameItem(stack, RequestItem); // add more logic (coals, stones)
        boolean isBothLog = stack.is(ItemTags.LOGS) && RequestItem.is(ItemTags.LOGS);

        if (!isExactMatch && !isBothLog) {
            return stack;
        }

        // check for item size
        int spaceLeft = targetAmount - currentAmount;
        if (spaceLeft <= 0) return stack;

        int toInsert = Math.min(stack.getCount(), spaceLeft);

        // check for Create simulation
        if (!simulate) {
            currentAmount += toInsert;

            if (currentAmount >= targetAmount) {
                stationLevel++;
                currentAmount = 0;

                if (level != null && !level.isClientSide()) {
                ItemStack reward = new ItemStack(Items.EXPERIENCE_BOTTLE, 3);
                ItemEntity rewardEntity = new ItemEntity(level,
                    getBlockPos().getX() + 0.5,
                    getBlockPos().getY() + 1.2,
                    getBlockPos().getZ() + 0.5,
                    reward
                );

                rewardEntity.setDeltaMovement(0, 0.2, 0);
                level.addFreshEntity(rewardEntity);

                level.playSound(null, getBlockPos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0F, 1.2F);

                generateNewContract();
            }
        }
            setChanged();
            System.out.println("Delivered! Current state: " + currentAmount + " / " + targetAmount);
        }

        // return rest of the unused material
        ItemStack remainder = stack.copy();
        remainder.shrink(toInsert);
        return remainder;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate){
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot){ return 64;}

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack){ return true;}
    };

    // --- Item selection per level ---
    private void generateNewContract(){
        RandomSource random = (this.level != null) ? this.level.random : RandomSource.create();
        List<ContractOption> availablePool = ALL_CONTRACTS.stream().filter(contract -> this.stationLevel >= contract.unlockLevel()).toList();
        ContractOption chosen = availablePool.get(random.nextInt(availablePool.size()));

        double multiplier = 1.0 + (this.stationLevel * 0.15);
        double itemSpecificMultiplier = 1.0 + (this.stationLevel - chosen.unlockLevel() * 0.15);
        int finalMin = (int) (chosen.minAmount() * itemSpecificMultiplier);
        int finalMax = (int) (chosen.maxAmount() * itemSpecificMultiplier);
        this.RequestItem = new ItemStack(chosen.item());

        if (finalMax <= finalMin) {
            this.targetAmount = finalMin;
        } else {
            this.targetAmount = random.nextInt(finalMax - finalMin + 1) +finalMin;
        }
        setChanged();
        }


// --- DISK WRITE ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);


        if (!this.RequestItem.isEmpty()) {
            tag.put("RequestItem", this.RequestItem.save(registries));
        }

        tag.putInt("TargetAmount", this.targetAmount);
        tag.putInt("CurrentAmount", this.currentAmount);
        tag.putInt("StationLevel", this.stationLevel);
    }

// --- DISK READ ---

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);


        if (tag.contains("RequestItem")) {
            this.RequestItem = ItemStack.parse(registries, tag.getCompound("RequestItem")).orElse(ItemStack.EMPTY);
        }
        if (tag.contains("TargetAmount")) {
            this.targetAmount = tag.getInt("TargetAmount");
        }
        if (tag.contains("CurrentAmount")) {
            this.currentAmount = tag.getInt("CurrentAmount");
        }
        if (tag.contains("StationLevel")) {
            this.stationLevel = tag.getInt("StationLevel");
        }

    }

// --- GETTERS ---

    public ItemStack getRequestedItem() {
        return RequestItem;
    }

    public int getTargetAmount() {
        return targetAmount;
    }

    public int getCurrentAmount() {
        return currentAmount;
    }

    public int getStationLevel() {
        return stationLevel;
    }

    public IItemHandler getItemHandler(){
        return itemHandler;
    }

    // --- SETTERS ---

    public void setRequestedItem(ItemStack requestedItem) {
        this.RequestItem = requestedItem;
        setChanged(); // disk write after setting of value
    }

    public void setTargetAmount(int targetAmount) {
        this.targetAmount = targetAmount;
        setChanged();
    }

    public void setCurrentAmount(int currentAmount) {
        this.currentAmount = currentAmount;
        setChanged();
    }
}
