package com.lahvacek.freight_trains.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;

public class DestinationCardItem extends Item {
    
    public DestinationCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        // get saved NBT data
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();

            if (tag.hasUUID("TargetStationId")) {
                int level = tag.getInt("TargetStationLevel");
                tooltipComponents.add(Component.literal("§bDestination (Level " + level + ")"));

                if (tag.contains("TargetManifest", Tag.TAG_LIST)) {
                    tooltipComponents.add(Component.literal("§7Requested cargo:"));
                    ListTag manifestList = tag.getList("TargetManifest", Tag.TAG_COMPOUND);
                    for (int i = 0; i < manifestList.size(); i++) {
                        CompoundTag itemTag = manifestList.getCompound(i);
                        ResourceLocation itemId = ResourceLocation.parse(itemTag.getString("Item"));
                        Item item = BuiltInRegistries.ITEM.get(itemId);
                        int amount = itemTag.getInt("Amount");
                        
                        String itemName = new ItemStack(item).getHoverName().getString();
                        tooltipComponents.add(Component.literal("§e- " + amount + "x §f" + itemName));
                    }
                }
            }
        } else {
            // if card is empty
            tooltipComponents.add(Component.literal("§cMissing station data :("));
        }
    }
}
