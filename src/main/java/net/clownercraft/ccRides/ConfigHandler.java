package net.clownercraft.ccRides;

import net.clownercraft.ccRides.rides.Ride;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConfigHandler {

    //Getting an instance of the main class
    private RidesPlugin instance;

    /*
    Config Objects
     */
    private FileConfiguration mainConfig = new YamlConfiguration();
    private FileConfiguration infoConfig = new YamlConfiguration();
    private FileConfiguration signsConfig = new YamlConfiguration();
    private List<FileConfiguration> rideConfigs = new ArrayList<>();

    private File mainFile = new File(instance.getDataFolder(), "config.yml");
    private File infoFile = new File(instance.getDataFolder(), "information.yml");
    private File signFile = new File(instance.getDataFolder(), "signs.yml");
    private List<File> rideFiles = new ArrayList<>();

    /*
    Ride Data
     */
    public HashMap<String,Ride> rides = new HashMap<>(); //Ride Name/ID, Ride Object
    public HashMap<Location,String> RideSigns = new HashMap<>(); //Sign Location, Ride ID
    public HashMap<Vehicle,String> rideVehicles = new HashMap<>(); //Vehicle Object, Ride ID
    public HashMap<Player,String> riders = new HashMap<>(); //All Players currently riding a ride
    /*
    Global Config Options
     */
    //None yet...

    /**
     * Init the configHandler
     * and Load all the files
     */
    public ConfigHandler() {
        instance = RidesPlugin.getInstance();

        //Load Base Config

        //Load Signs File

        //Load Rides Files
    }

    /**
     *
     * @param file
     * The file to test if it exists
     * @param name
     * The name the file should be saved as
     */
    private void testConfig(File file, String name) {

        if(!(file.exists())) {
            instance.saveResource(name, true);
            instance.getLogger().info(name + " default config saved.");
        }
        instance.getLogger().info(name + " config has been loaded.");


    }

    /**
     * This is to be used in the main class in the onEnable() method
     * This method creates the custom config files if they do not exist
     * This method loads the custom configuration files into a read/writeable state
     */
    public void createConfig() {

        testConfig(mainFile, "config.yml");
        testConfig(infoFile, "information.yml");
        testConfig(signFile, "signs.yml");

        //TODO add ride files

        try {

            //Loading the custom config file into the file configuration
            infoConfig.load(infoFile);
            signsConfig.load(signFile);
            mainConfig.load(mainFile);

            //TODO add ride files

        } catch (InvalidConfigurationException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This enum is to be populated with the different config files
     * This enum is used in the setConfigPath() and getConfigPath() methods
     */
    public enum ConfigType
        {
            INFORMATION,
            MAIN,
            SIGNS,
            RIDE
        }


    /**
     *
     * @param type
     * The type of config file that the method will use
     * @param path
     * The String path that the method will select within the config file
     * @param entry
     * The String that will be saved and overwrite the current configuration
     */
   public void setConfigPath(ConfigType type, String path, String entry) {
            switch (type) {
                case INFORMATION:

                    infoConfig.set(path, entry);
                    break;

                case SIGNS:

                    signsConfig.set(path, entry);
                    break;

            }
        }

    /**
     *
     * @param type
     * The type of config file that the method will use
     * @param path
     * The String that will be saved and overwrite the current configuration
     * @return
     * Returns the String that is saved within the config file to be given to the player
     */
        public String getConfigPath(ConfigType type, String path) {
            switch (type) {
                case INFORMATION:

                    return (String) infoConfig.get(path);

                case SIGNS:

                    return (String) signsConfig.get(path);


                default:
                    return "No config files at the moment.";
            }
        }
}


