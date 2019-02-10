package net.acomputerdog.bigwarps;

import net.acomputerdog.bigwarps.warp.TPMap;
import net.acomputerdog.bigwarps.warp.WarpList;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Main plugin class
 */
public class PluginBigWarps extends JavaPlugin implements Listener {
    private WarpList warps; //stores list of warps
    private TPMap tpMap; //maps player teleport requests and locations
    private CommandHandler commandHandler; //processes commands


    //total max warps warps per player
    private int maxWarpsTotal = 15;

    //max public warps per player
    private int maxPublicWarps = 5;

    @Override
    public void onEnable() {
        try {
            loadConfig();

            warps = new WarpList(this);
            tpMap = new TPMap(this);
            commandHandler = new CommandHandler(this, warps, tpMap);

            getServer().getPluginManager().registerEvents(this, this);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Exception during startup.  Plugin will be disabled.", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            //unregister handlers to prevent duplicate event firing
            HandlerList.unregisterAll((JavaPlugin) this);
            //clear all tasks to prevent glitches or memory leaks
            getServer().getScheduler().cancelTasks(this);

            warps = null;
            tpMap = null;
            commandHandler = null;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Exception during shutdown.", e);
        }
    }

    private void loadConfig() {
        saveDefaultConfig(); //only saves if it doesn't actually exist

        maxWarpsTotal = getConfig().getInt("total_warps_limit", maxWarpsTotal);
        maxPublicWarps = getConfig().getInt("public_warps_limit", maxPublicWarps);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return commandHandler.onCommand(sender, command, label, args);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLogin(PlayerJoinEvent e) {
        //todo separate thread ?
        warps.onPlayerLogin(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        //todo separate thread ?
        warps.onPlayerLogout(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent e) {
        tpMap.updateReturnPoint(e.getEntity());
        e.getEntity().sendMessage(ChatColor.YELLOW + "You have died!  Use /back to return to your death point.");
    }

    public int getMaxWarpsTotal() {
        return maxWarpsTotal;
    }

    public int getMaxPublicWarps() {
        return maxPublicWarps;
    }
}
