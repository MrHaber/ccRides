package net.clownercraft.ccRides.config;

import net.clownercraft.ccRides.RidesPlugin;
import net.clownercraft.ccRides.rides.Ride;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigHandler {

    //Getting an instance of the main class
    private final RidesPlugin instance;

    /*
    Config Objects
     */
    private FileConfiguration mainConfig;
    private FileConfiguration signsConfig;

    private final File mainFile;
    private final File signFile;

    File rideFolder;

    /* Ride Data */
    public ConcurrentHashMap<String,Ride> rides = new ConcurrentHashMap<>(); //Ride Name/ID, Ride Object

    //Player tracking for commands
    public ConcurrentHashMap<UUID,String> ridePlayers = new ConcurrentHashMap<>();
    public ConcurrentHashMap<UUID,String> queueingPlayers = new ConcurrentHashMap<>();

    /* Signs */
    public ConcurrentHashMap<Location,String> rideSigns = new ConcurrentHashMap<>(); //Sign Location, Ride ID

    /*
    Global Config Options
     */
    public boolean LinkVault;
    public boolean LinkPlaceholderAPI;
    //None yet...

    /**
     * Init the configHandler
     * and Load all the files
     */
    public ConfigHandler() {
        instance = RidesPlugin.getInstance();

        //Load Files
        mainFile = new File(instance.getDataFolder(), "config.yml");
        File messagesFile = new File(instance.getDataFolder(), "messages.yml");
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
        YamlConfiguration messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        signsConfig = YamlConfiguration.loadConfiguration(signFile);

        instance.getLogger().info("Main Configs Loaded");

        //Load Global Settings into memory
        LinkVault = mainConfig.getBoolean("Integrations.vault");
        LinkPlaceholderAPI = mainConfig.getBoolean("Integrations.placeholderAPI");

        //Load Messages
        Messages.init(messagesConfig,this);

        assert rideFiles!=null;
        //Load Ride configurations
        for (File f: rideFiles) {
            YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);
            String Id = conf.getString("Generic.ID");
            instance.getLogger().info("\"" + Id + "\" ride config loaded.");
            assert Id != null;
            rides.put(Id, Objects.requireNonNull(Ride.RideFromConfig(conf)));
            instance.getLogger().info("\"" + Id + "\" ride config loaded.");
        }
        if (rideFiles.length==0) instance.getLogger().info("No Rides Configured yet.");

        //Load signs
        Set<String> keys = signsConfig.getKeys(false);
        if (!keys.isEmpty()) {
            for (String key : keys) {
                rideSigns.put(signsConfig.getLocation(key + ".location"), signsConfig.getString(key + ".ride"));
            }
        }
    }

    /**
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

        try {
            conf.save(new File(rideFolder,ID+".yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
   }


    public void saveMessageConfig(FileConfiguration conf) {
        try {
            conf.save(new File(instance.getDataFolder(), "messages.yml"));
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

       //re-init the ride
       rides.put(rideID, Objects.requireNonNull(Ride.RideFromConfig(conf)));
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
       Iterator<Location> iterator = rideSigns.keySet().iterator();

       while (iterator.hasNext()) {
           Location loc = iterator.next();
           String rname = rideSigns.get(loc);
           if (rname != null && rname.equals(name)) {
               rideSigns.remove(loc);
           }
       }
       saveSignConfig();

       //delete the ride config
       File rideFile = new File(rideFolder,name+".yml");
       if (!rideFile.delete()) instance.getLogger().warning("Couldn't delete ride config. Please remove manually.");
   }

}


