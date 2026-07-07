package com.lahvacek.freight_trains.block;
import com.lahvacek.freight_trains.item.WayBillItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.lahvacek.freight_trains.item.DestinationCardItem;
import com.lahvacek.freight_trains.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public class DepotTerminalBlock extends Block implements EntityBlock {
    
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public DepotTerminalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DepotTerminalBlockEntity(pos, state);
    }


    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof DestinationCardItem) {
            if (!level.isClientSide()) {
                BlockEntity be = level.getBlockEntity(pos);

                if (!(be instanceof DepotTerminalBlockEntity depotBE)) {
                    System.out.println("chyba zde v be");
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }

                CustomData cardData = stack.get(DataComponents.CUSTOM_DATA);
                if (cardData == null || !cardData.copyTag().hasUUID("TargetStationId")) {
                    player.displayClientMessage(Component.literal("§cThis card seems to be misleading :P"), true);
                    return ItemInteractionResult.sidedSuccess(false);
                }

                CompoundTag cardTag = cardData.copyTag();
                UUID targetId = cardTag.getUUID("TargetStationId");
                int cardLevel = cardTag.getInt("TargetStationLevel");

                // Already present data check (lvls, uuids)
                Map<UUID, Integer> known = depotBE.getKnownStations();
                if (known.containsKey(targetId)) {
                    int knownLevel = known.get(targetId);
                    
                    if (cardLevel < knownLevel) {
                        // remove old cargo list
                        player.displayClientMessage(Component.literal("§cOutdate destination card, removing... " + knownLevel + "."), false);
                        level.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0f, 1.5f);
                        
                        if (!player.isCreative()) {
                            stack.shrink(1);
                        }
                        return ItemInteractionResult.sidedSuccess(false);
                    }
                }

                // Pokud jsme prošli auditem, můžeme si paměť aktualizovat
                depotBE.updateStationLevel(targetId, cardLevel);

                Direction facing = state.getValue(FACING);
                BlockPos targetPos = pos.relative(facing);
                IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, facing.getOpposite());

                if (handler == null) {
                    player.displayClientMessage(Component.literal("§c No container present"), false);
                    return ItemInteractionResult.sidedSuccess(false);
                }

                // check items and other waybills
                Map<Item, Integer> availableContents = new HashMap<>();
                List<ItemStack> existingWaybills = new ArrayList<>();

                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stackInSlot = handler.getStackInSlot(i);
                    if (stackInSlot.isEmpty()) continue;
                    
                    if (stackInSlot.getItem() instanceof WayBillItem) {
                        existingWaybills.add(stackInSlot);
                    } else {
                        availableContents.put(stackInSlot.getItem(), availableContents.getOrDefault(stackInSlot.getItem(), 0) + stackInSlot.getCount());
                    }
                }

                // check and decrement already reserved resources
                for (ItemStack wb : existingWaybills) {
                    CustomData wbData = wb.get(DataComponents.CUSTOM_DATA);
                    if (wbData != null) {
                        CompoundTag wbTag = wbData.copyTag();
                        if (wbTag.contains("TargetManifest", Tag.TAG_LIST)) {
                            ListTag list = wbTag.getList("TargetManifest", Tag.TAG_COMPOUND);
                            for (int i = 0; i < list.size(); i++) {
                                CompoundTag itemTag = list.getCompound(i);
                                Item reservedItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemTag.getString("Item")));
                                int reservedAmount = itemTag.getInt("Amount");
                                
                                if (availableContents.containsKey(reservedItem)) {
                                    availableContents.put(reservedItem, availableContents.get(reservedItem) - reservedAmount);
                                }
                            }
                        }
                    }
                }

                // NBT data
                Map<Item, Integer> requiredContents = new HashMap<>();
                if (cardTag.contains("TargetManifest", Tag.TAG_LIST)) {
                    ListTag list = cardTag.getList("TargetManifest", Tag.TAG_COMPOUND);
                    for (int i = 0; i < list.size(); i++) {
                        CompoundTag itemTag = list.getCompound(i);
                        Item reqItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemTag.getString("Item")));
                        requiredContents.put(reqItem, itemTag.getInt("Amount"));
                    }
                }

                // check amount
                boolean hasEnough = true;
                for (Map.Entry<Item, Integer> req : requiredContents.entrySet()) {
                    if (availableContents.getOrDefault(req.getKey(), 0) < req.getValue()) {
                        hasEnough = false;
                        break;
                    }
                }

                if (!hasEnough) {
                    player.displayClientMessage(Component.literal("§cNot enough resources to satisfy request"), false);
                    level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 1.0f, 1.0f);
                    return ItemInteractionResult.sidedSuccess(false);
                }

                // finalizing with waybill
                ItemStack waybill = new ItemStack(ModItems.WAYBILL.get());
                CustomData.update(DataComponents.CUSTOM_DATA, waybill, tag -> {
                    tag.putUUID("TargetStationId", cardTag.getUUID("TargetStationId"));
                    tag.putInt("TargetStationLevel", cardLevel);
                    tag.put("TargetManifest", cardTag.getList("TargetManifest", Tag.TAG_COMPOUND));
                    
                    // XYZ coordinates for checking
                    tag.putInt("DepotX", pos.getX());
                    tag.putInt("DepotY", pos.getY());
                    tag.putInt("DepotZ", pos.getZ());
                });

                // insert waybill
                ItemStack remainder = ItemHandlerHelper.insertItem(handler, waybill, true);
                if (!remainder.isEmpty()) {
                    player.displayClientMessage(Component.literal("§cContainer is full, free up atleast 1 space for paperwork"), false);
                    return ItemInteractionResult.sidedSuccess(false);
                }

                ItemHandlerHelper.insertItem(handler, waybill, false);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                
                player.displayClientMessage(Component.literal("§aCargo list accepted, ready to ship"), false);
                level.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0f, 0.8f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
