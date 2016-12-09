package net.acomputerdog.bigwarps;

import net.acomputerdog.bigwarps.warp.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.logging.Level;

public class CommandHandler {
    private final JavaPlugin plugin;
    private final WarpList warps;
    private final TPMap tpMap;

    //used to simplify sending messages
    private CommandSender currentSender = null;

    public CommandHandler(JavaPlugin plugin, WarpList warps, TPMap tpMap) {
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
                    cmdRmwarp(sender, args);
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
                default:
                    sendRed("Bug detected: unknown command passed to plugin!  Please report this!");
                    plugin.getLogger().severe("Unknown command passed to plugin: " + command.getName());
                    break;
            }
        } catch (Exception e) {
            sendRed("An exception occurred while running this command!");
            plugin.getLogger().log(Level.SEVERE, "Exception occurred running command!", e);
        }
        currentSender = null;
        return true;
    }

    private boolean checkPerms(CommandSender sender, String... perms) {
        for (String perm : perms) {
            if (!sender.hasPermission(perm)) {
                sendRed("You do not have permission!");
                return false;
            }
        }
        return true;
    }

    private boolean checkPermsPlayer(CommandSender sender, String... perms) {
        if (sender == null || !(sender instanceof Player)) {
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
                Player owner = p;
                String name = args[0];

                int idx = args[0].indexOf('.');
                if (idx >= 0 && idx < args.length - 1) {
                    name = args[0].substring(idx + 1);
                    owner = plugin.getServer().getPlayer(args[0].substring(0, idx));
                    if (owner == null) {
                        sendRed("That player could not be found!");
                    }
                }

                if (owner != null) {
                    Warp warp = warps.getWarp(owner.getUniqueId(), name);
                    if (warp != null) {
                        tpMap.onWarp(p, warp);
                    } else {
                        sendRed("That warp could not be found!");
                    }
                }
            } else {
                sendRed("Usage: /warp [owner.]<name>");
            }
        }
    }

    private void cmdMkwarp(CommandSender s, Player p, String[] args) {
        if (checkPerms(s, "bigwarps.command.mkwarp")) {
            if (args.length == 1) {
                //can be null if used by console or command block
                if (p != null) {
                    Location loc = p.getLocation();
                    WarpOwner owner = new WarpOwner(p.getUniqueId(), p.getName());
                    makeWarp(owner, args[0], loc);
                } else {
                    sendRed("This command can only be used by a player.");
                }
            } else if (args.length == 5) {
                WarpOwner owner = null;
                if (p != null) {
                    owner = new WarpOwner(p.getUniqueId(), p.getName());
                }
                try {
                    String world = args[1];
                    double x = Double.parseDouble(args[2]);
                    double y = Double.parseDouble(args[3]);
                    double z = Double.parseDouble(args[4]);

                    makeWarp(owner, args[0], world, x, y, z);
                } catch (NumberFormatException e) {
                    sendRed("x, y, and z must be valid decimal numbers.");
                }
            } else {
                sendRed("Usage: /mkwarp <name> [<world> <x> <y> <z>]");
            }
        }
    }

    private void makeWarp(WarpOwner owner, String name, String world, double x, double y, double z) {
        if (warpNameValid(name)) {
            Warp warp = new Warp(plugin, x, y, z, world, owner, name, Warp.now(), true);
            warps.addWarp(warp);
            sendAqua("Warp created.");
        } else {
            sendRed("Invalid name!  You may only use letters, numbers, and underscores.");
        }
    }

    private void makeWarp(WarpOwner owner, String name, Location loc) {
        if (warpNameValid(name)) {
            Warp warp = new Warp(plugin, loc, owner, name);
            warps.addWarp(warp);
            sendAqua("Warp created.");
        } else {
            sendRed("Invalid name!  You may only use letters, numbers, and underscores.");
        }
    }

    private boolean warpNameValid(String name) {
        for (char chr : name.toCharArray()) {
            if (chr >= 97 && chr <= 122) {
                continue;
            }
            if (chr >= 65 && chr <= 90) {
                continue;
            }
            if (chr >= 48 && chr <= 57) {
                continue;
            }
            if (chr == 95) {
                continue;
            }
            return false;
        }
        return true;
    }

    private void cmdRmwarp(CommandSender s, String[] args) {
        if (checkPerms(s, "bigwarps.command.rmwarp")) {
            if (args.length == 1) {
                Player owner = null;
                String name = args[0];

                int idx = args[0].indexOf('.');
                if (idx >= 0 && idx < args.length - 1) {
                    name = args[0].substring(idx + 1);
                    owner = plugin.getServer().getPlayer(args[0].substring(0, idx));
                    if (owner == null) {
                        sendRed("That player could not be found.");
                    }
                }

                if (owner != null) {
                    if (owner.equals(s) || s.hasPermission("bigwarps.warp.editothers")) {
                        Warp warp = warps.getWarp(owner.getUniqueId(), name);
                        warps.removeWarp(warp);
                        sendAqua("Warp removed.");
                    } else {
                        sendRed("You do not have permission to edit other players' warps.");
                    }
                }
            } else {
                sendRed("Usage: /rmwarp [owner.]<name>");
            }
        }
    }

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

    private void cmdLspublic(CommandSender s) {
        if (checkPerms(s, "bigwarps.command.lspublic")) {
            PlayerWarps publicWarps = warps.getPublicWarps();
            sendYellow("Public warps:");
            for (Warp warp : publicWarps) {
                String owner = "none";
                //if a warp owner is set, then use it
                if (warp.getOwner() != null && !"null".equals(warp.getOwner().getName())) {
                    owner = warp.getOwner().getName();
                }

                s.sendMessage(ChatColor.AQUA + warp.getName() + ": " + ChatColor.BLUE + warp.locationToString() + " - " + ChatColor.DARK_PURPLE + owner);
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
