package com.lahvacek.freight_trains.block;

import com.lahvacek.freight_trains.block.CargoInspectorBlockEntity;
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

import java.util.HashMap;
import java.util.Map;
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof CargoInspectorBlockEntity inspector)) {
                return InteractionResult.PASS;
            }

            // Get manifest
            Map<Item, Integer> manifest = inspector.getActiveManifest();
            
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

            if (handler == null) {
                player.displayClientMessage(Component.literal("§c[No container found]"), false);
                return InteractionResult.sidedSuccess(false);
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
                // Nemáme všechno, vypíšeme co chybí
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

                // sound
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
                inspector.completeContract();
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
