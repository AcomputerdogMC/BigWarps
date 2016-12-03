package net.acomputerdog.bigwarps.warp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerWarps {
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
        return warps.get(name);
    }

    public Warp removeWarp(String name) {
        return warps.remove(name);
    }

    public Warp addWarp(String name, Warp warp) {
        return warps.put(name, warp);
    }

    // package-private method for WarpList
    Map<String, Warp> getWarpMap() {
        return warps;
    }
}
