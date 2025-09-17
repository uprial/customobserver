package com.gmail.uprial.customobserver.listeners;

import com.gmail.uprial.customobserver.CustomObserver;
import com.gmail.uprial.customobserver.common.CustomLogger;
import com.gmail.uprial.customobserver.storage.CustomStorage;
import com.gmail.uprial.customobserver.storage.StorageData;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Observer;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;

import java.util.*;

import static com.gmail.uprial.customobserver.common.Formatter.format;
import static com.gmail.uprial.customobserver.common.Utils.joinStrings;

public class ObserverListener implements Listener {
    private static final String KEY_DELIMITER = ":";

    private final CustomObserver plugin;
    private final CustomLogger customLogger;

    private final Map<Location, Location> signs = new HashMap<>();
    private final Map<Location, Location> observers = new HashMap<>();
    private final Map<Location, Location> targets = new HashMap<>();

    public ObserverListener(final CustomObserver plugin,
                            final CustomLogger customLogger) {
        this.plugin = plugin;
        this.customLogger = customLogger;

        load();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onSignChange(final SignChangeEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Block sign = event.getBlock();
        final Block observer = getSignObserver(sign);
        if(observer == null) {
            return;
        }

        Integer distance = null;
        for (final String line : event.getLines()) {
            final int tmpDistance;
            try {
                tmpDistance = Integer.valueOf(line);

                if ((distance == null) || (tmpDistance > distance)) {
                    distance = tmpDistance;
                }
            } catch (NumberFormatException ignored) {
                // nop
            }
        }
        if (distance == null) {
            customLogger.debug(String.format("No distance: [%s]",
                    joinStrings("|", Arrays.asList(event.getLines()))));
            return;
        } else if (distance < 2) {
            customLogger.debug(String.format("No natural distance: %d", distance));
            return;
        } else if (distance > plugin.getServer().getSimulationDistance() * 16) {
            customLogger.debug(String.format("Too big distance: %d", distance));
            return;
        }

        final BlockFace observerFacing = ((Observer) observer.getBlockData()).getFacing();
        if (Math.abs(observerFacing.getModX() + observerFacing.getModY() + observerFacing.getModZ()) != 1) {
            customLogger.debug(String.format("Wrong observer facing: %s", observerFacing));
            return;
        }

        final Block target = getBlockInDirection(observer, observerFacing, distance);

        final Location otherSignLocation = signs.get(observer.getLocation());
        if(otherSignLocation != null) {
            if(!otherSignLocation.equals(sign.getLocation())) {
                event.getPlayer().sendMessage(ChatColor.RED +
                        String.format("Another sign already exists: %s",
                                format(sign)));
                customLogger.info(
                        String.format("Another sign already exists for %s: %s",
                                format(event.getPlayer()), format(sign)));
                event.setCancelled(true);
                return;
            }
        }

        event.getPlayer().sendMessage(
                String.format("Aimed %s at %s",
                        format(observer), format(target)));
        customLogger.info(
                String.format("Aimed %s at %s by %s",
                        format(observer), format(target), format(event.getPlayer())));

        observers.put(target.getLocation(), observer.getLocation());
        targets.put(observer.getLocation(), target.getLocation());
        signs.put(observer.getLocation(), sign.getLocation());
        save();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPhysics(final BlockPhysicsEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!event.getSourceBlock().getLocation().equals(event.getBlock().getLocation())) {
            return;
        }

        final Location observerLocation = observers.get(event.getSourceBlock().getLocation());
        if(observerLocation == null) {
            return;
        }

        /*
            According to https://minecraft.wiki/w/Observer,
            When it detects something,
            the observer emits a redstone pulse ... for 2 game ticks.
            ...
            the pulse is emitted with a delay of 2 game ticks.
         */
        schedule(() -> setPowered(observerLocation, true,
                () -> schedule(() -> setPowered(observerLocation, false, () -> {}),
                        2)),
                2);

        customLogger.debug(String.format("Changed %s", format(event.getSourceBlock())));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(final BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Block observer = getSignObserver(event.getBlock());
        if(observer != null) {
            final Location signLocation = signs.get(observer.getLocation());
            if((signLocation != null)
                    && (signLocation.equals(event.getBlock().getLocation()))) {
                remove(observer, false);
            }
        } else if (event.getBlock().getType().equals(Material.OBSERVER)) {
            remove(event.getBlock(), true);
        }
    }

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPistonExtend(final BlockPistonExtendEvent event) {
        if (event.isCancelled()) {
            return;
        }

        for(final Block block : event.getBlocks()) {
            if (block.getType().equals(Material.OBSERVER)) {
                remove(block, true);
            }
        }
    }

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPistonRetract(final BlockPistonRetractEvent event) {
        if (event.isCancelled()) {
            return;
        }

        for(final Block block : event.getBlocks()) {
            if (block.getType().equals(Material.OBSERVER)) {
                remove(block, true);
            }
        }
    }

    private Block getSignObserver(final Block sign) {
        if (!(sign.getBlockData() instanceof WallSign)) {
            //customLogger.debug(String.format("Not a WallSign: %s", format(sign)));
            return null;
        }

        final BlockFace signFacing = ((WallSign) sign.getBlockData()).getFacing();
        if ((signFacing.getModY() != 0)
                || (Math.abs(signFacing.getModX() + signFacing.getModZ()) != 1)) {
            //customLogger.debug(String.format("Wrong sign facing: %s", signFacing));
            return null;
        }

        final Block observer = getBlockInDirection(sign, signFacing, -1);
        if (!observer.getType().equals(Material.OBSERVER)) {
            //customLogger.debug(String.format("No observer: %s", format(observer)));
            return null;
        }

        return observer;
    }

    private void setPowered(final Location observerLocation,
                            final boolean isPowered,
                            final Runnable callback) {
        final Block observer = observerLocation.getWorld().getBlockAt(observerLocation);
        if (!observer.getType().equals(Material.OBSERVER)) {
            customLogger.error(String.format("No observer: %s", format(observer)));
            return;
        }

        final Observer observerData = (Observer) observer.getBlockData();
        if(observerData.isPowered() != isPowered) {
            observerData.setPowered(isPowered);
            observer.setBlockData(observerData);

            customLogger.debug(String.format("%s power set to %b",
                    format(observer), isPowered));

            callback.run();
        }
    }

    private void remove(final Block observer, final boolean breakSign) {
        final Location observerLocation = observer.getLocation();
        if(targets.containsKey(observerLocation)) {
            if(breakSign) {
                observer.getWorld()
                        .getBlockAt(signs.get(observerLocation))
                        .breakNaturally();
            }

            observers.remove(targets.get(observerLocation));
            targets.remove(observerLocation);
            signs.remove(observerLocation);
            save();

            customLogger.info(String.format("%s deactivated", format(observer)));
        }
    }

    private Block getBlockInDirection(final Block block,
                                             final BlockFace direction,
                                             final int distance) {
        return block.getWorld().getBlockAt(
                block.getX() + direction.getModX() * distance,
                block.getY() + direction.getModY() * distance,
                block.getZ() + direction.getModZ() * distance);
    }

    private void schedule(final Runnable runnable, long delay) {
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, runnable, delay);
    }

    private void save() {
        save(observers, "observers.txt");
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
        load(observers, "observers.txt");
        load(targets, "targets.txt");
        load(signs, "signs.txt");
    }

    private void load(final Map<Location, Location> map, final String filename) {
        final StorageData data = new CustomStorage(plugin.getDataFolder(), filename, customLogger).load();

        final Map<Location, Location> newMap = new HashMap<>();
        for (final Map.Entry<String,String> entry : data.entrySet()) {
            newMap.put(string2location(entry.getKey()), string2location(entry.getValue()));
        }

        map.clear();
        map.putAll(newMap);
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
            customLogger.error(String.format("Can't load from dump: %s", e));
            throw e;
        }
    }
}