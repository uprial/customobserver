package com.gmail.uprial.customobserver;

import com.gmail.uprial.customobserver.common.CustomLogger;
import com.gmail.uprial.customobserver.config.InvalidConfigException;
import com.gmail.uprial.customobserver.listeners.ObserverListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

import static com.gmail.uprial.customobserver.CustomObserverCommandExecutor.COMMAND_NS;

public final class CustomObserver extends JavaPlugin {
    private final String CONFIG_FILE_NAME = "config.yml";
    private final File configFile = new File(getDataFolder(), CONFIG_FILE_NAME);

    private CustomLogger consoleLogger = null;
    private CustomObserverConfig customObserverConfig = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        consoleLogger = new CustomLogger(getLogger());
        customObserverConfig = loadConfig(getConfig(), consoleLogger);

        getServer().getPluginManager().registerEvents(new ObserverListener(this, consoleLogger), this);

        getCommand(COMMAND_NS).setExecutor(new CustomObserverCommandExecutor(this));
        consoleLogger.info("Plugin enabled");
    }

    public CustomObserverConfig getCustomObserverConfig() {
        return customObserverConfig;
    }

    void reloadConfig(CustomLogger userLogger) {
        reloadConfig();
        customObserverConfig = loadConfig(getConfig(), userLogger, consoleLogger);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        consoleLogger.info("Plugin disabled");
    }

    @Override
    public void saveDefaultConfig() {
        if (!configFile.exists()) {
            saveResource(CONFIG_FILE_NAME, false);
        }
    }

    @Override
    public FileConfiguration getConfig() {
        return YamlConfiguration.loadConfiguration(configFile);
    }

    static CustomObserverConfig loadConfig(FileConfiguration config, CustomLogger customLogger) {
        return loadConfig(config, customLogger, null);
    }

    private static CustomObserverConfig loadConfig(FileConfiguration config, CustomLogger mainLogger, CustomLogger secondLogger) {
        CustomObserverConfig customObserverConfig = null;
        try {
            boolean isDebugMode = CustomObserverConfig.isDebugMode(config, mainLogger);
            mainLogger.setDebugMode(isDebugMode);
            if(secondLogger != null) {
                secondLogger.setDebugMode(isDebugMode);
            }

            customObserverConfig = CustomObserverConfig.getFromConfig(config, mainLogger);
        } catch (InvalidConfigException e) {
            mainLogger.error(e.getMessage());
        }

        return customObserverConfig;
    }
}
