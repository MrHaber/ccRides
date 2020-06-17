package net.clownercraft.ccRides;

import net.clownercraft.ccRides.config.ConfigHandler;
import net.clownercraft.ccRides.commands.AdminCommandExecutor;
import net.clownercraft.ccRides.commands.PlayerCommandExecutor;
import net.clownercraft.ccRides.rides.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class RidesPlugin extends JavaPlugin {
    //creating variables for class instances
    private static RidesPlugin instance;
    private ConfigHandler configHandler;
    private Economy econ = null;

    /**
     *
     * @return
     * Returns an instance of RidesPlugin
     */
   public static RidesPlugin getInstance() { return instance; }

    /**
     *
     * @return
     * Returns an instance of ConfigHandler
     */
   public ConfigHandler getConfigHandler() { return configHandler; }



   @Override
   public void onEnable() {

       //Grabbing instances of the classes to pass around
       instance = this;

       //Register ride types
       Ride.registerType("CAROUSEL", Carousel.class);
       Ride.registerType("FERRIS_WHEEL", FerrisWheel.class);
       Ride.registerType("CHAIRSWING", Chairswing.class);
       Ride.registerType("JETS", Jets.class);

       //Load Configs
       configHandler = new ConfigHandler();

       AdminCommandExecutor adminCommandExecutor = new AdminCommandExecutor();
       PlayerCommandExecutor playerCommandExecutor = new PlayerCommandExecutor();

       RidesListener listener = new RidesListener();

       getServer().getPluginManager().registerEvents(listener, instance);

       Objects.requireNonNull(getCommand("ride")).setExecutor(playerCommandExecutor);
       Objects.requireNonNull(getCommand("rideadmin")).setExecutor(adminCommandExecutor);


       //Link Vault
       if (configHandler.LinkVault) {
               if (setupEconomy()) {
                   getLogger().info("Linked into Vault for Economy");
               } else {
                   configHandler.LinkVault = false;
               }

       }

       //init rides

       for (Ride r: configHandler.rides.values()) {
           r.init();
           getLogger().info("Started " + r.rideID + " Ride!");
       }

       //Link PAPI
       if (configHandler.LinkPlaceholderAPI) {
           if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
               new PlaceholderProvider(this).register();
               getLogger().info("Linked PlaceholderAPI");
           } else {
               getLogger().warning("Couldn't link PlaceholerAPI - is it installed?");
               configHandler.LinkPlaceholderAPI = false;
           }
       }

   }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Couldn't link to vault - it is not installed.");
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("Couldn't link to vault!");
            return false;
        }
        econ = rsp.getProvider();
        return true;
    }

    /**
     * Check if a player can afford a ride.
     * returns true if vault is disabled or didn't link.
     * @param player - the player to check
     * @param cost - the cost of the ride
     * @return whether to allow the player on the ride
     */
    public boolean canAfford(Player player, int cost) {
        if (configHandler.LinkVault && cost > 0) return econ.has(player,cost);
        else return true;
    }

    /**
     * Take payment from player for a ride
     * Does nothing if vault disabled or didn't link.
     * @param player - the player to take from
     * @param cost - the cost of the ride
     */
    public void takePayment(Player player, int cost) {
        if (configHandler.LinkVault && cost > 0) {
            econ.withdrawPlayer(player,cost);
        }
    }

   @Override
   public void onDisable() {

       //Disable the rides to throw players out & despawn vehicles
       for (Ride r:configHandler.rides.values()) {
           getLogger().info("Stopping " + r.rideID + " ride.");
           r.disable();
       }

       getLogger().info("Stopping RidesPlugin.");
   }

    /**
     * Called to reload the whole plugin for any config changes
     * @param sender the commandSender that requested the reload.
     */
   public void reload(CommandSender sender) {

       //Disable the rides (kicking players to exit location, removing carts
       //Also unregister the rides as listeners
       sender.sendMessage("Disabling Rides to be safe...");

       for (Ride r:configHandler.rides.values()) {
           r.disable();
           HandlerList.unregisterAll(r);
       }

       //recreate the config handler which will load all configs from disk again
       sender.sendMessage("Reloading Configs");
       configHandler = new ConfigHandler();

       //Link Vault
       if (configHandler.LinkVault) {
           if (setupEconomy()) {
               getLogger().info("Linked into Vault for Economy");
           } else {
               configHandler.LinkVault = false;
           }

       }

       //init rides
       for (Ride r: configHandler.rides.values()) {
           r.init();
       }
       sender.sendMessage("Re-Started Rides");

       //link Papi
       if (configHandler.LinkPlaceholderAPI) {
           if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
               new PlaceholderProvider(this).register();
               getLogger().info("Linked PlaceholderAPI");
           } else {
               getLogger().warning("Couldn't link PlaceholerAPI - is it installed?");
               configHandler.LinkPlaceholderAPI = false;
           }
       }
   }


}
