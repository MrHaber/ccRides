package net.clownercraft.ccRides.rides;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class DropTower extends Ride {

    public void startRide() {
        //TODO
    }

    public void stopRide() {
        //TODO
    }

    @Override
    public void tickPositions() {

    }

    @Override
    public Location getPosition(int seatNum) {
        return null;
    }

    @Override
    public void respawnSeats() {

    }

    /**
     * Put a player onto the ride
     *
     * @param player the player to add
     */
    @Override
    public void addPlayer(Player player) {

    }

    /**
     * @return a config with all this rides settings
     */
    @Override
    public FileConfiguration createConfig() {
        return null;
    }

    /**
     * Get the list of options you can set
     *
     * @return A string containing a list of options, comma seperated.
     */
    @Override
    public List<String> getConfigOptions() {
        return super.getConfigOptions();
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
        return false;
    }

    /**
     * Disable the ride
     */
    @Override
    public void disable() {

    }
}
