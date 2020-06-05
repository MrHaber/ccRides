package net.clownercraft.ccRides.Config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Messages {
    static final int expectedVersion = 1;

    /*
    Default options are defined here, and will be saved to the file if the version is newer
     */
    public static String prefix = "&9&lccRides &b» ";

    //General command messages
    public static String command_not_player = "&cThis command must be run by a player.";
    public static String command_listRides = "&9Available rides are: \n{ridelist}";
    public static String command_ride_not_exist = "&cThat ride does not exist.";

    //Player command messages
    public static String command_player_help = "&1&m–------–––&b ccRides Help: &1&m––------––\n"+
            "&7/ride &8[&7name&8]&9 - Ride a ride\n"+
            "&7/ride exit&9 - Exit a ride before it ends\n"+
            "&7/ride lq&9 - Leave a ride queue\n"+
            "&7/ride list&9 - see a list of all rides\n"+
            "&7/ride help&9 - show this help\n"+
            "&1&m------------------------------------------";
    public static String command_player_exit = "&9You exited {ride}";
    public static String command_player_exit_notriding = "&9You're not on a ride.";
    public static String command_player_leavequeue = "&9You abandoned queueing for {ride}";
    public static String command_player_leavequeue_notqueueing = "&9You're not in a queue";

    //admin command messages
    public static String command_admin_help = "&4&m–––----–&c ccRides Admin Help: &4&m––----––\n" +
            "  &cAliases: &7/rideadmin /rideadm\n" +
            "&7/rideadm help &c- show help\n" +
            "&7/rideadm reload &8<&7all&8|&7rideName&8> &c- reload a ride or everything\n" +
            "&7/rideadm create &8<&7rideName&8> <&7rideType&8> &c- create a new ride\n" +
            "&7/rideadm delete &8<&7rideName&8> &c- delete a ride\n" +
            "&7/rideadm linksign &8<&7rideName&8> &c- link a sign to a ride\n" +
            "&7/rideadm unlinksign &c- un-link a sign from a ride\n" +
            "&7/rideadm list &c- see a list of all rides\n" +
            "&7/rideadm &8<&7name&8> <&7info&8|&7reload&8|&7enable&8|&7disable&8>\n" +
            "&7/rideadm &8<&7name&8> &7setting &8<&7key&8> <&7value&8> &c- change ride settings." +
            "&4&m------------------------------------------------";
    public static String command_admin_reload_all = "&9Reloading plugin";
    public static String command_admin_reload_ride = "&9Reloading {ride} ride";
    public static String command_admin_create_ride = "&9Created new ride: {ride} Type: {type}";
    public static String command_admin_create_ride_exists = "&c{ride} Already Exists";
    public static String command_admin_create_ride_syntax = "&cMissing/Incorrect arguments! \n" +
            "&cUsage &7/rideadm create <name> <type>\n" +
            "&cAvailable ride types: {types}";
    public static String command_admin_delete_ride = "&9Deleted ride {test}";
    public static String command_admin_delete_ride_syntax = "&cMissing arguments! \n&cUsage &7/rideadm delete &8<&7name&8>";
    public static String command_admin_ride_enable = "&9{ride} has been enabled";
    public static String command_admin_ride_enable_fail =
            "&c{ride} could not be enabled. \n" +
            "&cPlease Check all it's settings are correct. ";
    public static String command_admin_ride_disable = "&9{ride} has been disabled";
    public static String command_admin_ride_invalid_sub = "&cUnrecognised sub-command, options are info, reload, enable, disable, setting";

    @Deprecated
    public static String command_admin_ride_info ="&bccRides: &9{ride} Info\n";

    public static String command_admin_ride_info_general = "&4&m---- &c&l ccRides: &c{ID} Info &4&m----\n" +
            "&4         -- &cGeneral Info &4--\n" +
            "&9Enabled: {ENABLED} &8// &9Running: {RUNNING} &8// &9Price: {PRICE}\n" +
            "&9Current Riders: {RIDER_COUNT} &8// &9Queue Size: {QUEUE_COUNT}\n" +
            "&9Start Players: {START_PLAYERS} &8// &9Start Delay: {START_DELAY} &9seconds\n" +
            "&9Join After Start: {JOIN_AFTER_START}\n" +
            "&9Exit Location: {EXIT_LOCATION}\n" +
            "&4&m------------------------------------------------";
    public static String command_admin_ride_info_carousel = "&4  -- &cCAROUSEL Specific Info &4--\n" +
            "&9Radius: {RADIUS} &8// &9Capacity: {CAPACITY} &9seats\n" +
            "&9Base Location: {BASE_LOCATION}\n" +
            "&9Rotate Speed: {ROTATE_SPEED} &9ticks per rotation &8// &9Ride Length {RIDE_LENGTH} &9rotations\n" +
            "&9Height Variation: ±{HEIGHT_VAR} &9blocks &8// &9Height Speed: {HEIGHT_SPEED} &9height cycles per rotation\n" +
            "&9Horse mode: {HORSE_MODE} &9(false = minecart seats)\n" +
            "&4&m------------------------------------------------";
    public static String command_admin_ride_info_ferrisWheel = "&4  -- &cFERRIS_WHEEL Specific Info &4--\n" +
            "&9Radius: {RADIUS} &8// &9Capacity: {CAPACITY} &9seats &8// &9Seat Width: {SEAT_WIDTH}\n" +
            "&9Base Location: {BASE_LOCATION} &8// &9Axis: {AXIS} &9(false = xy, true=zy)\n" +
            "&9Rotate Speed: {ROTATE_SPEED} &9ticks per rotation &8// &9Ride Length {RIDE_LENGTH} &9rotations\n" +
            "&4&m------------------------------------------------";
    public static String command_admin_ride_setting_list = "&9Available Settings for this ride: \n{settings}";
    public static String command_admin_linksign_syntax = "&cPlease specify a ride to link the sign to\n&cUsage: &7/rideadm linksign &8<&7rideName&8>";
    public static String command_admin_linksign_click = "&9Please right click the sign to link it";
    public static String command_admin_unlinksign_click = "&9Please right click the sign to unlink it";
    public static String command_admin_linksign_linked = "&9Sign now linked to {ride}";
    public static String command_admin_unlinksign_unlinked = "&9Sign no longer linked to a ride";


    //Ride Messages
    public static String ride_queue_joined = "&9You joined {ride}'s queue. &b{place} &9players are ahead of you.\n&9Use &7/ride lq&9 to abandon the queueing";
    public static String ride_queue_already = "&9You are already in the Queue. \n&9You are in place &b{place} of {total}";
    public static String ride_queue_other_queue = "&9You're already queuing for {ride}. \n&9Use &7/ride lq &9to abandon queueing.";
    public static String ride_queue_other_riding = "&9You can't join a queue while on a ride. \n&9Use &7/ride exit&9 to leave the ride";
    public static String ride_queue_left = "&9You left {ride}'s queue.";

    public static String ride_cant_afford = "&cYou don't have enough &6tokens&c to ride this ride! \n&cCome back when you have at least &6{price} tokens.";
    public static String ride_paid = "&9You paid &6{price} tokens&9 to ride on {ride}";


    public static String ride_starting_needMoreRiders = "&9Waiting for {count} more players to start.";
    public static String ride_starting_countdown = "&9The ride will start in {time} seconds.";
    public static String ride_starting_seatsFUll = "&9All seats are filled! Ride starting.";
    /**
     * Loads messages fields based on given config file
     * @param conf a config file containing all messages settings
     */
    public static void init(YamlConfiguration conf) {
        int confVersion = conf.getInt("MessagesVersion");
        //Misc/General Messages
        prefix = conf.getString("Prefix",prefix);

        //General Command messages
        command_not_player = conf.getString("Command.notPlayer",command_not_player);
        command_listRides = conf.getString("Command.listRides",command_listRides);
        command_ride_not_exist =  conf.getString("Command.rideNotExist",command_ride_not_exist);

        //Player Command Messages
        command_player_help = conf.getString("Command.Player.help",command_player_help);
        command_player_exit =  conf.getString("Command.Player.exit.main",command_player_exit);
        command_player_exit_notriding = conf.getString("Command.Player.exit.notRiding",command_player_exit_notriding);
        command_player_leavequeue = conf.getString("Command.Player.leaveQueue.main",command_player_leavequeue);
        command_player_leavequeue_notqueueing = conf.getString("Command.Player.leaveQueue.notQueueing",command_player_leavequeue_notqueueing);

        //Admin Command Messages
        command_admin_help = conf.getString("Command.Admin.help",command_admin_help);
        command_admin_reload_all = conf.getString("Command.Admin.reload.all",command_admin_reload_all);
        command_admin_reload_ride = conf.getString("Command.Admin.reload.ride",command_admin_reload_ride);
        command_admin_create_ride = conf.getString("Command.Admin.createRide.main",command_admin_create_ride);
        command_admin_create_ride_exists = conf.getString("Command.Admin.createRide.exists",command_admin_create_ride_exists);
        command_admin_create_ride_syntax = conf.getString("Command.Admin.createRide.syntax",command_admin_create_ride_syntax);
        command_admin_delete_ride = conf.getString("Command.Admin.deleteRide.main",command_admin_delete_ride);
        command_admin_delete_ride_syntax = conf.getString("Command.Admin.deleteRide.syntax",command_admin_delete_ride_syntax);
        command_admin_ride_enable = conf.getString("Command.Admin.ride.enable.main",command_admin_ride_enable);
        command_admin_ride_enable_fail = conf.getString("Command.Admin.ride.enable.fail",command_admin_ride_enable_fail);
        command_admin_ride_disable = conf.getString("Command.Admin.ride.disable",command_admin_ride_disable);
        command_admin_ride_info_general = conf.getString("Command.Admin.ride.info.general",command_admin_ride_info_general);
        command_admin_ride_info_carousel = conf.getString("Command.Admin.ride.info.carousel",command_admin_ride_info_carousel);
        command_admin_ride_info_ferrisWheel = conf.getString("Command.Admin.ride.info.ferrisWheel",command_admin_ride_info_ferrisWheel);
        command_admin_ride_setting_list = conf.getString("Command.Admin.ride.setting.list",command_admin_ride_setting_list);
        //TODO setting errors
        command_admin_ride_invalid_sub = conf.getString("Command.Admin.ride.invalidSub",command_admin_ride_invalid_sub);
        command_admin_linksign_syntax = conf.getString("Command.Admin.linksign.syntax",command_admin_linksign_syntax);
        command_admin_linksign_click = conf.getString("Command.Admin.linksign.click",command_admin_linksign_click);
        command_admin_linksign_linked = conf.getString("Command.Admin.linksign.linked",command_admin_linksign_linked);
        command_admin_unlinksign_click = conf.getString("Command.Admin.unlinksign.click",command_admin_unlinksign_click);
        command_admin_unlinksign_unlinked = conf.getString("Command.Admin.unlinksign.unlinked",command_admin_unlinksign_unlinked);

        //Ride Messagea
        ride_queue_joined = conf.getString("Ride.queue.joined",ride_queue_joined);
        ride_queue_already = conf.getString("Ride.queue.already",ride_queue_already);
        ride_queue_other_queue = conf.getString("Ride.queue.alreadyOther",ride_queue_other_queue);
        ride_queue_other_riding = conf.getString("Ride.queue.onRide",ride_queue_other_riding);
        ride_queue_left = conf.getString("Ride.queue.left",ride_queue_left);
        ride_cant_afford = conf.getString("Ride.cantAfford",ride_cant_afford);
        ride_paid = conf.getString("Ride.paid",ride_paid);
        ride_starting_needMoreRiders = conf.getString("Ride.starting.needMoreRiders",ride_starting_needMoreRiders);
        ride_starting_countdown = conf.getString("Ride.starting.countdown",ride_starting_countdown);
        ride_starting_seatsFUll = conf.getString("Ride.starting.seatsFull",ride_starting_seatsFUll);

        if (expectedVersion>confVersion) updateFile();
    }

    private static void updateFile() {
        YamlConfiguration conf = new YamlConfiguration();


        //TODO set things and save it
    }


    /**
     * Used for filtering tab-complete results based on what the user started typing
     * @param list the full list to filter
     * @param regex regex to filter by
     * @return the filtered list
     */
    public static ArrayList<String> filterList(ArrayList<String> list, String regex) {
        Pattern filter = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        return list.stream()
                .filter(filter.asPredicate()).collect(Collectors.toCollection(ArrayList::new));
    }


}
