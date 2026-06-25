package com.ellinet13.ellinet13plugin;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class FakeBlockManager {

    private final JavaPlugin plugin;
    private final Map<String, Material> fakeBlocks = new HashMap<>();
    private final File configFile;
    private boolean revealMode = false;
    private boolean allowAirReplacement = false;

    private static final Material REVEAL_MATERIAL = Material.GREEN_WOOL;

    public FakeBlockManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "fakeblocks.yml");
        loadConfig();
    }

    /**
     * Creates a key for a block location
     */
    public static String makeKey(World world, int x, int y, int z) {
        return world.getName() + ";" + x + ";" + y + ";" + z;
    }

    /**
     * Adds a fake block mapping
     */
    public boolean setFakeBlock(World world, int x, int y, int z, Material fakeMaterial) {
        if (!canDisplayFakeBlockAt(world, x, y, z)) {
            return false;
        }

        String key = makeKey(world, x, y, z);
        fakeBlocks.put(key, fakeMaterial);
        saveConfig();
        return true;
    }

    /**
     * Removes a fake block mapping
     */
    public void removeFakeBlock(World world, int x, int y, int z) {
        String key = makeKey(world, x, y, z);
        fakeBlocks.remove(key);
        saveConfig();
    }

    /**
     * Clears all fake blocks
     */
    public void clearAllFakeBlocks() {
        fakeBlocks.clear();
        saveConfig();
    }

    /**
     * Clears fake blocks in a specific region and returns them as a map
     * @return map of location key to the fake material that was stored
     */
    public Map<String, Material> clearFakeBlocksInRegion(World world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        Map<String, Material> clearedBlocks = new HashMap<>();
        String prefix = world.getName() + ";";

        // Find all blocks in the region
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    String key = makeKey(world, x, y, z);
                    Material fakeMaterial = fakeBlocks.remove(key);
                    if (fakeMaterial != null) {
                        clearedBlocks.put(key, fakeMaterial);
                    }
                }
            }
        }

        if (!clearedBlocks.isEmpty()) {
            saveConfig();
        }

        return clearedBlocks;
    }

    /**
     * Gets the fake material for a location, or null if not faked
     */
    public Material getFakeBlock(World world, int x, int y, int z) {
        if (!canDisplayFakeBlockAt(world, x, y, z)) {
            return null;
        }

        return fakeBlocks.get(makeKey(world, x, y, z));
    }

    /**
     * Gets the fake material for a location key
     */
    public Material getFakeBlock(String key) {
        return fakeBlocks.get(key);
    }

    /**
     * Gets all fake block entries
     */
    public Map<String, Material> getAllFakeBlocks() {
        return new HashMap<>(fakeBlocks);
    }

    /**
     * Gets the count of fake blocks
     */
    public int getFakeBlockCount() {
        return fakeBlocks.size();
    }

    /**
     * Toggles reveal mode (shows fake blocks as green wool)
     */
    public boolean toggleRevealMode() {
        revealMode = !revealMode;
        return revealMode;
    }

    /**
     * Checks if reveal mode is enabled
     */
    public boolean isRevealMode() {
        return revealMode;
    }

    /**
     * Checks whether fake blocks are allowed to replace air blocks.
     */
    public boolean isAirReplacementAllowed() {
        return allowAirReplacement;
    }

    /**
     * Checks whether a fake block may be shown at this real block position.
     */
    public boolean canDisplayFakeBlockAt(World world, int x, int y, int z) {
        return allowAirReplacement || !world.getBlockAt(x, y, z).getType().isAir();
    }

    /**
     * Gets the material to display (either the fake material or green wool if reveal mode is on)
     */
    public Material getDisplayMaterial(Material fakeMaterial) {
        if (revealMode && fakeMaterial != null) {
            return REVEAL_MATERIAL;
        }
        return fakeMaterial;
    }

    /**
     * Loads fake blocks from config file
     */
    @SuppressWarnings("deprecation")
    private void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("fakeblocks.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        allowAirReplacement = config.getBoolean("settings.allow-air-replacement", false);
        ConfigurationSection blocksSection = config.getConfigurationSection("fake-blocks");

        if (blocksSection != null) {
            for (String worldName : blocksSection.getKeys(false)) {
                ConfigurationSection worldSection = blocksSection.getConfigurationSection(worldName);
                if (worldSection != null) {
                    for (String key : worldSection.getKeys(false)) {
                        String materialName = worldSection.getString(key);
                        if (materialName != null) {
                            Material material = Material.matchMaterial(materialName);
                            if (material != null) {
                                String[] coordinates = key.split("_", 3);
                                if (coordinates.length == 3) {
                                    fakeBlocks.put(worldName + ";" + coordinates[0] + ";" + coordinates[1] + ";" + coordinates[2], material);
                                } else {
                                    plugin.getLogger().warning(() -> "Invalid fake block location in config: " + worldName + "." + key);
                                }
                            } else {
                                plugin.getLogger().warning(() -> "Unknown material in config: " + materialName);
                            }
                        }
                    }
                }
            }
        }

        plugin.getLogger().info(() -> "Loaded " + fakeBlocks.size() + " fake block mappings");
    }

    /**
     * Saves fake blocks to config file
     */
    private void saveConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("settings.allow-air-replacement", allowAirReplacement);
        Map<String, Map<String, String>> blocksByWorld = new HashMap<>();

        for (Map.Entry<String, Material> entry : fakeBlocks.entrySet()) {
            String key = entry.getKey();
            String[] parts = key.split(";", 4);
            if (parts.length == 4) {
                String worldName = parts[0];
                String locationKey = parts[1] + "_" + parts[2] + "_" + parts[3];
                String materialName = entry.getValue().name();

                blocksByWorld.computeIfAbsent(worldName, k -> new HashMap<>()).put(locationKey, materialName);
            }
        }

        ConfigurationSection blocksSection = config.createSection("fake-blocks");
        for (Map.Entry<String, Map<String, String>> worldEntry : blocksByWorld.entrySet()) {
            ConfigurationSection worldSection = blocksSection.createSection(worldEntry.getKey());
            for (Map.Entry<String, String> locationEntry : worldEntry.getValue().entrySet()) {
                worldSection.set(locationEntry.getKey(), locationEntry.getValue());
            }
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save fake blocks config", e);
        }
    }
}
