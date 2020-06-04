package net.clownercraft.ccRides.rides;

import net.clownercraft.ccRides.Config.ConfigHandler;
import net.clownercraft.ccRides.Config.Messages;
import net.clownercraft.ccRides.RidesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.*;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;

/**
 * Represents a generic single ride.
 */
public abstract class Ride implements Listener {
    public static HashMap<String,Class<? extends Ride>> RideTypes = new HashMap<>();

    private final Method[] methods = ((Supplier<Method[]>) () -> {
        try {
            Method getHandle = Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".entity.CraftEntity").getDeclaredMethod("getHandle");
            return new Method[] {
                    getHandle, getHandle.getReturnType().getDeclaredMethod("setPositionRotation", double.class, double.class, double.class, float.class, float.class)
            };
        } catch (Exception ex) {
            return null;
        }
    }).get();

    /*
    SETTINGS
     */
    public String TYPE; //Which type of ride this is
    public String ID; //A Unique ID/Name for the ride. Used in Commands
    Integer CAPACITY; //The number of available seats
    Location BASE_LOCATION; //Center location for the ride layout
    Location EXIT_LOCATION; //The Location to teleport players to when they exit or the ride is over.
    Integer MIN_START_PLAYERS = 1; //How many players need to join the ride before it starts
    Integer START_WAIT_TIME = 30; // How long to wait after the minimum passes for more players
    Integer PRICE = 0; //The cost in tokens
    boolean JOIN_AFTER_START = false; //Whether players can join once the ride has started.
    boolean ENABLED = true; //Whether the ride is enabled/disabled.

    /*
    Running Data
     */
    boolean RUNNING = false; //Whether the ride is operating or not
    boolean COUNTDOWN_STARTED = false;
    ArrayList<Vehicle> seats = new ArrayList<>(); //stores vehicle entities for the seats
    ArrayList<Player> riders = new ArrayList<>(); //The list of player on the ride
    ArrayList<Player> QUEUE = new ArrayList<>(); //The queue for players waiting to join the ride when it next runs
    BukkitTask countdownTask;


    public void init() {
        //Register Listener
        RidesPlugin.getInstance().getServer().getPluginManager().registerEvents(this,RidesPlugin.getInstance());

        //Spawn Seats if enabled
        if (ENABLED) respawnSeats();
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
     * Gets the location of a single seat of the ride
     * @param seatNumber The number of the seat to check
     * @return the current location the seat should be in
     */
    public abstract Location getPosition(int seatNumber);



    /**
     * Triggers the ride to set the position of all it's vehicles,
     */
    public void tickPositions() {
        for (int i=0;i<seats.size();i++) {
            Vehicle v = seats.get(i);
            Location loc = getPosition(i);
            //Teleport cart
            teleportWithPassenger(v,loc);

        }
    }

    public void teleportWithPassenger(Vehicle v, Location loc) {
        try {
            methods[1].invoke(methods[0].invoke(v), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());


//            for (Entity ent:v.getPassengers()) {
//                methods[1].invoke(methods[0].invoke(ent), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
//            }

        } catch (Exception ex) {
            ex.printStackTrace();
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
     * Forces the ride to remove all it's vehicles/seats and respawn them.
     */
    public void respawnSeats() {
        despawnSeats();

        for (int i=0;i<CAPACITY;i++){
            Location loc2 = getPosition(i);
            Vehicle cart = (Vehicle) loc2.getWorld().spawnEntity(loc2, EntityType.MINECART);
            //Make carts invulnerable, not affected by gravity and have no velocity

            cart.setInvulnerable(true);
            cart.setGravity(false);
            cart.setVelocity(new Vector(0, 0, 0));
            cart.setSilent(true);
            seats.add(cart);
        }
    }

    /**
     * Put a player onto the ride
     * Will add to the queue if the ride is full or already running
     * Will do nothing if the ride is disabled.
     * @param player the player to add
     */
    public void addPlayer(Player player) {
        //Check if the fude is enabled
        if (!ENABLED) return;

        //Check if Ride is full or already running
        //add to queue if the player can't join the ruide
        if (isFull()) {
            addToQueue(player);
            return;
        }
        if (isRunning() && !JOIN_AFTER_START) {
            addToQueue(player);
            return;
        }

        //TODO check balance.

        //Otherwise, add them to riders and put them in a seat.
        riders.add(player);
        RidesPlugin.getInstance().getConfigHandler().ridePlayers.put(player,ID);
        int i = riders.indexOf(player);

        seats.get(i).addPassenger(player);

        if (isFull()) {
            if (COUNTDOWN_STARTED) countdownTask.cancel();
            messageRiders(ChatColor.BLUE + "All Seats filled! Enjoy the ride!");
            startRide();
        }
        else if (riders.size()>=MIN_START_PLAYERS && !COUNTDOWN_STARTED) startCountdown();

    }

    /**
     * Starts the waiting time once the minimum players have joined the ride
     */
    public void startCountdown() {

        //schedule the ride to start after the wait time
        countdownTask = Bukkit.getScheduler().runTaskLater(RidesPlugin.getInstance(), new Runnable() {
            @Override
            public void run() {
                startRide();
            }
        },START_WAIT_TIME*20);
        COUNTDOWN_STARTED = true;

        //Send messages to riders
        messageRiders(ChatColor.BLUE + "The ride will start in " + START_WAIT_TIME + "Seconds.");
        if (START_WAIT_TIME>10) {
            Bukkit.getScheduler().runTaskLater(RidesPlugin.getInstance(), new Runnable() {
                @Override
                public void run() {
                    messageRiders(ChatColor.BLUE + "Ride Starting in 10 Seconds!");
                }
            },(START_WAIT_TIME-10)*20);
        }
        if (START_WAIT_TIME>5) {
            Bukkit.getScheduler().runTaskLater(RidesPlugin.getInstance(), new Runnable() {
                @Override
                public void run() {
                    messageRiders(ChatColor.BLUE + "Ride Starting in 5 Seconds!");
                }
            },(START_WAIT_TIME-5)*20);
        }
    }

    /**
     * Sends a message to all players currently riding
     * @param message the message to send
     */
    public void messageRiders(String message) {
        for (Player p:riders) {
            p.sendMessage(message);
        }
    }

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

            RidesPlugin.getInstance().getConfigHandler().ridePlayers.remove(player);
            seats.get(i).eject();

            //Teleport player to the exit location
            Bukkit.getScheduler().runTaskLater(RidesPlugin.getInstance(), new Runnable() {
                @Override
                public void run() {
                    player.teleport(EXIT_LOCATION);
                }
            },5l);
        }
    }

    /**
     * Called at the end of the ride to check if theres people in the queue
     */
    public void checkQueue() {
        if (QUEUE.size()>0) {
            for (Player p:(List<Player>) QUEUE.clone()) {
                removeFromQueue(p);
                addPlayer(p);
            }
        }
    }


    /**
     * @return a config with all this rides settings
     */
    public FileConfiguration createConfig() {
        FileConfiguration out = new YamlConfiguration();

        try{
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
        } catch (NullPointerException ignored) {}


        return out;
    }

    public static Ride RideFromConfig(YamlConfiguration conf) {
        String type = conf.getString("Generic.RideType");
        if (RideTypes.containsKey(type)) {
            try {
                Ride out = RideTypes.get(type).getConstructor(conf.getClass()).newInstance(conf);
                return out;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                RidesPlugin.getInstance().getLogger().severe("Couldn't load ride from Config! Unrecognised type: " + type);
                e.printStackTrace();
                return null;
            }
        } else return null;
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

        //Check if player already in the queue
        if (QUEUE.contains(player)) {
            String message = ChatColor.translateAlternateColorCodes('&', Messages.ride_queue_already);
            message = message.replaceAll("\\{place}",Integer.toString(QUEUE.indexOf(player)));
            message = message.replaceAll("\\{total}",Integer.toString(QUEUE.size()));
            player.sendMessage(message);
            return;
        }

        ConfigHandler conf = RidesPlugin.getInstance().getConfigHandler();
        //Check if player queuing for another ride
        if (conf.queueingPlayers.containsKey(player)) {
            String message = ChatColor.translateAlternateColorCodes('&', Messages.ride_queue_other_queue);
            message = message.replaceAll("\\{ride}",conf.ridePlayers.get(player));
            player.sendMessage(message);
            return;
        }

        //check if player is riding another ride
        if (conf.ridePlayers.containsKey(player)) {
            String message = ChatColor.translateAlternateColorCodes('&', Messages.ride_queue_other_riding);
            player.sendMessage(message);
            return;
        }

        //add to the queue
        QUEUE.add(player);
        RidesPlugin.getInstance().getConfigHandler().queueingPlayers.put(player,ID);

        String message = ChatColor.translateAlternateColorCodes('&',Messages.ride_queue_joined);
        message = message.replaceAll("\\{ride}",ID);
        message = message.replaceAll("\\{place}",Integer.toString(QUEUE.indexOf(player)));
        player.sendMessage(message);

    }

    public void removeFromQueue(Player player) {
        //remove from the queue
        QUEUE.remove(player);
        RidesPlugin.getInstance().getConfigHandler().queueingPlayers.remove(player);
        String message = ChatColor.translateAlternateColorCodes('&',Messages.ride_queue_left);
        message = message.replaceAll("\\{ride}",ID);
        player.sendMessage(message);
    }

    /**
     * Get the list of options you can set
     * @return A string List containing all options.
     */
    public List<String> getConfigOptions() {
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
        String[] values = value.split(" ");
        switch(key) {
            case "ENABLED":
                //Value should be true/false
                if (Boolean.parseBoolean(values[0])) {
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
                    double x,y,z;
                    try{
                        x = Double.parseDouble(values[0]);
                        y = Double.parseDouble(values[1]);
                        z = Double.parseDouble(values[2]);

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
                    int cap = Integer.parseInt(values[0]);
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
                    double x,y,z;
                    try{
                        x = Double.parseDouble(values[0]);
                        y = Double.parseDouble(values[1]);
                        z = Double.parseDouble(values[2]);

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
                    int startPl = Integer.parseInt(values[0]);
                    MIN_START_PLAYERS = startPl;
                    out = "Minimum START_PLAYERS set to " + startPl;
                } catch (NumberFormatException e) {
                    out = "Incorrect Value: START_PLAYERS must be an integer.";
                }

                break;
            case "START_DELAY":
                try{
                    int delay = Integer.parseInt(values[0]);
                    START_WAIT_TIME = delay;
                    out = "START_DELAY set to " + delay + " seconds";
                } catch (NumberFormatException e) {
                    out = "Incorrect Value: START_DELAY must be an integer (number of seconds).";
                }

                break;
            case "JOIN_AFTER_START":
                //Value should be true/false
                if (Boolean.parseBoolean(values[0])) {
                    JOIN_AFTER_START = true;
                    out = "JOIN_AFTER_START set to true. Players can now join the ride even if it has started.";
                } else {
                    JOIN_AFTER_START = false;
                    out = "JOIN_AFTER_START set to false. Players will join queue if the ride is running.";
                }
                break;
            case "PRICE":
                try{
                    int price = Integer.parseInt(values[0]);
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
        ENABLED = conf.getBoolean("Generic.Enabled");
        ID = conf.getString("Generic.ID");
        CAPACITY = conf.getInt("Generic.CAPACITY");
        BASE_LOCATION = conf.getLocation("Generic.BASE_LOCATION");
        EXIT_LOCATION = conf.getLocation("Generic.EXIT_LOCATION");
        MIN_START_PLAYERS = conf.getInt("Generic.Start.MIN_PLAYERS");
        START_WAIT_TIME = conf.getInt("Generic.Start.WAIT_TIME");
        JOIN_AFTER_START = conf.getBoolean("Generic.Start.ALLOW_JOIN_AFTER_START");
        PRICE = conf.getInt("Generic.Price");
    }

    /**
     * @return a map of the ride's settings with a human friendly name
     */
    public HashMap<String,String> getRideInfo() {
        HashMap<String,String> out = new HashMap<>();
        out.put("Ride Name",ID);
        out.put("Ride Type",TYPE);
        out.put("ENABLED",Boolean.toString(ENABLED));
        out.put("START_PLAYERS &o(Minimum riders)",Integer.toString(MIN_START_PLAYERS));
        out.put("START_DELAY &o(Seconds)",Integer.toString(START_WAIT_TIME));
        out.put("JOIN_AFTER_START",Boolean.toString(JOIN_AFTER_START));
        out.put("PRICE &o(Cost to ride)",Integer.toString(PRICE));
        out.put("RUNNING",Boolean.toString(RUNNING));

        //Nullable
        if (CAPACITY==null||CAPACITY==0) out.put("CAPACITY &o(Number of seats)","NOT SET"); else out.put("CAPACITY &o(Number of seats)",Integer.toString(CAPACITY));
        if (BASE_LOCATION==null) out.put("BASE_LOCATION &o(Centre of ride)","NOT SET"); else out.put("BASE_LOCATION &o(Centre of ride)",BASE_LOCATION.getWorld().getName() + " x"+BASE_LOCATION.getX() + " y"+BASE_LOCATION.getY() + " z" + BASE_LOCATION.getZ());
        if (EXIT_LOCATION==null) out.put("EXIT_LOCATION","NOT SET"); else out.put("EXIT_LOCATION",EXIT_LOCATION.getWorld().getName() + " x"+EXIT_LOCATION.getX() + " y"+EXIT_LOCATION.getY() + " z" + EXIT_LOCATION.getZ());
        return out;
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
            if (riders.contains(e.getExited())) e.setCancelled(true);
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

            if (    e.getEntered() instanceof Player
                    && riders.contains(e.getEntered())
                    && riders.indexOf(e.getEntered())==seats.indexOf(e.getVehicle())
            ) { } else e.setCancelled(true);
        }
    }

    @EventHandler
    public void onCartBreak(VehicleDamageEvent e) {
        if (seats.contains(e.getVehicle())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onCartBreak(VehicleDestroyEvent e) {
        if (seats.contains(e.getVehicle())) {
            e.setCancelled(true);
        }
    }


    @EventHandler
    public void onCartPush(VehicleEntityCollisionEvent e) {
        if (seats.contains(e.getVehicle())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onCartMove(VehicleMoveEvent e) {
        if (seats.contains(e.getVehicle())) {
            e.getVehicle().setVelocity(new Vector(0,0,0));
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
