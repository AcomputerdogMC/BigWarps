package net.acomputerdog.bigwarps.warp;

import java.util.UUID;

public class WarpOwner {

    private final UUID uuid;
    private final String uuidString;
    private String name;

    public WarpOwner(UUID uuid, String name) {
        this.uuid = uuid;
        this.uuidString = uuid.toString();
        this.name = name;
    }

    public WarpOwner(UUID uuid) {
        this(uuid, null);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WarpOwner)) return false;

        WarpOwner warpOwner = (WarpOwner) o;

        return uuid != null ? uuid.equals(warpOwner.uuid) : warpOwner.uuid == null;

    }

    @Override
    public int hashCode() {
        return uuid != null ? uuid.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "WarpOwner{" +
                "uuid=" + uuid +
                '}';
    }
}
