package net.acomputerdog.bigwarps.warp;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WarpList {
    private final JavaPlugin plugin;


    //server warps are uuid null
    private final Map<UUID, PlayerWarps> privateWarps;
    private final File privateWarpsDir;

    private final PlayerWarps publicWarps;
    private final File publicWarpsFile;

    private final QuickWarps quickWarps;

    public WarpList(JavaPlugin plugin) {
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

    public Warp getWarp(UUID owner, String name) {
        PlayerWarps warps = getPlayerWarps(owner);
        Warp warp = warps.getWarp(name);
        if (warp == null) {
            //fall back on public warps if owner does not have a warp by that name
            warp = publicWarps.getWarp(name);
            if (warp == null) {
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
        PlayerWarps warps = getPlayerWarps(warp.getOwner().getUuid());
        warps.addWarp(warp);
        savePlayerWarps(warps);
    }

    public void removeWarp(Warp warp) {
        PlayerWarps warps = getPlayerWarps(warp.getOwner().getUuid());
        warps.removeWarp(warp.getName());
        savePlayerWarps(warps);

        publicWarps.removeWarp(getRealName(warp)); //use real name
        quickWarps.removeName(warp.getName()); //use short name
        savePublicWarps();
    }

    private PlayerWarps getPlayerWarps(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("Cannot get warps for null player, use getPublicWarps() instead!");
        }
        PlayerWarps warps = privateWarps.get(uuid);
        if (warps == null) {
            warps = loadPlayerWarps(uuid);
        }
        return warps;
    }

    public PlayerWarps getPrivateWarps(UUID uuid) {
        return new ImmutablePlayerWarps(getPlayerWarps(uuid));
    }

    public PlayerWarps getPublicWarps() {
        return new ImmutablePlayerWarps(publicWarps);
    }

    public void togglePublic(Player p, String name) {
        PlayerWarps playerWarps = getPlayerWarps(p.getUniqueId());
        Warp warp = playerWarps.getWarp(name);
        if (warp != null) {
            String realName = getRealName(warp);
            if (warp.isPublic()) {
                publicWarps.removeWarp(realName);
                warp.setPublic(false);
                quickWarps.increaseCount(name, realName);
                p.sendMessage(ChatColor.AQUA + "Warp is now private.");
            } else {
                publicWarps.addWarp(realName, warp);
                warp.setPublic(true);
                quickWarps.decreaseCount(name, realName);
                p.sendMessage(ChatColor.AQUA + "Warp is now public.");
            }
            savePlayerWarps(playerWarps);
            savePublicWarps();
        } else {
            p.sendMessage(ChatColor.RED + "No warp could be found matching that name!");
        }
    }

    private PlayerWarps loadPlayerWarps(UUID uuid) {
        File warpFile = new File(privateWarpsDir, uuid.toString() + ".lst");
        PlayerWarps warps = new PlayerWarps(uuid);
        if (warpFile.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(warpFile))) {
                readWarps(plugin, reader, warps.getWarpMap(), false);
                privateWarps.put(uuid, warps);
            } catch (IOException e) {
                plugin.getLogger().warning("IOException reading warps for player " + uuid);
                e.printStackTrace();
            }
        }
        return warps;
    }

    private void loadPublicWarps() {
        if (publicWarpsFile.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(publicWarpsFile))) {
                readWarps(plugin, reader, publicWarps.getWarpMap(), true);
            } catch (IOException e) {
                plugin.getLogger().warning("IOException reading public warps!");
                e.printStackTrace();
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
            writeWarps(writer, warps.getWarpMap().values());
        } catch (IOException e) {
            plugin.getLogger().warning("IOException saving warps for: " + warps.getOwner());
            e.printStackTrace();
        }
    }

    private void savePublicWarps() {
        try (Writer writer = new FileWriter(publicWarpsFile)) {
            writeWarps(writer, publicWarps.getWarpMap().values());
        } catch (IOException e) {
            plugin.getLogger().warning("IOException saving public warps!");
            e.printStackTrace();
        }
    }

    private void writeWarps(Writer writer, Collection<Warp> warps) throws IOException {
        for (Warp warp : warps) {
            writer.write(warp.toString());
            writer.write("\n");
        }
    }

    private void readWarps(JavaPlugin plugin, BufferedReader reader, Map<String, Warp> warps, boolean isPublic) throws IOException {
        while (reader.ready()) {
            String line = reader.readLine().trim();
            if (!line.startsWith("#")) {
                Warp warp = Warp.parse(plugin, line);
                if (warp == null) {
                    plugin.getLogger().warning("Malformed warp: \"" + line + "\"");
                } else {
                    String name = warp.getName();
                    if (isPublic) {
                        name = getRealName(warp);
                        quickWarps.increaseCount(warp.getName(), name);
                    }
                    Warp oldWarp = warps.put(name, warp);
                    if (oldWarp != null) {
                        plugin.getLogger().warning("Duplicate warp: \"" + warp.getName() + "\"");
                    }
                }
            }
        }
    }

    private String getRealName(Warp warp) {
        String ownerName = "none";
        if (warp.getOwner() != null) {
            ownerName = warp.getOwner().getName();
        }
        return (ownerName + "." + warp.getName()).toLowerCase();
    }
}
