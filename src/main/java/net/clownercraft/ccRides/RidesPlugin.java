package net.clownercraft.ccRides;

import org.bukkit.plugin.java.JavaPlugin;

public class RidesPlugin extends JavaPlugin {

   private static RidesPlugin instance;

    public static RidesPlugin getRidesPlugin() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
    }

    @Override
    public void onDisable() {}
}
