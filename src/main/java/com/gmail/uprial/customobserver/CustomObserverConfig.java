package com.gmail.uprial.customobserver;

import com.gmail.uprial.customobserver.common.CustomLogger;
import com.gmail.uprial.customobserver.config.ConfigReaderSimple;
import com.gmail.uprial.customobserver.config.InvalidConfigException;
import org.bukkit.configuration.file.FileConfiguration;

public final class CustomObserverConfig {
    private final boolean enabled;

    private CustomObserverConfig(final boolean enabled) {
        this.enabled = enabled;
    }

    static boolean isDebugMode(FileConfiguration config, CustomLogger customLogger) throws InvalidConfigException {
        return ConfigReaderSimple.getBoolean(config, customLogger, "debug", "'debug' flag", false);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static CustomObserverConfig getFromConfig(FileConfiguration config, CustomLogger customLogger) throws InvalidConfigException {
        final boolean enabled = ConfigReaderSimple.getBoolean(config, customLogger, "enabled", "'enabled' flag", true);

        return new CustomObserverConfig(enabled);
    }

    public String toString() {
        return String.format("enabled: %b", enabled);
    }
}
