package net.acomputerdog.bigwarps.warp;

import java.util.Iterator;
import java.util.UUID;

/**
 * Immutable subclass of PlayerWarps
 */
public class ImmutablePlayerWarps extends PlayerWarps {
    private final PlayerWarps passthrough;

    public ImmutablePlayerWarps(PlayerWarps passthrough) {
        super(passthrough.getOwner());
        this.passthrough = passthrough;
    }

    @Override
    public UUID getOwner() {
        return passthrough.getOwner();
    }

    @Override
    public Warp getWarp(String name) {
        return passthrough.getWarp(name);
    }

    @Override
    public Warp removeWarp(String name) {
        throw new UnsupportedOperationException("Cannot remove from an immutable warp list.");
    }

    @Override
    public Warp addWarp(Warp warp) {
        throw new UnsupportedOperationException("Cannot add to an immutable warp list.");
    }

    @Override
    public Iterator<Warp> iterator() {
        return passthrough.iterator();
    }

    @Override
    public Warp addWarp(String name, Warp warp) {
        throw new UnsupportedOperationException("Cannot add to an immutable warp list.");
    }

    @Override
    public boolean hasWarp(String name) {
        return passthrough.hasWarp(name);
    }

    @Override
    public int getNumTotalWarps() {
        return passthrough.getNumTotalWarps();
    }

    @Override
    public int getNumPublicWarps() {
        return passthrough.getNumPublicWarps();
    }
}
