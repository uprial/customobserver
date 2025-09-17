package com.gmail.uprial.customobserver.common;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class Formatter {
    public static String format(final Block block) {
        return String.format("%s[%s]",
                block.getType(),
                format(block.getLocation()));
    }

    public static String format(final Player player) {
        return String.format("%s[%s]",
                player.getName(),
                format(player.getLocation()));
    }

    public static String format(final Location location) {
        return String.format("%s:%.0f:%.0f:%.0f",
                location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ());
    }
}
