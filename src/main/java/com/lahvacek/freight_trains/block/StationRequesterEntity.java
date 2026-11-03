package com.lahvacek.freight_trains.block;

import com.lahvacek.freight_trains.CreateFreightTrains;
import com.lahvacek.freight_trains.registry.ModBlocks;
import com.lahvacek.freight_trains.util.CargoEntry;
import com.lahvacek.freight_trains.util.CargoPoolManager;
import com.lahvacek.freight_trains.util.RewardEntry;
import com.lahvacek.freight_trains.util.RewardPoolManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class StationRequesterEntity extends BlockEntity {

    public enum PlotState {
        EMPTY, CONSTRUCTING, ACTIVE, HALTED
    }

    public enum BuildingType {
        NONE, MINE, FARM
    }

    // --- DATOVÝ MODEL (Až 4 položky) ---
    public final List<ItemStack> requestedItems = new ArrayList<>();
    private final List<ItemStack> rewardItems = new ArrayList<>();
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
    private long lastProcessedDay = -1;
    private PlotState plotState = PlotState.EMPTY;
    private BuildingType buildingType = BuildingType.NONE;

    public final List<ItemStack> upgradeRequestedItems = new ArrayList<>();
    public final int[] upgradeTargetAmounts = new int[4];
    public final int[] upgradeCurrentAmounts = new int[4];

    // --- SYNCHRONIZACE S KLIENTEM ---
    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index < 4) return currentAmounts[index]; // Index 0-3: Current Amount
            if (index < 8) return targetAmounts[index - 4]; // Index 4-7: Target Amount
            if (index == 8) return stationLevel;
            if (index < 13) return upgradeCurrentAmounts[index - 9];
            if (index < 17) return upgradeTargetAmounts[index - 13];
            if (index == 17) return plotState.ordinal();
            if (index == 18) return buildingType.ordinal();
            return 0;
        }

        @Override
        public void set(int index, int value) {
            if (index < 4) currentAmounts[index] = value;
            else if (index < 8) targetAmounts[index - 4] = value;
            else if (index == 8) stationLevel = value;
            else if (index < 13) upgradeCurrentAmounts[index - 9] = value;
            else if (index < 17) upgradeTargetAmounts[index - 13] = value;
            else if (index == 17 && value >= 0 && value < PlotState.values().length) {
                plotState = PlotState.values()[value];
            } else if (index == 18 && value >= 0 && value < BuildingType.values().length) {
                buildingType = BuildingType.values()[value];
            }
        }

        @Override
        public int getCount() { return 19; }
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

        if (!RewardPoolManager.INSTANCE.hasAnyEntries() && this.level != null) {
            RewardPoolManager.INSTANCE.loadFromResources(this.level.getServer().getResourceManager());
        }
        int rewardCount = random.nextInt(3) + 1;
        switch (stationLevel) {
            case 1,2:
                rewardCount = 1;
                break;
            case 3:
                rewardCount = 2;
                break;
            case 4,5:
                rewardCount = 3;
                break;
            default:
                break;
        }
        this.rewardItems.clear();
        List<RewardEntry> availableRewards = new ArrayList<>(
                RewardPoolManager.INSTANCE.getPoolForLevel(this.stationLevel));
        Collections.shuffle(availableRewards);
        rewardCount = Math.min(rewardCount, availableRewards.size());
        for (int i = 0; i < rewardCount; i++) {
            ItemStack reward = createReward(availableRewards.get(i));
            if (!reward.isEmpty()) {
                this.rewardItems.add(reward);
            }
        }
        Collections.shuffle(availablePool); // Zamícháme pro náhodný výběr[cite: 4]

        // Zvolíme 2 až 4 různé položky
        int typesToRequest = random.nextInt(3) + 2;
        typesToRequest = Math.min(typesToRequest, availablePool.size());
        typesToRequest = Math.min(typesToRequest, 4); // Bezpečnostní limit na max 4

        for (int i = 0; i < typesToRequest; i++) {
            CargoEntry chosen = availablePool.get(i);
            ResourceLocation itemId = ResourceLocation.parse(chosen.itemId());
            Item item = BuiltInRegistries.ITEM.get(itemId);
            int amount = chosen.minAmount() + random.nextInt((chosen.maxAmount() - chosen.minAmount()) + 1);
            if (stationLevel < 3) {
                amount = (int)amount/2;
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
            ItemStack remaining = stack.copy();

            if (plotState != PlotState.EMPTY && !upgradeRequestedItems.isEmpty()) {
                for (int i = 0; i < upgradeRequestedItems.size(); i++) {
                    if (ItemStack.isSameItem(remaining, upgradeRequestedItems.get(i))) {
                        int spaceLeft = upgradeTargetAmounts[i] - upgradeCurrentAmounts[i];
                        int toInsert = Math.min(remaining.getCount(), Math.max(0, spaceLeft));
                        if (!simulate && toInsert > 0) {
                            upgradeCurrentAmounts[i] += toInsert;
                            setChanged();
                            checkUpgradeContractCompletion();
                        }
                        remaining.shrink(toInsert);
                        if (remaining.isEmpty()) return ItemStack.EMPTY;
                        break;
                    }
                }
            }

            for (int i = 0; i < requestedItems.size(); i++) {
                if (ItemStack.isSameItem(remaining, requestedItems.get(i))) {
                    int spaceLeft = targetAmounts[i] - currentAmounts[i];
                    int toInsert = Math.min(remaining.getCount(), Math.max(0, spaceLeft));
                    if (!simulate && toInsert > 0) {
                        currentAmounts[i] += toInsert;
                        setChanged();
                        checkContractCompletion();
                    }
                    remaining.shrink(toInsert);
                    break;
                }
            }
            return remaining;
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

    public void startBuildingConstruction(BuildingType selectedType) {
        if (plotState != PlotState.EMPTY || selectedType == BuildingType.NONE) return;

        buildingType = selectedType;
        plotType = selectedType.name().toLowerCase();
        plotState = PlotState.CONSTRUCTING;
        clearUpgradeContract();

        switch (selectedType) {
            case MINE -> {
                addUpgradeRequirement(0, Items.COBBLESTONE, 256);
                addUpgradeRequirement(1, Items.OAK_PLANKS, 128);
                addUpgradeRequirement(2, Items.IRON_INGOT, 32);
                addUpgradeRequirement(3, Items.LANTERN, 8);
            }
            case FARM -> {
                addUpgradeRequirement(0, Items.OAK_LOG, 64);
                addUpgradeRequirement(1, Items.OAK_FENCE, 64);
                addUpgradeRequirement(2, Items.DIRT, 128);
                addUpgradeRequirement(3, Items.HAY_BLOCK, 16);
            }
            default -> { }
        }
        setChanged();
    }

    public void generateDailyUpkeepContract() {
        clearUpgradeContract();
        switch (buildingType) {
            case MINE -> {
                addUpgradeRequirement(0, Items.OAK_LOG, 32);
                addUpgradeRequirement(1, Items.COAL, 16);
            }
            case FARM -> {
                addUpgradeRequirement(0, Items.BONE_MEAL, 32);
                addUpgradeRequirement(1, Items.WHEAT_SEEDS, 64);
            }
            default -> { }
        }
        setChanged();
    }

    private void clearUpgradeContract() {
        upgradeRequestedItems.clear();
        for (int i = 0; i < 4; i++) {
            upgradeTargetAmounts[i] = 0;
            upgradeCurrentAmounts[i] = 0;
        }
    }

    private void addUpgradeRequirement(int slotIndex, Item item, int amount) {
        if (slotIndex < 0 || slotIndex >= 4) return;
        upgradeRequestedItems.add(new ItemStack(item));
        upgradeTargetAmounts[slotIndex] = amount;
    }

    public boolean isUpgradeContractCompleted() {
        if (upgradeRequestedItems.isEmpty()) return false;
        for (int i = 0; i < upgradeRequestedItems.size(); i++) {
            if (upgradeCurrentAmounts[i] < upgradeTargetAmounts[i]) return false;
        }
        return true;
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

        if (this.level != null && !this.level.isClientSide()) {
            spawnReward();

            this.stationLevel++;

            // Okamžité vygenerování nového úkolu
            generateNewContract();
        }
    }

    private void checkUpgradeContractCompletion() {
        if (!isUpgradeContractCompleted()) return;

        if (plotState == PlotState.CONSTRUCTING) {
            plotState = PlotState.ACTIVE;
            plotTier = 1;
            if (level != null) {
                long dayTime = level.getDayTime();
                lastProcessedDay = (dayTime % 24000L < 12000L)
                        ? (dayTime / 24000L) - 1
                        : (dayTime / 24000L);
            }
            placeStructureOnPlot();
            generateDailyUpkeepContract();
            this.setChanged();
            System.out.println("Stavba budovy " + buildingType.name() + " dokončena! Stav: ACTIVE.");
        } else if (plotState == PlotState.HALTED) {
            plotState = PlotState.ACTIVE;
            // Splněný kontrakt byl spotřebován pro obnovení dnešního provozu.
            // Nový kontrakt se kontroluje pro následující večer.
            generateDailyUpkeepContract();
            setChanged();
            System.out.println("Údržba doplněna! Budova " + buildingType.name() + " je opět ACTIVE.");
        }
    }

    private void spawnReward() { // spawn reward from chosen JSON
        for (ItemStack rewardItem : this.rewardItems) {
            ItemEntity rewardEntity = new ItemEntity(this.level,
                    getBlockPos().getX() + 0.5,
                    getBlockPos().getY() + 1.2,
                    getBlockPos().getZ() + 0.5,
                    rewardItem.copy());
            rewardEntity.setDeltaMovement(0, 0.2, 0);
            this.level.addFreshEntity(rewardEntity);
        }
    }

    private ItemStack createReward(RewardEntry chosen) {
        if (!RewardPoolManager.INSTANCE.hasAnyEntries() && this.level != null) {
            RewardPoolManager.INSTANCE.loadFromResources(this.level.getServer().getResourceManager());
        }

        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(chosen.itemId()));
        if (item == Items.AIR) {
            System.err.println("Reward pool contains unknown item: " + chosen.itemId());
            return ItemStack.EMPTY;
        }

        RandomSource random = (this.level != null) ? this.level.random : RandomSource.create();
        int amount = chosen.minAmount() + random.nextInt(chosen.maxAmount() - chosen.minAmount() + 1);
        if (stationLevel > 5) {
            amount = amount*2;
        }
        return new ItemStack(item, amount);
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
        tag.putLong("LastProcessedDay", this.lastProcessedDay);

        // Uložení pole předmětů přes ListTag[cite: 4]
        ListTag itemsTag = new ListTag();
        for (ItemStack stack : this.requestedItems) {
            if (!stack.isEmpty()) {
                itemsTag.add(stack.save(registries));
            }
        }
        tag.put("RequestedItems", itemsTag);
        ListTag rewardsTag = new ListTag();
        for (ItemStack reward : this.rewardItems) {
            if (!reward.isEmpty()) {
                rewardsTag.add(reward.save(registries));
            }
        }
        tag.put("RewardItems", rewardsTag);

        // Uložení číselných polí
        tag.putIntArray("TargetAmounts", this.targetAmounts);
        tag.putIntArray("CurrentAmounts", this.currentAmounts);
        ListTag upgradeItemsTag = new ListTag();
        for (ItemStack stack : upgradeRequestedItems) {
            if (!stack.isEmpty()) upgradeItemsTag.add(stack.save(registries));
        }
        tag.put("UpgradeRequestedItems", upgradeItemsTag);
        tag.putIntArray("UpgradeTargetAmounts", upgradeTargetAmounts);
        tag.putIntArray("UpgradeCurrentAmounts", upgradeCurrentAmounts);
        tag.putString("PlotState", plotState.name());
        tag.putString("BuildingType", buildingType.name());
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
        this.rewardItems.clear();
        if (tag.contains("RewardItems", Tag.TAG_LIST)) {
            ListTag rewardsTag = tag.getList("RewardItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < rewardsTag.size(); i++) {
                ItemStack reward = ItemStack.parse(registries, rewardsTag.getCompound(i)).orElse(ItemStack.EMPTY);
                if (!reward.isEmpty()) {
                    this.rewardItems.add(reward);
                }
            }
        } else if (tag.contains("RewardItem", Tag.TAG_COMPOUND)) {
            ItemStack reward = ItemStack.parse(registries, tag.getCompound("RewardItem")).orElse(ItemStack.EMPTY);
            if (!reward.isEmpty()) {
                this.rewardItems.add(reward);
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
        upgradeRequestedItems.clear();
        if (tag.contains("UpgradeRequestedItems", Tag.TAG_LIST)) {
            ListTag upgradeItemsTag = tag.getList("UpgradeRequestedItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < upgradeItemsTag.size(); i++) {
                upgradeRequestedItems.add(ItemStack.parse(registries, upgradeItemsTag.getCompound(i))
                        .orElse(ItemStack.EMPTY));
            }
        }
        if (tag.contains("UpgradeTargetAmounts")) {
            int[] loadedTargets = tag.getIntArray("UpgradeTargetAmounts");
            System.arraycopy(loadedTargets, 0, upgradeTargetAmounts, 0, Math.min(loadedTargets.length, 4));
        }
        if (tag.contains("UpgradeCurrentAmounts")) {
            int[] loadedCurrents = tag.getIntArray("UpgradeCurrentAmounts");
            System.arraycopy(loadedCurrents, 0, upgradeCurrentAmounts, 0, Math.min(loadedCurrents.length, 4));
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
        if (tag.contains("PlotState")) {
            try {
                plotState = PlotState.valueOf(tag.getString("PlotState"));
            } catch (IllegalArgumentException ignored) {
                plotState = PlotState.EMPTY;
            }
        }
        if (tag.contains("BuildingType")) {
            try {
                buildingType = BuildingType.valueOf(tag.getString("BuildingType"));
            } catch (IllegalArgumentException ignored) {
                buildingType = BuildingType.NONE;
            }
        } else if (!"empty".equals(plotType)) {
            try {
                buildingType = BuildingType.valueOf(plotType.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                buildingType = BuildingType.NONE;
            }
        }
        if (tag.contains("PlotTier")) {
            this.plotTier = tag.getInt("PlotTier");
        }
        if (tag.contains("LastProcessedDay")) {
            this.lastProcessedDay = tag.getLong("LastProcessedDay");
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
                this.plotState = PlotState.EMPTY;
                this.buildingType = BuildingType.NONE;
                this.clearUpgradeContract();
                this.generateNewContract();
                this.setChanged();
            } else if (this.requestedItems.isEmpty()) {
                this.generateNewContract();
            } else if (this.rewardItems.isEmpty()) {
                generateNewContract();
                this.setChanged();
            }
            if (this.plotPos == null) {
                findVillagePlot();
            }
        }
    }

    public void placeStructureOnPlot() {
        if (!(this.level instanceof ServerLevel serverLevel)) return;

        if (this.plotPos == null) {
            findVillagePlot();
        }
        if (this.plotPos == null || !serverLevel.isLoaded(this.plotPos)) {
            System.err.println("Nelze postavit budovu: parcela nebyla nalezena nebo chunk není načtený.");
            return;
        }

        String templateName = switch (this.buildingType) {
            case MINE -> "mine_tier_" + this.plotTier;
            case FARM -> "farm_tier_" + this.plotTier;
            default -> null;
        };
        if (templateName == null) return;

        StructureTemplateManager manager = serverLevel.getStructureManager();
        ResourceLocation templateId = ResourceLocation.fromNamespaceAndPath(
                "createfreighttrains", templateName);
        var templateOptional = manager.get(templateId);
        if (templateOptional.isEmpty()) {
            System.err.println("Šablona budovy nebyla nalezena: " + templateId);
            return;
        }

        StructureTemplate template = templateOptional.get();
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(this.plotRotation)
                .setIgnoreEntities(false);

        Vec3i size = template.getSize(this.plotRotation);
        serverLevel.playSound(null, this.plotPos, SoundEvents.ANVIL_USE,
                SoundSource.BLOCKS, 1.0f, 1.0f);
        for (int x = 0; x < size.getX(); x += 2) {
            for (int z = 0; z < size.getZ(); z += 2) {
                serverLevel.sendParticles(ParticleTypes.POOF,
                        this.plotPos.getX() + x + 0.5,
                        this.plotPos.getY() + 1.5,
                        this.plotPos.getZ() + z + 0.5,
                        8, 0.3, 0.5, 0.3, 0.05);
            }
        }

        template.placeInWorld(serverLevel, this.plotPos, this.plotPos, settings,
                serverLevel.random, Block.UPDATE_ALL);
        System.out.println("Budova " + templateId + " úspěšně postavena na " + this.plotPos);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  StationRequesterEntity blockEntity) {
        if (level.isClientSide()) return;
        if (blockEntity.plotState != PlotState.ACTIVE
                && blockEntity.plotState != PlotState.HALTED) return;

        long dayTime = level.getDayTime();
        long currentDay = dayTime / 24000L;
        long timeOfDay = dayTime % 24000L;
        if (currentDay < blockEntity.lastProcessedDay) {
            blockEntity.lastProcessedDay = currentDay - 1;
        }

        if (timeOfDay >= 12000L && currentDay > blockEntity.lastProcessedDay) {
            blockEntity.lastProcessedDay = currentDay;
            blockEntity.processEndOfDay((ServerLevel) level);
        }
    }

    private void processEndOfDay(ServerLevel serverLevel) {
        if (this.plotState == PlotState.ACTIVE) {
            produceDailyResources(serverLevel);
        }

        if (isUpgradeContractCompleted()) {
            this.plotState = PlotState.ACTIVE;
            generateDailyUpkeepContract();
            System.out.println("Denní údržba splněna! Budova " + buildingType.name()
                    + " pokračuje v provozu (ACTIVE).");
        } else {
            this.plotState = PlotState.HALTED;
            System.out.println("Denní údržba CHYBÍ! Budova " + buildingType.name()
                    + " zastavila provoz (HALTED).");
        }
        this.setChanged();
    }

    private void produceDailyResources(ServerLevel serverLevel) {
        List<ItemStack> outputs = new ArrayList<>();
        if (this.buildingType == BuildingType.MINE) {
            outputs.add(new ItemStack(Items.RAW_IRON, 16));
            outputs.add(new ItemStack(Items.COAL, 32));
            outputs.add(new ItemStack(Items.COBBLESTONE, 64));
        } else if (this.buildingType == BuildingType.FARM) {
            outputs.add(new ItemStack(Items.WHEAT, 48));
            outputs.add(new ItemStack(Items.CARROT, 32));
            outputs.add(new ItemStack(Items.POTATO, 32));
        }

        BlockPos abovePos = this.worldPosition.above();
        IItemHandler aboveStorage = serverLevel.getCapability(
                Capabilities.ItemHandler.BLOCK, abovePos, Direction.DOWN);

        for (ItemStack stack : outputs) {
            ItemStack remainder = stack.copy();
            if (aboveStorage != null) {
                remainder = ItemHandlerHelper.insertItemStacked(aboveStorage, remainder, false);
            }
            if (!remainder.isEmpty()) {
                ItemEntity droppedItem = new ItemEntity(serverLevel,
                        this.worldPosition.getX() + 0.5,
                        this.worldPosition.getY() + 1.2,
                        this.worldPosition.getZ() + 0.5,
                        remainder);
                droppedItem.setDeltaMovement(0, 0.2, 0);
                serverLevel.addFreshEntity(droppedItem);
            }
        }
        System.out.println("Budova " + buildingType.name() + " vygenerovala denní produkci!");
    }

    public UUID getStationId() {
        return stationId;
    }

    public List<ItemStack> getRequestedItems() {
        return requestedItems;
    }

    public List<ItemStack> getRewardItems() {
        return rewardItems;
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

    public List<ItemStack> getUpgradeRequestedItems() {
        return upgradeRequestedItems;
    }

    public int[] getUpgradeTargetAmounts() {
        return upgradeTargetAmounts;
    }

    public int[] getUpgradeCurrentAmounts() {
        return upgradeCurrentAmounts;
    }

    public PlotState getPlotState() {
        return plotState;
    }

    public BuildingType getBuildingType() {
        return buildingType;
    }

    public BlockPos getPlotPos() {
        return plotPos;
    }

    public Rotation getPlotRotation() {
        return plotRotation;
    }

    public int getPlotTier() {
        return plotTier;
    }

    public ContainerData getData() {
        return data;
    }


}