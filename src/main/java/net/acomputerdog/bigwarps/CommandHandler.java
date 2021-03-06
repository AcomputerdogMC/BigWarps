package net.acomputerdog.bigwarps;

import net.acomputerdog.bigwarps.warp.PlayerWarps;
import net.acomputerdog.bigwarps.warp.TPMap;
import net.acomputerdog.bigwarps.warp.Warp;
import net.acomputerdog.bigwarps.warp.WarpList;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Command Handler.  Takes every command line and passes it on to specific methods.
 * Also includes lots of utility functions to simplify command processing
 */
public class CommandHandler {
    private final PluginBigWarps plugin;
    private final WarpList warps;
    private final TPMap tpMap;

    //the sender of the current command; used to simplify sending messages
    private CommandSender currentSender = null;

    public CommandHandler(PluginBigWarps plugin, WarpList warps, TPMap tpMap) {
        this.plugin = plugin;
        this.warps = warps;
        this.tpMap = tpMap;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        currentSender = sender;
        Player p = null;
        if (sender instanceof Player) {
            p = (Player) sender;
        }
        try {
            switch (command.getName()) {
                case "back":
                    cmdBack(p);
                    break;
                case "tpa":
                    cmdTpa(p, args);
                    break;
                case "tpaccept":
                    cmdTpaccept(p);
                    break;
                case "tpdeny":
                    cmdTpdeny(p);
                    break;
                case "tpcancel":
                    cmdTpcancel(p);
                    break;
                case "warp":
                    cmdWarp(p, args);
                    break;
                case "mkwarp":
                    cmdMkwarp(sender, p, args);
                    break;
                case "rmwarp":
                    cmdRmwarp(p, args);
                    break;
                case "lswarps":
                    cmdLswarps(sender, args);
                    break;
                case "lspublic":
                    cmdLspublic(sender);
                    break;
                case "setpublic":
                    cmdSetpublic(p, args);
                    break;
                case "bwreload":
                    cmdReload(sender);
                    break;
                case "tp":
                    cmdTp(p, args);
                    break;
                case "bed":
                    cmdBed(p);
                    break;
                default:
                    sendRed("Bug detected: unknown command passed to plugin!  Please report this!");
                    plugin.getLogger().severe("Unknown command passed to plugin: " + command.getName());
                    break;
            }
        } catch (Exception e) {
            sendRed("An error occurred while running this command!");
            plugin.getLogger().log(Level.SEVERE, "Exception occurred running command!", e);
        }
        currentSender = null;
        return true;
    }

