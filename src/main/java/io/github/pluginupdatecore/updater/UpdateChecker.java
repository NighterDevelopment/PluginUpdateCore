package io.github.pluginupdatecore.updater;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.pluginupdatecore.version.Version;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Automatic update checker for Minecraft plugins using the Modrinth API.
 * This class provides automatic update detection from Modrinth, including:
 *
 * <ul>
 *   <li>Version comparison between current and latest releases</li>
 *   <li>Server version compatibility checking</li>
 *   <li>Console notifications with colored output</li>
 *   <li>In-game notifications for operators</li>
 *   <li>Daily notification limits to avoid spam</li>
 *   <li>Automatic event registration</li>
 * </ul>
 *
 * The checker automatically registers itself as a Bukkit listener and will
 * notify operators when they join the server (once per day maximum).
 *
 * @author PluginUpdateCore Team
 * @version 1.0.0
 * @since 1.0.0
 *
 *
 * <pre>{@code
 * public class MyPlugin extends JavaPlugin {
 *     @Override
 *     public void onEnable() {
 *         // Initialize update checker with your Modrinth project ID
 *         new UpdateChecker(this, "YOUR_PROJECT_ID");
 *     }
 * }
 * }</pre>
 */
public class UpdateChecker implements Listener {
    private final JavaPlugin plugin;
    private final String projectId;
    private boolean updateAvailable = false;
    private final String currentVersion;
    private String latestVersion = "";
    private String downloadUrl = "";
    private String directLink = "";
    private boolean serverVersionSupported = true;
    private JsonArray latestSupportedVersions = null;

    // Console color codes for pretty output
    private static final String CONSOLE_RESET = "\u001B[0m";
    private static final String CONSOLE_BRIGHT_GREEN = "\u001B[92m";
    private static final String CONSOLE_YELLOW = "\u001B[33m";
    private static final String CONSOLE_INDIGO = "\u001B[38;5;93m";
    private static final String CONSOLE_LAVENDER = "\u001B[38;5;183m";
    private static final String CONSOLE_BRIGHT_PURPLE = "\u001B[95m";
    private static final String CONSOLE_RED = "\u001B[91m";

    private final Map<UUID, LocalDate> notifiedPlayers = new HashMap<>();

    /**
     * Constructs an UpdateChecker and immediately checks for updates.
     *
     * This constructor:
     * <ul>
     *   <li>Registers the update checker as a Bukkit event listener</li>
     *   <li>Starts an asynchronous update check</li>
     *   <li>Displays console notifications if updates are available</li>
     * </ul>
     *
     *
     * @param plugin    The JavaPlugin instance
     * @param projectId The Modrinth project ID (e.g., "9tQwxSFr")
     *
     *
     * <pre>{@code
     * // Find your project ID in your Modrinth project URL:
     * // https://modrinth.com/plugin/YOUR_PROJECT_ID
     * new UpdateChecker(this, "9tQwxSFr");
     * }</pre>
     */
    public UpdateChecker(JavaPlugin plugin, String projectId) {
        this.plugin = plugin;
        this.projectId = projectId;
        this.currentVersion = plugin.getDescription().getVersion();

        // Register event listener for player join notifications
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Start async update check
        checkForUpdates().thenAccept(hasUpdate -> {
            if (hasUpdate && serverVersionSupported) {
                displayConsoleUpdateMessage();
            } else if (!serverVersionSupported) {
                displayUnsupportedVersionMessage();
            }
        }).exceptionally(ex -> {
            plugin.getLogger().warning("Failed to check for updates: " + ex.getMessage());
            return null;
        });
    }

    /**
     * Displays a formatted update notification in the console.
     * <p>
     * The message includes:
     * <ul>
     *   <li>Current and latest version numbers</li>
     *   <li>Download link to Modrinth</li>
     *   <li>Colored, formatted output for better visibility</li>
     * </ul>
     * </p>
     */
    private void displayConsoleUpdateMessage() {
        String modrinthLink = "https://modrinth.com/plugin/" + projectId + "/version/" + latestVersion;
        String frameColor = CONSOLE_INDIGO;

        plugin.getLogger().info(frameColor +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + CONSOLE_RESET);
        plugin.getLogger().info(frameColor + CONSOLE_BRIGHT_GREEN +
                "                🔮 UPDATE AVAILABLE 🔮" + CONSOLE_RESET);
        plugin.getLogger().info(frameColor +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + CONSOLE_RESET);
        plugin.getLogger().info("");
        plugin.getLogger().info(frameColor +
                CONSOLE_RESET + "📦 Current version: " + CONSOLE_YELLOW  + formatConsoleText(currentVersion, 31) + CONSOLE_RESET);
        plugin.getLogger().info(frameColor +
                CONSOLE_RESET + "✅ Latest version: " + CONSOLE_BRIGHT_GREEN + formatConsoleText(latestVersion, 32) + CONSOLE_RESET);
        plugin.getLogger().info("");
        plugin.getLogger().info(frameColor +
                CONSOLE_RESET + "📥 Download the latest version at:" + CONSOLE_RESET);
        plugin.getLogger().info(frameColor + " " +
                CONSOLE_LAVENDER + formatConsoleText(modrinthLink, 100) + CONSOLE_RESET);
        plugin.getLogger().info("");
        plugin.getLogger().info(frameColor +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + CONSOLE_RESET);
    }

