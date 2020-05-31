package net.clownercraft.ccRides.rides;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.util.Vector;

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
        super.TYPE = "CAROUSEL";
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
        //TODO
        return null;
    }

    /**
     * Get the list of options you can set
     *
     * @return A string containing a list of options, comma seperated.
     */
    @Override
    public String getConfigOptions() {
        return null;
    }

    /**
     * @param key    the key of the setting
     * @param value  the new value of the setting
     * @param sender the player that executed the setting change
     * @return String containing the message to tell player
     */
    @Override
    public String setConfigOption(String key, String value, Player sender) {
        return null;
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
