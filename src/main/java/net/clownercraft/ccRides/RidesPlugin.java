package net.clownercraft.ccRides;

import net.clownercraft.ccRides.rides.Carousel;
import net.clownercraft.ccRides.rides.DropTower;
import net.clownercraft.ccRides.rides.FerrisWheel;
import net.clownercraft.ccRides.rides.Ride;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class RidesPlugin extends JavaPlugin {
    //creating variables for class instances
    private static RidesPlugin instance;
    private ConfigHandler configHandler;
    private CommandExecutor commandExecutor;
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
    public CommandExecutor getCommandExecutor() {return commandExecutor; }

   @Override
   public void onEnable() {

       //Grabbing instances of the classes to pass around
       instance = this;
       configHandler = new ConfigHandler();
       commandExecutor = new CommandExecutor();
       listener = new RidesListener();

       getServer().getPluginManager().registerEvents(listener, instance);

       Objects.requireNonNull(getCommand("ccrides")).setExecutor(commandExecutor);

       //this should create the config files
       configHandler.createConfig();


       //Register Rides
       Ride.registerType("CAROUSEL", Carousel.class);
       Ride.registerType("DROP_TOWER", DropTower.class);
       Ride.registerType("FERRIS_WHEEL", FerrisWheel.class);

       //init rides
       //register rides as event listener

       Bukkit.getLogger().info("Starting RidesPlugin!");

   }

   @Override
   public void onDisable() {

        //Eject all players so they don't login where they shouldn't be

        //TODO Remove all vehicles to prevent duplicates in the world.

       Bukkit.getLogger().info("Stopping RidesPlugin.");
   }


}
