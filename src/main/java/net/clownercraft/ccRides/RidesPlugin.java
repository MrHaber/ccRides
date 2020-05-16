package net.clownercraft.ccRides;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class RidesPlugin extends JavaPlugin {

    //creating variables for class instances
   private static RidesPlugin instance;
   private ConfigHandler configHandler;
   private CommandExecutor commandExecutor;

    /**
     *
     * @return
     * Returns an instance of RidesPlugin
     */
   static RidesPlugin getInstance() { return instance; }

    /**
     *
     * @return
     * Returns an instance of ConfigHandler
     */
   public ConfigHandler getConfigHandler() { return configHandler; }

    /**
     *
     * @return
     * Returns an instance of RidesPlugin
     */
    public CommandExecutor getCommandExecutor() {return commandExecutor; }

   @Override
   public void onEnable() {

       //Grabbing instances of the classes to pass around
       instance = this;
       configHandler = new ConfigHandler();
       commandExecutor = new CommandExecutor();

       Objects.requireNonNull(getCommand("ccrides")).setExecutor(commandExecutor);

       //this should create the config files
       configHandler.createConfig();

       Bukkit.getLogger().info("Starting RidesPlugin!");
   }

   @Override
   public void onDisable() {
       Bukkit.getLogger().info("Stopping RidesPlugin.");
   }
}
