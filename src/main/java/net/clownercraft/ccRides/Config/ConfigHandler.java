package net.clownercraft.ccRides.Config;

import net.clownercraft.ccRides.RidesPlugin;
import net.clownercraft.ccRides.rides.Ride;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConfigHandler {

    //Getting an instance of the main class
    private RidesPlugin instance;

    /*
    Config Objects
     */
    private FileConfiguration mainConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration signsConfig;
    private HashMap<String,FileConfiguration> rideConfigs; //Ride ID, Ride Configuration

    private final File mainFile;
    private final File messagesFile;
    private final File signFile;
    File rideFolder;

    /* Ride Data */
    public HashMap<String,Ride> rides = new HashMap<>(); //Ride Name/ID, Ride Object

    //Player tracking for commands
    public HashMap<Player,String> ridePlayers = new HashMap<>();
    public HashMap<Player,String> queueingPlayers = new HashMap<>();

    /* Signs */
    public HashMap<Location,String> rideSigns = new HashMap<>(); //Sign Location, Ride ID

    /*
    Global Config Options
     */
    public boolean LinkVault = false;
    public boolean LinkPlaceholderAPI = false;
    //None yet...

    /**
     * Init the configHandler
     * and Load all the files
     */
    public ConfigHandler() {
        instance = RidesPlugin.getInstance();

        //Load Files
        mainFile = new File(instance.getDataFolder(), "config.yml");
        messagesFile = new File(instance.getDataFolder(), "messages.yml");
        signFile = new File(instance.getDataFolder(), "signs.yml");
        rideFolder = new File(instance.getDataFolder(),"Rides");

        //Create Files if they don't exist
        testConfig(mainFile);
        testConfig(messagesFile);
        testConfig(signFile);
        if (rideFolder.mkdir()) instance.getLogger().info("Rides folder doesn't exist- creating it.");

        File[] rideFiles = rideFolder.listFiles();

        //Load Main configurations
        mainConfig = YamlConfiguration.loadConfiguration(mainFile);
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        signsConfig = YamlConfiguration.loadConfiguration(signFile);

        instance.getLogger().info("Main Configs Loaded");

        //Load Global Settings into memory
        LinkVault = mainConfig.getBoolean("Integrations.vault");
        LinkPlaceholderAPI = mainConfig.getBoolean("Integrations.placeholderAPI");

        //Load Messages
        Messages.init((YamlConfiguration) messagesConfig);

        //Load Ride configurations
        rideConfigs = new HashMap<>();
        for (File f: rideFiles) {
            YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);
            String Id = conf.getString("Generic.ID");
            rideConfigs.put(Id,conf);
            instance.getLogger().info("\"" + Id + "\" ride config loaded.");
            rides.put(Id,Ride.RideFromConfig(conf));
            instance.getLogger().info("\"" + Id + "\" ride config loaded.");
        }
        if (rideFiles.length==0) instance.getLogger().info("No Rides Configured yet.");

        //Load signs
        Set<String> keys = signsConfig.getKeys(false);
        for (String key:keys) {
            rideSigns.put(signsConfig.getLocation(key+".location"),signsConfig.getString(key+".ride"));
        }
    }

    /**
     *
     * @param file
     * The file to test if it exists
     */
    private void testConfig(File file) {
        String name = file.getName();
        if(!(file.exists())) {
            instance.saveResource(name, true);
            instance.getLogger().info(name + " default config saved.");
        }
    }


    /**
     * Saves a ride's configuration to disk
     * @param conf = the rides configuration
     */
   public void saveRideConfig(FileConfiguration conf) {
        String ID = conf.getString("Generic.ID");
        rideConfigs.put(ID,conf);

        try {
            conf.save(new File(rideFolder,ID+".yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
   }

    /**
     * Reload a ride's config
     * @param rideID = the id of the ride to load
     * @param sender = The commandSender that requested the reload
     */
   public void reloadRide(String rideID, CommandSender sender) {
       //Disable the ride for safety
       sender.sendMessage("Disabling " + rideID + " ride to be safe...");
       Ride r = rides.get(rideID);
       r.disable();

       //Unregister ride event listeners
       HandlerList.unregisterAll(r);

       sender.sendMessage("Reloading the config...");

       //Load the updated config
       YamlConfiguration conf = YamlConfiguration.loadConfiguration(new File(rideFolder,rideID+".yml"));
       rideConfigs.put(rideID,conf);


       //re-init the ride
       rides.put(rideID,Ride.RideFromConfig(conf));
       rides.get(rideID).init();
       sender.sendMessage("Restarted the ride...");

   }

    /**
     * Save signs data to file
     */
   public void saveSignConfig() {
       //create a new config
       signsConfig = new YamlConfiguration();

       //add signs to it
       int i=0;
       for (Location loc:rideSigns.keySet()) {
           signsConfig.set(i+".location",loc);
           signsConfig.set(i+".ride",rideSigns.get(loc));
           i++;
       }

       //save the file
       try {
           signsConfig.save(signFile);
       } catch (IOException e) {
           e.printStackTrace();
       }
   }

    /**
     * Save the main config to file
     */
   public void saveMainConfig() {
       //Create a new config
       mainConfig = new YamlConfiguration();

       //set settings
       mainConfig.set("Integrations.Vault",LinkVault);
       mainConfig.set("Integrations.PlaceholderAPI",LinkPlaceholderAPI);

       //Save the file
       try {
           mainConfig.save(mainFile);
       } catch (IOException e) {
           e.printStackTrace();
       }
   }

   public void deleteRide(String name) {
       Ride r = rides.get(name);

       //Disable the ride
       //Ejects players, despawns seats, clears queue
       r.disable();

       //unregister listeners
       HandlerList.unregisterAll(r);

       //remove ride object
        rides.remove(name);

       //remove any linked signs
       for (Location loc : rideSigns.keySet()) {
           String rname = rideSigns.get(loc);
           if (rname!=null && rname.equals(name)) {
               rideSigns.remove(loc);
           }
       }
       saveSignConfig();

       //delete the ride config
       rideConfigs.remove(name);
       File rideFile = new File(rideFolder,name+".yml");
       rideFile.delete();
   }

}


