package net.acomputerdog.bigwarps.warp;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WarpList {
    private final JavaPlugin plugin;

    private final File publicWarpsFile;
    private final File privateWarpsDir;

    private final Map<String, Warp> publicWarps;
    private final Map<UUID, PlayerWarps> privateWarps;

    public WarpList(JavaPlugin plugin) {
        this.plugin = plugin;
        this.privateWarpsDir = new File(plugin.getDataFolder(), "private_warps/");
        this.privateWarpsDir.mkdirs();
        this.publicWarpsFile = new File(plugin.getDataFolder(), "public_warps.lst");
        this.publicWarps = new HashMap<>();
        this.privateWarps = new HashMap<>();
        loadPublicWarps();
    }

    public Warp getWarp(UUID owner, String name) {
        if (owner != null) {
            PlayerWarps warps = getPlayerWarps(owner);
            Warp warp = warps.getWarp(name);
            if (warp != null) {
                return warp;
            }
        }

        //fall back on public warps if owner is null or owner does not have a warp by that name
        return publicWarps.get(name);
    }

    public void addWarp(Warp warp) {
        WarpOwner owner = warp.getOwner();
        if (owner.isPlayer()) {
            PlayerWarps warps = getPlayerWarps(owner.getUuid());
            warps.addWarp(warp.getName(), warp);
            savePlayerWarps(warps);
        } else {
            publicWarps.put(warp.getName(), warp);
            savePublicWarps();
        }
    }

    public void removeWarp(Warp warp) {
        WarpOwner owner = warp.getOwner();
        if (owner.isPlayer()) {
            PlayerWarps warps = getPlayerWarps(owner.getUuid());
            warps.removeWarp(warp.getName());
            savePlayerWarps(warps);
        } else {
            publicWarps.remove(warp.getName());
            savePublicWarps();
        }
    }

    private void loadPublicWarps() {
        try (BufferedReader reader = new BufferedReader(new FileReader(publicWarpsFile))) {
            readWarps(plugin, reader, publicWarps);
        } catch (FileNotFoundException e) {
            plugin.getLogger().warning("Public warps list file not found.");
        } catch (IOException e) {
            plugin.getLogger().warning("IOException loading public warps!");
            e.printStackTrace();
        }
    }

    private PlayerWarps getPlayerWarps(UUID uuid) {
        PlayerWarps warps = privateWarps.get(uuid);
        if (warps == null) {
            warps = loadPlayerWarps(uuid);
        }
        return warps;
    }

    private PlayerWarps loadPlayerWarps(UUID uuid) {
        File warpFile = getPlayerWarpsFile(uuid);
        PlayerWarps warps = new PlayerWarps(uuid);
        if (warpFile.exists()) {
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

    public void preloadPlayerWarps(UUID uuid) {
        loadPlayerWarps(uuid);
    }

    public void unloadPlayerWarps(UUID uuid) {
        privateWarps.remove(uuid);
    }

    private void savePublicWarps() {
        try (Writer writer = new FileWriter(publicWarpsFile)) {
            writeWarps(writer, publicWarps.values());
        } catch (IOException e) {
            plugin.getLogger().warning("IOException saving public warps!");
            e.printStackTrace();
        }
    }

    private void savePlayerWarps(PlayerWarps warps) {
        try (Writer writer = new FileWriter(getPlayerWarpsFile(warps.getOwner()))) {
            writeWarps(writer, warps.getWarpMap().values());
        } catch (IOException e) {
            plugin.getLogger().warning("IOException saving private warps for: " + warps.getOwner());
            e.printStackTrace();
        }
    }

    private File getPlayerWarpsFile(UUID uuid) {
        return new File(privateWarpsDir, uuid.toString() + ".lst");
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
                    plugin.getLogger().warning("Malformed warps: \"" + line + "\"");
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