    /**
     * Displays a warning message when the server version is no longer supported.
     * <p>
     * This message includes:
     * <ul>
     *   <li>Current server version</li>
     *   <li>Latest plugin version</li>
     *   <li>List of supported server versions</li>
     *   <li>Warning that update notifications are disabled</li>
     * </ul>
     * </p>
     */
    private void displayUnsupportedVersionMessage() {
        String frameColor = CONSOLE_RED;
        String serverVersion = Bukkit.getVersion();

        plugin.getLogger().warning(frameColor +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + CONSOLE_RESET);
        plugin.getLogger().warning(frameColor + CONSOLE_YELLOW +
                "      ⚠️  SERVER VERSION NO LONGER SUPPORTED  ⚠️" + CONSOLE_RESET);
        plugin.getLogger().warning(frameColor +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + CONSOLE_RESET);
        plugin.getLogger().warning("");
        plugin.getLogger().warning(frameColor +
                CONSOLE_RESET + "🖥️ Your server version: " + CONSOLE_YELLOW + serverVersion + CONSOLE_RESET);
        plugin.getLogger().warning(frameColor +
                CONSOLE_RESET + "📦 Latest plugin version: " + CONSOLE_BRIGHT_GREEN + latestVersion + CONSOLE_RESET);
        plugin.getLogger().warning(frameColor +
                CONSOLE_RESET + "🎯 Supported server versions: " + CONSOLE_LAVENDER + getSupportedVersionsString() + CONSOLE_RESET);
        plugin.getLogger().warning("");
        plugin.getLogger().warning(frameColor +
                CONSOLE_RESET + "⚠️  This server version is no longer supported" + CONSOLE_RESET);
        plugin.getLogger().warning(frameColor +
                CONSOLE_RESET + "📋 Update notifications disabled" + CONSOLE_RESET);
        plugin.getLogger().warning("");
        plugin.getLogger().warning(frameColor +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + CONSOLE_RESET);
    }

    /**
     * Gets a comma-separated string of supported server versions.
     *
     * @return String of supported versions or "N/A" if unavailable
     */
    private String getSupportedVersionsString() {
        if (latestSupportedVersions == null || latestSupportedVersions.isEmpty()) {
            return "N/A";
        }

        return latestSupportedVersions.asList().stream()
                .map(JsonElement::getAsString)
                .collect(Collectors.joining(", "));
    }

    /**
     * Formats console text to a fixed width for alignment.
     *
     * @param text      The text to format
     * @param maxLength Maximum length (truncates with "..." if longer)
     * @return Formatted text padded to maxLength
     */
    private String formatConsoleText(String text, int maxLength) {
        if (text.length() > maxLength) {
            return text.substring(0, maxLength - 3) + "...";
        }
        return text + " ".repeat(maxLength - text.length());
    }

