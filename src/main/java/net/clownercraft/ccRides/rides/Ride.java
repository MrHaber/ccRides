package net.clownercraft.ccRides.rides;

import net.clownercraft.ccRides.RidesPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;

import java.util.*;

/**
 * Represents a generic single ride.
 */
public abstract class Ride {
    /*
    SETTINGS
     */
    RideType TYPE; //Which type of ride this is
    String ID; //A Unique ID/Name for the ride. Used in Commands
    int CAPACITY = 0; //The number of available seats
    Location BASE_LOCATION; //Center location for the ride layout
    Location EXIT_LOCATION; //The Location to teleport players to when they exit or the ride is over.
    int MIN_START_PLAYERS = 1; //How many players need to join the ride before it starts
    int START_WAIT_TIME = 30; // How long to wait after the minimum passes for more players
    boolean JOIN_AFTER_START = false; //Whether players can join once the ride has started.

    /*
    Running Data
     */
    boolean ENABLED = true; //Whether the ride is enabled/disabled.
    boolean RUNNING = false; //Whether the ride is operating or not
    List<Vehicle> seats = new ArrayList<>(); //stores vehicle entities for the seats
    List<Player> riders = new ArrayList<>(); //The list of player on the ride
    PriorityQueue<Player> waitingPlayers = new PriorityQueue<>(); //The queue for players waiting to join the ride when it next runs


    /**
     * Starts the movement of the ride.
     */
    public abstract void startRide();

    /**
     * Halts the ride operation and kicks players out
     */
    public abstract void stopRide();

    /**
     * Triggers the ride to set the position of all it's vehicles,
     */
    public abstract void tickPositions();

    /**
     * Gets the location of a single seat of the ride
     * @param seatNumber The number of the seat to check
     * @return the current location the seat should be in
     */
    public abstract Location getPosition(int seatNumber);

    /**
     * Forces the ride to remove all it's vehicles/seats and respawn them.
     * Should also re-seat all players in their vehicle after they are respawned
     * May be triggered before a worldsave to prevent extra vehicles saving to world.
     */
    public abstract void respawnSeats();

    /**
     * Put a player onto the ride
     * @param player the player to add
     */
    public abstract void addPlayer(Player player);

    /**
     * Eject a player from the ride and send them to the exitLocation
     * @param player the player to eject
     */
    public void ejectPlayer(Player player) {
        if (riders.contains(player)) {
            //Find the player's seat
            int i = riders.indexOf(player);

            //Eject the player and remove them from riders fields
            riders.remove(player);
            seats.get(i).eject();
            RidesPlugin.getInstance().getConfigHandler().riders.remove(player);

            //Teleport player to the exit location
            player.teleport(EXIT_LOCATION);
        }
    }

    /**
     * despawns all this rides minecarts, to prevent duplicates appearing after a shutdown
     */
    public void despawnSeats() {
        for (Vehicle v : seats) {
            v.remove();
        }
        seats.clear();
    }

    /**
     * @return a config with all this rides settings
     */
    public abstract FileConfiguration createConfig();

    /**
     * Enum of all Available Ride Types
     */
    public enum RideType {
        CAROUSEL,
        DROP_TOWER,
        FERRIS_WHEEL;

    }
    public static Ride RideFromConfig(FileConfiguration conf) {
        String type = conf.getString("RideType");
        RideType Rtype = RideType.valueOf(type);

        Ride out;

        switch (Rtype) {
            case CAROUSEL:
                out = new Carousel(conf);
                //TODO
                break;
            case DROP_TOWER:
                //TODO
                break;
            case FERRIS_WHEEL:
                //TODO
                break;
        }
        return null;
    }

    /**
     * @return whether all seats are taken on the ride
     */
    public boolean isFull() {
        return riders.size() >= CAPACITY;
    }

    /**
     * @return if the ride is already running
     */
    public boolean isRunning() {
        return RUNNING;
    }

    public void addToQueue() {

    }
}
