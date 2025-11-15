# PluginUpdateCore

Automatic update checking and configuration management library for Minecraft plugins.

## Features

- 🔄 **Modrinth Update Checker** - Automatic update detection from Modrinth
- 📦 **Version Comparison** - Semantic versioning support
- ⚙️ **Config Auto-Update** - Version-tracked configuration file updates
- 💾 **Smart Backups** - Only creates backups when meaningful changes detected
- 🎨 **Rich Notifications** - Formatted console and in-game update messages
- 🔒 **User Preservation** - Keeps all user customizations during updates
- 🎯 **Server Compatibility** - Automatic server version compatibility checking
- 📅 **Daily Limits** - Prevents notification spam (once per day per player)

## Installation

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.NighterDevelopment</groupId>
        <artifactId>PluginUpdateCore</artifactId>
        <version>1.0.0</version>
        <scope>compile</scope>
    </dependency>
</dependencies>
```

### Gradle

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.NighterDevelopment:PluginUpdateCore:1.0.0'
}
```

## Quick Start

### Update Checker

```java
import io.github.pluginupdatecore.updater.UpdateChecker;

public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // Initialize update checker with your Modrinth project ID
        // Find your project ID at: https://modrinth.com/plugin/YOUR_PROJECT
        new UpdateChecker(this, "YOUR_MODRINTH_PROJECT_ID");
    }
}
```

**Features:**
- Automatic version checking on startup
- Console notifications with colored output
- In-game notifications for operators (once per day)
- Clickable download links
- Server version compatibility checking

### Config Updater

```java
import io.github.pluginupdatecore.updater.ConfigUpdater;

public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // Update config.yml automatically
        ConfigUpdater configUpdater = new ConfigUpdater(this);
        configUpdater.checkAndUpdateConfig();
        
        // Then load your config
        reloadConfig();
    }
}
```

**Features:**
- Automatic version tracking in config.yml
- Preserves user customizations
- Adds new keys from defaults
- Creates timestamped backups (only when needed)
- Smart change detection

## Usage Examples

### Example 1: Basic Setup

```java
package com.example.myplugin;

import io.github.pluginupdatecore.updater.UpdateChecker;
import io.github.pluginupdatecore.updater.ConfigUpdater;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        // Check for updates from Modrinth
        new UpdateChecker(this, "9tQwxSFr"); // Your Modrinth project ID
        
        // Update config.yml
        ConfigUpdater configUpdater = new ConfigUpdater(this);
        configUpdater.checkAndUpdateConfig();
        
        // Continue with plugin initialization
        getLogger().info("Plugin enabled!");
    }
}
```

### Example 2: Manual Update Check

```java
import io.github.pluginupdatecore.updater.UpdateChecker;

public class UpdateCommand implements CommandExecutor {
    private final UpdateChecker updateChecker;
    
    public UpdateCommand(JavaPlugin plugin, String projectId) {
        this.updateChecker = new UpdateChecker(plugin, projectId);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("Checking for updates...");
        
        updateChecker.checkForUpdates().thenAccept(hasUpdate -> {
            if (hasUpdate) {
                sender.sendMessage("Update available: " + updateChecker.getLatestVersion());
                sender.sendMessage("Download: " + updateChecker.getDownloadUrl());
            } else {
                sender.sendMessage("You are running the latest version!");
            }
        });
        
        return true;
    }
}
```

### Example 3: Config Update with Custom Logic

```java
import io.github.pluginupdatecore.updater.ConfigUpdater;

public class MyPlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        // Create updater
        ConfigUpdater configUpdater = new ConfigUpdater(this);
        
        // Check and update config
        configUpdater.checkAndUpdateConfig();
        
        // Reload and validate config
        reloadConfig();
        validateConfig();
    }
    
    private void validateConfig() {
        // Your custom config validation logic
        if (!getConfig().contains("some-required-key")) {
            getLogger().warning("Config is missing required key!");
        }
    }
}
```

## How It Works

### UpdateChecker

1. **On Initialization**:
    - Fetches all versions from Modrinth API
    - Finds the latest release version
    - Compares with current plugin version
    - Checks server version compatibility
    - Displays console notification if update available

2. **On Player Join** (Operators only):
    - Checks if update is available
    - Verifies player hasn't been notified today
    - Sends formatted in-game message after 2-second delay
    - Plays level-up sound
    - Records notification to prevent spam

