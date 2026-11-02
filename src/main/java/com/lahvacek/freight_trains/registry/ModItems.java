package com.lahvacek.freight_trains.registry;

import com.lahvacek.freight_trains.item.CannedFoodItem;
import com.lahvacek.freight_trains.item.DestinationCardItem;
import com.lahvacek.freight_trains.item.EmptyCan;
import com.lahvacek.freight_trains.item.IncompleteCannedFood;
import com.lahvacek.freight_trains.item.WayBillItem;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("createfreighttrains");

    // --- BLOCK ITEMS ---
    public static final DeferredItem<BlockItem> STATION_REQUESTER_ITEM = ITEMS.registerSimpleBlockItem(
        "station_requester",
        ModBlocks.STATION_REQUESTER
    );
    public static final DeferredItem<BlockItem> CARGO_INSPECTOR_ITEM = ITEMS.registerSimpleBlockItem(
        "cargo_inspector",
        ModBlocks.CARGO_INSPECTOR
    );
    public static final DeferredItem<BlockItem> DEPOT_TERMINAL = ITEMS.registerSimpleBlockItem(
        "depot_terminal",
        ModBlocks.DEPOT_TERMINAL);

    // --- ITEMS ---
    public static final DeferredItem<Item> DESTINATION_CARD = ITEMS.register("destination_card",
    () -> new DestinationCardItem(new Item.Properties()
            .stacksTo(1)));
    public static final DeferredItem<Item> WAYBILL = ITEMS.register("waybill",
    () -> new WayBillItem(new Item.Properties()
            .stacksTo(1)));
    public static final DeferredItem<Item> EMPTY_CAN = ITEMS.register("empty_can",
    () -> new EmptyCan(new Item.Properties()
            .stacksTo(64)));
    public static final DeferredItem<Item> INCOMPLETE_CANNED_FOOD = ITEMS.register("incomplete_canned_food",
    () -> new IncompleteCannedFood(new Item.Properties()
            .stacksTo(64)));
    public static final DeferredItem<Item> CANNED_BEEF = ITEMS.registerItem(
    "canned_beef",
     props -> new CannedFoodItem(props, 16, 1.0f, false)
);
    public static final DeferredItem<Item> CANNED_GOULASH = ITEMS.registerItem(
    "canned_goulash",
    props -> new CannedFoodItem(props, 20, 1.0f, false, () -> new MobEffectInstance(MobEffects.SATURATION, 200, 0))
);
    public  static final DeferredItem<Item> CANNED_APPLE_JUICE = ITEMS.registerItem(
    "canned_apple_juice",
    props -> new CannedFoodItem(props, 10, 0.7f)
);

}
