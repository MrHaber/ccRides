package net.clownercraft.ccRides.rides;

import net.clownercraft.ccRides.Config.Messages;
import net.clownercraft.ccRides.RidesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

public class Carousel extends Ride {
    Double radius;//the radius of the carousel seats
    Integer rotatespeed; //number of ticks per full rotation of the carousel
    Integer length; //number of full rotations per ride.
    Double heightVariation = 0.0; //The maximum change in height while riding (this is +/-)
    Double heightSpeed = 1.0; //How many full sine waves per rotation
    Boolean horseMode = false; //Use horses instead of minecarts

    /*
    Running data
     */
    Double currentRotation = 0.0; //Current rotation of the carousel in radians
    BukkitTask updateTask;


    /**
     * Create a Carousel based on an existing config
     * @param conf the config to read
     */
    public Carousel(YamlConfiguration conf) {
        //Set the type to CAROUSEL
        super.TYPE = "CAROUSEL";

        //load generic options
        super.setRideOptions(conf);

        //load carousel specific options
        radius = conf.getDouble("Carousel.Radius");
        rotatespeed = conf.getInt("Carousel.Rotation.Ticks_Per_Full_Rotation");
        length = conf.getInt("Carousel.Rotation.Num_Cycles");
        heightVariation = conf.getDouble("Carousel.Height.Max_Change_±");
        heightSpeed = conf.getDouble("Carousel.Height.Cycles_Per_Rotation");
        horseMode = conf.getBoolean("Carousel.HorseMode");

        if (ENABLED) enable();
    }

    /**
     * Create a new Carousel, with no settings
     * @param name the ID/Name for this ride
     */
    public Carousel(String name) {
        //Setyp basic settings
        super.TYPE = "CAROUSEL";
        super.ID = name;

        //Save a default config
        RidesPlugin.getInstance().getConfigHandler().saveRideConfig(createConfig());
    }

    @Override
    public void init() {
        //Register Listener
        RidesPlugin.getInstance().getServer().getPluginManager().registerEvents(this,RidesPlugin.getInstance());

        //Spawn Seats if enabled
        if (ENABLED) respawnSeats();
    }

    public void startRide() {
        //RidesPlugin.getInstance().getLogger().info("Starting Carousel " + ID);
        double rotationStep = 2*Math.PI / rotatespeed;
        updateTask = Bukkit.getScheduler().runTaskTimer(RidesPlugin.getInstance(), () -> {
            currentRotation += rotationStep;
            tickPositions();
            if (currentRotation>=2*Math.PI*length) stopRide();
        },1l,1l);
        RUNNING = true;
        COUNTDOWN_STARTED = false;
    }

    public void stopRide() {
        //Cancel movement & reset Positions
        if (updateTask!=null) updateTask.cancel();

        currentRotation = 0.0;

        COUNTDOWN_STARTED = false;
        RUNNING = false;
        //Eject Players
        for (Player p:riders.keySet()) {
            ejectPlayer(p);
        }

        Bukkit.getScheduler().runTaskLater(RidesPlugin.getInstance(), () -> { if (ENABLED) checkQueue();},10l);
    }


    /**
     * Forces the ride to remove all it's vehicles/seats and respawn them.
     */
    @Override
    public void respawnSeats() {
        despawnSeats();

        for (int i=0;i<CAPACITY;i++){
            Location loc2 = getPosition(i);
            if (horseMode) {

                //remove any stray horses within a few blocks of each location in case there's duplicates in the world.
                for (Entity e:loc2.getWorld().getNearbyEntities(loc2,3,3,3)) {
                    if (e.getType().equals(EntityType.HORSE)
                            && !seats.contains(e)) e.remove();
                }


                Horse horse = (Horse) loc2.getWorld().spawnEntity(loc2, EntityType.HORSE);
                //Make carts invulnerable, not affected by gravity and have no velocity

                horse.setInvulnerable(true);
                horse.setGravity(false);
                horse.setVelocity(new Vector(0, 0, 0));
                horse.setSilent(true);
                horse.setAgeLock(true);
                horse.setBaby();
                horse.setAI(false);
                seats.add(horse);
            } else {

                //remove any stray minecarts within a few blocks of each location in case there's duplicates in the world.
                for (Entity e:loc2.getWorld().getNearbyEntities(loc2,3,3,3)) {
                    if (e.getType().equals(EntityType.MINECART)
                            && !seats.contains(e)) e.remove();
                }


                Minecart cart = (Minecart) loc2.getWorld().spawnEntity(loc2, EntityType.MINECART);
                //Make carts invulnerable, not affected by gravity and have no velocity

                cart.setInvulnerable(true);
                cart.setGravity(false);
                cart.setVelocity(new Vector(0, 0, 0));
                cart.setSilent(true);
                cart.setMaxSpeed(0);
                seats.add(cart);
            }

        }
    }