    /**
     * Checks if a sender has all of a list of permissions.  If not print a message and return false
     */
    private boolean checkPerms(CommandSender sender, String... perms) {
        for (String perm : perms) {
            if (!sender.hasPermission(perm)) {
                sendRed("You do not have permission!");
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a sender is a player, and if they have permissions
     */
    private boolean checkPermsPlayer(CommandSender sender, String... perms) {
        if (!(sender instanceof Player)) {
            sendRed("You must be a player to use this command.");
            return false;
        }
        return checkPerms(sender, perms);
    }

    private void cmdBack(Player p) {
        if (checkPermsPlayer(p, "bigwarps.command.back")) {
            tpMap.onBack(p);
        }
    }

    private void cmdTpa(Player p, String[] args) {
        if (checkPermsPlayer(p, "bigwarps.tpa")) {
            if (args.length == 1) {
                Player target = plugin.getServer().getPlayer(args[0]);
                if (target != null) {
                    tpMap.onTpa(p, target);
                } else {
                    sendRed("That player could not be found.");
                }
            } else {
                sendRed("Usage: /tpa <player>");
            }
        }
    }

    private void cmdTpaccept(Player p) {
        if (checkPermsPlayer(p, "bigwarps.tpa")) {
            tpMap.onTpaccept(p);
        }
    }

    private void cmdTpcancel(Player p) {
        if (checkPermsPlayer(p, "bigwarps.tpa")) {
            sendRed("You do not have any teleport requests.");
            tpMap.cancelSentTpa(p);
        }
    }

    private void cmdTpdeny(Player p) {
        if (checkPermsPlayer(p, "bigwarps.tpa")) {
            sendRed("No one has requested to teleport to you.");
            tpMap.cancelReceivedTpa(p);
        }
    }

    private void cmdWarp(Player p, String[] args) {
        if (checkPermsPlayer(p, "bigwarps.command.warp")) {
            if (args.length == 1) {
                String name = args[0].toLowerCase();

                Warp warp = warps.getWarp(p.getUniqueId(), name);
                if (warp != null) {
                    tpMap.onWarp(p, warp);
                } else {
                    sendRed("That warp could not be found!");
                }
            } else {
                sendRed("Usage: /warp [owner.]<name>");
            }
        }
    }

    private void cmdMkwarp(CommandSender s, Player p, String[] args) {
        if (checkPerms(s, "bigwarps.command.mkwarp")) {
            //if the warp is at the current location
            if (args.length == 1) {
                //can be null if used by console or command block
                if (p != null) {
                    Location loc = p.getLocation();
                    makeWarp(s, p.getUniqueId(), args[0], loc);
                } else {
                    sendRed("This command can only be used by a player.");
                }
                //else if the warp is being set manually
            } else if (args.length == 5) {
                try {
                    String world = args[1];
                    double x = Double.parseDouble(args[2]);
                    double y = Double.parseDouble(args[3]);
                    double z = Double.parseDouble(args[4]);

                    makeWarp(s, p.getUniqueId(), args[0], world, x, y, z);
                } catch (NumberFormatException e) {
                    sendRed("x, y, and z must be valid decimal numbers.");
                }
            } else {
                sendRed("Usage: /mkwarp <name> [<world> <x> <y> <z>]");
            }
        }
    }

    /**
     * Creates a warp and adds it to the warp list
     */
    private void makeWarp(CommandSender sender, UUID owner, String name, String world, double x, double y, double z) {
        if (warpNameValid(name)) {
            Warp warp = new Warp(plugin, x, y, z, world, owner, name, Warp.now(), true);
            addWarp(sender, owner, warp);
        } else {
            sendRed("Invalid name!  You may only use letters, numbers, and underscores.");
        }
    }

    /**
     * Makes a warp and adds to warp list
     */
    private void makeWarp(CommandSender sender, UUID owner, String name, Location loc) {
        if (warpNameValid(name)) {
            Warp warp = new Warp(plugin, loc, owner, name);
            addWarp(sender, owner, warp);
        } else {
            sendRed("Invalid name!  You may only use letters, numbers, and underscores.");
        }
    }

    private void addWarp(CommandSender sender, UUID owner, Warp warp) {
        if (warps.getPrivateWarps(owner).getNumTotalWarps() < plugin.getMaxWarpsTotal() || sender.hasPermission("bigwarps.ignoretotallimit")) {
            warps.addWarp(warp);
            sendAqua("Warp created.");
        } else {
            sendRed("You have too many private warps.");
        }
    }

    /**
     * Checks if a warp name is valid.
     * Valid warp names have only letters, numbers, and underscores
     */
    private boolean warpNameValid(String name) {
        for (char chr : name.toCharArray()) {
            //lower case letters
            if (chr >= 97 && chr <= 122) {
                continue;
            }
            //upper case letters
            if (chr >= 65 && chr <= 90) {
                continue;
            }
            //numbers
            if (chr >= 48 && chr <= 57) {
                continue;
            }
            //underscores
            if (chr == 95) {
                continue;
            }
            return false;
        }
        return true;
    }

    private void cmdRmwarp(Player p, String[] args) {
        if (checkPermsPlayer(p, "bigwarps.command.rmwarp")) {
            if (args.length == 1) {
                Warp warp = warps.getWarp(p.getUniqueId(), args[0]);
                if (warp != null) {
                    warps.removeWarp(warp);
                    sendAqua("Warp removed.");
                } else {
                    sendRed("Unable to find a warp by that name!");
                }
            } else {
                sendRed("Usage: /rmwarp [owner.]<name>");
            }
        }
    }

    /**
     * Lists a player's private warps
     */
    private void cmdLswarps(CommandSender s, String[] args) {
        if (checkPerms(s, "bigwarps.command.lswarps")) {
            UUID uuid;
            //if nothing is specified, then get warps for the current player
            if (args.length == 0) {
                if (s instanceof Player) {
                    uuid = ((Player) s).getUniqueId();
                } else {
                    sendRed("This command must be used as a player, or you must specify who's warps to view.");
                    return;
                }
                //otherwise get warps for another player
            } else if (args.length == 1) {
                if (s.hasPermission("bigwarps.list.showother")) {
                    //check for logged in player names first
                    Player p = plugin.getServer().getPlayer(args[0]);
                    if (p != null) {
                        uuid = p.getUniqueId();
                    } else {
                        try {
                            //if that fails then try to parse at a UUID
                            uuid = UUID.fromString(args[0]);
                        } catch (IllegalArgumentException e) {
                            //TODO write some kind of name caching library
                            sendRed("You (currently) must enter a valid UUID to lookup warps for an offline player.");
                            return;
                        }
                    }
                } else {
                    sendRed("You do not have permission to view other players' warps.");
                    return;
                }
            } else {
                sendRed("Usage: /lswarps [player]");
                return;
            }

            //if we have reached this point, then a UUID was found.
            PlayerWarps privateWarps = warps.getPrivateWarps(uuid);
            sendYellow("Personal warps:");
            for (Warp warp : privateWarps) {
                String visibility = warp.isPublic() ? "public" : "private";
                s.sendMessage(ChatColor.AQUA + warp.getName() + ": " + ChatColor.BLUE + warp.locationToString() + " - " + ChatColor.DARK_PURPLE + visibility);
            }
        }
    }

    /**
     * List public warps
     */
    private void cmdLspublic(CommandSender s) {
        if (checkPerms(s, "bigwarps.command.lspublic")) {
            PlayerWarps publicWarps = warps.getPublicWarps();
            sendYellow("Public warps:");
            for (Warp warp : publicWarps) {
                s.sendMessage(ChatColor.AQUA + warp.getName() + ": " + ChatColor.BLUE + warp.locationToString() + " - " + ChatColor.DARK_PURPLE + warp.getOwnerName());
            }
        }
    }

    private void cmdSetpublic(Player p, String[] args) {
        if (checkPermsPlayer(p, "bigwarps.command.setpublic")) {
            if (args.length == 1) {
                String name = args[0];
                warps.togglePublic(p, name);
            } else {
                sendRed("Usage: /setpublic <warp name>");
            }
        }
    }

    private void cmdReload(CommandSender s) {
        if (checkPerms(s, "bigwarps.command.reload")) {
            sendAqua("Reloading...");
            plugin.onDisable();
            plugin.onEnable();
            sendAqua("Done.");
        }
    }

    /**
     * Passes through to the vanilla TP command, but records position for /back
     */
    private void cmdTp(Player p, String[] args) {
        if (checkPermsPlayer(p, "bigwarps.tp.use")) {

            //rebuild command with minecraft teleport command
            StringBuilder cmd = new StringBuilder((1 + args.length) * 2);
            cmd.append("minecraft:tp");
            for (String str : args) {
                cmd.append(' ');
                cmd.append(str);
            }

            //check if player has access to bypass normal /tp permissions
            if (p.hasPermission("bigwarps.tp.force")) {
                PermissionAttachment attachment = p.addAttachment(plugin, 1);
                attachment.setPermission("bukkit.command.teleport", true);
                attachment.setPermission("minecraft.command.tp", true);
            }

            Location l = p.getLocation();
            //if player teleported successfully, then update return point (for /back)
            if (p.performCommand(cmd.toString())) {
                tpMap.setReturnPoint(p, l);
            }
        }
    }

    private void cmdBed(Player p) {
        if (checkPermsPlayer(p, "bigwarps.command.bed")) {
            sendAqua("Warped to home bed.");
            tpMap.updateReturnPoint(p);
            if (p.getBedSpawnLocation() != null) {
                p.teleport(p.getBedSpawnLocation());
            }
        }
    }

    private void sendRaw(String message) {
        currentSender.sendMessage(message);
    }

    private void sendAqua(String message) {
        currentSender.sendMessage(ChatColor.AQUA + message);
    }

    private void sendBlue(String message) {
        currentSender.sendMessage(ChatColor.BLUE + message);
    }

    private void sendYellow(String message) {
        currentSender.sendMessage(ChatColor.YELLOW + message);
    }

    private void sendRed(String message) {
        currentSender.sendMessage(ChatColor.RED + message);
    }

    private static void sendAqua(CommandSender p, String message) {
        p.sendMessage(ChatColor.AQUA + message);
    }

    private static void sendBlue(CommandSender p, String message) {
        p.sendMessage(ChatColor.BLUE + message);
    }

    private static void sendYellow(CommandSender p, String message) {
        p.sendMessage(ChatColor.YELLOW + message);
    }

    private static void sendRed(CommandSender p, String message) {
        p.sendMessage(ChatColor.RED + message);
    }
}
