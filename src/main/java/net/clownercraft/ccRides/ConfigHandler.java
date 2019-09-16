package net.clownercraft.ccRides;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

class ConfigHandler {

    //Getting an instance of the main class
    private RidesPlugin instance = RidesPlugin.getInstance();

    private File information = new File(instance.getDataFolder(), "information.yml");

    private FileConfiguration info = new YamlConfiguration();

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
        }

    }

    /**
     * This is to be used in the main class in the onEnable() method
     * This method creates the custom config files if they do not exist
     * This method loads the custom configuration files into a read/writeable state
     */
    public void createConfig() {

        //creating this file again, as it is strictly for informative purposes
        instance.saveResource("information.yml", true);

        try{
            //Loading the custom config file into the file configuration
            info.load(information);

        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
