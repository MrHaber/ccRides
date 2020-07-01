package net.clownercraft.ccRides.rides;

import net.clownercraft.ccRides.config.ConfigHandler;
import net.clownercraft.ccRides.config.Messages;
import net.clownercraft.ccRides.RidesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.*;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Represents a generic single ride.
 */
public abstract class Ride implements Listener {
    public static HashMap<String,Class<? extends Ride>> RideTypes = new HashMap<>();
    /**
     * Gets the method to set a vehicle's position by NMS.
     * Necessary to allow vehicles to move with their passengers.
     */
    private final Method[] setPositionRotationMethod = ((Supplier<Method[]>) () -> {
        try {
            Method getHandle = Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".entity.CraftEntity").getDeclaredMethod("getHandle");
            return new Method[] {
                    getHandle, getHandle.getReturnType().getDeclaredMethod("setPositionRotation", double.class, double.class, double.class, float.class, float.class)
            };
        } catch (Exception ex) {
            return null;
        }
    }).get();

    /* SETTINGS */
    public String type; //Which type of ride this is
    public String rideID; //A Unique ID/Name for the ride. Used in Commands
    Integer capacity = 10; //The number of available seats
    Location baseLocation; //Center location for the ride layout
    Location exitLocation; //The Location to teleport players to when they exit or the ride is over.
    Integer minStartPlayers = 1; //How many players need to join the ride before it starts
    Integer startWaitTime = 15; // How long to wait after the minimum passes for more players
    Integer price = 0; //The cost in tokens
    boolean joinAfterStart = false; //Whether players can join once the ride has started.

    boolean enabled = false; //Whether the ride is enabled/disabled.

    /* Running Data */
    boolean running = false; //Whether the ride is operating or not
    boolean countdownStarted = false;
    ArrayList<UUID> seats = new ArrayList<>(); //stores vehicle entities for the seats
    ConcurrentHashMap<UUID,Integer> riders = new ConcurrentHashMap<>(); //Players mapped to their seat numbers
    ArrayList<Player> queue = new ArrayList<>(); //The queue for players waiting to join the ride when it next runs
    BukkitTask countdownTask;

    /**
     * Initialises the ride, register listeners and spawn seats if enabled.
     */
    public void init() {
        //Register Listener
        RidesPlugin.getInstance().getServer().getPluginManager().registerEvents(this,RidesPlugin.getInstance());

        //Spawn Seats if enabled
        if (enabled) respawnSeats();
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
            Vehicle v = (Vehicle) Bukkit.getEntity(seats.get(i));
            Location loc = getPosition(i);
            //Teleport cart
            teleportWithPassenger(v,loc);

        }
    }

    /**
     * Teleport an entity using CraftEntity method, to move with passenger.
     * @param v vehicle/entity to teleport
     * @param loc location to teleport to
     */
    public void teleportWithPassenger(Entity v, Location loc) {
        try { setPositionRotationMethod[1].invoke(setPositionRotationMethod[0].invoke(v), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    /**
     * despawns all this rides minecarts, to prevent duplicates appearing after a shutdown
     */
    public void despawnSeats() {
        for (UUID uuid : seats) {
            Vehicle v = (Vehicle) Bukkit.getEntity(uuid);
            v.remove();
        }
        seats.clear();
    }

    /**
     * Makes the ride spawn in it's seats. Removes existing first to prevent duplicate entities.
     */
    public void respawnSeats() {
        despawnSeats();

        for (int i = 0; i< capacity; i++){
            Location loc2 = getPosition(i);

            //remove any minecarts within a few blocks of each location in case there's duplicates in the world.
            for (Entity e: Objects.requireNonNull(loc2.getWorld()).getNearbyEntities(loc2,3,3,3)) {
                if (e.getType().equals(EntityType.MINECART)
                && !seats.contains(e.getUniqueId())) e.remove();
            }
            //Spawn new minecarts
            Minecart cart = (Minecart) loc2.getWorld().spawnEntity(loc2, EntityType.MINECART);
            //Make carts invulnerable, not affected by gravity and have no velocity

            cart.setInvulnerable(true);
            cart.setGravity(false);
            cart.setVelocity(new Vector(0, 0, 0));
            cart.setSilent(true);
            cart.setMaxSpeed(0);
            seats.add(cart.getUniqueId());
        }
    }

    /**
     * @return the first empty seat on the ride. 0 if the ride is full or completely empty.
     */
    public int firstAvailableSeat() {
        Collection<Integer> taken = riders.values();
        for (int i = 0; i< capacity; i++) {
            if (!taken.contains(i)) return i;
        }
        return 0; //If we don't find a seat, return 0.
    }
    /**
     * Put a player onto the ride
     * Will add to the queue if the ride is full or already running
     * Will do nothing if the ride is disabled.
     * @param player the player to add
     */
    public void addPlayer(Player player) {
        //Check if the fude is enabled
        if (!enabled) return;

        //Check if player can afford to ride
        if (!RidesPlugin.getInstance().canAfford(player, price)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',Messages.ride_cant_afford.replaceAll("\\{price}",Integer.toString(price))));
            return;
        }

        //Check if Ride is full or already running
        //add to queue if the player can't join the ruide
        if (isFull()) {
            addToQueue(player);
            return;
        }
        if (isRunning() && !joinAfterStart) {
            addToQueue(player);
            return;
        }

        int seatnum = firstAvailableSeat();

        //Otherwise, add them to riders and put them in a seat.
        riders.put(player.getUniqueId(),seatnum);
        RidesPlugin.getInstance().getConfigHandler().ridePlayers.put(player.getUniqueId(), rideID);

        ((Vehicle) Bukkit.getEntity(seats.get(seatnum))).addPassenger(player);

        //charge them the price
        if (price !=0) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    Messages.prefix + Messages.ride_paid
                        .replaceAll("\\{price}",Integer.toString(price))
                        .replaceAll("\\{ride}", rideID)));
            RidesPlugin.getInstance().takePayment(player, price);
        }


        if (isFull()) {
            if (countdownStarted) countdownTask.cancel();
            messageRiders(ChatColor.translateAlternateColorCodes('&',Messages.prefix + Messages.ride_starting_seatsFUll));
            startRide();
        }
        else if (riders.size()>= minStartPlayers && !countdownStarted) startCountdown();
        else messageRiders(Messages.prefix +
                    Messages.ride_starting_needMoreRiders
                            .replaceAll("\\{count}",
                                    Integer.toString(minStartPlayers -riders.size())));

    }

    /**
     * Starts the waiting time once the minimum players have joined the ride
     */
    public void startCountdown() {

        //schedule the ride to start after the wait time
        countdownTask = Bukkit.getScheduler().runTaskLater(RidesPlugin.getInstance(),
                () -> startRide(), startWaitTime *20);

        countdownStarted = true;

        //Send messages to riders
        messageRiders(Messages.prefix +
                Messages.ride_starting_countdown
                        .replaceAll("\\{time}",Integer.toString(startWaitTime)));

        if (startWaitTime >10) {
            Bukkit.getScheduler().runTaskLater(RidesPlugin.getInstance(),
                    () -> messageRiders(Messages.prefix +
                            Messages.ride_starting_countdown
                                    .replaceAll("\\{time}",Integer.toString(10))),
                    (startWaitTime -10)*20);
        }
        if (startWaitTime >5) {
            Bukkit.getScheduler().runTaskLater(RidesPlugin.getInstance(),
                    () -> messageRiders(Messages.prefix +
                            Messages.ride_starting_countdown
                                    .replaceAll("\\{time}",Integer.toString(5))),
                    (startWaitTime -5)*20);
        }
    }

    /**
     * Sends a message to all players currently riding
     * @param message the message to send
     */
    public void messageRiders(String message) {
        message = ChatColor.translateAlternateColorCodes('&',message);
        for (UUID uid:riders.keySet()) {
            Objects.requireNonNull(Bukkit.getPlayer(uid)).sendMessage(message);
        }
    }

    /**
     * Eject a player from the ride and send them to the exitLocation
     * @param player the player to eject
     */
    public void ejectPlayer(Player player) {
        if (riders.containsKey(player.getUniqueId())) {
            //Find the player's seat
            int i = riders.get(player.getUniqueId());

            //Eject the player and remove them from riders fields
            riders.remove(player.getUniqueId());

            RidesPlugin.getInstance().getConfigHandler().ridePlayers.remove(player.getUniqueId());

            ((Vehicle) Bukkit.getEntity(seats.get(i))).eject();
            player.teleport(exitLocation);
        }
    }

    /**
     * Called at the end of the ride to check if theres people in the queue
     */
    public void checkQueue() {
        if (queue.size()>0) {
            for (Player p:(List<Player>) queue.clone()) {
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
            out.set("Generic.RideType", type);
            out.set("Generic.Enabled", enabled);
            out.set("Generic.ID", rideID);
            out.set("Generic.CAPACITY", capacity);
            out.set("Generic.BASE_LOCATION", baseLocation);
            out.set("Generic.EXIT_LOCATION", exitLocation);
            out.set("Generic.Start.MIN_PLAYERS", minStartPlayers);
            out.set("Generic.Start.WAIT_TIME", startWaitTime);
            out.set("Generic.Start.ALLOW_JOIN_AFTER_START", joinAfterStart);
            out.set("Generic.Price", price);
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
        return riders.size() >= capacity;
    }

    /* Getters for a bunch of status things */
    public boolean isRunning() { return running; }
    public boolean isCountdownStarted() { return countdownStarted; }
    public Integer getCapacity() { return capacity; }
    public Integer getNumRiders() { return riders.size(); }
    public Integer getNumQueue() { return queue.size(); }
    public Integer getPrice() { return price; }
    public boolean isEnabled() { return enabled; }

    public void addToQueue(Player player) {

        //Check if player already in the queue
        if (queue.contains(player)) {
            String message = ChatColor.translateAlternateColorCodes('&', Messages.ride_queue_already);
            message = message.replaceAll("\\{place}",Integer.toString(queue.indexOf(player)));
            message = message.replaceAll("\\{total}",Integer.toString(queue.size()));
            player.sendMessage(message);
            return;
        }

        ConfigHandler conf = RidesPlugin.getInstance().getConfigHandler();
        //Check if player queuing for another ride
        if (conf.queueingPlayers.containsKey(player.getUniqueId())) {
            String message = ChatColor.translateAlternateColorCodes('&', Messages.ride_queue_other_queue);
            message = message.replaceAll("\\{ride}",conf.queueingPlayers.get(player.getUniqueId()));
            player.sendMessage(message);
            return;
        }

        //check if player is riding another ride
        if (conf.ridePlayers.containsKey(player.getUniqueId())) {
            String message = ChatColor.translateAlternateColorCodes('&', Messages.ride_queue_other_riding);
            player.sendMessage(message);
            return;
        }

        //add to the queue
        queue.add(player);
        RidesPlugin.getInstance().getConfigHandler().queueingPlayers.put(player.getUniqueId(), rideID);

        String message = ChatColor.translateAlternateColorCodes('&',Messages.ride_queue_joined);
        message = message.replaceAll("\\{ride}", rideID);
        message = message.replaceAll("\\{place}",Integer.toString(queue.indexOf(player)));
        player.sendMessage(message);

    }

    public void removeFromQueue(Player player) {
        //remove from the queue
        queue.remove(player);
        RidesPlugin.getInstance().getConfigHandler().queueingPlayers.remove(player.getUniqueId());
        String message = ChatColor.translateAlternateColorCodes('&',Messages.ride_queue_left);
        message = message.replaceAll("\\{ride}", rideID);
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
                    if (enable()) {out = Messages.command_admin_ride_enable.replaceAll("\\{ride}", rideID);} else {
                        out = Messages.command_admin_ride_enable_fail.replaceAll("\\{ride}", rideID);
                    }
                }
                else {
                    disable();
                    out = Messages.command_admin_ride_disable.replaceAll("\\{ride}", rideID);
                }
                break;
            case "BASE_LOCATION":
                //value should be blank or "x y z"
                Location location = sender.getLocation().clone();
                if (value.equalsIgnoreCase(" ")) {
                    baseLocation = location;
                    out = Messages.command_admin_ride_setting_LOCATION_player;
                } else {
                    double x,y,z;
                    try{
                        x = Double.parseDouble(values[0]);
                        y = Double.parseDouble(values[1]);
                        z = Double.parseDouble(values[2]);

                        location.setX(x);
                        location.setY(y);
                        location.setZ(z);
                        baseLocation = location;
                        out = Messages.command_admin_ride_setting_LOCATION_coords
                                .replaceAll("\\{X}",Double.toString(x))
                                .replaceAll("\\{Y}",Double.toString(y))
                                .replaceAll("\\{Z}",Double.toString(z));
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        out = Messages.command_admin_ride_setting_LOCATION_fail;
                    }
                }
                break;
            case "CAPACITY":
                try{
                    int cap = Integer.parseInt(values[0]);
                    capacity = cap;
                    out = Messages.command_admin_ride_setting_GENERAL_success.replaceAll("\\{VALUE}",cap + " seats");
                } catch (NumberFormatException e) {
                    out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeInt;
                }
                break;
            case "EXIT_LOCATION":
                //value should be blank or "x y z"
                Location loc2 = sender.getLocation().clone();
                if (value.equalsIgnoreCase(" ")) {
                    exitLocation = loc2;
                    out = Messages.command_admin_ride_setting_LOCATION_player;
                } else {
                    double x,y,z;
                    try{
                        x = Double.parseDouble(values[0]);
                        y = Double.parseDouble(values[1]);
                        z = Double.parseDouble(values[2]);

                        loc2.setX(x);
                        loc2.setY(y);
                        loc2.setZ(z);
                        exitLocation = loc2;
                        out = Messages.command_admin_ride_setting_LOCATION_coords
                                .replaceAll("\\{X}",Double.toString(x))
                                .replaceAll("\\{Y}",Double.toString(y))
                                .replaceAll("\\{Z}",Double.toString(z));
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        out = Messages.command_admin_ride_setting_LOCATION_fail;
                    }
                }
                break;
            case "START_PLAYERS":
                try{
                    int startPl = Integer.parseInt(values[0]);
                    minStartPlayers = startPl;
                    out = Messages.command_admin_ride_setting_GENERAL_success.replaceAll("\\{VALUE}",Integer.toString(startPl));
                } catch (NumberFormatException e) {
                    out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeInt;
                }

                break;
            case "START_DELAY":
                try{
                    int delay = Integer.parseInt(values[0]);
                    startWaitTime = delay;
                    out = Messages.command_admin_ride_setting_GENERAL_success.replaceAll("\\{VALUE}",Integer.toString(delay));
                } catch (NumberFormatException e) {
                    out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeInt;
                }

                break;
            case "JOIN_AFTER_START":
                //Value should be true/false
                if (Boolean.parseBoolean(values[0])) {
                    joinAfterStart = true;
                    out = Messages.command_admin_ride_setting_GENERAL_success.replaceAll("\\{VALUE}","TRUE");
                } else {
                    joinAfterStart = false;
                    out = Messages.command_admin_ride_setting_GENERAL_success.replaceAll("\\{VALUE}","FALSE");
                }
                break;
            case "PRICE":
                try{
                    int price = Integer.parseInt(values[0]);
                    this.price = price;
                    out = Messages.command_admin_ride_setting_GENERAL_success.replaceAll("\\{VALUE}",Integer.toString(price));
                } catch (NumberFormatException e) {
                    out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeInt;
                }

                break;
        }

        return out.replaceAll("\\{OPTION}",key).replaceAll("\\{RIDE}", rideID);
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
        enabled = conf.getBoolean("Generic.Enabled");
        rideID = conf.getString("Generic.ID");
        capacity = conf.getInt("Generic.CAPACITY");
        baseLocation = conf.getLocation("Generic.BASE_LOCATION");
        exitLocation = conf.getLocation("Generic.EXIT_LOCATION");
        minStartPlayers = conf.getInt("Generic.Start.MIN_PLAYERS");
        startWaitTime = conf.getInt("Generic.Start.WAIT_TIME");
        joinAfterStart = conf.getBoolean("Generic.Start.ALLOW_JOIN_AFTER_START");
        price = conf.getInt("Generic.Price");
    }

    /**
     *
     * @return a formatted string with the ride info in human readable format
     */
    public String getRideInfoStr() {
        String out = Messages.command_admin_ride_info_general;
        out = out.replaceAll("\\{ID}", rideID);
        out = out.replaceAll("\\{ENABLED}",Boolean.toString(enabled));
        out = out.replaceAll("\\{PRICE}",Integer.toString(price));
        out = out.replaceAll("\\{RUNNING}",Boolean.toString(running));
        out = out.replaceAll("\\{RIDER_COUNT}",Integer.toString(riders.size()));
        out = out.replaceAll("\\{QUEUE_COUNT}",Integer.toString(queue.size()));
        out = out.replaceAll("\\{START_PLAYERS}",Integer.toString(minStartPlayers));
        out = out.replaceAll("\\{START_DELAY}",Integer.toString(startWaitTime));
        out = out.replaceAll("\\{JOIN_AFTER_START}",Boolean.toString(joinAfterStart));

        if (capacity ==null|| capacity ==0)         out = out.replaceAll("\\{CAPACITY}","NOT SET");
        else out = out.replaceAll("\\{CAPACITY}",Integer.toString(capacity));

        String exit,base;
        if (exitLocation ==null) exit = "NOT SET"; else exit = exitLocation.getWorld().getName() + " x"+ exitLocation.getX() + " y"+ exitLocation.getY() + " z" + exitLocation.getZ();
        if (baseLocation ==null) base = "NOT SET"; else base = baseLocation.getWorld().getName() + " x"+ baseLocation.getX() + " y"+ baseLocation.getY() + " z" + baseLocation.getZ();
        out = out.replaceAll("\\{EXIT_LOCATION}",exit);
        out = out.replaceAll("\\{BASE_LOCATION}",base);

        return out;
    }

    /* EVENT LISTENERS */
    /**
     * Prevent players exiting their seat on the ride
     * @param e - the vehicle Exit event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCartExit(VehicleExitEvent e) {
        if (seats.contains(e.getVehicle().getUniqueId())) {

            if (e.getExited() instanceof Player) {
                UUID uuid = e.getExited().getUniqueId();

                if (riders.containsKey(uuid)) {

                    if (seats.indexOf(e.getVehicle().getUniqueId()) == riders.get(uuid)) {
                        if (RidesPlugin.getInstance().isEnabled()) {
                            Bukkit.getScheduler().runTaskLater(
                                    RidesPlugin.getInstance(),
                                    () -> e.getVehicle().addPassenger(e.getExited()),
                                    0);
                        }
                    } else e.setCancelled(false);
                } else e.setCancelled(false);
            }

        }
    }

    /**
     * Prevent players entering vehicles that are part of a ride
     * @param e - the vehicle Enter event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCartEnter(VehicleEnterEvent e) {
        if (seats.contains(e.getVehicle().getUniqueId())) {
            if (e.getEntered() instanceof Player) {
                if (!riders.containsKey(e.getEntered().getUniqueId()) || seats.indexOf(e.getVehicle().getUniqueId()) != riders.get(e.getEntered().getUniqueId())) {
                    e.setCancelled(true);
                }
            } else {
                e.setCancelled(true);
            }

        }
    }


    @EventHandler
    public void onCartBreak(VehicleDamageEvent e) {
        if (seats.contains(e.getVehicle().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onCartBreak(VehicleDestroyEvent e) {
        if (seats.contains(e.getVehicle().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onCartPush(VehicleEntityCollisionEvent e) {
        if (seats.contains(e.getVehicle().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onCartMove(VehicleMoveEvent e) {
        if (seats.contains(e.getVehicle().getUniqueId())) {
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
        if (riders.containsKey(e.getPlayer().getUniqueId())) {
            ejectPlayer(e.getPlayer());
        }

        //If in the ride queue
        if (queue.contains(e.getPlayer())) {
            removeFromQueue(e.getPlayer());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (riders.containsKey(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (riders.containsKey(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }
}
