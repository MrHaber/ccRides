package net.clownercraft.ccRides.Config;

import org.bukkit.ChatColor;

public class Messages {

    //General command messages
    public static String command_not_player = "&bccRides: &9This command must be run by a player.";

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
    public static String command_player_listRides = "&cccRides: &9Available rides are: \n{ridelist}";

    //admin command messages



    //Ride Messages
    public static String ride_queue_joined = "&bccRides: &9You joined {ride}'s queue. &b{place} &9players are ahead of you.\n&9Use &7/ride lq&9 to abandon the queueing";
    public static String ride_queue_already = "&bccRides: &9You are already in the Queue. \n&9You are in place &b{place} of {total}";
    public static String ride_queue_other_queue = "&bccRides: &9You're already queuing for {ride}. \n&9Use &7/ride lq &9to abandon queueing.";
    public static String ride_queue_other_riding = "&bccRides: &9You can't join a queue while on a ride. \n&9Use &7/ride exit&9 to leave the ride";
    public static String ride_queue_left = "&bccRides: &9You left {ride}'s queue.";







    //TODO make this configurable
}
