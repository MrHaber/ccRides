package net.clownercraft.ccRides.rides;

import net.clownercraft.ccRides.RidesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FerrisWheel extends Ride {
    Integer radius;//the radius of the FerrisWheel seats
    Integer rotatespeed; //number of ticks per full rotation of the FerrisWheel
    Integer length; //number of full rotations per ride.
    boolean axis = false; //false = xy, true=zy
    Integer cartWidth = 1;

    /*
    Running data
     */
    Double currentRotation = 0.0; //Current rotation of the FerrisWheel in radians
    BukkitTask updateTask;


    /**
     * Create a FerrisWheel based on an existing config
     * @param conf the config to read
     */
    public FerrisWheel(YamlConfiguration conf) {
        //Set the type to FerrisWheel
        super.TYPE = "FERRIS_WHEEL";

        //load generic options
        super.setRideOptions(conf);

        //load FerrisWheel specific options
        radius = conf.getInt("FerrisWheel.Radius");
        rotatespeed = conf.getInt("FerrisWheel.Rotation.Ticks_Per_Full_Rotation");
        length = conf.getInt("FerrisWheel.Rotation.Num_Cycles");
        axis = conf.getBoolean("FerrisWheel.axis");
        cartWidth = conf.getInt("FerrisWheel.cartWidth");

        if (ENABLED) enable();
    }

    /**
     * Create a new FerrisWheel, with no settings
     * @param name the ID/Name for this ride
     */
    public FerrisWheel(String name) {
        //Setyp basic settings
        super.TYPE = "FERRIS_WHEEL";
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
        //TODO charge tokens
        RidesPlugin.getInstance().getLogger().info("Starting FerrisWheel " + ID);
        double rotationStep = 2*Math.PI / rotatespeed;
        updateTask = Bukkit.getScheduler().runTaskTimer(RidesPlugin.getInstance(), new Runnable() {
            @Override
            public void run() {
                currentRotation += rotationStep;
                tickPositions();
                if (currentRotation>=2*Math.PI*length) stopRide();
            }
        },1l,1l);
        RUNNING = true;
        COUNTDOWN_STARTED = false;
    }

    public void stopRide() {
        //Cancel movement & reset Positions
        if (updateTask!=null) updateTask.cancel();

        currentRotation = 0.0;

        //Eject Players
        for (Player p:(ArrayList<Player>) riders.clone()) {
            ejectPlayer(p);
        }
        COUNTDOWN_STARTED = false;
        RUNNING = false;

        Bukkit.getScheduler().runTaskLater(RidesPlugin.getInstance(), () -> { if (ENABLED) checkQueue();},10l);
    }




    /**
     * gets the position of a seat at the current point in the ride cycle
     *
     * @param seatNum = the index of the seat to check
     * @return = the location the seat should currently be
     */
    public Location getPosition(int seatNum) {
        int positionNum = seatNum/cartWidth;
        int positionCap = CAPACITY/cartWidth;
        double angle = currentRotation + ((positionNum/(double)positionCap))*2*Math.PI - Math.PI/2;

        Location loc = BASE_LOCATION.clone();
        double xvec = radius * Math.cos(angle);
        double yvec = radius * Math.sin(angle);
        double zvec = seatNum%cartWidth;

        if (axis) loc.add(xvec,yvec,zvec);
        else loc.add(zvec,yvec,xvec);
        loc.setPitch(0.0f);

        if (axis) loc.setYaw(0.0f);
        else loc.setYaw(90.0f);
        return loc;
    }

    /**
     * @return A FileConfiguration containing all of this FerrisWheel's settings
     */
    @Override
    public FileConfiguration createConfig() {
        //Get generic options
        FileConfiguration out = super.createConfig();

        //Add FerrisWheel Specific Options
        try{
            out.set("FerrisWheel.Radius",radius);
            out.set("FerrisWheel.Rotation.Ticks_Per_Full_Rotation",rotatespeed);
            out.set("FerrisWheel.Rotation.Num_Cycles",length);
            out.set("FerrisWheel.axis",axis);
            out.set("FerrisWheel.cartWidth",cartWidth);


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
        //Add FerrisWheel Specific Options
        out.add("RADIUS");
        out.add("ROTATE_SPEED");
        out.add("RIDE_LENGTH");
        out.add("AXIS");
        out.add("CART_WIDTH");
        return out;
    }

    /**
     * @param key    the key of the setting
     *               FerrisWheel Options are:
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

            //The setting wasn't one of the defaults, so let's set FerrisWheel specific ones.
            switch (key) {

                case "RADIUS": //integer, in number of blocks
                    try{
                        radius = Integer.parseInt(values[0]);
                        out = "Radius set to " + radius + " blocks.";
                    } catch (NumberFormatException e) {
                        out = "Radius must be an integer number of blocks.";
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

                case "CART_WIDTH": //double, the number of full height cycles per rotation
                    try{
                        cartWidth = Integer.parseInt(values[0]);
                        out = "CART_WIDTH set to " + cartWidth + " cycles per rotation.";
                    } catch (NumberFormatException e) {
                        out = "CART_WIDTH must be an integer number of carts.";
                    }
                    break;
                case "AXIS": //boolean
                    if (Boolean.parseBoolean(values[0])) {
                        axis = true;
                        out = "AXIS set to true. Using z/y.";
                    } else {
                        axis = false;
                        out = "AXIS set to false. Using x/y.";
                    }
                    break;
            }
        }


        //If out is still empty, we didn't recognise the option key
        //if this isn't the case, save the changes
        if (out.equals("")) out = key + " not found as an option";
        else RidesPlugin.getInstance().getConfigHandler().saveRideConfig(createConfig());


        //return the message
        return out;
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
                        || cartWidth == null
                        || CAPACITY == 0
                        || radius == 0
                        || rotatespeed == 0
                        || length == 0
                        || cartWidth == 0
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
     * @return a map of the ride's settings with a human friendly name
     */
    @Override
    public HashMap<String, String> getRideInfo() {
        HashMap<String,String> out = super.getRideInfo();

        if (radius==null||radius==0) out.put("RADIUS &o(blocks)","NOT SET"); else out.put("RADIUS &o(Blocks)",Integer.toString(radius));
        if (rotatespeed==null||rotatespeed==0)  out.put("ROTATE_SPEED &o(Ticks per rotation)","NOT SET"); else  out.put("ROTATE_SPEED &o(Ticks per rotation)",Integer.toString(rotatespeed));
        if (length==null||length==0) out.put("RIDE_LENGTH &o(Rotations per ride)", "NOT SET"); else out.put("RIDE_LENGTH &o(Rotations per ride)",Integer.toString(length));

        //These are set by default, so can't be null
        out.put("CART_WIDTH &o(How many carts next to eachother)",Integer.toString(cartWidth));
        out.put("AXIS &o(Which direction the wheel faces)",Boolean.toString(axis));


        return out;
    }
}
