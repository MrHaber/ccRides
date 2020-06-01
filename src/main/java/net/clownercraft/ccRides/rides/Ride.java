package net.clownercraft.ccRides.rides;

import net.clownercraft.ccRides.RidesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Represents a generic single ride.
 */
public abstract class Ride implements Listener {
    public static HashMap<String,Class<? extends Ride>> RideTypes = new HashMap<>();

    /*
    SETTINGS
     */
    String TYPE; //Which type of ride this is
    String ID; //A Unique ID/Name for the ride. Used in Commands
    int CAPACITY = 0; //The number of available seats
    Location BASE_LOCATION; //Center location for the ride layout
    Location EXIT_LOCATION; //The Location to teleport players to when they exit or the ride is over.
    int MIN_START_PLAYERS = 1; //How many players need to join the ride before it starts
    int START_WAIT_TIME = 30; // How long to wait after the minimum passes for more players
    int PRICE = 0; //The cost in tokens
    boolean JOIN_AFTER_START = false; //Whether players can join once the ride has started.
    boolean ENABLED = true; //Whether the ride is enabled/disabled.


    /*
    Running Data
     */
    boolean RUNNING = false; //Whether the ride is operating or not
    List<Vehicle> seats = new ArrayList<>(); //stores vehicle entities for the seats
    List<Player> riders = new ArrayList<>(); //The list of player on the ride
    PriorityQueue<Player> QUEUE = new PriorityQueue<>(); //The queue for players waiting to join the ride when it next runs

    public void init() {
        //Register Listener
        RidesPlugin.getInstance().getServer().getPluginManager().registerEvents(this,RidesPlugin.getInstance());

        //Spawn Seats
        respawnSeats();
    }

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
    //This may not need to be abstract...
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
    public FileConfiguration createConfig() {
        FileConfiguration out = new YamlConfiguration();

        out.set("Generic.RideType",TYPE);
        out.set("Generic.Enabled",ENABLED);
        out.set("Generic.ID",ID);
        out.set("Generic.CAPACITY",CAPACITY);
        out.set("Generic.BASE_LOCATION",BASE_LOCATION);
        out.set("Generic.EXIT_LOCATION",EXIT_LOCATION);
        out.set("Generic.Start.MIN_PLAYERS",MIN_START_PLAYERS);
        out.set("Generic.Start.WAIT_TIME",START_WAIT_TIME);
        out.set("Generic.Start.ALLOW_JOIN_AFTER_START",JOIN_AFTER_START);
        out.set("Generic.Price",PRICE);

        return out;
    }

