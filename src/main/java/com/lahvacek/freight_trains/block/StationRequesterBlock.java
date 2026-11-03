package com.lahvacek.freight_trains.block;

import com.lahvacek.freight_trains.CreateFreightTrains;
import com.lahvacek.freight_trains.menu.StationRequesterMenu;
import com.lahvacek.freight_trains.registry.ModBlocks;
import com.lahvacek.freight_trains.menu.*;
import net.minecraft.core.BlockPos;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;


import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;


public class StationRequesterBlock extends BaseEntityBlock{

    public static final MapCodec<StationRequesterBlock> CODEC = simpleCodec(StationRequesterBlock::new);
    public StationRequesterBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlocks.STATION_REQUESTER_BE.get().create(pos, state);
    }

  @Override
  public RenderShape getRenderShape(BlockState state){
    return RenderShape.MODEL;
  }

  @Override
  protected MapCodec<? extends BaseEntityBlock> codec() {
    return CODEC;
  }

  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
          Level level, BlockState state, BlockEntityType<T> type) {
    if (level.isClientSide()) {
      return null;
    }
    return (lvl, pos, st, blockEntity) -> {
      if (blockEntity instanceof StationRequesterEntity requester) {
        StationRequesterEntity.serverTick(lvl, pos, st, requester);
      }
    };
  }

  @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);

            if (be instanceof StationRequesterEntity requester && player instanceof ServerPlayer serverPlayer) {

                // STARÝ KÓD, KTERÝ MAŽEME:
                // ItemStack requested = requester.getRequestedItem();
                // int current = requester.getCurrentAmount();
                // int target = requester.getTargetAmount();

                ItemStack reward = requester.getRewardItems().isEmpty()
                        ? ItemStack.EMPTY
                        : requester.getRewardItems().get(0);

                // Zavoláme NeoForge API pro otevření okna
                serverPlayer.openMenu(new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.literal("Station Requester");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
                        return new StationRequesterMenu(
                                windowId, inv, pos,
                                requester.requestedItems,
                                requester.upgradeRequestedItems,
                                reward, requester.data
                        );
                    }
                }, buffer -> {
                    buffer.writeBlockPos(pos);

                    buffer.writeInt(requester.requestedItems.size());
                    for (ItemStack item : requester.requestedItems) {
                        ItemStack.STREAM_CODEC.encode(buffer, item);
                    }

                    buffer.writeInt(requester.upgradeRequestedItems.size());
                    for (ItemStack item : requester.upgradeRequestedItems) {
                        ItemStack.STREAM_CODEC.encode(buffer, item);
                    }

                    ItemStack.STREAM_CODEC.encode(buffer, reward);
                });
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

}
