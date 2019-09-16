package net.clownercraft.ccRides;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a generic single ride.
 */
public abstract class Ride {
    public int capacity = 0; //The number of available seats
    public List<Vehicle> seats = new ArrayList<Vehicle>(); //stores vehicle entities for the seats

    public Location base; //Centre location for the ride layout
    public boolean rideRunning = false;

    public List<Player> riders = new ArrayList<Player>();

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
     * @param seatNum
     * @return the current location the seat should be in
     */
    public abstract Location getPosition(int seatNum);

    /**
     * Forces the ride to remove all it's vehicles/seats and respawn them.
     * Should also re-seat all players in their vehicle after they are respawned
     * May be triggered before a worldsave to prevent extra vehicles saving to world.
     */
    public abstract void respawnSeats();

    /**
     * despawns all this rides minecarts, to prevent duplicates appearing after a shutdown
     */
    public void despawnSeats() {
        for (Vehicle v : seats) {
            v.remove();
        }
        seats.clear();
    }

}
