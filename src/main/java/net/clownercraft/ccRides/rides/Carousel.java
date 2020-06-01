package net.clownercraft.ccRides.rides;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.util.Vector;

import java.util.List;

public class Carousel extends Ride {
    int radius;//the radius of the carousel seats
    double currentRotation = 0.0; //Current rotation of the carousel in radians
    int rotatespeed; //number of ticks per full rotation of the carousel
    int length; //number of full rotations per ride.
    double heightVariation; //The maximum change in height while riding (this is +/-)
    double heightSpeed; //How many full sine waves per rotation

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
        //TODO
    }

    /**
     * Create a new Carousel, with no settings
     * @param name the ID/Name for this ride
     */
    public Carousel(String name) {
        super.TYPE = "CAROUSEL";
    //TODO
    }


//    /**
//     * Create a carousel
//     * @param loc the location of the centre of rotaion
//     * @param cap the maximum capacity (number of players)
//     * @param radius radius in blocks
//     * @param rotspeed in ticks per revolution
//     * @param length Number of full rotations
//     */
//    public Carousel(Location loc, int cap, int radius, int rotspeed, int length, double heightAmt, double heightCycles) {
//        super.CAPACITY = cap;
//        super.BASE_LOCATION = loc;
//        this.radius = radius;
//        this.rotatespeed = rotspeed;
//        this.length = length;
//        this.heightVariation = heightAmt;
//        this.heightSpeed = heightCycles;
//
//        //Spawn a minecart for each seat of the ride.
//        for (int i = 0; i < cap; i++) {
//            Location loc2 = getPosition(i);
//            Vehicle cart = (Vehicle) loc2.getWorld().spawnEntity(loc2, EntityType.MINECART);
//            //Make carts invulnerable, not affected by gravity and have no velocity
//            cart.setInvulnerable(true);
//            cart.setGravity(false);
//            cart.setVelocity(new Vector(0, 0, 0));
//            super.seats.add(cart);
//        }
//    }


    public void startRide() {
        RUNNING = true;
        //TODO
    }

    public void stopRide() {
        currentRotation = 0.0;
        RUNNING = false;
        //TODO
    }

    public void tickPositions() {
        //TODO
    }

    public void respawnSeats() {
        //despawn minecarts & clear seats list
        super.despawnSeats();
        //Spawn a minecart for each seat of the ride.
        for (int i = 0; i < CAPACITY; i++) {
            Location loc2 = getPosition(i);
            Vehicle cart = (Vehicle) loc2.getWorld().spawnEntity(loc2, EntityType.MINECART);
            //Make carts invulnerable, not affected by gravity and have no velocity
            cart.setInvulnerable(true);
            cart.setGravity(false);
            cart.setVelocity(new Vector(0, 0, 0));
            seats.add(cart);
        }
    }

    /**
     * Put a player onto the ride
     *
     * @param player the player to add
     */
    @Override
    public void addPlayer(Player player) {

    }

    @Override
    public void ejectPlayer(Player player) {

    }

    /**
     * @return A FileConfiguration containing all of this Carousel's settings
     */
    @Override
    public FileConfiguration createConfig() {
        //Get generic options
        FileConfiguration out = super.createConfig();

        //Add Carousel Specific Options
        out.set("Carousel.Radius",radius);
        out.set("Carousel.Rotation.Ticks_Per_Full_Rotation",rotatespeed);
        out.set("Carousel.Rotation.Num_Cycles",length);
        out.set("Carousel.Height.Max_Change_±",heightVariation);
        out.set("Carousel.Height.Cycles_Per_Rotation",heightSpeed);

        return out;
    }

    /**
     * Get the list of options you can set
     *
     * @return A string list containing all options.
     */
    public static List<String> getConfigOptions() {
        //Get Default Options
        List<String> out = Ride.getConfigOptions();
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
            //The setting wasn't one of the defaults, so let's set ride specicis ones.
            switch (key) {
                case "RADIUS": //integer, in number of blocks
                    //TODO
                    break;
                case "ROTATE_SPEED": //integer, ticks per full rotation
                    //TODO
                    break;
                case "RIDE_LENGTH": //integer, number of full rotations per ride
                    //TODO
                    break;
                case "HEIGHT_VAR": //double, the max +/- height variation in blocks
                    //TODO
                    break;
                case "HEIGHT_SPEED": //double, the number of full height cycles per rotation
                    //TODO
                    break;
            }
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
        //TODO Check if all settings are set


        //TODO Actually Enable the ride
        return false;
    }

    /**
     * Disable the ride
     */
    @Override
    public void disable() {
        //TODO disable the ride.
    }

    /**
     * gets the position of a seat
     *
     * @param seatNum = the index of the seat to check
     * @return = the location the seat should currently be
     */
    public Location getPosition(int seatNum) {
        return null;
        //todo
    }

}
