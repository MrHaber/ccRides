package net.clownercraft.ccRides.commands;

import net.clownercraft.ccRides.Config.ConfigHandler;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.List;

/**
 * Handles the Ride Admin Command
 *
 * Aliases: /rideadmin /rideadm
 * /rideadm reload - reload the whole plugin
 * /rideadm reload [ridename] - reload a specific plugin
 * /rideadm create [ridename] - create a new ride
 * /rideadm delete [ridename] - create a new ride
 * /rideadm linksign [ridename] - link a sign to a ride
 * /rideadm list - see a list of all rides
 *
 * /rideadm <ridename> - View ride info
 * /rideadm <ridename> reload - reload a single ride
 * /rideadm <ridename> enable/disable - enable/disable ride
 * /rideadm <ridename> setting [setting] [value] - reload a ride setting.
 */
public class AdminCommandExecutor implements CommandExecutor, TabCompleter {

    ConfigHandler configHandler = new ConfigHandler();

    ConfigHandler.ConfigType type;

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String alias, String[] argument) {
        //TODO Redo this vvv
        if(alias.equalsIgnoreCase("ccrides")) {

            try{

                //show-rides sub-command
                if(argument[0].equalsIgnoreCase("show-rides")) {

                    //TO-DO
                    /*
                    get names
                    get locations
                    display them
                     */

                }

                //delete-ride sub-command
                if(argument[0].equalsIgnoreCase("delete-ride")) {

                    //TO-DO
                    /*
                    get name
                    delete config path
                     */

                }

                //set-ride sub-command
                if (argument[0].equalsIgnoreCase("set-ride")){

                    //TO-DO
                    /*
                    save location
                    save name
                    save ride type
                     */

                }

                //help sub-command
                if(argument[0].equalsIgnoreCase("help")){

                    return false;

                }

                //config sub-command
                if(argument[0].equalsIgnoreCase("config")) {

                    //getting the enum type
                    switch(argument[1].toLowerCase()) {

                        case "information":

                            type = ConfigHandler.ConfigType.INFORMATION;
                            break;

//                        case "carousel":
//
//                            type = ConfigHandler.ConfigType.CAROUSEL;
//                            break;
//
//                        case "droptower":
//
//                            type = ConfigHandler.ConfigType.DROPTOWER;
//                            break;
//
//                        case "ferriswheel":
//
//                            type = ConfigHandler.ConfigType.FERRISWHEEL;
//                            break;

                        case "help":

                            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('@', "@aThere are currently these config files, " + Arrays.toString(ConfigHandler.ConfigType.values())));
                            return false;

                        default:

                            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('@', "@cSelect a config file to access! " + Arrays.toString(ConfigHandler.ConfigType.values())));
                            return true;

                    }

                    //error detection: testing if config path exists
                    String oldEntry = configHandler.getConfigPath(type, argument[3]);
                    if(oldEntry == null) {

                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('@', "@cThat config path doesn't exist for the selected config file!"));
                        return true;

                    }

                    switch(argument[2].toLowerCase()){

                        case "set":

                            StringBuilder newEntry;
                            newEntry = new StringBuilder("\"");
                            int argumentLength = argument.length;
                            for(int i = 2; i <= argumentLength; ++i) {
                                newEntry.append(" ").append(argument[i]);
                            }
                            newEntry.append('"');
                            configHandler.setConfigPath(type, argument[3], newEntry.toString());
                            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('@', "@2You have successfully set @d" + argument[3] + "@2 in @b" + argument[2] + "@2 from @6" + oldEntry + "@2 to @e" + newEntry + "@2!"));
                            return true;

                        case "get":

                            String entry = configHandler.getConfigPath(type, argument[3]);
                            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('@', "@2The current entry in @b" + argument[1] + "@2 under @d" +argument[3] + "@2 is @6" + entry + "@2!"));
                            return true;

                        case "help":

                            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('@', "@a Choose between 'set' or 'get'. 'set' replaces the value in the config file with the new one. 'get' returns to you the current value of the config file."));

                        default:
                            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('@', "@cYou must choose between 'set' or 'get'!"));
                            return false;

                    }

                }

            } catch (ArrayIndexOutOfBoundsException e) {

                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('@', "@cYou must include an argument!"));
                return false;

            }

            return false;

        }

        return false;

    }

    /**
     * Implement Tab completion
     * @param commandSender
     * @param command
     * @param s
     * @param strings
     * @return
     */
    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return null;
        //TODO
    }
}
