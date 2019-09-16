package net.clownercraft.ccRides.rides;

import net.clownercraft.ccRides.Ride;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Vehicle;
import org.bukkit.util.Vector;

public class Carousel extends Ride {
    int radius;//the radius of the carousel seats
    double currentRotation = 0.0; //Current rotation of the carousel in radians
    int speed; //number of ticks per full rotation of the carousel
    int length; //number of full rotations per ride.

    public Carousel(Location loc, int cap, int radius, int speed, int length) {
        super.capacity = cap;
        super.base = loc;
        this.radius = radius;
        this.speed = speed;
        this.length = length;

        //Spawn a minecart for each seat of the ride.
        for (int i=0;i<cap;i++) {
            Location loc2 = getPosition(i);
            Vehicle cart = (Vehicle) loc2.getWorld().spawnEntity(loc2, EntityType.MINECART);
            //Make carts invulnerable, not affected by gravity and have no velocity
            cart.setInvulnerable(true);
            cart.setGravity(false);
            cart.setVelocity(new Vector(0,0,0));
            super.seats.add(cart);
        }
    }

    public void startRide() {
        rideRunning = true;
        //TODO
    }

    public void stopRide() {
        currentRotation = 0.0;
        rideRunning = false;
        //TODO
    }

    public void tickPositions() {
        //TODO
    }

    public void respawnSeats() {
        //despawn minecarts & clear seats list
        super.despawnSeats();
        //Spawn a minecart for each seat of the ride.
        for (int i=0;i<capacity;i++) {
            Location loc2 = getPosition(i);
            Vehicle cart = (Vehicle) loc2.getWorld().spawnEntity(loc2, EntityType.MINECART);
            //Make carts invulnerable, not affected by gravity and have no velocity
            cart.setInvulnerable(true);
            cart.setGravity(false);
            cart.setVelocity(new Vector(0,0,0));
            seats.add(cart);
        }
    }

    /**
     * gets the position of a seat
     * @param seatNum = the index of the seat to check
     * @return = the location the seat should currently be
     */
    public Location getPosition(int seatNum) {
        return null;
    }

}
