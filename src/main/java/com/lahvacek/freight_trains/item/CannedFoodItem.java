package com.lahvacek.freight_trains.item;

import com.lahvacek.freight_trains.registry.ModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class CannedFoodItem extends Item {

    // 1. alwaysEdible + player Effect
    public CannedFoodItem(Properties properties, int nutrition, float saturation, boolean alwaysEdible, @Nullable Supplier<MobEffectInstance> effect) {
        super(buildProperties(properties, nutrition, saturation, alwaysEdible, effect));
    }

    // 2. alwaysEdible
    public CannedFoodItem(Properties properties, int nutrition, float saturation, boolean alwaysEdible) {
        this(properties, nutrition, saturation, alwaysEdible, null);
    }

    // 3. basic food constructor
    public CannedFoodItem(Properties properties, int nutrition, float saturation) {
        this(properties, nutrition, saturation, false, null);
    }

    private static Properties buildProperties(Properties properties, int nutrition, float saturation, boolean alwaysEdible, @Nullable Supplier<MobEffectInstance> effect) {
        FoodProperties.Builder foodBuilder = new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationModifier(saturation);

        if (alwaysEdible) {
            foodBuilder.alwaysEdible();
        }

        if (effect != null) {
            foodBuilder.effect(effect, 1.0f);
        }

        return properties
                .stacksTo(99)
                .food(foodBuilder.build());
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entityLiving) {
        ItemStack result = super.finishUsingItem(stack, level, entityLiving);

        if (!level.isClientSide() && entityLiving instanceof Player player && !player.isCreative()) {
            ItemStack emptyCan = new ItemStack(ModItems.EMPTY_CAN.get());
            if (!player.getInventory().add(emptyCan)) {
                player.drop(emptyCan, false);
            }
        }

        return result;
    }
}