    /**
     * gets the position of a seat at the current point in the ride cycle
     *
     * @param seatNum = the index of the seat to check
     * @return = the location the seat should currently be
     */
    public Location getPosition(int seatNum) {
        double angle = currentRotation + (seatNum/(double)CAPACITY)*2*Math.PI;

        Location loc = BASE_LOCATION.clone();
        double xvec = radius * Math.cos(angle);
        double zvec = radius * Math.sin(angle);
        double yvec = heightVariation * Math.sin(angle*heightSpeed);

        loc.add(xvec,yvec,zvec);
        loc.setPitch(0.0f);
        if (horseMode) loc.setYaw((float) Math.toDegrees(angle));
        else loc.setYaw((float) Math.toDegrees(angle)+90f);

        return loc;
    }

    /**
     * @return A FileConfiguration containing all of this Carousel's settings
     */
    @Override
    public FileConfiguration createConfig() {
        //Get generic options
        FileConfiguration out = super.createConfig();

        //Add Carousel Specific Options
        try{
        out.set("Carousel.Radius",radius);
        out.set("Carousel.Rotation.Ticks_Per_Full_Rotation",rotatespeed);
        out.set("Carousel.Rotation.Num_Cycles",length);
        out.set("Carousel.Height.Max_Change_±",heightVariation);
        out.set("Carousel.Height.Cycles_Per_Rotation",heightSpeed);
        out.set("Carousel.HorseMode",horseMode);

        } catch (NullPointerException ignored) {}

        return out;
    }

    /**
     * Get the list of options you can set
     *
     * @return A string list containing all options.
     */
    @Override
    public List<String> getConfigOptions() {
        //Get Default Options
        List<String> out = super.getConfigOptions();
        //Add Carousel Specific Options
        out.add("RADIUS");
        out.add("ROTATE_SPEED");
        out.add("RIDE_LENGTH");
        out.add("HEIGHT_VAR");
        out.add("HEIGHT_SPEED");
        out.add("HORSE_MODE");
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
        String[] values = value.split(" ");
        if (out.equals("")) {

            //The setting wasn't one of the defaults, so let's set carousel specific ones.
            switch (key) {

                case "RADIUS": //integer, in number of blocks
                    try{
                        radius = Double.parseDouble(values[0]);
                    out = "Radius set to " + radius + " blocks.";
                    } catch (NumberFormatException e) {
                        out = "Radius must be a number of blocks. Decimals Allowed";
                    }
                    break;
                case "ROTATE_SPEED": //integer, ticks per full rotation
                    try{
                        rotatespeed = Integer.parseInt(values[0]);
                        out = "Rotate_Speed set to " + rotatespeed + " ticks per rotation.";
                    } catch (NumberFormatException e) {
                        out = "Rotate_Speed must be an integer number of ticks per rotation.";
                    }
                    break;
                case "RIDE_LENGTH": //integer, number of full rotations per ride
                    try{
                        length = Integer.parseInt(values[0]);
                        out = "RIDE_LENGTH set to " + length + " rotations.";
                    } catch (NumberFormatException e) {
                        out = "RIDE_LENGTH must be an integer number of rotations.";
                    }
                    break;
                case "HEIGHT_VAR": //double, the max +/- height variation in blocks
                    try{
                        heightVariation = Double.parseDouble(values[0]);
                        out = "HEIGHT_VAR set to ±" + heightVariation + " blocks.";
                    } catch (NumberFormatException e) {
                        out = "HEIGHT_VAR must be an double number of blocks.";
                    }
                    break;
                case "HEIGHT_SPEED": //double, the number of full height cycles per rotation
                    try{
                        heightSpeed = Double.parseDouble(values[0]);
                        out = "HEIGHT_SPEED set to " + heightSpeed + " cycles per rotation.";
                    } catch (NumberFormatException e) {
                        out = "HEIGHT_VAR must be an double number of cycles per rotation.";
                    }
                    break;
                case "HORSE_MODE": //boolean
                    if (Boolean.parseBoolean(values[0])) {
                        horseMode = true;
                        out = "HORSE_MODE set to true. Using Horses as seats.";
                    } else {
                        horseMode = false;
                        out = "HORSE_MODE set to false. Using Minecarts as seats.";
                    }
                    break;
            }
        }


        //If out is still empty, we didn't recognise the option key
        //if this isn't the case, save the changes
        if (out.equals("")) out = key + " not found as an option";
        else RidesPlugin.getInstance().getConfigHandler().saveRideConfig(createConfig());

        //If enabled re-enable the ride to introduce setting
        if (ENABLED) enable();

        //return the message
        return out.replaceAll("\\{OPTION}",key);
    }

