package com.lahvacek.freight_trains.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
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

public class RewardPoolManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();

    public static final RewardPoolManager INSTANCE = new RewardPoolManager();

    private final List<RewardEntry> allEntries = new ArrayList<>();

    public RewardPoolManager() {
        super(GSON, "reward_pools");
    }

    public void loadFromResources(ResourceManager resourceManager) {
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                "reward_pools", path -> path.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (Reader reader = new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8)) {
                readEntries(entry.getKey(), GSON.fromJson(reader, JsonObject.class));
            } catch (Exception e) {
                System.err.println("Chyba při načítání Reward Poolu z resource manageru ("
                        + entry.getKey() + "): " + e.getMessage());
            }
        }
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objectIn, ResourceManager resourceManagerIn,
                         ProfilerFiller profilerIn) {
        allEntries.clear();
        int parsedEntries = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : objectIn.entrySet()) {
            try {
                parsedEntries += readEntries(entry.getKey(), entry.getValue().getAsJsonObject());
            } catch (Exception e) {
                System.err.println("Chyba při parsování Reward Poolu z JSONu ("
                        + entry.getKey() + "): " + e.getMessage());
            }
        }

        System.out.println("Loaded " + parsedEntries + " reward pool entries from JSON files.");
    }

    private int readEntries(ResourceLocation resourceId, JsonObject json) {
        JsonArray entries = json.getAsJsonArray("entries");
        int parsedEntries = 0;

        for (JsonElement entryElement : entries) {
            JsonObject entry = entryElement.getAsJsonObject();
            allEntries.add(new RewardEntry(
                    entry.get("item").getAsString(),
                    entry.get("min_level").getAsInt(),
                    entry.get("max_level").getAsInt(),
                    entry.get("min_amount").getAsInt(),
                    entry.get("max_amount").getAsInt()
            ));
            parsedEntries++;
        }

        return parsedEntries;
    }

    public List<RewardEntry> getPoolForLevel(int currentStationLevel) {
        return allEntries.stream()
                .filter(entry -> currentStationLevel >= entry.minLevel()
                        && currentStationLevel <= entry.maxLevel())
                .collect(Collectors.toList());
    }

    public boolean hasAnyEntries() {
        return !allEntries.isEmpty();
    }
}
