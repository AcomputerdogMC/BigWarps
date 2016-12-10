package net.acomputerdog.bigwarps.warp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Holds a list of a single player's warps
 */
public class PlayerWarps implements Iterable<Warp> {
    private final UUID owner;

    private final Map<String, Warp> warps;

    private int numPublicWarps = 0;

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
        Warp warp = warps.remove(name.toLowerCase());
        if (warp != null) {
            if (warp.isPublic()) {
                numPublicWarps--;
            }
        }
        return warp;
    }

    public Warp addWarp(String name, Warp warp) {
        Warp old = removeWarp(name);
        warps.put(name, warp);

        if (warp.isPublic()) {
            numPublicWarps++;
        }

        return old;
    }

    public Warp addWarp(Warp warp) {
        return addWarp(warp.getName().toLowerCase(), warp);
    }

    public boolean hasWarp(String name) {
        return warps.containsKey(name);
    }

    public int getNumTotalWarps() {
        return warps.size();
    }

    public int getNumPublicWarps() {
        return numPublicWarps;
    }

    @Override
    public Iterator<Warp> iterator() {
        return warps.values().iterator();
    }
}
