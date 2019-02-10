package net.acomputerdog.bigwarps.warp;

import net.acomputerdog.bigwarps.PluginBigWarps;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Keeps track of warp locations and states
 *
 * TODO this needs to be a database
 */
public class WarpList {
    private final PluginBigWarps plugin;

    //warps made by server console are uuid == null
    private final Map<UUID, PlayerWarps> privateWarps;
    private final File privateWarpsDir;

    private final PlayerWarps publicWarps;
    private final File publicWarpsFile;

    //utility class that maps "simple" names to "full" names
    //   ex. big_castle -> player1.big_castle
    private final QuickWarps quickWarps;

    public WarpList(PluginBigWarps plugin) {
        this.plugin = plugin;

        this.privateWarps = new HashMap<>();
        this.publicWarps = new PlayerWarps(null);
        this.quickWarps = new QuickWarps();

        this.privateWarpsDir = new File(plugin.getDataFolder(), "private_warps/");
        this.publicWarpsFile = new File(plugin.getDataFolder(), "public_warps.lst");
        if (!this.privateWarpsDir.isDirectory() && !this.privateWarpsDir.mkdirs()) {
            plugin.getLogger().warning("Unable to create private warps directory!");
        }

        loadPublicWarps();
    }

    /**
     * Looks up a warp by owner UUID and name
     */
    public Warp getWarp(UUID owner, String name) {
        PlayerWarps warps = getPlayerWarps(owner);
        Warp warp = warps.getWarp(name);
        if (warp == null) {
            //fall back on public warps if owner does not have a warp by that name
            warp = publicWarps.getWarp(name);
            if (warp == null) {
                //use quickwarps to try and look up the real name
                String realName = quickWarps.getRealName(name);
                if (realName != null) {
                    //fall back on quickwarps if the public name did not match
                    warp = publicWarps.getWarp(realName);
                }
            }
        }
        return warp;
    }

    public void addWarp(Warp warp) {
        PlayerWarps warps = getPlayerWarps(warp.getOwner());
        warps.addWarp(warp);
        savePlayerWarps(warps);
    }

    public void removeWarp(Warp warp) {
        PlayerWarps warps = getPlayerWarps(warp.getOwner());
        warps.removeWarp(warp.getName());
        savePlayerWarps(warps);

        publicWarps.removeWarp(getRealName(warp)); //use real name
        quickWarps.removeName(warp.getName()); //use short name
        savePublicWarps();
    }

    /**
     * Gets a list of all the warps belonging to a player
     */
    private PlayerWarps getPlayerWarps(UUID owner) {
        if (owner == null) {
            throw new IllegalArgumentException("Cannot get warps for null player, use getPublicWarps() instead!");
        }
        PlayerWarps warps = privateWarps.get(owner);
        if (warps == null) {
            warps = loadPlayerWarps(owner);
        }
        return warps;
    }

    /**
     * Gets all of a player's private warps
     */
    public PlayerWarps getPrivateWarps(UUID owner) {
        return new ImmutablePlayerWarps(getPlayerWarps(owner));
    }

    /**
     * Get all public warps on the server
     */
    public PlayerWarps getPublicWarps() {
        return new ImmutablePlayerWarps(publicWarps);
    }

    /**
     * Toggles a warp between public and private
     */
    public void togglePublic(Player owner, String name) {
        PlayerWarps playerWarps = getPlayerWarps(owner.getUniqueId());
        Warp warp = playerWarps.getWarp(name);
        if (warp != null) {
            String realName = getRealName(warp);
            if (warp.isPublic()) {
                publicWarps.removeWarp(realName);
                warp.setPublic(false);
                //notify quickwarps to check for a name collision
                quickWarps.increaseCount(name, realName);
                owner.sendMessage(ChatColor.AQUA + "Warp is now private.");
            } else {
                if (playerWarps.getNumPublicWarps() < plugin.getMaxPublicWarps() || owner.hasPermission("bigwarps.ignoretotallimit")) {
                    publicWarps.addWarp(realName, warp);
                    warp.setPublic(true);
                    //notify quickwarps to check if name no longer collides
                    quickWarps.decreaseCount(name, realName);
                    owner.sendMessage(ChatColor.AQUA + "Warp is now public.");
                } else {
                    owner.sendMessage(ChatColor.RED + "You have too many public warps.");
                }
            }
            savePlayerWarps(playerWarps);
            savePublicWarps();
        } else {
            owner.sendMessage(ChatColor.RED + "No warp could be found matching that name!");
        }
    }

    private PlayerWarps loadPlayerWarps(UUID owner) {
        File warpFile = new File(privateWarpsDir, owner.toString() + ".lst");
        PlayerWarps warps = new PlayerWarps(owner);
        if (warpFile.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(warpFile))) {
                readWarps(plugin, reader, warps, false);
                privateWarps.put(owner, warps);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "IOException reading warps for player " + owner, e);
            }
        }
        return warps;
    }

    private void loadPublicWarps() {
        if (publicWarpsFile.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(publicWarpsFile))) {
                readWarps(plugin, reader, publicWarps, true);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "IOException reading public warps", e);
            }
        }
    }

    public void onPlayerLogin(UUID uuid) {
        loadPlayerWarps(uuid);
    }

    public void onPlayerLogout(UUID uuid) {
        privateWarps.remove(uuid);
    }

    private void savePlayerWarps(PlayerWarps warps) {
        try (Writer writer = new FileWriter(new File(privateWarpsDir, warps.getOwner().toString() + ".lst"))) {
            writeWarps(writer, warps);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "IOException saving warps for player " + warps.getOwner(), e);
        }
    }

    private void savePublicWarps() {
        try (Writer writer = new FileWriter(publicWarpsFile)) {
            writeWarps(writer, publicWarps);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "IOException saving public warps", e);
        }
    }

    private void writeWarps(Writer writer, PlayerWarps warps) throws IOException {
        for (Warp warp : warps) {
            writer.write(warp.toString());
            writer.write("\n");
        }
    }

    private void readWarps(JavaPlugin plugin, BufferedReader reader, PlayerWarps warps, boolean isPublic) throws IOException {
        while (reader.ready()) {
            String line = reader.readLine().trim();
            if (!line.startsWith("#")) {
                try {
                    Warp warp = Warp.parse(plugin, line);
                    String name = warp.getName();
                    if (isPublic) {
                        name = getRealName(warp);
                        quickWarps.increaseCount(warp.getName(), name);
                    }
                    Warp oldWarp = warps.addWarp(name, warp);
                    if (oldWarp != null) {
                        plugin.getLogger().warning(() -> "Duplicate warp: \"" + warp.getName() + "\"");
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning(() -> "Malformed warp: \"" + line + "\"");
                }
            }
        }
    }

    /**
     * Converts a "simple" warp name into a full, "real" public warp name.
     * ex. big_castle -> player1.big_castle
     */
    private String getRealName(Warp warp) {
        return (warp.getOwnerName() + "." + warp.getName()).toLowerCase();
    }
}
