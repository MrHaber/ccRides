package net.clownercraft.ccRides;

import net.clownercraft.ccRides.Config.ConfigHandler;
import net.clownercraft.ccRides.commands.AdminCommandExecutor;
import net.clownercraft.ccRides.commands.PlayerCommandExecutor;
import net.clownercraft.ccRides.rides.Carousel;
import net.clownercraft.ccRides.rides.DropTower;
import net.clownercraft.ccRides.rides.FerrisWheel;
import net.clownercraft.ccRides.rides.Ride;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class RidesPlugin extends JavaPlugin {
    //creating variables for class instances
    private static RidesPlugin instance;
    private ConfigHandler configHandler;
    private AdminCommandExecutor adminCommandExecutor;

    /**
     * @return the player command executor
     */
    public PlayerCommandExecutor getPlayerCommandExecutor() {
        return playerCommandExecutor;
    }

    private PlayerCommandExecutor playerCommandExecutor;
    private RidesListener listener;

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

    /**
     *
     * @return
     * Returns an instance of the Command Executor
     */
    public AdminCommandExecutor getAdminCommandExecutor() {return adminCommandExecutor; }

   @Override
   public void onEnable() {

       //Grabbing instances of the classes to pass around
       instance = this;

       //Register rides
       Ride.registerType("CAROUSEL", Carousel.class);
       Ride.registerType("DROP_TOWER", DropTower.class);
       Ride.registerType("FERRIS_WHEEL", FerrisWheel.class);

       //Load Configs
       configHandler = new ConfigHandler();

       adminCommandExecutor = new AdminCommandExecutor();
       playerCommandExecutor = new PlayerCommandExecutor();

       listener = new RidesListener();

       getServer().getPluginManager().registerEvents(listener, instance);

       Objects.requireNonNull(getCommand("ride")).setExecutor(playerCommandExecutor);
       Objects.requireNonNull(getCommand("rideadmin")).setExecutor(adminCommandExecutor);


       //init rides

       for (Ride r: configHandler.rides.values()) {
           r.init();
           getLogger().info("Started " + r.ID + " Ride!");
       }

   }

   @Override
   public void onDisable() {

       //Disable the rides to throw players out & despawn vehicles
       for (Ride r:configHandler.rides.values()) {
           getLogger().info("Stopping " + r.ID + " ride.");
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

       //init rides
       for (Ride r: configHandler.rides.values()) {
           r.init();
       }
       sender.sendMessage("Re-Started Rides");
   }

}
