package net.acomputerdog.bigwarps.warp;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

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
    Map<String, Warp> getWarpMap() {
        return Collections.unmodifiableMap(passthrough.getWarpMap());
    }
}