    public static Ride RideFromConfig(FileConfiguration conf) {
        String type = conf.getString("Generic.RideType");
        if (RideTypes.containsKey(type)) {
            try {
                Ride out = RideTypes.get(type).getConstructor(conf.getClass()).newInstance(conf);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                RidesPlugin.getInstance().getLogger().severe("Couldn't load ride from Config! Unrecognised type: " + type);
                e.printStackTrace();
                return null;
            }
        } else return null;
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

    public void addToQueue(Player player) {
        //TODO
    }

    public void removeFromQueue(Player player) {
        //TODO
    }

    /**
     * Get the list of options you can set
     * @return A string List containing all options.
     */
    public static List<String> getConfigOptions() {
        List<String> out = new ArrayList<>();
        out.add("ENABLED");
        out.add("BASE_LOCATION");
        out.add("CAPACITY");
        out.add("EXIT_LOCATION");
        out.add("START_PLAYERS");
        out.add("START_DELAY");
        out.add("JOIN_AFTER_START");
        out.add("PRICE");
        return out;
    }

    /**
     *
     * @param key the key of the setting
     *            options are:
     *            ENABLED, BASE_LOCATION, CAPACITY, EXIT_LOCATION, START_PLAYERS,
     *            START_DELAY, JOIN_AFTER_START, PRICE
     * @param value the new value of the setting
     * @param sender the player that executed the setting change
     * @return String containing the message to tell player
     */
    public String setConfigOption(String key, String value, Player sender) {
        String out = "";
        switch(key) {
            case "ENABLED":
                //Value should be true/false
                if (Boolean.parseBoolean(value)) {
                    if (enable()) {out = "Ride Enabled.";} else {
                        out = "Could Not Enable Ride. Check All Options are set.";
                    }
                }
                else {
                    disable();
                    out = "Ride Disabled.";
                }
                break;
            case "BASE_LOCATION":
                //value should be blank or "x y z"
                Location location = sender.getLocation().clone();
                if (value.equalsIgnoreCase("")) {
                    BASE_LOCATION = location;
                    out = "BASE_LOCATION set to your current position";
                } else {
                    String[] strings = value.split(" ");
                    double x,y,z;
                    try{
                        x = Double.parseDouble(strings[0]);
                        y = Double.parseDouble(strings[1]);
                        z = Double.parseDouble(strings[2]);

                        location.setX(x);
                        location.setY(y);
                        location.setZ(z);
                        BASE_LOCATION = location;
                        out = "BASE_LOCATION set to x: " + x + " y: " + y + " z: " + z;
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        out = "Incorrect Value: BASE_LOCATION Must be three doubles, or leave blank to use your current location.";
                    }
                }
                break;
            case "CAPACITY":
                try{
                    int cap = Integer.parseInt(value);
                    CAPACITY = cap;
                    out = "CAPACITY set to " + cap + " seats";
                } catch (NumberFormatException e) {
                    out = "Incorrect Value: CAPACITY must be an integer.";
                }

                break;
            case "EXIT_LOCATION":
                //value should be blank or "x y z"
                Location loc2 = sender.getLocation().clone();
                if (value.equalsIgnoreCase("")) {
                    EXIT_LOCATION = loc2;
                    out = "EXIT_LOCATION set to your current position";
                } else {
                    String[] strings = value.split(" ");
                    double x,y,z;
                    try{
                        x = Double.parseDouble(strings[0]);
                        y = Double.parseDouble(strings[1]);
                        z = Double.parseDouble(strings[2]);

                        loc2.setX(x);
                        loc2.setY(y);
                        loc2.setZ(z);
                        EXIT_LOCATION = loc2;
                        out = "EXIT_LOCATION set to x: " + x + " y: " + y + " z: " + z;
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        out = "Incorrect Value: EXIT_LOCATION Must be three doubles, or leave blank to use your current location.";
                    }
                }
                break;
            case "START_PLAYERS":
                try{
                    int startPl = Integer.parseInt(value);
                    MIN_START_PLAYERS = startPl;
                    out = "Minimum START_PLAYERS set to " + startPl;
                } catch (NumberFormatException e) {
                    out = "Incorrect Value: START_PLAYERS must be an integer.";
                }

                break;
            case "START_DELAY":
                try{
                    int delay = Integer.parseInt(value);
                    START_WAIT_TIME = delay;
                    out = "START_DELAY set to " + delay + " seconds";
                } catch (NumberFormatException e) {
                    out = "Incorrect Value: START_DELAY must be an integer (number of seconds).";
                }

                break;
            case "JOIN_AFTER_START":
                //Value should be true/false
                if (Boolean.parseBoolean(value)) {
                    JOIN_AFTER_START = true;
                    out = "JOIN_AFTER_START set to true. Players can now join the ride even if it has started.";
                } else {
                    JOIN_AFTER_START = false;
                    out = "JOIN_AFTER_START set to false. Players will join queue if the ride is running.";
                }
                break;
            case "PRICE":
                try{
                    int price = Integer.parseInt(value);
                    PRICE = price;
                    out = "PRICE set to " + price + " tokens";
                } catch (NumberFormatException e) {
                    out = "Incorrect Value: Price must be an integer.";
                }

                break;
        }

        return out;
    }

    /**
     * Enable the ride
     * @return false if failed (ie not all settings configured yet)
     */
    public abstract boolean enable();

    /**
     * Disable the ride
     */
    public abstract void disable();

    /**
     * Register a ride type
     * @param type String identifier for the ride type
     * @param rideClass The Class for that ride
     */
    public static void registerType(String type, Class<? extends Ride> rideClass) {
        RideTypes.put(type,rideClass);
    }

    public void setRideOptions(FileConfiguration conf) {
        //TODO set the ride options from config file
    }

    /*
    EVENT LISTENERS
     */

    /**
     * Prevent players exiting vehicles that are part of a ride
     * @param e - the vehicle Exit event
     */
    @EventHandler
    public void onCartExit(VehicleExitEvent e) {
        if (seats.contains(e.getVehicle())) {
            e.setCancelled(true);
            //e.getExited().sendMessage(ChatColor.DARK_PURPLE + "Carnival: "+ ChatColor.BLUE + "Use "+ChatColor.GRAY+ "/ride exit" + ChatColor.BLUE + "to leave the ride early.");
        }
    }

    /**
     * Prevent players entering vehicles that are part of a ride
     * @param e - the vehicle Enter event
     */
    @EventHandler
    public void onCartEnter(VehicleEnterEvent e) {
        if (seats.contains(e.getVehicle())) {
            e.setCancelled(true);
        }
    }

    /**
     * Remove players from rides/queues when they disconnect
     * @param e - the player disconnect event
     */
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e){
        //If on this ride
        if (riders.contains(e.getPlayer())) {
            ejectPlayer(e.getPlayer());
        }

        //If in the ride queue
        if (QUEUE.contains(e.getPlayer())) {
            removeFromQueue(e.getPlayer());
        }
    }



}
