package net.acomputerdog.bigwarps.warp;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Representation of a warp point both in the world and stored to a file
 */
public class Warp implements Listener {

    private final Server server;

    private final String worldName;
    private final double x;
    private final double y;
    private final double z;

    private final UUID owner; //UUID of owner, this should be used to check ownership
    //TODO update at some specific interval, or when a player logs in
    private String ownerName; //name of owner, only for printing and "real" name generation.  DO NOT USE IN equals() or hashCode()
    private final String name; //warp name
    private final long time;
    private boolean isPublic;

    //populated after load.  Will be null until server starts.
    private World world;
    private Location location;

    public Warp(JavaPlugin plugin, Location l, UUID owner, String name) {
        this(plugin, l.getX(), l.getY(), l.getZ(), l.getWorld(), owner, name, now(), false);
        this.location = l;
    }

    public Warp(JavaPlugin plugin, double x, double y, double z, String worldName, UUID owner, String name, long time, boolean isPublic) {
        this.server = plugin.getServer();
        this.x = x;
        this.y = y;
        this.z = z;
        this.worldName = worldName;
        this.owner = owner;
        this.ownerName = owner == null ? "none" : plugin.getServer().getOfflinePlayer(owner).getName();
        this.name = name;
        this.time = time;
        this.isPublic = isPublic;
        server.getPluginManager().registerEvents(this, plugin);
    }

    public Warp(JavaPlugin plugin, double x, double y, double z, World world, UUID owner, String name, long time, boolean isPublic) {
        this(plugin, x, y, z, world.getName(), owner, name, time, isPublic);
        this.world = world;
    }


    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public UUID getOwner() {
        return owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getName() {
        return name;
    }

    public long getTime() {
        return time;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public World getWorld() {
        if (world == null) {
            world = server.getWorld(worldName);
            if (world != null) {
                location = new Location(world, x, y, z);
            }
        }
        return world;
    }

    public Location getLocation() {
        if (location == null) {
            location = new Location(getWorld(), x, y, z);
        }
        return location;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldLoad(WorldLoadEvent e) {
        if (worldName.equals(e.getWorld().getName())) {
            world = e.getWorld();
            location = new Location(world, x, y, z);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent e) {
        if (e.getWorld() == this.world) {
            world = null;
            location = null;
        }
    }

    @Override
    public String toString() {
        return name + "," + owner + "," + worldName + "," + x + "," + y + "," + z + "," + time + "," + isPublic;
    }

    public String locationToString() {
        return worldName + "@[" + String.format("%.2f", x) + ", " + String.format("%.2f", y) + ", " + String.format("%.2f", z) + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Warp)) return false;

        Warp warp = (Warp) o;

        if (Double.compare(warp.x, x) != 0) return false;
        if (Double.compare(warp.y, y) != 0) return false;
        if (Double.compare(warp.z, z) != 0) return false;
        if (time != warp.time) return false;
        if (isPublic != warp.isPublic) return false;
        if (!worldName.equals(warp.worldName)) return false;
        if (owner != null ? !owner.equals(warp.owner) : warp.owner != null) return false;
        return name.equals(warp.name);

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = worldName.hashCode();
        temp = Double.doubleToLongBits(x);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(z);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + name.hashCode();
        result = 31 * result + (int) (time ^ (time >>> 32));
        result = 31 * result + (isPublic ? 1 : 0);
        return result;
    }

    public static long now() {
        return System.currentTimeMillis() / 1000L;
    }

    /**
     * Parses a current format warp file.
     * name,owneruuid,world,x,y,z [time, public]
     */
    public static Warp parse(JavaPlugin plugin, String str) {
        if (str != null) {
            String[] parts = str.split(",");
            if (parts.length >= 6) {
                try {
                    String name = parts[0];

                    UUID owner = UUID.fromString(parts[1]);

                    String world = parts[2];

                    double x = Double.parseDouble(parts[3]);
                    double y = Double.parseDouble(parts[4]);
                    double z = Double.parseDouble(parts[5]);
                    long time = now();
                    boolean isPublic = false;
                    if (parts.length >= 8) {
                        time = Long.parseLong(parts[6]);
                        isPublic = Boolean.parseBoolean(parts[7]);
                    }

                    return new Warp(plugin, x, y, z, world, owner, name, time, isPublic);
                } catch (IllegalArgumentException ignored) {
                }//will return null
            }
        }
        return null;
    }
}
