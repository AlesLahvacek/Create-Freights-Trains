package com.lahvacek.freight_trains.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CargoPoolManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    
    public static final CargoPoolManager INSTANCE = new CargoPoolManager();
    
    private final List<CargoEntry> allEntries = new ArrayList<>();

    public CargoPoolManager() {
        super(GSON, "cargo_pools"); 
    }

    public void loadFromResources(ResourceManager resourceManager) {
        Map<ResourceLocation, Resource> resources = resourceManager.listResources("cargo_pools", path -> path.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (Reader reader = new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                JsonArray items = json.getAsJsonArray("entries");

                for (JsonElement itemElem : items) {
                    JsonObject itemObj = itemElem.getAsJsonObject();
                    String itemId = itemObj.get("item").getAsString();
                    int minLevel = itemObj.get("min_level").getAsInt();
                    int maxLevel = itemObj.get("max_level").getAsInt();
                    int minAmount = itemObj.get("min_amount").getAsInt();
                    int maxAmount = itemObj.get("max_amount").getAsInt();

                    this.allEntries.add(new CargoEntry(itemId, minLevel, maxLevel, minAmount, maxAmount));
                }
            } catch (Exception e) {
                System.err.println("Chyba při načítání Cargo Poolu z resource manageru (" + entry.getKey() + "): " + e.getMessage());
            }
        }
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objectIn, ResourceManager resourceManagerIn, ProfilerFiller profilerIn) {
        allEntries.clear();
        int parsedEntries = 0;
        
        for (Map.Entry<ResourceLocation, JsonElement> entry : objectIn.entrySet()) {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                JsonArray items = json.getAsJsonArray("entries");
                
                for (JsonElement itemElem : items) {
                    JsonObject itemObj = itemElem.getAsJsonObject();
                    
                    String itemId = itemObj.get("item").getAsString();
                    int minLevel = itemObj.get("min_level").getAsInt();
                    int maxLevel = itemObj.get("max_level").getAsInt();
                    int minAmount = itemObj.get("min_amount").getAsInt();
                    int maxAmount = itemObj.get("max_amount").getAsInt();
                    
                    allEntries.add(new CargoEntry(itemId, minLevel, maxLevel, minAmount, maxAmount));
                    parsedEntries++;
                }
            } catch (Exception e) {
                System.err.println("Chyba při parsování Cargo Poolu z JSONu (" + entry.getKey() + "): " + e.getMessage());
            }
        }

        System.out.println("Loaded " + parsedEntries + " cargo pool entries from JSON files.");
    }

    /**
     * Returns a List of all possible minecraft Item entries from available JSONs.
     * Final entries are filtered by their level limitations, depending currentStationLevel.
     */
    public List<CargoEntry> getPoolForLevel(int currentStationLevel) {
        return allEntries.stream()
                .filter(entry -> currentStationLevel >= entry.minLevel() && currentStationLevel <= entry.maxLevel())
                .collect(Collectors.toList());
    }

    public boolean hasAnyEntries() {
        return !allEntries.isEmpty();
    }
}
