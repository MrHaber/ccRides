package net.clownercraft.ccRides;

import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.world.WorldUnloadEvent;

class RidesListener implements Listener {

    //getting instance of main class
    private RidesPlugin instance = RidesPlugin.getInstance();

    /**
     Detect when player clicks a sign to use ride
     */
    @EventHandler
    public void onSignClick(PlayerInteractEvent e) {

        if(e.getAction() == Action.RIGHT_CLICK_BLOCK) {

            if(e.getClickedBlock().getState() instanceof Sign) {
                Player player = e.getPlayer();
                player.sendMessage(ChatColor.translateAlternateColorCodes('@',"@eDing!"));

            }

        }

    }

    @EventHandler
    public void onCartExit(VehicleExitEvent e) {
        //Cancel if cart is part of a ride.
        //TODO
    }

    @EventHandler
    public void onCartEnter(VehicleEnterEvent e) {
        //Cancel if cart is part of a ride.
        //TODO
    }

    /**
     * Check when players disconnect if they are on a ride, and eject them from it if they are
     * @param event the quit event
     */
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        //TODO
    }

    /**
     * Remove minecarts from the world as it's unloaded, to prevent them being saved
     * @param event the world unload event
     */
    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        //TODO

    }


}