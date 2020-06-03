package net.clownercraft.ccRides.commands;

import net.clownercraft.ccRides.Config.ConfigHandler;
import net.clownercraft.ccRides.Config.Messages;
import net.clownercraft.ccRides.RidesPlugin;
import net.clownercraft.ccRides.rides.Ride;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Handles the Ride Admin Command
 *
 * Command Structure:
 *
 * Aliases: /rideadmin /rideadm
 * /rideadm help - show help
 * /rideadm reload - reload the whole plugin
 * /rideadm reload [all|ridename] - reload a specific ride or 'all' for everything
 * /rideadm create [ridename] - create a new ride
 * /rideadm delete [ridename] - create a new ride
 * /rideadm linksign [ridename] - link a sign to a ride
 * /rideadm list - see a list of all rides
 *
 * /rideadm <ridename> - View ride info
 * /rideadm <ridename> info - View ride info
 * /rideadm <ridename> reload - reload a single ride
 * /rideadm <ridename> enable/disable - enable/disable ride
 * /rideadm <ridename> setting [setting] [value] - reload a ride setting.
 */
public class AdminCommandExecutor implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String alias, String[] args) {
        if (!commandSender.hasPermission("ccrides.admin")) return false;
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.command_not_player));
            return true;
        }
        if (args.length==0) {
            //Show help
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.command_admin_help));
            return true;
        }
        ConfigHandler conf = RidesPlugin.getInstance().getConfigHandler();
        switch (args[0]) {
            case "help":
                //Show help
                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.command_admin_help));
                return true;
            case "reload":
                if (args.length>1) {
                    if (args[1].equalsIgnoreCase("all")) {
                        //Reload entire plugin
                        //Reload method sends messages
                        RidesPlugin.getInstance().reload(commandSender);
                    } else {
                        //Reload single ride
                        if(conf.rides.containsKey(args[1])) {
                            //Reload method sends messages
                            conf.reloadRide(args[1],commandSender);
                        }
                    }
                } else {
                    //reload all if no ride specified
                    //Reload method sends messages
                    RidesPlugin.getInstance().reload(commandSender);
                }
                return true;
            case "list":
                //list the available rides
                Set<String> rides = RidesPlugin.getInstance().getConfigHandler().rides.keySet();

                StringBuilder list = new StringBuilder();
                for (String r:rides) {
                    list.append("&9").append(r).append("&1, ");
                }
                String finalList = list.substring(0,list.length()-4);

                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.command_listRides).replaceAll("\\{ridelist}", finalList));
                return true;
            case "create":
                Set<String> rideTypes = Ride.RideTypes.keySet();

                StringBuilder typeList = new StringBuilder();
                for (String r:rideTypes) {
                    typeList.append("&9").append(r).append("&1, ");
                }
                String finalTypeList = typeList.substring(0,typeList.length()-4);

                if (args.length<3) {
                    //Missing arguments
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.command_admin_create_ride_syntax).replaceAll("\\{types}", finalTypeList));
                    return true;
                }
                String rideName = args[1];
                String rideType = args[2].toUpperCase();
                if(!Ride.RideTypes.containsKey(rideType)) {
                    //Invalid ride type
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.command_admin_create_ride_syntax).replaceAll("\\{types}", finalTypeList));
                }

                try {
                    conf.rides.put(rideName, Ride.RideTypes.get(rideType).getConstructor(new Class[]{String.class}).newInstance(rideName));
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.command_admin_create_ride).replaceAll("\\{ride}", rideName).replaceAll("\\{type}",rideType));
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    e.printStackTrace();
                }

                return true;
            case "delete":
                //TODO
                return true;
            case "linksign":
                //TODO
                return true;
            default:
                //args[0] is a ride name or not valid.
                //TODO
        }

        return false;

    }

    /**
     * Implement Tab completion
     */
    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String label, String[] args) {
        List<String> out = new ArrayList<>();

        //First level options
        if (args.length==1) {
            //Add basic options
            out = Arrays.asList("help", "reload", "create", "delete", "linksign", "list");

            //add ride names
            for (String ride: RidesPlugin.getInstance().getConfigHandler().rides.keySet()) {
                out.add(ride);
            }

            //Filter by what they've already typed
            out = Messages.filterList(out,"^"+args[0]);
        } else if (args.length==2) {
            //Second level options
            String subcommand = args[0];
            switch (subcommand) {
                case "reload":
                    out.add("all");
                    for (String ride: RidesPlugin.getInstance().getConfigHandler().rides.keySet()) {
                        out.add(ride);
                    }
                    break;
                case "delete":
                case "linksign":
                    for (String ride: RidesPlugin.getInstance().getConfigHandler().rides.keySet()) {
                        out.add(ride);
                    }
                    break;
                case "help":
                case "list":
                    //Add empty string, they have no sub commands
                    out.add("");
                    break;
                default:
                    out = Arrays.asList("info", "enable", "disable", "reload", "setting");
                    break;
            }
            out = Messages.filterList(out,"^"+args[1]);
        } else if (args.length==3) {
            String ridename = args[0];
            String ridecmd = args[1];

            switch (ridecmd) {
                case "info":
                case "enable":
                case "disable":
                case "reload":
                    out.add("");
                    //Add empty string, they have no sub commands
                    break;
                case "setting":
                    ConfigHandler conf = RidesPlugin.getInstance().getConfigHandler();
                    if (RidesPlugin.getInstance().getConfigHandler().rides.containsKey(ridename)) {
                        out = conf.rides.get(ridename).getConfigOptions();
                    }
                    break;
            }
            out = Messages.filterList(out,"^"+args[2]);
        } else if (args.length>3) {
            out.add("");
        }
        return out;
    }
}
