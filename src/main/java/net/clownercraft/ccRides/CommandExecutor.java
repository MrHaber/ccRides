package net.clownercraft.ccRides;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandExecutor implements org.bukkit.command.CommandExecutor {

    ConfigHandler configHandler = new ConfigHandler();

    ConfigHandler.ConfigType type;

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String string, String[] argument) {

        if(string.toString().equalsIgnoreCase("ccrides")) {

            try{

                if(argument[0].equalsIgnoreCase("help")){

                    return false;

                }

                if(argument[0].equalsIgnoreCase("config")) {

                    //error detection: making sure there is a minimum number of arguments
                    if(argument.length < 4) {

                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('@', "@cThere must be at least 4 arguments for the config command!"));
                        return false;

                    }

                    //getting the enum type
                    switch(argument[1].toLowerCase()) {

                        case "information":
                            type = ConfigHandler.ConfigType.INFORMATION;
                            break;
                        case "carousel":
                            type = ConfigHandler.ConfigType.CAROUSEL;
                            break;
                        case "droptower":
                            type = ConfigHandler.ConfigType.DROPTOWER;
                            break;
                        case "ferriswheel":
                            type = ConfigHandler.ConfigType.FERRISWHEEL;
                            break;

                        default:
                            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('@', "@aSelect a config file to access!"));
                            return true;
                    }

                    //error detection: testing if config path exists
                    String oldEntry = configHandler.getConfigPath(type, argument[3]);
                    if(oldEntry == null) {

                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('@', "@aThat config path doesn't exist for the selected config file!"));
                        return true;

                    }

                    switch(argument[2]){

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
                            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('@', "@2The current entry in @b" + argument[2] + "@2 under @d" +argument[3] + "@2 is @6" + entry + "@2!"));
                            return true;

                        default:
                            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('@', "@aYou must choose between 'set' or 'get'!"));
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
}
