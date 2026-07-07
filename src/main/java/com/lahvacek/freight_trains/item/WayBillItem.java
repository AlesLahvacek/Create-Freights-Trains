package com.lahvacek.freight_trains.item;

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

public class WayBillItem extends Item {

    public WayBillItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack){
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();

            if (tag.hasUUID("TargetStationId")) {
                tooltipComponents.add(Component.literal("§a[Final cargo list]"));
                
                // requesting inspector location
                if (tag.contains("DepotX")) {
                    int x = tag.getInt("DepotX");
                    int y = tag.getInt("DepotY");
                    int z = tag.getInt("DepotZ");
                    tooltipComponents.add(Component.literal("§8Deliver to: [" + x + ", " + y + ", " + z + "]"));
                }

                if (tag.contains("TargetManifest", Tag.TAG_LIST)) {
                    tooltipComponents.add(Component.literal("§7Final cargo:"));
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
        }
    }
}
