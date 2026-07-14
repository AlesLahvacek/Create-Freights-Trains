package com.lahvacek.freight_trains.block;

import com.lahvacek.freight_trains.block.CargoInspectorBlockEntity;
import com.lahvacek.freight_trains.registry.ModItems;
import com.lahvacek.freight_trains.item.*;

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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

public class CargoInspectorBlock  extends Block implements EntityBlock{

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public CargoInspectorBlock(Properties properties){
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context){
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(Items.PAPER)) {
            if (!level.isClientSide()) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof CargoInspectorBlockEntity inspector) {
                    long currentTime = level.getGameTime();
                if (inspector.isOnCooldown(currentTime)) {
                    long ticksLeft = inspector.getRemainingCooldownTicks(currentTime);
                    int daysLeft = (int) Math.ceil(ticksLeft / 24000.0);
                    
                    player.displayClientMessage(Component.literal("§c[!] Local demand too low | no new requests"), true);
                    player.displayClientMessage(Component.literal("§cTry again in " + daysLeft + " days"), false);
                    return ItemInteractionResult.sidedSuccess(false);
                }
                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }

                    ItemStack card = new ItemStack(ModItems.DESTINATION_CARD.get());

                    // 3. NBT data
                    CustomData.update(DataComponents.CUSTOM_DATA, card, tag -> {
                        // ID + LVL
                        tag.putUUID("TargetStationId", inspector.getStationId());
                        tag.putInt("TargetStationLevel", inspector.getStationLevel());
                        // Cargo manifest
                        ListTag manifestList = new ListTag();
                        for (Map.Entry<Item, Integer> entry : inspector.getActiveManifest().entrySet()) {
                            CompoundTag itemTag = new CompoundTag();
                            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(entry.getKey());
                            itemTag.putString("Item", itemId.toString());
                            itemTag.putInt("Amount", entry.getValue());
                            manifestList.add(itemTag);
                        }
                        tag.put("TargetManifest", manifestList);
                    });

                    // drop if inventory full
                    if (!player.getInventory().add(card)) {
                        player.drop(card, false);
                    }

                    player.displayClientMessage(Component.literal("§a[!] Printed destination card "), true);
                    level.playSound(null, pos, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        // pass to useWithoutItem if no paper is held
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof CargoInspectorBlockEntity inspector)) {
                return InteractionResult.PASS;
            }
            long currentTime = level.getGameTime();
                if (inspector.isOnCooldown(currentTime)) {
                    long ticksLeft = inspector.getRemainingCooldownTicks(currentTime);
                    int daysLeft = (int) Math.ceil(ticksLeft / 24000.0);
                    
                    player.displayClientMessage(Component.literal("§c[!] Local demand too low | no new requests"), true);
                    player.displayClientMessage(Component.literal("§cTry again in " + daysLeft + " days"), false);
                    return InteractionResult.sidedSuccess(false);
                }

            // Get manifest
            Map<Item, Integer> manifest = inspector.getActiveManifest();
            
            if (manifest.isEmpty()) {
                inspector.generateNewManifest();
                manifest = inspector.getActiveManifest();
            }
            
            player.displayClientMessage(Component.literal("§6=== CARGO LIST (Level " + inspector.getStationLevel() + ") ==="), false);
            if (manifest.isEmpty()) {
                player.displayClientMessage(Component.literal("There is currently no request"), false);
                return InteractionResult.sidedSuccess(false);
            } else {
                manifest.forEach((item, amount) -> {
                    String itemName = new ItemStack(item).getHoverName().getString();
                    player.displayClientMessage(Component.literal("§e- " + amount + "x §f" + itemName), false);
                });
            }
            player.displayClientMessage(Component.literal("§6============================="), false);

            // 2. Check if there is container
            Direction facing = state.getValue(FACING);
            BlockPos targetPos = pos.relative(facing);
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, facing.getOpposite());

            ItemStack validWaybill = ItemStack.EMPTY;
            int waybillSlot = -1;

            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stackInSlot = handler.getStackInSlot(i);

                if (stackInSlot.getItem() instanceof WayBillItem) {
                    CustomData customData = stackInSlot.get(DataComponents.CUSTOM_DATA);
                    
                    if (customData != null && customData.copyTag().hasUUID("TargetStationId")) {
                        UUID waybillId = customData.copyTag().getUUID("TargetStationId");

                        if (waybillId.equals(inspector.getStationId())) {
                            validWaybill = stackInSlot;
                            waybillSlot = i;
                            break;
                        }
                    }
                }
            }

            if (validWaybill.isEmpty()) {
                player.displayClientMessage(Component.literal("§c[!] No cargo list present for this station"), false);
                level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 1.0f, 1.0f);
                return InteractionResult.sidedSuccess(false);
            }

            player.displayClientMessage(Component.literal("§a[!] Cargo verified"), false);

            if (handler == null) {
                player.displayClientMessage(Component.literal("§c[No container found]"), false);
                return InteractionResult.sidedSuccess(false);
            }

            boolean isTooClose = false;

            CompoundTag wbTag = validWaybill.get(DataComponents.CUSTOM_DATA).copyTag();
            if (wbTag.contains("DepotX")) {
                int startX = wbTag.getInt("DepotX");
                int startY = wbTag.getInt("DepotY");
                int startZ = wbTag.getInt("DepotZ");

                BlockPos startPos = new BlockPos(startX, startY, startZ);
                double distance = Math.sqrt(pos.distSqr(startPos));
                double minDistance = 300.0; 

                if (distance < minDistance) {
                    isTooClose = true;
                    player.displayClientMessage(Component.literal("§e[!] Cargo list accepted, however destination is too close (" + (int)distance + " m)"), false);
                    player.displayClientMessage(Component.literal("§cIf delivered station will stop demanding for next 5 days"), false);
                    level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0f, 0.5f);
                } else {
                    player.displayClientMessage(Component.literal("§a[!] Distance to destination (" + (int)distance + " m)."), false);
                }
            }

            Map<Item, Integer> contents = new HashMap<>();
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    contents.put(stack.getItem(), contents.getOrDefault(stack.getItem(), 0) + stack.getCount());
                }
            }

            // Logic check
            boolean hasAllRequirements = true;
            for (Map.Entry<Item, Integer> entry : manifest.entrySet()) {
                if (contents.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                    hasAllRequirements = false;
                    break;
                }
            }

            // item removal
            if (!hasAllRequirements) {
                // missing items
                player.displayClientMessage(Component.literal("§c[!] Delivery denied: Some items are missing"), false);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.VILLAGER_NO, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
            } else {
                player.displayClientMessage(Component.literal("§a[!] Delivery successful! Container emptied"), false);
                
                // new map to update the values mid process
                Map<Item, Integer> toExtract = new HashMap<>(manifest);
                
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stackInSlot = handler.getStackInSlot(i);
                    if (stackInSlot.isEmpty()) continue;
                    
                    Item slotItem = stackInSlot.getItem();
                    if (toExtract.containsKey(slotItem)) {
                        int needed = toExtract.get(slotItem);
                        if (needed > 0) {
                            // physically remove the items (simulation to false)
                            ItemStack extracted = handler.extractItem(i, needed, false);
                            toExtract.put(slotItem, needed - extracted.getCount());
                        }
                    }
                }

                handler.extractItem(waybillSlot, 1, false);

                // sound
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
                inspector.completeContract(isTooClose);
            }
        }
        
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CargoInspectorBlockEntity(pos, state);
    }
}
