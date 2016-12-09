package net.acomputerdog.bigwarps.warp;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WarpList {

    /*

    TODO all warps must be loaded at start
    TODO we still need a public warps list
         SO we will have:
           map: uuid -> personal
             null == server
           list: public

     */

    private final JavaPlugin plugin;


    //server warps are uuid null
    private final Map<UUID, PlayerWarps> privateWarps;
    private final File privateWarpsDir;

    private final PlayerWarps publicWarps;
    private final File publicWarpsFile;

    public WarpList(JavaPlugin plugin) {
        this.plugin = plugin;

        this.privateWarps = new HashMap<>();
        this.publicWarps = new PlayerWarps(null);

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
            //fall back on public warps if owner is null or owner does not have a warp by that name
            warp = publicWarps.getWarp(name);
        }
        return warp;
    }

    public void addWarp(Warp warp) {
        WarpOwner owner = warp.getOwner();
        if (owner != null) {
            PlayerWarps warps = getPlayerWarps(owner.getUuid());
            warps.addWarp(warp);
            savePlayerWarps(warps);
        } else {
            publicWarps.addWarp(warp);
            savePublicWarps();
        }
    }

    public void removeWarp(Warp warp) {
        WarpOwner owner = warp.getOwner();
        if (owner != null) {
            PlayerWarps warps = getPlayerWarps(owner.getUuid());
            warps.removeWarp(warp.getName());
            savePlayerWarps(warps);
        } else {
            publicWarps.removeWarp(warp.getName());
            savePublicWarps();
        }
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

    public PlayerWarps getWarpsForPlayer(UUID uuid) {
        return new ImmutablePlayerWarps(getPlayerWarps(uuid));
    }

    public PlayerWarps getPublicWarps() {
        return new ImmutablePlayerWarps(publicWarps);
    }

    /*
    public void makeWarpPublic(Warp warp) {
        //remove from normal PlayerWarps
        WarpOwner owner = warp.getOwner();
        if (owner != null) {
            PlayerWarps warps = getPlayerWarps(owner.getUuid());
            warps.removeWarp(warp.getName());
        }

        //add to public PlayerWarps
        warp.setPublic(true);
        publicWarps.addWarp(warp);
    }
    */

    private PlayerWarps loadPlayerWarps(UUID uuid) {
        File warpFile = new File(privateWarpsDir, uuid.toString() + ".lst");
        PlayerWarps warps = new PlayerWarps(uuid);
        if (warpFile.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(warpFile))) {
                readWarps(plugin, reader, warps.getWarpMap());
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
                readWarps(plugin, reader, publicWarps.getWarpMap());
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

    private static void writeWarps(Writer writer, Collection<Warp> warps) throws IOException {
        for (Warp warp : warps) {
            writer.write(warp.toString());
            writer.write("\n");
        }
    }

    private static void readWarps(JavaPlugin plugin, BufferedReader reader, Map<String, Warp> warps) throws IOException {
        while (reader.ready()) {
            String line = reader.readLine().trim();
            if (!line.startsWith("#")) {
                Warp warp = Warp.parse(plugin, line);
                if (warp == null) {
                    plugin.getLogger().warning("Malformed warp: \"" + line + "\"");
                } else {
                    Warp oldWarp = warps.put(warp.getName(), warp);
                    if (oldWarp != null) {
                        plugin.getLogger().warning("Duplicate warp: \"" + warp.getName() + "\"");
                    }
                }
            }
        }
    }
}
