package net.clownercraft.ccRides.rides;

import net.clownercraft.ccRides.RidesPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class Carousel extends Ride {
    Integer radius;//the radius of the carousel seats
    Double currentRotation = 0.0; //Current rotation of the carousel in radians
    Integer rotatespeed; //number of ticks per full rotation of the carousel
    Integer length; //number of full rotations per ride.
    Double heightVariation; //The maximum change in height while riding (this is +/-)
    Double heightSpeed; //How many full sine waves per rotation


    /**
     * Create a Carousel based on an existing config
     * @param conf the config to read
     */
    public Carousel(FileConfiguration conf) {
        //Set the type to CAROUSEL
        super.TYPE = "CAROUSEL";

        //load generic options
        super.setRideOptions(conf);

        //load carousel specific options
        radius = conf.getInt("Carousel.Radius");
        rotatespeed = conf.getInt("Carousel.Rotation.Ticks_Per_Full_Rotation");
        length = conf.getInt("Carousel.Rotation.Num_Cycles");
        heightVariation = conf.getDouble("Carousel.Height.Max_Change_±");
        heightSpeed = conf.getDouble("Carousel.Height.Cycles_Per_Rotation");
    }

    /**
     * Create a new Carousel, with no settings
     * @param name the ID/Name for this ride
     */
    public Carousel(String name) {
        //Setyp basic settings
        super.TYPE = "CAROUSEL";
        super.ID = name;

        //Save a default config
        RidesPlugin.getInstance().getConfigHandler().saveRideConfig(createConfig());
    }

    public void startRide() {
        RUNNING = true;


        //TODO
    }

    public void stopRide() {
        //TODO Cancel movement & reset Positions

        currentRotation = 0.0;

        //Eject Players
        for (Player p:riders) {
            ejectPlayer(p);
        }

        RUNNING = false;
    }

    /**
     * gets the position of a seat at the current point in the ride cycle
     *
     * @param seatNum = the index of the seat to check
     * @return = the location the seat should currently be
     */
    public Location getPosition(int seatNum) {
        return null;
        //todo
    }

    /**
     * @return A FileConfiguration containing all of this Carousel's settings
     */
    @Override
    public FileConfiguration createConfig() {
        //Get generic options
        FileConfiguration out = super.createConfig();

        //Add Carousel Specific Options
        try{
        out.set("Carousel.Radius",radius);
        out.set("Carousel.Rotation.Ticks_Per_Full_Rotation",rotatespeed);
        out.set("Carousel.Rotation.Num_Cycles",length);
        out.set("Carousel.Height.Max_Change_±",heightVariation);
        out.set("Carousel.Height.Cycles_Per_Rotation",heightSpeed);
        } catch (NullPointerException ignored) {}

        return out;
    }

    /**
     * Get the list of options you can set
     *
     * @return A string list containing all options.
     */
    @Override
    public List<String> getConfigOptions() {
        //Get Default Options
        List<String> out = super.getConfigOptions();
        //Add Carousel Specific Options
        out.add("RADIUS");
        out.add("ROTATE_SPEED");
        out.add("RIDE_LENGTH");
        out.add("HEIGHT_VAR");
        out.add("HEIGHT_SPEED");
        return out;
    }

    /**
     * @param key    the key of the setting
     *               Carousel Options are:
     *                  RADIUS, ROTATE_SPEED, RIDE_LENGTH, HEIGHT_VAR, HEIGHT_SPEED
     *               Plus default Ride options:
     *                  BASE_LOCATION, CAPACITY, EXIT_LOCATION, START_PLAYERS,
     *                  START_DELAY, JOIN_AFTER_START, ENABLED, PRICE
     * @param value  the new value of the setting
     * @param sender the player that executed the setting change
     * @return String containing the message to tell player
     */
    @Override
    public String setConfigOption(String key, String value, Player sender) {
        String out = super.setConfigOption(key,value,sender);
        if (out.equals("")) {

            //The setting wasn't one of the defaults, so let's set carousel specific ones.
            switch (key) {

                case "RADIUS": //integer, in number of blocks
                    try{
                        radius = Integer.parseInt(value);
                    out = "Radius set to " + radius + " blocks.";
                    } catch (NumberFormatException e) {
                        out = "Radius must be an integer number of blocks.";
                    }
                    break;
                case "ROTATE_SPEED": //integer, ticks per full rotation
                    try{
                        rotatespeed = Integer.parseInt(value);
                        out = "Rotate_Speed set to " + rotatespeed + " ticks per rotation.";
                    } catch (NumberFormatException e) {
                        out = "Rotate_Speed must be an integer number of ticks per rotation.";
                    }
                    break;
                case "RIDE_LENGTH": //integer, number of full rotations per ride
                    try{
                        length = Integer.parseInt(value);
                        out = "RIDE_LENGTH set to " + length + " rotations.";
                    } catch (NumberFormatException e) {
                        out = "RIDE_LENGTH must be an integer number of rotations.";
                    }
                    break;
                case "HEIGHT_VAR": //double, the max +/- height variation in blocks
                    try{
                        heightVariation = Double.parseDouble(value);
                        out = "HEIGHT_VAR set to ±" + heightVariation + " blocks.";
                    } catch (NumberFormatException e) {
                        out = "HEIGHT_VAR must be an double number of blocks.";
                    }
                    break;
                case "HEIGHT_SPEED": //double, the number of full height cycles per rotation
                    try{
                        heightSpeed = Double.parseDouble(value);
                        out = "HEIGHT_SPEED set to " + heightSpeed + " cycles per rotation.";
                    } catch (NumberFormatException e) {
                        out = "HEIGHT_VAR must be an double number of cycles per rotation.";
                    }
                    break;
            }
            RidesPlugin.getInstance().getConfigHandler().saveRideConfig(createConfig());
        }

        //If out is still empty, we didn't recognise the option key
        if (out.equals("")) out = key + " not found as an option";

        //return the message
        return out;
    }

    /**
     * Enable the ride
     *
     * @return false if failed (ie not all settings configured yet)
     */
    @Override
    public boolean enable() {
        //Check if all settings are set
        if (
                BASE_LOCATION == null
                        || EXIT_LOCATION == null
                        || CAPACITY == null
                        || ID == null
                        || TYPE == null
                        || radius == null
                        || rotatespeed == null
                        || length == null
                        || heightVariation == null
                        || heightSpeed == null
        ) return false;

        //Enable the ride and spawn in seats
        respawnSeats();
        ENABLED = true;

        return true;
    }

    /**
     * Disable the ride
     */
    @Override
    public void disable() {
        stopRide();
        despawnSeats();
        ENABLED = false;
    }


}
