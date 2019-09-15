package net.clownercraft.ccRides;

/**
 * Represents a generic single ride.
 */
public abstract class Ride {


    /**
     * Starts the movement of the ride.
     */
    public abstract void startRide();

    /**
     * Halts the ride operation and kicks players out
     */
    public abstract void stopRide();


}
