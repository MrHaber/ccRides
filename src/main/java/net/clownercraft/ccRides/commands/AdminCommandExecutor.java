package net.clownercraft.ccRides.commands;

import net.clownercraft.ccRides.config.ConfigHandler;
import net.clownercraft.ccRides.config.Messages;
import net.clownercraft.ccRides.RidesListener;
import net.clownercraft.ccRides.RidesPlugin;
import net.clownercraft.ccRides.rides.Ride;
import net.clownercraft.ccRides.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

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
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.prefix + Messages.command_not_player));
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
                String finalList = "";
                if (list.length()>4) {
                    finalList = list.substring(0,list.length()-4);
                }
                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.prefix + Messages.command_listRides.replaceAll("\\{ridelist}", finalList)));
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
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.prefix + Messages.command_admin_create_ride_syntax.replaceAll("\\{types}", finalTypeList)));
                    return true;
                }
                String rideName = args[1];
                if (conf.rides.containsKey(rideName)) {
                    //Ride already exists
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.prefix + Messages.command_admin_create_ride_exists.replaceAll("\\{ride}", rideName)));

                    return true;

                }
                String rideType = args[2].toUpperCase();
                if(!Ride.RideTypes.containsKey(rideType)) {
                    //Invalid ride type
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.prefix + Messages.command_admin_create_ride_syntax.replaceAll("\\{types}", finalTypeList)));
                }

                try {
                    Ride r = Ride.RideTypes.get(rideType).getConstructor(new Class[]{String.class}).newInstance(rideName);
                    conf.rides.put(rideName, r);
                    r.init();
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.prefix + Messages.command_admin_create_ride.replaceAll("\\{ride}", rideName).replaceAll("\\{type}",rideType)));
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    e.printStackTrace();
                }

                return true;
            case "delete":
                if (args.length<2) {
                    //Missing arguments
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.prefix + Messages.command_admin_delete_ride_syntax));
                } else {
                    String rideName2 = args[1];
                    if (conf.rides.containsKey(rideName2)) {
                        //Disable the ride
                        conf.deleteRide(rideName2);
                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.prefix + Messages.command_admin_delete_ride).replaceAll("\\{ride}",rideName2));
                    } else {
                        //Ride name is invalid
                        Set<String> rides2 = RidesPlugin.getInstance().getConfigHandler().rides.keySet();

                        StringBuilder list2 = new StringBuilder();
                        for (String r:rides2) {
                            list2.append("&9").append(r).append("&1, ");
                        }
                        String finalList2 = list2.substring(0,list2.length()-4);

                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.prefix + Messages.command_ride_not_exist));
                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.prefix + Messages.command_listRides.replaceAll("\\{ridelist}", finalList2)));
                    }
                }
                return true;
            case "linksign":
                if (args.length<2) {
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.prefix + Messages.command_admin_linksign_syntax));
                    return true;
                }
                String rideName2 = args[1];
                if (conf.rides.containsKey(rideName2)) {
                    RidesListener.waitingSignClick = true;
                    RidesListener.waitingUnlink = false;
                    RidesListener.waitingRideID = rideName2;
                    RidesListener.waitingSender = commandSender;
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.prefix + Messages.command_admin_linksign_click.replaceAll("\\{ride}",rideName2)));
                } else {
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.prefix + Messages.command_ride_not_exist));
                }
                return true;
            case "unlinksign":
                RidesListener.waitingSignClick = true;
                RidesListener.waitingUnlink = true;
                RidesListener.waitingSender = commandSender;
                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.prefix + Messages.command_admin_unlinksign_click));
                return true;

            default:
                //args[0] is a ride name or not valid.
                String rideName3 = args[0];
                if (conf.rides.containsKey(rideName3)) {
                    Ride ride = conf.rides.get(rideName3);

                    if (args.length<2) {
                        // Show Ride Info
                        String out = ride.getRideInfoStr();
                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',out));

                    } else {
                        switch (args[1]) {
                            case "info":
                                // Show ride info
                                String out = ride.getRideInfoStr();
                                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',out));

                                return true;
                            case "reload":
                                //reloadRide sends messages
                                conf.reloadRide(rideName3,commandSender);
                                return true;
                            case "enable":

                                //Try to enable the ride
                                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.prefix + ride.setConfigOption("ENABLED","true",(Player) commandSender)));

                                return true;
                            case "disable":
                                //Disable the ride
                                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.prefix + ride.setConfigOption("ENABLED","false",(Player) commandSender)));
                                return true;
                            case "setting":
                                //Check if a setting is given
                                if (args.length<3) {

                                    //Show available settings
                                    List<String> options = ride.getConfigOptions();
                                    StringBuilder optionStr = new StringBuilder();
                                    for (String str:options) {
                                        optionStr.append("&9").append(str).append("&1, ");
                                    }
                                    String optionStr2 = optionStr.toString().substring(0,optionStr.length()-4);
                                    String message = Messages.prefix + Messages.command_admin_ride_setting_list.replaceAll("\\{settings}",optionStr2);
                                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',message));
                                    return true;
                                }
                                String settingKey = args[2];
                                if (ride.getConfigOptions().contains(settingKey.toUpperCase())) {
                                    //Valid option, try setting the value
                                    StringBuilder value = new StringBuilder();
                                    if (args.length>=4) {
                                        for (int i=3;i<args.length;i++) {
                                            value.append(args[i]).append(" ");
                                        }
                                    } else value.append(" ");
                                    String result = ride.setConfigOption(settingKey.toUpperCase(), value.toString(), (Player) commandSender);
                                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.prefix + result));
                                } else {
                                    //invalid setting. Print options.
                                    //Show available settings
                                    List<String> options = ride.getConfigOptions();
                                    StringBuilder optionStr = new StringBuilder();
                                    for (String str:options) {
                                        optionStr.append("&b").append(str).append("&9, ");
                                    }
                                    String optionStr2 = optionStr.toString().substring(0,optionStr.length()-4);
                                    String message = Messages.prefix + Messages.command_admin_ride_setting_list.replaceAll("\\{settings}",optionStr2);
                                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',message));
                                }
                                return true;
                            default:
                                //Invalid sub command
                                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.prefix + Messages.command_admin_ride_invalid_sub));
                                return true;
                        }

                    }


                } else {
                    //Ride name is invalid
                    Set<String> rides2 = RidesPlugin.getInstance().getConfigHandler().rides.keySet();

                    StringBuilder list2 = new StringBuilder();
                    for (String r:rides2) {
                        list2.append("&9").append(r).append("&1, ");
                    }
                    String finalList2 = list2.substring(0,list2.length()-4);

                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.prefix + Messages.command_ride_not_exist));
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.prefix + Messages.command_listRides.replaceAll("\\{ridelist}", finalList2)));
                }
                return true;

        }

    }

    /**
     * Implement Tab completion
     */
    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String label, String[] args) {
        ArrayList<String> out = new ArrayList<>();

        //First level options
        if (args.length==1) {
            //Add basic options
            out = new ArrayList<>(Arrays.asList("help", "reload", "create", "delete", "linksign","unlinksign", "list"));

            //add ride names
            out.addAll(RidesPlugin.getInstance().getConfigHandler().rides.keySet());

            //Filter by what they've already typed
            out = Utils.filterList(out,"^"+args[0]);
        } else if (args.length==2) {
            //Second level options
            String subcommand = args[0];
            switch (subcommand) {
                case "reload":
                    out.add("all");
                    out.addAll(RidesPlugin.getInstance().getConfigHandler().rides.keySet());
                    break;
                case "delete":
                case "linksign":
                    out.addAll(RidesPlugin.getInstance().getConfigHandler().rides.keySet());
                    break;
                case "help":
                case "list":
                case "create":
                case "unlinksign":
                    //Add empty string, they have no sub commands
                    out.add("");
                    break;
                default:
                    out = new ArrayList<>(Arrays.asList("info", "enable", "disable", "reload", "setting"));
                    break;
            }
            out = Utils.filterList(out,"^"+args[1]);
        } else if (args.length==3) {
            if (args[0].equalsIgnoreCase("create")) {
                out = new ArrayList<>(Ride.RideTypes.keySet());
            }
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
                        out = new ArrayList<>(conf.rides.get(ridename).getConfigOptions());
                    }
                    break;
            }
            out = Utils.filterList(out,"^"+args[2]);
        } else if (args.length>3) {
            out.add("");
        }
        return out;
    }
}
