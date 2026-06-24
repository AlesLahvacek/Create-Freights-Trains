package com.lahvacek.freight_trains.block;

import com.lahvacek.freight_trains.CreateFreightTrains;
import com.lahvacek.freight_trains.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
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
  protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult){
    if (!level.isClientSide) {
      BlockEntity blockEntity = level.getBlockEntity(pos);

      if (blockEntity instanceof StationRequesterEntity requester) {
        ItemStack requested = requester.getRequestedItem();
        Component message;

        if (requested.isEmpty()) {
          message = Component.literal("No Current requests");
        } else {
          String itemName;
          if (requested.is(ItemTags.LOGS)) { // add more logic (coals, stones)
            itemName = "Log";
          } else {
            itemName = requested.getHoverName().getString();
          }
          int current = requester.getCurrentAmount();
          int target = requester.getTargetAmount();
          int stationLevel = requester.getStationLevel();
          message = Component.literal("LVL: "+ stationLevel + " | " + "Request: " + itemName + " | Progress: " + current + " / " + target);
        }

        player.displayClientMessage(message, true);

      }
    }

    return InteractionResult.sidedSuccess(level.isClientSide);

  }

}