    /**
     * Checks if the current server version is supported by the latest plugin version.
     * <p>
     * This method extracts the Minecraft version from Bukkit's version string
     * and compares it against the game_versions array from Modrinth.
     * </p>
     *
     * @param latestVersionObj The JSON object containing the latest version info
     * @return true if server version is supported, false otherwise
     */
    private boolean isServerVersionSupported(JsonObject latestVersionObj) {
        try {
            String serverVersion = Bukkit.getVersion();

            JsonArray gameVersions = latestVersionObj.getAsJsonArray("game_versions");
            if (gameVersions == null || gameVersions.isEmpty()) {
                return true; // No version restrictions
            }

            String cleanServerVersion = extractMinecraftVersion(serverVersion);

            for (JsonElement versionElement : gameVersions) {
                String supportedVersion = versionElement.getAsString();
                if (isVersionCompatible(cleanServerVersion, supportedVersion)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking server version compatibility: " + e.getMessage());
            return true; // Assume compatible if check fails
        }
    }

    /**
     * Extracts the Minecraft version from Bukkit's version string.
     * <p>
     * Handles various version string formats:
     * <ul>
     *   <li>git-Paper-123 (MC: 1.21.1)</li>
     *   <li>1.21.1-R0.1-SNAPSHOT</li>
     *   <li>Custom versions with version numbers</li>
     * </ul>
     * </p>
     *
     * @param serverVersion The Bukkit version string
     * @return The extracted Minecraft version (e.g., "1.21.1")
     */
    private String extractMinecraftVersion(String serverVersion) {
        // Try to extract from "MC: x.x.x" format
        if (serverVersion.contains("MC: ")) {
            String mcPart = serverVersion.substring(serverVersion.indexOf("MC: ") + 4);
            if (mcPart.contains(")")) {
                mcPart = mcPart.substring(0, mcPart.indexOf(")"));
            }
            return mcPart.trim();
        }

        // Try to find version pattern (x.x or x.x.x)
        if (serverVersion.matches(".*\\d+\\.\\d+(\\.\\d+)?.*")) {
            String[] parts = serverVersion.split("\\s+");
            for (String part : parts) {
                if (part.matches("\\d+\\.\\d+(\\.\\d+)?")) {
                    return part;
                }
            }
        }

        return serverVersion;
    }

    /**
     * Checks if two Minecraft versions are compatible.
     * <p>
     * Versions are considered compatible if their major and minor versions match.
     * For example, 1.21.1 is compatible with 1.21 and 1.21.0.
     * </p>
     *
     * @param serverVersion    The server's Minecraft version
     * @param supportedVersion The supported version from Modrinth
     * @return true if versions are compatible, false otherwise
     */
    private boolean isVersionCompatible(String serverVersion, String supportedVersion) {
        try {
            if (serverVersion.equals(supportedVersion)) {
                return true;
            }

            String[] serverParts = serverVersion.split("\\.");
            String[] supportedParts = supportedVersion.split("\\.");

            // Compare major and minor versions
            if (serverParts.length >= 2 && supportedParts.length >= 2) {
                int serverMajor = Integer.parseInt(serverParts[0]);
                int serverMinor = Integer.parseInt(serverParts[1]);
                int supportedMajor = Integer.parseInt(supportedParts[0]);
                int supportedMinor = Integer.parseInt(supportedParts[1]);

                return serverMajor == supportedMajor && serverMinor == supportedMinor;
            }

            return false;
        } catch (NumberFormatException e) {
            return serverVersion.equals(supportedVersion);
        }
    }

    /**
     * Checks for updates asynchronously using the Modrinth API.
     *
     * This method:
     * <ol>
     *   <li>Fetches all versions from Modrinth</li>
     *   <li>Finds the latest release version</li>
     *   <li>Checks server version compatibility</li>
     *   <li>Compares versions using semantic versioning</li>
     * </ol>
     *
     *
     * @return CompletableFuture that resolves to true if an update is available, false otherwise
     *
     *
     * <pre>{@code
     * updateChecker.checkForUpdates().thenAccept(hasUpdate -> {
     *     if (hasUpdate) {
     *         getLogger().info("Update available!");
     *     }
     * });
     * }</pre>
     */
    public CompletableFuture<Boolean> checkForUpdates() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL("https://api.modrinth.com/v2/project/" + projectId + "/version");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "PluginUpdateCore/1.0");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() != 200) {
                    plugin.getLogger().warning("Failed to check for updates. HTTP Error: " + connection.getResponseCode());
                    return false;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String response = reader.lines().collect(Collectors.joining("\n"));
                reader.close();

                JsonArray versions = JsonParser.parseString(response).getAsJsonArray();
                if (versions.isEmpty()) {
                    return false;
                }

                // Find the latest release version
                JsonObject latestVersionObj = null;
                for (JsonElement element : versions) {
                    JsonObject version = element.getAsJsonObject();
                    String versionType = version.get("version_type").getAsString();
                    if (versionType.equals("release")) {
                        if (latestVersionObj == null) {
                            latestVersionObj = version;
                        } else {
                            String currentDate = latestVersionObj.get("date_published").getAsString();
                            String newDate = version.get("date_published").getAsString();
                            if (newDate.compareTo(currentDate) > 0) {
                                latestVersionObj = version;
                            }
                        }
                    }
                }

                if (latestVersionObj == null) {
                    return false;
                }

                latestVersion = latestVersionObj.get("version_number").getAsString();
                downloadUrl = "https://modrinth.com/plugin/" + projectId + "/version/" + latestVersion;

                // Get direct download link
                JsonArray files = latestVersionObj.getAsJsonArray("files");
                if (!files.isEmpty()) {
                    JsonObject primaryFile = files.get(0).getAsJsonObject();
                    directLink = primaryFile.get("url").getAsString();
                }

                serverVersionSupported = isServerVersionSupported(latestVersionObj);
                latestSupportedVersions = latestVersionObj.getAsJsonArray("game_versions");

                Version latest = new Version(latestVersion);
                Version current = new Version(currentVersion);

                updateAvailable = latest.compareTo(current) > 0;
                return updateAvailable;

            } catch (Exception e) {
                plugin.getLogger().warning("Error checking for updates: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Sends an in-game update notification to a player.
     * <p>
     * The notification includes:
     * <ul>
     *   <li>Formatted chat message with version information</li>
     *   <li>Clickable link to download page</li>
     *   <li>Sound effect</li>
     * </ul>
     * Only shown to players with admin permissions.
     * </p>
     *
     * @param player The player to notify
     */
    private void sendUpdateNotification(Player player) {
        if (!updateAvailable || !serverVersionSupported || !player.hasPermission("admin")) {
            return;
        }

        TextColor primaryPurple = TextColor.fromHexString("#ab7afd");
        TextColor deepPurple = TextColor.fromHexString("#7b68ee");
        TextColor brightGreen = TextColor.fromHexString("#37eb9a");
        TextColor yellow = TextColor.fromHexString("#f0c857");
        TextColor white = TextColor.fromHexString("#e6e6fa");

        Component borderTop = Component.text("━━━━━━━━ UPDATE AVAILABLE ━━━━━━━━").color(deepPurple);
        Component borderBottom = Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(deepPurple);

        Component updateMsg = Component.text("➤ New update available!").color(brightGreen);

        Component versionsComponent = Component.text("✦ Current: ")
                .color(white)
                .append(Component.text(currentVersion).color(yellow))
                .append(Component.text("  ✦ Latest: ").color(white))
                .append(Component.text(latestVersion).color(brightGreen));

        Component downloadButton = Component.text("▶ [Click to download latest version]")
                .color(primaryPurple)
                .clickEvent(ClickEvent.openUrl(downloadUrl))
                .hoverEvent(HoverEvent.showText(
                        Component.text("Download version ")
                                .color(white)
                                .append(Component.text(latestVersion).color(brightGreen))
                ));

        player.sendMessage(" ");
        player.sendMessage(borderTop);
        player.sendMessage(" ");
        player.sendMessage(updateMsg);
        player.sendMessage(versionsComponent);
        player.sendMessage(downloadButton);
        player.sendMessage(" ");
        player.sendMessage(borderBottom);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
    }

    /**
     * Handles player join events to send update notifications.
     *
     * Notifications are sent only:
     * <ul>
     *   <li>To operators</li>
     *   <li>Once per day maximum per player</li>
     *   <li>After a 2-second delay</li>
     *   <li>When an update is available and server version is supported</li>
     * </ul>
     *
     *
     * @param event The player join event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (player.isOp()) {
            UUID playerId = player.getUniqueId();
            LocalDate today = LocalDate.now();

            // Clean up old entries
            notifiedPlayers.entrySet().removeIf(entry -> entry.getValue().isBefore(today));

            // Check if already notified today
            if (notifiedPlayers.containsKey(playerId) && notifiedPlayers.get(playerId).isEqual(today)) {
                return;
            }

            if (updateAvailable && serverVersionSupported) {
                // Schedule notification after 2 seconds
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    sendUpdateNotification(player);
                    notifiedPlayers.put(playerId, today);
                }, 40L); // 40 ticks = 2 seconds
            } else if (!serverVersionSupported) {
                return; // Don't notify for unsupported versions
            } else {
                // Check for updates if not already checked
                checkForUpdates().thenAccept(hasUpdate -> {
                    if (hasUpdate && serverVersionSupported) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            sendUpdateNotification(player);
                            notifiedPlayers.put(playerId, today);
                        });
                    }
                });
            }
        }
    }

    /**
     * Gets whether an update is currently available.
     *
     * @return true if an update is available, false otherwise
     */
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    /**
     * Gets the latest version number from Modrinth.
     *
     * @return The latest version string
     */
    public String getLatestVersion() {
        return latestVersion;
    }

    /**
     * Gets the download URL for the latest version.
     *
     * @return The Modrinth download page URL
     */
    public String getDownloadUrl() {
        return downloadUrl;
    }

    /**
     * Gets the direct download link for the latest version JAR file.
     *
     * @return The direct download URL
     */
    public String getDirectLink() {
        return directLink;
    }

    /**
     * Gets whether the current server version is supported by the latest plugin version.
     *
     * @return true if server version is supported, false otherwise
     */
    public boolean isServerVersionSupported() {
        return serverVersionSupported;
    }
}