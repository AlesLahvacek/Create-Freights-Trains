package com.lahvacek.freight_trains.block; // Uprav podle svého přesného balíčku

import com.lahvacek.freight_trains.registry.ModBlocks;
import com.lahvacek.freight_trains.util.CargoEntry;
import com.lahvacek.freight_trains.util.CargoPoolManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class StationRequesterEntity extends BlockEntity {

    // --- DATOVÝ MODEL (Až 4 položky) ---
    public final List<ItemStack> requestedItems = new ArrayList<>();
    public final int[] targetAmounts = new int[4];
    public final int[] currentAmounts = new int[4];
    private int stationLevel = 1;
    private UUID stationId;
    private BlockPos boundPos;

    // --- HODNOTY PRO PLOT ---
    private BlockPos plotPos = null;
    private Rotation plotRotation = Rotation.NONE;
    private String plotType = "empty"; // "empty", "mine", "farm",...
    private int plotTier = 0;

    // --- SYNCHRONIZACE S KLIENTEM ---
    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index < 4) return currentAmounts[index]; // Index 0-3: Current Amount
            if (index < 8) return targetAmounts[index - 4]; // Index 4-7: Target Amount
            return 0;
        }

        @Override
        public void set(int index, int value) {
            if (index < 4) currentAmounts[index] = value;
            else if (index < 8) targetAmounts[index - 4] = value;
        }

        @Override
        public int getCount() { return 8; }
    };

    public StationRequesterEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.STATION_REQUESTER_BE.get(), pos, state);
    }

    // --- GENERÁTOR KONTRAKTŮ ---
    public void generateNewContract() {
        RandomSource random = (this.level != null) ? this.level.random : RandomSource.create();

        // Vyčištění předchozího stavu
        this.requestedItems.clear();
        for (int i = 0; i < 4; i++) {
            this.targetAmounts[i] = 0;
            this.currentAmounts[i] = 0;
        }

        // Načtení poolu přes existující JSON manažer[cite: 4]
        List<CargoEntry> availablePool = new ArrayList<>(CargoPoolManager.INSTANCE.getPoolForLevel(this.stationLevel));
        if (availablePool.isEmpty()) return;

        Collections.shuffle(availablePool); // Zamícháme pro náhodný výběr[cite: 4]

        // Zvolíme 2 až 4 různé položky
        int typesToRequest = random.nextInt(3) + 2;
        typesToRequest = Math.min(typesToRequest, availablePool.size());
        typesToRequest = Math.min(typesToRequest, 4); // Bezpečnostní limit na max 4

        for (int i = 0; i < typesToRequest; i++) {
            CargoEntry chosen = availablePool.get(i);
            ResourceLocation itemId = ResourceLocation.parse(chosen.itemId());
            Item item = BuiltInRegistries.ITEM.get(itemId);
            int amount = 0;
            if (stationLevel < 3) {
                amount = chosen.minAmount() + random.nextInt((((int)chosen.maxAmount()/2) - ((int)chosen.minAmount()/2)) + 1);
            }else{
                amount = chosen.minAmount() + random.nextInt((chosen.maxAmount() - chosen.minAmount()) + 1);
            }
            this.requestedItems.add(new ItemStack(item));
            this.targetAmounts[i] = amount;
        }

        this.setChanged();
        System.out.println("Vygenerován kontrakt pro Station Requester Lvl " + this.stationLevel + " s " + typesToRequest + " položkami.");
    }

    // --- INVENTÁŘ A PŘIJÍMACÍ LOGIKA ---
    private final IItemHandler itemHandler = new IItemHandler() {
        @Override
        public int getSlots() { return 1; }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            // 1. Zjistíme, jestli a na jakém indexu předmět požadujeme
            int matchedIndex = -1;
            for (int i = 0; i < requestedItems.size(); i++) {
                if (ItemStack.isSameItem(stack, requestedItems.get(i))) {
                    matchedIndex = i;
                    break;
                }
            }

            // Pokud ho nechceme, vrátíme ho beze změny
            if (matchedIndex == -1) return stack;

            // 2. Kontrola zbývajícího místa
            int spaceLeft = targetAmounts[matchedIndex] - currentAmounts[matchedIndex];
            if (spaceLeft <= 0) return stack;

            int toInsert = Math.min(stack.getCount(), spaceLeft);

            if (!simulate) {
                currentAmounts[matchedIndex] += toInsert;
                setChanged(); // Nutné pro uložení chunku[cite: 4]
                checkContractCompletion();
            }

            // 3. Vrátíme případný přebytek[cite: 4]
            ItemStack remainder = stack.copy();
            remainder.shrink(toInsert);
            return remainder;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) { return 64; }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) { return true; }
    };

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    // --- KONTROLA SPLNĚNÍ KONTRAKTU ---
    private void checkContractCompletion() {
        if (requestedItems.isEmpty()) return;

        // Pokud alespoň u jednoho itemu nemáme cíl, přerušíme metodu
        for (int i = 0; i < requestedItems.size(); i++) {
            if (currentAmounts[i] < targetAmounts[i]) {
                return;
            }
        }

        // Všechny položky jsou splněny
        this.stationLevel++;

        if (this.level != null && !this.level.isClientSide()) {
            // Provizorní odměna
            ItemStack reward = new ItemStack(Items.EMERALD, 5);
            ItemEntity rewardEntity = new ItemEntity(this.level,
                getBlockPos().getX() + 0.5,
                getBlockPos().getY() + 1.2,
                getBlockPos().getZ() + 0.5,
                reward
            );
            rewardEntity.setDeltaMovement(0, 0.2, 0);
            this.level.addFreshEntity(rewardEntity);

            // Okamžité vygenerování nového úkolu
            generateNewContract();
        }
    }

    // --- DATA EMPTY PLOTU ---
    public void findVillagePlot() { // Finds empty plot in train station's generated village
    if (!(this.level instanceof ServerLevel serverLevel) || this.plotPos != null) return;

    // Získáme strukturu vesnice, ve které se tento StationRequester nachází
    StructureStart start = serverLevel.structureManager().getStructureWithPieceAt(this.worldPosition, StructureTags.VILLAGE);

    if (start.isValid()) {
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof PoolElementStructurePiece poolPiece) {
                String pieceName = poolPiece.getElement().toString();

                // Hledáme náš dílek prázdné parcely
                if (pieceName.contains("createfreighttrains:plot_empty")) {
                    this.plotPos = poolPiece.getPosition();
                    this.plotRotation = poolPiece.getRotation();
                    this.setChanged();
                    System.out.println("Found parcel at: " + this.plotPos + " rotation: " + this.plotRotation);
                    break;
                }
            }
        }
    }
}

    // --- SERIALIZACE (Ukládání na disk) ---
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("StationLevel", this.stationLevel);
        if (this.stationId != null) {
            tag.putUUID("StationId", this.stationId);
        }
        if (this.boundPos != null) {
            tag.putLong("BoundPos", this.boundPos.asLong());
        }

        if (this.boundPos != null) {
            tag.putLong("BoundPos", this.boundPos.asLong());
        }
        if (this.plotPos != null) {
            tag.putLong("PlotPos", this.plotPos.asLong());
        }
        tag.putString("PlotRotation", this.plotRotation.name());
        tag.putString("PlotType", this.plotType);
        tag.putInt("PlotTier", this.plotTier);

        // Uložení pole předmětů přes ListTag[cite: 4]
        ListTag itemsTag = new ListTag();
        for (ItemStack stack : this.requestedItems) {
            itemsTag.add(stack.save(registries));
        }
        tag.put("RequestedItems", itemsTag);

        // Uložení číselných polí
        tag.putIntArray("TargetAmounts", this.targetAmounts);
        tag.putIntArray("CurrentAmounts", this.currentAmounts);
    }

    // --- DESERIALIZACE (Načítání z disku) ---
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains("StationLevel")) {
            this.stationLevel = tag.getInt("StationLevel");
        }
        if (tag.hasUUID("StationId")) {
            this.stationId = tag.getUUID("StationId");
        }
        if (tag.contains("BoundPos", Tag.TAG_LONG)) {
            this.boundPos = BlockPos.of(tag.getLong("BoundPos"));
        }

        // Načtení pole předmětů[cite: 4]
        this.requestedItems.clear();
        if (tag.contains("RequestedItems", Tag.TAG_LIST)) {
            ListTag itemsTag = tag.getList("RequestedItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < itemsTag.size(); i++) {
                this.requestedItems.add(ItemStack.parse(registries, itemsTag.getCompound(i)).orElse(ItemStack.EMPTY));
            }
        }

        // Načtení číselných polí
        if (tag.contains("TargetAmounts")) {
            int[] loadedTargets = tag.getIntArray("TargetAmounts");
            System.arraycopy(loadedTargets, 0, this.targetAmounts, 0, Math.min(loadedTargets.length, 4));
        }
        if (tag.contains("CurrentAmounts")) {
            int[] loadedCurrents = tag.getIntArray("CurrentAmounts");
            System.arraycopy(loadedCurrents, 0, this.currentAmounts, 0, Math.min(loadedCurrents.length, 4));
        }

        if (tag.contains("BoundPos")) {
            this.boundPos = BlockPos.of(tag.getLong("BoundPos"));
        }
        if (tag.contains("PlotPos")) {
            this.plotPos = BlockPos.of(tag.getLong("PlotPos"));
        } else {
            this.plotPos = null;
        }

        if (tag.contains("PlotRotation")) {
            try {
                this.plotRotation = Rotation.valueOf(tag.getString("PlotRotation"));
            } catch (IllegalArgumentException e) {
                this.plotRotation = Rotation.NONE;
            }
        }

        if (tag.contains("PlotType")) {
            this.plotType = tag.getString("PlotType");
        }
        if (tag.contains("PlotTier")) {
            this.plotTier = tag.getInt("PlotTier");
        }
    }

    // --- SPUŠTĚNÍ PŘI POLOŽENÍ ---
    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()) {
            if (!this.getBlockPos().equals(this.boundPos)) {
                this.boundPos = this.getBlockPos();
                this.plotPos = null;
                this.plotRotation = Rotation.NONE;
                this.plotType = "empty";
                this.plotTier = 0;
                this.generateNewContract();
                this.setChanged();
            } else if (this.requestedItems.isEmpty()) {
                this.generateNewContract();
            }
            if (this.plotPos == null) {
                findVillagePlot();
            }
        }
    }

    public UUID getStationId() {
        return stationId;
    }

    public List<ItemStack> getRequestedItems() {
        return requestedItems;
    }

    public int[] getTargetAmount() {
        return targetAmounts;
    }

    public int[] getCurrentAmount() {
        return currentAmounts;
    }

    public int getStationLevel() {
        return stationLevel;
    }

    public ContainerData getData() {
        return data;
    }


}