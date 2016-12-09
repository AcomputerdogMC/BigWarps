package net.acomputerdog.bigwarps;

import net.acomputerdog.bigwarps.warp.TPMap;
import net.acomputerdog.bigwarps.warp.WarpList;
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

public class PluginBigWarps extends JavaPlugin implements Listener {
    private WarpList warps;
    private TPMap tpMap;
    private CommandHandler commandHandler;

    @Override
    public void onEnable() {
        try {
            warps = new WarpList(this);
            tpMap = new TPMap(this);
            commandHandler = new CommandHandler(this, warps, tpMap);

            getServer().getPluginManager().registerEvents(this, this);
        } catch (Exception e) {
            getLogger().severe("Exception during startup!  Plugin will be disabled.");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            HandlerList.unregisterAll((JavaPlugin) this);
            warps = null;
            tpMap = null;
            commandHandler = null;
        } catch (Exception e) {
            getLogger().severe("Exception during shutdown!");
            e.printStackTrace();
        }
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
    }
}
