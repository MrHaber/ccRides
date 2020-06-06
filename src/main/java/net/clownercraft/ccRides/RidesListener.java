package net.clownercraft.ccRides;

import net.clownercraft.ccRides.Config.ConfigHandler;
import net.clownercraft.ccRides.Config.Messages;
import net.clownercraft.ccRides.rides.Ride;
import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class RidesListener implements Listener {
    public static boolean waitingSignClick = false; //Whether we're waiting for a player to click a sign for a command
    public static boolean waitingUnlink = false; //Whether the command was unlink instead of link
    public static CommandSender waitingSender; //The commandSender to check for
    public static String waitingRideID = ""; //The ID of the ride to wait for.


    //getting instance of main class
    private ConfigHandler conf = RidesPlugin.getInstance().getConfigHandler();

    /**
     Detect when player clicks a sign to use ride
     */
    @EventHandler
    public void onSignClick(PlayerInteractEvent e) {

        if(e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (waitingSignClick) {
                if (e.getPlayer().equals(waitingSender) && e.getClickedBlock().getState() instanceof Sign) {
                    //Set or remove the sign as a ride sign
                    if (waitingUnlink) {
                        conf.rideSigns.remove(e.getClickedBlock().getLocation());
                        waitingSender.sendMessage(ChatColor.translateAlternateColorCodes('&', Messages.prefix + Messages.command_admin_unlinksign_unlinked));
                    } else {
                        conf.rideSigns.put(e.getClickedBlock().getLocation(),waitingRideID);
                        waitingSender.sendMessage(ChatColor.translateAlternateColorCodes('&', Messages.prefix + Messages.command_admin_linksign_linked.replaceAll("\\{ride}",waitingRideID)));
                    }
                    conf.saveSignConfig();
                    waitingSignClick = false;
                }
            } else if(conf.rideSigns.containsKey(e.getClickedBlock().getLocation())) {
                //Clicked sign was a ride sign.
                if (e.getPlayer().hasPermission("ccrides.user")) {
                    //IF player has permission, put them on the ride!
                    String rideID = conf.rideSigns.get(e.getClickedBlock().getState().getLocation());
                    Ride ride = conf.rides.get(rideID);
                    ride.addPlayer(e.getPlayer());
                }

            }

        }

    }


}
