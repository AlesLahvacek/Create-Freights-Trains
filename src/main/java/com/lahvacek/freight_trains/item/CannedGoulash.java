package com.lahvacek.freight_trains.item;

import com.lahvacek.freight_trains.registry.ModItems;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CannedGoulash extends Item {

    public CannedGoulash(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entityLiving) {
        ItemStack result = super.finishUsingItem(stack, level, entityLiving);

        if (entityLiving instanceof Player player && !player.isCreative()) {
            ItemStack emptyCan = new ItemStack(ModItems.EMPTY_CAN.get());
            if (!player.getInventory().add(emptyCan)) {
                player.drop(emptyCan, false);
            }
        }

        return result;
    }
}
