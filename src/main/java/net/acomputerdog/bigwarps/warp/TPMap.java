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
 *
 * //TODO database?  Maybe for parts?
 */
public class TPMap implements Listener {
    //the time until a TPA request expires
    private static final long TPA_TIMEOUT = 2L * 20L * 60L; //two minutes in ticks (at full server performance)

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

    /**
     * Teleports a player back to their previous warp point
     */
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

    /**
     * Sends a TPA request from one player to another
     */
    public void onTpa(Player sender, Player target) {
        if (requestMap.containsA(sender)) {
            cancelSentTpa(sender);
        }
        if (requestMap.containsB(target)) {
            cancelReceivedTpa(target);
        }

        sender.sendMessage(ChatColor.YELLOW + "Teleport request sent to " + target.getName() + ".");
        target.sendMessage(ChatColor.YELLOW + sender.getName() + " has requested to teleport to you.  Use /tpaccept to accept, or /tpdeny to deny.");
        requestMap.put(sender, target);
        startTimeout(sender);
    }

    /**
     * Cancels a TPA request from the sender side
     */
    public void cancelSentTpa(Player sender) {
        Player target = requestMap.removeA(sender);
        if (target != null) {
            sendTpaCancel(sender, target);
            stopTimeout(sender);
        }
    }

    /**
     * Cancels a TPA request from the receiver side
     */
    public void cancelReceivedTpa(Player target) {
        Player sender = requestMap.removeB(target);
        if (sender != null) {
            sendTpaCancel(sender, target);
            stopTimeout(sender);
        }
    }

    /**
     * Accepts a tp request
     */
    public void onTpaccept(Player target) {
        Player sender = requestMap.removeB(target);

        if (sender != null) {
            sender.sendMessage(ChatColor.YELLOW + "Teleporting to " + target.getName() + ".");
            target.sendMessage(ChatColor.YELLOW + "Request from " + sender.getName() + " accepted.");
            updateReturnPoint(sender);
            sender.teleport(target);
            stopTimeout(sender);
        } else {
            target.sendMessage(ChatColor.RED + "No one has requested to teleport to you.");
        }
    }

    /**
     * Removes any TPA request from this player, then sends a "request canceled" message.
     */
    private void expireTpa(Player from) {
        Player p2 = requestMap.removeA(from);
        if (p2 != null) {
            sendTpaCancel(from, p2);
        }
    }

    /**
     * Register a timer to process TPA timeout
     */
    private void startTimeout(Player from) {
        int id = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> expireTpa(from), TPA_TIMEOUT);
        timeoutIDMap.put(from, id);
    }

    /**
     * Cancels a timeout timer.  Called when a player actually teleports.
     */
    private void stopTimeout(Player from) {
        if (timeoutIDMap.containsKey(from)) {
            int id = timeoutIDMap.get(from);
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
