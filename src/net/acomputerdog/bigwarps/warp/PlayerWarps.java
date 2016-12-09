package net.acomputerdog.bigwarps.warp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class PlayerWarps implements Iterable<Warp> {
    private final UUID owner;

    private final Map<String, Warp> warps;

    public PlayerWarps(UUID owner) {
        this.owner = owner;
        this.warps = new HashMap<>();
    }

    public UUID getOwner() {
        return owner;
    }

    public Warp getWarp(String name) {
        return warps.get(name.toLowerCase());
    }

    public Warp removeWarp(String name) {
        return warps.remove(name.toLowerCase());
    }

    public Warp addWarp(Warp warp) {
        String name = warp.getName().toLowerCase();
        warps.remove(name);
        return warps.put(name, warp);
    }

    // package-private method for WarpList
    Map<String, Warp> getWarpMap() {
        return warps;
    }

    @Override
    public Iterator<Warp> iterator() {
        return warps.values().iterator();
    }
}
