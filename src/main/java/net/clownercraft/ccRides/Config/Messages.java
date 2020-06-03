package net.clownercraft.ccRides.Config;

import org.bukkit.ChatColor;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Messages {

    //General command messages
    public static String command_not_player = "&bccRides: &9This command must be run by a player.";
    public static String command_listRides = "&cccRides: &9Available rides are: \n{ridelist}";

    //Player command messages
    public static String command_player_help =
            "&1&m––––&b ccRides Help: &1&m––––\n" +
                    "&7/ride &8[&7name&8]&9 - Ride a ride \n" +
                    "&7/ride exit&9 - Exit a ride before it ends \n" +
                    "&7/ride lq&9 - Leave a ride queue\n" +
                    "&7/ride list&9 - see a list of all rides\n" +
                    "&7/ride help&9 - show this help";

    public static String command_player_exit = "&bccRides: &9You exited {ride}";
    public static String command_player_exit_notriding = "&bccRides: &9You're not on a ride.";
    public static String command_player_leavequeue_notqueueing = "&bccRides: &9You 're not in a queue";

    //admin command messages
    /*
     Aliases: /rideadmin /rideadm
     /rideadm help - show help
     /rideadm reload <all|ridename> - reload a specific ride or 'all' for everything
     /rideadm create <ridename> - create a new ride
     /rideadm delete <ridename> - create a new ride
     /rideadm linksign <ridename> - link a sign to a ride
     /rideadm list - see a list of all rides
    
     /rideadm <ridename> <info|reload|enable|disable> 
     /rideadm <ridename> setting <setting> <value> - reload a ride setting.
     */
    public static String command_admin_help = //TODO - Change this to the admin commands
        "&1&m––––&c ccRides Admin Help: &1&m––––\n" +
        "&9Aliases: &7/rideadmin /rideadm\n" +
        "&7/rideadm help &9- show help\n" +
        "&7/rideadm reload &8<&7all&8|&7rideName&8> &9- reload a ride or the whole plugin\n" +
        "&7/rideadm create &8<&7rideName&8> <&7rideType&8>&9- create a new ride\n" +
        "&7/rideadm delete &8<&7rideName&8> &9- create a new ride\n" +
        "&7/rideadm linksign &8<&7rideName&8> &9- link a sign to a ride\n" +
        "&7/rideadm list &9- see a list of all rides\n" +
        "&7/rideadm <&7rideName&8> <&7info&8|&7reload&8|&7enable&8|&7disable>\n" +
        "&7/rideadm <&7rideName&8> &7setting &8<&7setting&8> <&7value&8> &9- reload a ride setting.\n";

    public static String command_admin_reload_all = "&bccRides: &9Reloading plugin";
    public static String command_admin_reload_ride = "&bccRides: &9Reloading {ride} ride";
    public static String command_admin_create_ride = "&bccRides: &9Created new ride: {ride} Type: {type}";
    public static String command_admin_create_ride_syntax =
            "&bccRides: &cMissing/Incorrect arguments! \n" +
                    "&cUsage &7/rideadm create <name> <type>\n" +
            "&cAvailable ride types: {types}";
    public static String command_admin_delete_ride = "&bccRides: &9Created new ride: {ride} Type:";
    public static String command_admin_delete_ride_syntax =
            "&bccRides: &cMissing arguments! \n" +
            "&cUsage &7/rideadm delete <name>";




    //Ride Messages
    public static String ride_queue_joined = "&bccRides: &9You joined {ride}'s queue. &b{place} &9players are ahead of you.\n&9Use &7/ride lq&9 to abandon the queueing";
    public static String ride_queue_already = "&bccRides: &9You are already in the Queue. \n&9You are in place &b{place} of {total}";
    public static String ride_queue_other_queue = "&bccRides: &9You're already queuing for {ride}. \n&9Use &7/ride lq &9to abandon queueing.";
    public static String ride_queue_other_riding = "&bccRides: &9You can't join a queue while on a ride. \n&9Use &7/ride exit&9 to leave the ride";
    public static String ride_queue_left = "&bccRides: &9You left {ride}'s queue.";

    //TODO make this configurable


    /**
     * Used for filtering tab-complete results based on what the user started typing
     * @param list the full list to filter
     * @param regex regex to filter by
     * @return the filtered list
     */
    public static List<String> filterList(List<String> list, String regex) {
        Pattern filter = Pattern.compile(regex);
        return list.stream()
                .filter(filter.asPredicate())
                .collect(Collectors.toList());
    }


}