3. **Console Output Example**:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
         🔮 UPDATE AVAILABLE 🔮
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📦 Current version: 1.0.0
✅ Latest version: 1.1.0

📥 Download the latest version at:
   https://modrinth.com/plugin/...

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### ConfigUpdater

1. **Version Tracking**:
    - Adds `config_version` key to config.yml
    - Compares file version with plugin version

2. **Update Process** (when plugin version is newer):
    - Loads current user configuration
    - Loads new default configuration
    - Compares for meaningful changes
    - Creates backup if changes detected (e.g., `config_backup_1.0.0.yml`)
    - Merges user values with new defaults
    - Saves updated configuration
    - Reloads plugin config

3. **Example Updated Config**:
```yaml
# Configuration version - Do not modify this value
config_version: 1.1.0

# User's settings are preserved
existing-setting: user-value

# New settings are added from defaults
new-setting: default-value
```

## Configuration

### For Your Plugin

Add to your plugin.yml:
```yaml
name: MyPlugin
version: 1.0.0  # This version is used for comparison
main: com.example.myplugin.MyPlugin
```

### Config Version Tracking

The updater automatically adds this to config.yml:
```yaml
# Configuration version - Do not modify this value
config_version: 1.0.0
```

Users should not modify this value manually.

## Finding Your Modrinth Project ID

1. Go to your plugin page on Modrinth
2. Look at the URL: `https://modrinth.com/plugin/YOUR_PROJECT_ID`
3. The project ID is the slug after `/plugin/`
4. Example: For SmartSpawner, the URL is `https://modrinth.com/plugin/9tQwxSFr`
5. So the project ID is: `9tQwxSFr`

## API Reference

### UpdateChecker

| Method | Description |
|--------|-------------|
| `new UpdateChecker(plugin, projectId)` | Initialize and start checking |
| `checkForUpdates()` | Manually check for updates (async) |
| `isUpdateAvailable()` | Check if update is available |
| `getLatestVersion()` | Get latest version string |
| `getDownloadUrl()` | Get Modrinth download page URL |
| `getDirectLink()` | Get direct JAR download URL |
| `isServerVersionSupported()` | Check if server version is supported |

### ConfigUpdater

| Method | Description |
|--------|-------------|
| `new ConfigUpdater(plugin)` | Create updater instance |
| `checkAndUpdateConfig()` | Check and update config if needed |
| `getCurrentVersion()` | Get current plugin version |
| `getConfigVersionKey()` | Get version key name ("config_version") |

### Version

| Method | Description |
|--------|-------------|
| `new Version(string)` | Parse version string |
| `compareTo(other)` | Compare versions |
| `isNewerThan(other)` | Check if newer |
| `isOlderThan(other)` | Check if older |
| `isEqualTo(other)` | Check if equal |
| `getMajor()` | Get major version number |
| `getMinor()` | Get minor version number |
| `getPatch()` | Get patch version number |
| `getBuild()` | Get build version number |

## Requirements

- **Minecraft**: 1.21+ (for Adventure API support)
- **Java**: 21+
- **Server**: Paper or compatible forks

## Best Practices

1. **Run ConfigUpdater before loading config**
   ```java
   configUpdater.checkAndUpdateConfig();
   reloadConfig(); // Then load
   ```

2. **Initialize UpdateChecker in onEnable()**
   ```java
   // Put at the beginning of onEnable()
   new UpdateChecker(this, "YOUR_PROJECT_ID");
   ```

3. **Don't modify config_version manually**
    - Let the updater manage this automatically

4. **Provide default config.yml in resources**
    - The updater needs it to merge with user config

5. **Test updates with version increments**
    - Change plugin version in plugin.yml
    - Restart server to test update process

## Troubleshooting

### Update Not Detected
- Verify your Modrinth project ID is correct
- Check if you're using release versions (not beta/alpha)
- Ensure plugin version in plugin.yml is correct

### Config Not Updating
- Verify default config.yml exists in resources
- Check logs for error messages
- Ensure config_version format is valid

### Notifications Not Showing
- Verify player has operator status
- Check if notification was already sent today
- Ensure update is actually available

## License

This library is licensed under the MIT License - see the LICENSE file for details.

## Credits

Originally extracted and refactored from the SmartSpawner plugin.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.