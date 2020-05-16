package net.clownercraft.ccRides;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

class ConfigHandler {

    //Getting an instance of the main class
    private RidesPlugin instance = RidesPlugin.getInstance();

    //creating the config
    private FileConfiguration carouselConfig = new YamlConfiguration();
    private FileConfiguration info = new YamlConfiguration();
    private FileConfiguration dropTowerConfig = new YamlConfiguration();
    private FileConfiguration ferrisWheelConfig = new YamlConfiguration();
    private File information = new File(instance.getDataFolder(), "information.yml");
    private File carousel = new File(instance.getDataFolder(), "carousel.yml");
    private File dropTower = new File(instance.getDataFolder(), "droptower.yml");
    private File ferrisWheel = new File(instance.getDataFolder(), "ferriswheel.yml");


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
            instance.getLogger().info(name + " has been loaded.");
        }

    }

    /**
     * This is to be used in the main class in the onEnable() method
     * This method creates the custom config files if they do not exist
     * This method loads the custom configuration files into a read/writeable state
     */
    public void createConfig() {
        
        testConfig(information, "information.yml");
        testConfig(dropTower, "droptower.yml");
        testConfig(carousel, "carousel.yml");
        testConfig(ferrisWheel, "ferriswheel.yml");

        try {

            //Loading the custom config file into the file configuration
            info.load(information);
            dropTowerConfig.load(dropTower);
            carouselConfig.load(carousel);
            ferrisWheelConfig.load(ferrisWheel);

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
            DROPTOWER,
            FERRISWHEEL,
            CAROUSEL
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

                    info.set(path, entry);
                    break;

                case CAROUSEL:

                    carouselConfig.set(path, entry);
                    break;

                case DROPTOWER:

                    dropTowerConfig.set(path, entry);
                    break;

                case FERRISWHEEL:

                    ferrisWheelConfig.set(path, entry);
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

                    return (String) info.get(path);

                case CAROUSEL:

                    return (String) carouselConfig.get(path);

                case DROPTOWER:

                    return (String) dropTowerConfig.get(path);

                case FERRISWHEEL:

                    return (String) ferrisWheelConfig.get(path);

                default:
                    return "No config files at the moment.";
            }
        }
}


