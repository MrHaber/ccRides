package net.clownercraft.ccRides.commands;

import net.clownercraft.ccRides.Config.ConfigHandler;
import net.clownercraft.ccRides.Config.Messages;
import net.clownercraft.ccRides.RidesPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Handles the player rides command
 *
 * Aliases: /ride /ccride /rides /ccrides
 * /ride - show help
 * /ride help - show help
 * /ride <name> - ride a ride
 * /ride exit - exit the ride before it's finished
 * /ride lq - leave a queue
 * /ride list - list all rides
 */
public class PlayerCommandExecutor implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.command_not_player));
            return true;
        }

        if (args.length==0) {
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.command_player_help));
            return true;
        } else {

            String subcommand = args[0];
            if (subcommand.equalsIgnoreCase("help")) {

                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.command_player_help));
                return true;

            } else if (subcommand.equalsIgnoreCase("exit")) {
                //Exit the ride
                Player player = (Player) commandSender;
                ConfigHandler conf = RidesPlugin.getInstance().getConfigHandler();
                HashMap<Player, String> ridePlayers = conf.ridePlayers;
                if (ridePlayers.containsKey(player)) {
                    conf.rides.get(ridePlayers.get(player)).ejectPlayer(player);
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.command_player_exit).replaceAll("\\{ride}", ridePlayers.get(player)));
                } else {
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.command_player_exit_notriding));
                }
                return true;
            } else if (subcommand.equalsIgnoreCase("lq")) {
                //leave the ride queue
                Player player = (Player) commandSender;
                ConfigHandler conf = RidesPlugin.getInstance().getConfigHandler();
                HashMap<Player, String> queue = conf.queueingPlayers;
                if (queue.containsKey(player)) {
                    conf.rides.get(queue.get(player)).removeFromQueue(player);
                    //Remove from queue sends message to player, we don't need to.
                } else {
                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.command_player_leavequeue_notqueueing));
                }
                return true;
            } else if (subcommand.equalsIgnoreCase("list")) {
                //list the available rides
                Set<String> rides = RidesPlugin.getInstance().getConfigHandler().rides.keySet();

                StringBuilder list = new StringBuilder();
                for (String r:rides) {
                    list.append("&9").append(r).append("&1, ");
                }
                String finalList = list.substring(0,list.length()-4);

                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.command_player_listRides).replaceAll("\\{ridelist}", finalList));
                return true;
            } else {
                // Check if they entered a ride name
                ConfigHandler conf = RidesPlugin.getInstance().getConfigHandler();
                if (conf.rides.keySet().contains(subcommand.toLowerCase())) {
                    //The arg is a ride name, add the player to it
                    //The addplayer method sends messages depending what it does, so we don't need to here.
                    conf.rides.get(subcommand).addPlayer((Player) commandSender);
                } else return false; //They entered something that isn't a sub command...
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String label, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length==1) {
            //Add basic options
            out = Arrays.asList("help", "exit", "lq", "list");

            //add ride names
            for (String ride: RidesPlugin.getInstance().getConfigHandler().rides.keySet()) {
                out.add(ride);
            }

            //Filter by what they've already typed
            out = filterList(out,"^"+args[0]);
        }
        return out;
    }

    public List<String> filterList(List<String> list, String regex) {
        Pattern filter = Pattern.compile(regex);
        return list.stream()
                .filter(filter.asPredicate())
                .collect(Collectors.toList());
    }

}