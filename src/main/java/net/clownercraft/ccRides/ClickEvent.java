package net.clownercraft.ccRides;

import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

class ClickEvent implements Listener {

    //getting instance of main class
    private RidesPlugin instance = RidesPlugin.getInstance();

    @EventHandler
    public void onSignClick(PlayerInteractEvent e) {

        if(e.getAction() == Action.RIGHT_CLICK_BLOCK) {

            if(e.getClickedBlock().getState() instanceof Sign) {
                Player player = e.getPlayer();
                player.sendMessage(ChatColor.translateAlternateColorCodes('@',"@eDing!"));

            }

        }

    }



}
