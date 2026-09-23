package com.lahvacek.freight_trains.item;

import com.lahvacek.freight_trains.registry.ModItems;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CannedBeef extends Item{

    public CannedBeef(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entityLiving) {
        // 1. Zavoláme původní Vanilla logiku (doplní hráči hlad a zmenší stack v ruce o 1)
        ItemStack result = super.finishUsingItem(stack, level, entityLiving);

        // 2. Ověříme, že jídlo snědl hráč (a ne např. ochočená liška) a že není v Creative módu
        if (entityLiving instanceof Player player && !player.isCreative()) {

            // 3. Vytvoříme novou prázdnou plechovku
            ItemStack emptyCan = new ItemStack(ModItems.EMPTY_CAN.get());

            // 4. Zkusíme ji vložit hráči rovnou do inventáře.
            // Pokud má plno (hrál s posledním volným slotem), vyhodíme ji bezpečně na zem.
            if (!player.getInventory().add(emptyCan)) {
                player.drop(emptyCan, false);
            }
        }

        // Vracíme výsledek (zmenšený stack původního jídla)
        return result;
    }
}
