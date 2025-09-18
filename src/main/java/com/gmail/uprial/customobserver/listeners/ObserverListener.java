package com.gmail.uprial.customobserver.listeners;

import com.gmail.uprial.customobserver.CustomObserver;
import com.gmail.uprial.customobserver.common.CustomLogger;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Observer;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.*;

import static com.gmail.uprial.customobserver.common.Formatter.format;
import static com.gmail.uprial.customobserver.common.Utils.joinStrings;

public class ObserverListener implements Listener {
    private final CustomObserver plugin;
    private final CustomLogger customLogger;

    private final ObserverStorage storage;

    public ObserverListener(final CustomObserver plugin,
                            final CustomLogger customLogger) {
        this.plugin = plugin;
        this.customLogger = customLogger;

        storage = new ObserverStorage(plugin, customLogger);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onSignChange(final SignChangeEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Block sign = event.getBlock();

        if (!(sign.getBlockData() instanceof WallSign)) {
            //customLogger.debug(String.format("Not a WallSign: %s", format(sign)));
            return;
        }

        final BlockFace signFacing = ((WallSign) sign.getBlockData()).getFacing();
        if ((signFacing.getModY() != 0)
                || (Math.abs(signFacing.getModX() + signFacing.getModZ()) != 1)) {
            //customLogger.debug(String.format("Wrong sign facing: %s", signFacing));
            return;
        }

        final Block observer = getBlockInDirection(sign, signFacing, -1);
        if (!observer.getType().equals(Material.OBSERVER)) {
            //customLogger.debug(String.format("No observer: %s", format(observer)));
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
        } else if (distance > plugin.getServer().getViewDistance() * 16) {
            customLogger.debug(String.format("Too big distance: %d", distance));
            return;
        }

        final BlockFace observerFacing = ((Observer) observer.getBlockData()).getFacing();
        if (Math.abs(observerFacing.getModX() + observerFacing.getModY() + observerFacing.getModZ()) != 1) {
            customLogger.debug(String.format("Wrong observer facing: %s", observerFacing));
            return;
        }

        final Location otherSignLocation = storage.getSignLocationByObserver(observer.getLocation());
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

        final Block target = getBlockInDirection(observer, observerFacing, distance);

        event.getPlayer().sendMessage(
                String.format("Aimed %s at %s",
                        format(observer), format(target)));
        customLogger.info(
                String.format("Aimed %s at %s by %s",
                        format(observer), format(target), format(event.getPlayer())));

        storage.add(observer.getLocation(), sign.getLocation(), target.getLocation());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPhysics(final BlockPhysicsEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!event.getSourceBlock().getLocation().equals(event.getBlock().getLocation())) {
            return;
        }

        final Location observerLocation = storage.getObserverLocationByTarget(event.getSourceBlock().getLocation());
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
        if (!event.isCancelled()) {
            onBreak(event.getBlock(), event.getPlayer());
        }
    }

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPistonExtend(final BlockPistonExtendEvent event) {
        if (!event.isCancelled()) {
            onBreak(event.getBlocks());
        }
    }

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPistonRetract(final BlockPistonRetractEvent event) {
        if (!event.isCancelled()) {
            onBreak(event.getBlocks());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockExplode(final BlockExplodeEvent event) {
        if (!event.isCancelled()) {
            onBreak(event.blockList());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityExplode(final EntityExplodeEvent event) {
        if (!event.isCancelled()) {
            onBreak(event.blockList());
        }
    }

    private void onBreak(final List<Block> blocks) {
        for (final Block block : blocks) {
            onBreak(block, null);
        }
    }

    private void onBreak(final Block block, final Player player) {
        Location observerLocation = storage.getObserverLocationBySign(block.getLocation());
        String verb = null;

        if(observerLocation != null) {
            storage.remove(observerLocation);
            verb = "deactivated";
        } else if (storage.isObserverLocation(block.getLocation())) {
            observerLocation = block.getLocation();
            block.getWorld()
                    .getBlockAt(storage.getSignLocationByObserver(observerLocation))
                    .breakNaturally();

            storage.remove(observerLocation);
            verb = "broken";
        }

        if(verb != null) {
            if (player != null) {
                customLogger.info(String.format("OBSERVER[%s] %s by %s",
                        format(observerLocation), verb, format(player)));
            } else {
                customLogger.info(String.format("OBSERVER[%s] %s",
                        format(observerLocation), verb));
            }
        }
    }

    private void setPowered(final Location observerLocation,
                            final boolean isPowered,
                            final Runnable callback) {
        final Block observer = observerLocation.getWorld().getBlockAt(observerLocation);
        if (!observer.getType().equals(Material.OBSERVER)) {
            customLogger.error(String.format("No observer: %s", format(observer)));
            storage.remove(observerLocation);
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
}