    /**
     * Enable the ride
     *
     * @return false if failed (ie not all settings configured yet)
     */
    @Override
    public boolean enable() {
        //Check if all settings are set
        if (
                BASE_LOCATION == null
                || EXIT_LOCATION == null
                || CAPACITY == null
                || ID == null
                || TYPE == null
                || radius == null
                || rotatespeed == null
                || length == null
                || heightVariation == null
                || heightSpeed == null
                || CAPACITY == 0
                || radius == 0
                || rotatespeed == 0
                || length == 0
                || heightSpeed == 0
        ) {
            disable();
            return false;
        }

        //Enable the ride and spawn in seats
        respawnSeats();
        ENABLED = true;

        return true;
    }

    /**
     * Disable the ride
     */
    @Override
    public void disable() {

        stopRide();

        despawnSeats();

        for (Player p:QUEUE) {
            removeFromQueue(p);
        }

        ENABLED = false;
    }

    /**
     *
     * @return a formatted string with the ride info in human readable format
     */
    @Override
    public String getRideInfoStr() {
        String out = super.getRideInfoStr();
        out = out + Messages.command_admin_ride_info_carousel;

        //leaving these here just incase they get put in the carousel specific message too
        out = out.replaceAll("\\{ID}",ID);
        out = out.replaceAll("\\{ENABLED}",Boolean.toString(ENABLED));
        out = out.replaceAll("\\{PRICE}",Integer.toString(PRICE));
        out = out.replaceAll("\\{RUNNING}",Boolean.toString(RUNNING));
        out = out.replaceAll("\\{RIDER_COUNT}",Integer.toString(riders.size()));
        out = out.replaceAll("\\{QUEUE_COUNT}",Integer.toString(QUEUE.size()));
        out = out.replaceAll("\\{START_PLAYERS}",Integer.toString(MIN_START_PLAYERS));
        out = out.replaceAll("\\{START_DELAY}",Integer.toString(START_WAIT_TIME));
        out = out.replaceAll("\\{JOIN_AFTER_START}",Boolean.toString(JOIN_AFTER_START));

        if (CAPACITY==null||CAPACITY==0) out = out.replaceAll("\\{CAPACITY}","NOT SET");
        else out = out.replaceAll("\\{CAPACITY}",Integer.toString(CAPACITY));

        String exit,base;
        if (EXIT_LOCATION==null) exit = "NOT SET"; else exit = EXIT_LOCATION.getWorld().getName() + " x"+EXIT_LOCATION.getX() + " y"+EXIT_LOCATION.getY() + " z" + EXIT_LOCATION.getZ();
        if (BASE_LOCATION==null) base = "NOT SET"; else base = BASE_LOCATION.getWorld().getName() + " x"+BASE_LOCATION.getX() + " y"+BASE_LOCATION.getY() + " z" + BASE_LOCATION.getZ();
        out = out.replaceAll("\\{EXIT_LOCATION}",exit);
        out = out.replaceAll("\\{BASE_LOCATION}",base);

        //Carousel specific stuff
        if (radius==null || radius==0)  out = out.replaceAll("\\{RADIUS}","NOT SET");
            else out = out.replaceAll("\\{RADIUS}",Double.toString(radius));

        if (rotatespeed==null || rotatespeed==0) out = out.replaceAll("\\{ROTATE_SPEED}","NOT SET");
        else out = out.replaceAll("\\{ROTATE_SPEED}",Integer.toString(rotatespeed));

        if(length==null||length==0) out = out.replaceAll("\\{RIDE_LENGTH}","NOT SET");
        else out = out.replaceAll("\\{RIDE_LENGTH}",Integer.toString(length));

        out = out.replaceAll("\\{HEIGHT_VAR}",Double.toString(heightVariation));
        out = out.replaceAll("\\{HEIGHT_SPEED}",Double.toString(heightSpeed));
        out = out.replaceAll("\\{HORSE_MODE}",Boolean.toString(horseMode));

        return out;
    }
}
