package net.clownercraft.ccRides;

import org.bukkit.plugin.java.JavaPlugin;

public class RidesPlugin extends JavaPlugin {

   private static RidesPlugin instance;
   private ConfigHandler configHandler;

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

   @Override
   public void onEnable() {

       //Grabbing instances of the lasses to pass around
       instance = this;
       configHandler = new ConfigHandler();

   }

   @Override
   public void onDisable() {}
}
