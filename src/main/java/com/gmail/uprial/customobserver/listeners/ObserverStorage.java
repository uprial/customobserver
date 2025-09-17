package com.gmail.uprial.customobserver.listeners;

import com.gmail.uprial.customobserver.CustomObserver;
import com.gmail.uprial.customobserver.common.CustomLogger;
import com.gmail.uprial.customobserver.storage.CustomStorage;
import com.gmail.uprial.customobserver.storage.StorageData;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class ObserverStorage {
    private final CustomObserver plugin;
    private final CustomLogger customLogger;

    private static final String KEY_DELIMITER = ":";

    private final Map<Location, Location> signs = new HashMap<>();
    private final Map<Location, Location> signsBack = new HashMap<>();

    private final Map<Location, Location> targets = new HashMap<>();
    private final Map<Location, Location> targetsBack = new HashMap<>();

    ObserverStorage(final CustomObserver plugin,
                    final CustomLogger customLogger) {
        this.plugin = plugin;
        this.customLogger = customLogger;

        load();
    }

    Location getSignLocationByObserver(final Location observerLocation) {
        return signs.get(observerLocation);
    }

    Location getObserverLocationBySign(final Location signLocation) {
        return signsBack.get(signLocation);
    }

    Location getObserverLocationByTarget(final Location targetLocation) {
        return targetsBack.get(targetLocation);
    }

    boolean isObserverLocation(final Location observerLocation) {
        return targets.containsKey(observerLocation);
    }

    void add(final Location observerLocation,
             final Location signLocation,
             final Location targetLocation) {

        signs.put(observerLocation, signLocation);
        signsBack.put(signLocation, observerLocation);

        targets.put(observerLocation, targetLocation);
        targetsBack.put(targetLocation, observerLocation);

        save();
    }

    void remove(final Location observerLocation) {
        targetsBack.remove(targets.get(observerLocation));
        targets.remove(observerLocation);

        signsBack.remove(signs.get(observerLocation));
        signs.remove(observerLocation);

        save();
    }

    private void save() {
        save(targets, "targets.txt");
        save(signs, "signs.txt");
    }

    private void save(final Map<Location, Location> map, final String filename) {
        final StorageData data = new StorageData();

        for (final Map.Entry<Location, Location> entry : map.entrySet()) {
            data.put(location2string(entry.getKey()), location2string(entry.getValue()));
        }

        new CustomStorage(plugin.getDataFolder(), filename, customLogger).save(data);
    }

    private String location2string(final Location location) {
        final String[] keyParts = new String[4];

        keyParts[0] = location.getWorld().getName();
        keyParts[1] = String.valueOf(location.getBlockX());
        keyParts[2] = String.valueOf(location.getBlockY());
        keyParts[3] = String.valueOf(location.getBlockZ());

        return StringUtils.join(keyParts, KEY_DELIMITER);
    }

    private void load() {
        load(targets, "targets.txt");
        load(signs, "signs.txt");

        checkDiff("targets", targets.keySet(), "signs", signs.keySet());
        checkDiff("signs", signs.keySet(), "targets", targets.keySet());

        loadBack(targetsBack, targets);
        loadBack(signsBack, signs);
    }

    private void checkDiff(final String title1, final Set<Location> set1,
                           final String title2, final Set<Location> set2) {
        final Set<Location> diff = new HashSet<>(set1);
        diff.removeAll(set2);

        if(!diff.isEmpty()) {
            throw new RuntimeException(
                    String.format("Not empty %s without %s: %s",
                            title1, title2, diff));
        }
    }

    private void loadBack(final Map<Location, Location> mapBack, final Map<Location, Location> map) {
        mapBack.clear();
        for(Map.Entry<Location, Location> entry : map.entrySet()) {
            mapBack.put(entry.getValue(), entry.getKey());
        }
    }

    private void load(final Map<Location, Location> map, final String filename) {
        final StorageData data = new CustomStorage(plugin.getDataFolder(), filename, customLogger).load();

        map.clear();
        for (final Map.Entry<String,String> entry : data.entrySet()) {
            map.put(string2location(entry.getKey()), string2location(entry.getValue()));
        }
    }

    private Location string2location(final String string) {
        final String[] keyParts = StringUtils.split(string, KEY_DELIMITER);
        try {
            return new Location(
                    plugin.getServer().getWorld(keyParts[0]),
                    Integer.valueOf(keyParts[1]),
                    Integer.valueOf(keyParts[2]),
                    Integer.valueOf(keyParts[3]));
        } catch (NumberFormatException e) {
            customLogger.error(String.format("Can't recognize location: %s", string));
            throw e;
        }
    }
}
