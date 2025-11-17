package io.github.pluginupdatecore.updater;

import io.github.pluginupdatecore.version.Version;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Automatic configuration file updater for Minecraft plugins.
 * This class handles automatic updates of config.yml when a plugin version changes.
 * It preserves user customizations while adding new keys and updating the version number.
 * Key features include:
 * <ul>
 *   <li>Version-based update detection</li>
 *   <li>Automatic backup creation before updates</li>
 *   <li>Preservation of user customizations</li>
 *   <li>Smart detection of meaningful changes</li>
 *   <li>Automatic file creation from defaults</li>
 * </ul>
 * The updater adds a {@code config_version} key to config.yml to track versions.
 * When the plugin version increases, it automatically merges new keys while preserving
 * user modifications.
 *
 * @author PluginUpdateCore Team
 * @version 1.0.0
 * @since 1.0.0
 *
 * <pre>{@code
 * public class MyPlugin extends JavaPlugin {
 *     @Override
 *     public void onEnable() {
 *         // Initialize config updater
 *         ConfigUpdater configUpdater = new ConfigUpdater(this);
 *         configUpdater.checkAndUpdateConfig();
 *     }
 * }
 * }</pre>
 */
public class ConfigUpdater {
    private final String currentVersion;
    private final JavaPlugin plugin;
    private static final String CONFIG_VERSION_KEY = "config_version";

    /**
     * Constructs a ConfigUpdater.
     * <p>
     * This constructor only initializes the updater. You must call
     * {@link #checkAndUpdateConfig()} to perform the actual update check.
     * </p>
     *
     * @param plugin The JavaPlugin instance
     *
     * <pre>{@code
     * ConfigUpdater updater = new ConfigUpdater(this);
     * updater.checkAndUpdateConfig();
     * }</pre>
     */
    public ConfigUpdater(JavaPlugin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    /**
     * Checks if the config needs to be updated and updates it if necessary.
     * This method:
     * <ol>
     *   <li>Checks if config.yml exists, creates it if not</li>
     *   <li>Compares config version with plugin version</li>
     *   <li>Creates backup if updates are needed</li>
     *   <li>Merges new keys with user values</li>
     *   <li>Saves updated configuration</li>
     *   <li>Reloads the plugin configuration</li>
     * </ol>
     * The backup file is only created if meaningful changes are detected,
     * avoiding unnecessary backups for version-only updates.
     *
     * <pre>{@code
     * // In onEnable()
     * ConfigUpdater updater = new ConfigUpdater(this);
     * updater.checkAndUpdateConfig();
     * }</pre>
     */
    public void checkAndUpdateConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");

        // If config doesn't exist, create it with the version header
        if (!configFile.exists()) {
            createDefaultConfigWithHeader(configFile);
            plugin.getLogger().info("Created new config.yml");
            return;
        }

        FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);
        String configVersionStr = currentConfig.getString(CONFIG_VERSION_KEY, "0.0.0");
        Version configVersion = new Version(configVersionStr);
        Version pluginVersion = new Version(currentVersion);

        // No update needed
        if (configVersion.compareTo(pluginVersion) >= 0) {
            return;
        }

        // Check if this is a first-time installation (version 0.0.0)
        boolean isFirstInstall = configVersionStr.equals("0.0.0");

        // Only log update messages if this is not a first-time installation
        if (!isFirstInstall) {
            plugin.getLogger().info("Updating config from version " + configVersionStr + " to " + currentVersion);
        }

