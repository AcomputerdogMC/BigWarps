package net.acomputerdog.bigwarps.warp;

import net.acomputerdog.bigwarps.PluginBigWarps;
import net.acomputerdog.bigwarps.util.BiMap;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps player teleport requests and location
 */
public class TPMap implements Listener {
    //the time until a TPA request expires
    private static final long TPA_TIMEOUT = 2 * 20 * 60; //two minutes in ticks (at full server performance)

    private final PluginBigWarps plugin;

    //map of player to /back return points
    private final Map<Player, Location> returnPoints;
    //map of player -> player TPA requests
    private final BiMap<Player, Player> requestMap;
    //map of player -> timeout IDs (generated from bukkit scheduler)
    private final Map<Player, Integer> timeoutIDMap;

    public TPMap(PluginBigWarps plugin) {
        this.plugin = plugin;

        returnPoints = new HashMap<>();
        requestMap = new BiMap<>();
        timeoutIDMap = new HashMap<>();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Records a player's current position as their return point
     */
    public void updateReturnPoint(Player p) {
        returnPoints.put(p, p.getLocation());
    }

    /**
     * Directly sets a player's return point
     */
    public void setReturnPoint(Player p, Location l) {
        returnPoints.put(p, l);
    }

    public void onBack(Player p) {
        Location prev = returnPoints.get(p);
        if (prev != null) {
            Location curr = p.getLocation();
            p.teleport(prev);
            returnPoints.put(p, curr);
        } else {
            p.sendMessage(ChatColor.RED + "You do not have anywhere to return to!");
        }
    }

    public void onTpa(Player p1, Player p2) {
        if (requestMap.containsA(p1)) {
            cancelSentTpa(p1);
        }
        if (requestMap.containsB(p2)) {
            cancelReceivedTpa(p2);
        }

        p1.sendMessage(ChatColor.YELLOW + "Teleport request sent to " + p2.getName() + ".");
        p2.sendMessage(ChatColor.YELLOW + p1.getName() + " has requested to teleport to you.  Use /tpaccept to accept, or /tpdeny to deny.");
        requestMap.put(p1, p2);
        startTimeout(p1);
    }

    public void cancelSentTpa(Player p) {
        Player p2 = requestMap.removeA(p);
        if (p2 != null) {
            sendTpaCancel(p, p2);
            stopTimeout(p);
        }
    }

    public void cancelReceivedTpa(Player p) {
        Player p1 = requestMap.removeB(p);
        if (p1 != null) {
            sendTpaCancel(p1, p);
            stopTimeout(p1);
        }
    }

    public void onTpaccept(Player p2) {
        Player p1 = requestMap.removeB(p2);

        if (p1 != null) {
            p1.sendMessage(ChatColor.YELLOW + "Teleporting to " + p2.getName() + ".");
            p2.sendMessage(ChatColor.YELLOW + "Request from " + p1.getName() + " accepted.");
            updateReturnPoint(p1);
            p1.teleport(p2);
            stopTimeout(p1);
        } else {
            p2.sendMessage(ChatColor.RED + "No one has requested to teleport to you.");
        }
    }

    /**
     * Removes any TPA request from this player, then sends a "request canceled" message.
     */
    private void expireTpa(Player p) {
        Player p2 = requestMap.removeA(p);
        if (p2 != null) {
            sendTpaCancel(p, p2);
        }
    }

    /**
     * Register a timer to process TPA timeout
     */
    private void startTimeout(Player p) {
        int id = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> expireTpa(p), TPA_TIMEOUT);
        timeoutIDMap.put(p, id);
    }

    /**
     * Cancels a timeout timer.  Called when a player actually teleports.
     */
    private void stopTimeout(Player p) {
        if (timeoutIDMap.containsKey(p)) {
            int id = timeoutIDMap.get(p);
            plugin.getServer().getScheduler().cancelTask(id);
        }
    }

    public void onWarp(Player p, Warp warp) {
        updateReturnPoint(p);
        p.sendMessage(ChatColor.AQUA + "Warping to " + warp.getName() + ".");
        p.teleport(warp.getLocation());
    }

    @EventHandler()
    public void onPlayerLogout(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        stopTimeout(p);
        expireTpa(p);
    }

    private static void sendTpaCancel(Player s, Player r) {
        s.sendMessage(ChatColor.YELLOW + "Teleport request to " + r.getName() + " has been canceled.");
        r.sendMessage(ChatColor.YELLOW + "Teleport request from " + s.getName() + " has been canceled.");
    }
}
