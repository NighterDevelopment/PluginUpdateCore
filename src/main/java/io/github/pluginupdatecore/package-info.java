/**
 * PluginUpdateCore - Automatic update checking and configuration management library for Minecraft plugins.
 *
 * <h2>Overview</h2>
 * <p>
 * PluginUpdateCore provides automatic update checking from Modrinth and configuration file
 * management with version tracking. It features smart update detection, user customization
 * preservation, and formatted console/in-game notifications.
 * </p>
 *
 * <h2>Main Components</h2>
 * <ul>
 *   <li>{@link io.github.pluginupdatecore.updater.UpdateChecker} - Modrinth update checker with notifications</li>
 *   <li>{@link io.github.pluginupdatecore.updater.ConfigUpdater} - Automatic config.yml updater</li>
 *   <li>{@link io.github.pluginupdatecore.version.Version} - Semantic version comparison</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * public class MyPlugin extends JavaPlugin {
 *     @Override
 *     public void onEnable() {
 *         // Check for updates from Modrinth
 *         new UpdateChecker(this, "YOUR_MODRINTH_PROJECT_ID");
 *
 *         // Update config.yml automatically
 *         ConfigUpdater configUpdater = new ConfigUpdater(this);
 *         configUpdater.checkAndUpdateConfig();
 *     }
 * }
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Automatic Modrinth update checking</li>
 *   <li>Server version compatibility detection</li>
 *   <li>Console and in-game notifications</li>
 *   <li>Version-tracked config file updates</li>
 *   <li>Smart backup creation (only when needed)</li>
 *   <li>User customization preservation</li>
 *   <li>Daily notification limits to avoid spam</li>
 * </ul>
 *
 * @since 1.0.0
 * @version 1.0.0
 */
package io.github.pluginupdatecore;