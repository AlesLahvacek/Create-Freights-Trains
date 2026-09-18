package com.lahvacek.freight_trains.block;

import com.lahvacek.freight_trains.CreateFreightTrains;
import com.lahvacek.freight_trains.menu.StationRequesterMenu;
import com.lahvacek.freight_trains.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.Items;

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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);

            if (be instanceof StationRequesterEntity requester && player instanceof ServerPlayer serverPlayer) {

                // Připravíme data z tvé BlockEntity
                ItemStack requested = requester.getRequestedItem();
                int current = requester.getCurrentAmount();
                int target = requester.getTargetAmount();

                // ZATÍM PROVIZORNÍ ODMĚNA (Později napojíme na dynamický systém)
                ItemStack reward = new ItemStack(Items.EMERALD, 5);

                // Zavoláme NeoForge API pro otevření okna a poslání dat
                serverPlayer.openMenu(new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.literal("Station Requester");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
                        // Předáváme requester.data místo konkrétních čísel
                        return new StationRequesterMenu(windowId, inv, requested, reward, requester.data);
                    }
                }, buffer -> {
                    // Do jednorázového bufferu už balíme JEN statické itemy
                    ItemStack.STREAM_CODEC.encode(buffer, requested);
                    ItemStack.STREAM_CODEC.encode(buffer, reward);
                });
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
  }

}