        try {
            // Store user's current values
            Map<String, Object> userValues = flattenConfig(currentConfig);

            // Create temp file with new default config
            File tempFile = new File(plugin.getDataFolder(), "config_new.yml");
            createDefaultConfigWithHeader(tempFile);

            FileConfiguration newConfig = YamlConfiguration.loadConfiguration(tempFile);
            newConfig.set(CONFIG_VERSION_KEY, currentVersion);

            // Check if there are actual differences before creating backup
            boolean configDiffers = hasConfigDifferences(userValues, newConfig);

            // Only create backup and log if this is not a first-time installation
            if (configDiffers && !isFirstInstall) {
                File backupFile = new File(plugin.getDataFolder(), "config_backup_" + configVersionStr + ".yml");
                Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("Config backup created at " + backupFile.getName());
            } else if (!isFirstInstall) {
                plugin.getLogger().info("No significant config changes detected, skipping backup creation");
            }

            // Apply user values and save
            applyUserValues(newConfig, userValues);
            newConfig.save(configFile);
            tempFile.delete();
            plugin.reloadConfig();

            // Only log success message if this is not a first-time installation
            if (!isFirstInstall) {
                plugin.getLogger().info("Successfully updated config.yml to version " + currentVersion);
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to update config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Determines if there are actual differences between old and new config.
     * <p>
     * This method compares the configurations to detect:
     * <ul>
     *   <li>Removed keys</li>
     *   <li>Changed default values</li>
     *   <li>New keys</li>
     * </ul>
     * If no meaningful changes are detected, backup creation is skipped.
     * </p>
     *
     * @param userValues Map of user's current configuration values
     * @param newConfig  The new default configuration
     * @return true if there are meaningful differences, false otherwise
     */
    private boolean hasConfigDifferences(Map<String, Object> userValues, FileConfiguration newConfig) {
        // Get all paths from new config (excluding config_version)
        Map<String, Object> newConfigMap = flattenConfig(newConfig);

        // Check for removed or changed keys
        for (Map.Entry<String, Object> entry : userValues.entrySet()) {
            String path = entry.getKey();
            Object oldValue = entry.getValue();

            // Skip config_version key
            if (path.equals(CONFIG_VERSION_KEY)) continue;

            // Check if path no longer exists
            if (!newConfig.contains(path)) {
                return true; // Found a removed path
            }

            // Check if default value changed
            Object newDefaultValue = newConfig.get(path);
            if (newDefaultValue != null && !newDefaultValue.equals(oldValue)) {
                return true; // Default value changed
            }
        }

        // Check for new keys
        for (String path : newConfigMap.keySet()) {
            if (!path.equals(CONFIG_VERSION_KEY) && !userValues.containsKey(path)) {
                return true; // Found a new path
            }
        }

        return false; // No significant differences
    }

    /**
     * Creates a default config file with a version header.
     * <p>
     * This method attempts to load the default config from the plugin's resources.
     * If the resource doesn't exist, it creates an empty file.
     * </p>
     *
     * @param destinationFile The file to create
     */
    private void createDefaultConfigWithHeader(File destinationFile) {
        try {
            // Ensure parent directory exists (plugins/PluginName/)
            File parentDir = destinationFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            try (InputStream in = plugin.getResource("config.yml")) {
                if (in != null) {
                    List<String> defaultLines = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                            .lines()
                            .toList();

                    List<String> newLines = new ArrayList<>();
                    newLines.add("# Configuration version - Do not modify this value");
                    newLines.add(CONFIG_VERSION_KEY + ": " + currentVersion);
                    newLines.add("");
                    newLines.addAll(defaultLines);

                    Files.write(destinationFile.toPath(), newLines, StandardCharsets.UTF_8);
                } else {
                    plugin.getLogger().warning("Default config.yml not found in the plugin's resources.");
                    destinationFile.createNewFile();
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create default config with header: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Flattens a configuration section into a map of path to value.
     * <p>
     * This method converts a nested YAML structure into a flat map where
     * keys are dot-separated paths and values are the leaf values.
     * </p>
     *
     * @param config The configuration section to flatten
     * @return A map of path to value
     *
     * @example
     * <pre>{@code
     * // YAML:
     * // player:
     * //   health: 20
     * //   level: 5
     *
     * // Result:
     * // {
     * //   "player.health": 20,
     * //   "player.level": 5
     * // }
     * }</pre>
     */
    private Map<String, Object> flattenConfig(ConfigurationSection config) {
        Map<String, Object> result = new HashMap<>();
        for (String key : config.getKeys(true)) {
            if (!config.isConfigurationSection(key)) {
                result.put(key, config.get(key));
            }
        }
        return result;
    }

    /**
     * Applies user values to the new configuration.
     * <p>
     * This method preserves user customizations by applying their values
     * to the new configuration. If a key no longer exists in the new config,
     * a warning is logged but the update continues.
     * </p>
     *
     * @param newConfig  The new configuration to update
     * @param userValues Map of user's custom values to apply
     */
    private void applyUserValues(FileConfiguration newConfig, Map<String, Object> userValues) {
        for (Map.Entry<String, Object> entry : userValues.entrySet()) {
            String path = entry.getKey();
            Object value = entry.getValue();

            // Don't override config_version
            if (path.equals(CONFIG_VERSION_KEY)) continue;

            if (newConfig.contains(path)) {
                newConfig.set(path, value);
            } else {
                plugin.getLogger().warning("Config path '" + path + "' from old config no longer exists in new config");
            }
        }
    }

    /**
     * Gets the current plugin version.
     *
     * @return The current plugin version string
     */
    public String getCurrentVersion() {
        return currentVersion;
    }

    /**
     * Gets the configuration version key used for tracking.
     *
     * @return The config version key ("config_version")
     */
    public static String getConfigVersionKey() {
        return CONFIG_VERSION_KEY;
    }
}