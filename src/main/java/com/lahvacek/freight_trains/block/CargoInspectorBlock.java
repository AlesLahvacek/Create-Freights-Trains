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
            
            // cord value of block face
            Direction facing = state.getValue(FACING);
            BlockPos targetPos = pos.relative(facing);

            // get inventory of adjanced block (IItemHandler)
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, facing.getOpposite());

            if (handler != null) {
                // 3. Kontejner nalezen! Přečteme jeho obsah
                player.displayClientMessage(Component.literal("Checking cargo..."), false);
                
                // Vytvoříme si "Mapu" pro sečtení stejných itemů (ve Vaultu mohou být rozházené ve více slotech)
                Map<Item, Integer> contents = new HashMap<>();
                
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        // Přičteme počet k už existujícímu záznamu, nebo založíme nový (getOrDefault)
                        contents.put(stack.getItem(), contents.getOrDefault(stack.getItem(), 0) + stack.getCount());
                    }
                }

                // 4. Vypsání výsledků hráči
                if (contents.isEmpty()) {
                    player.displayClientMessage(Component.literal("Container is empty"), false);
                } else {
                    player.displayClientMessage(Component.literal("Items in container:"), false);
                    contents.forEach((item, count) -> {
                        String itemName = new ItemStack(item).getHoverName().getString();
                        player.displayClientMessage(Component.literal("- " + count + "x " + itemName), false);
                    });
                }
                
            } else {
                // Na těchto souřadnicích není nic s inventářem
                player.displayClientMessage(Component.literal("No container to check"), true);
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
