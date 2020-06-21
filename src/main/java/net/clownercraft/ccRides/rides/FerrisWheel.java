package net.clownercraft.ccRides.rides;

import net.clownercraft.ccRides.config.Messages;
import net.clownercraft.ccRides.RidesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;

public class FerrisWheel extends Ride {
    Double radius;//the radius of the FerrisWheel seats
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
        super.type = "FERRIS_WHEEL";

        //load generic options
        super.setRideOptions(conf);

        //load FerrisWheel specific options
        radius = conf.getDouble("FerrisWheel.Radius");
        rotatespeed = conf.getInt("FerrisWheel.Rotation.Ticks_Per_Full_Rotation");
        length = conf.getInt("FerrisWheel.Rotation.Num_Cycles");
        axis = conf.getBoolean("FerrisWheel.axis");
        cartWidth = conf.getInt("FerrisWheel.cartWidth");

        if (enabled) enable();
    }

    /**
     * Create a new FerrisWheel, with no settings
     * @param name the ID/Name for this ride
     */
    public FerrisWheel(String name) {
        //Setyp basic settings
        super.type = "FERRIS_WHEEL";
        super.rideID = name;

        //Save a default config
        RidesPlugin.getInstance().getConfigHandler().saveRideConfig(createConfig());
    }

    @Override
    public void init() {
        //Register Listener
        RidesPlugin.getInstance().getServer().getPluginManager().registerEvents(this,RidesPlugin.getInstance());

        //Spawn Seats if enabled
        if (enabled) respawnSeats();
    }

    public void startRide() {
        RidesPlugin.getInstance().getLogger().info("Starting FerrisWheel " + rideID);
        double rotationStep = 2*Math.PI / rotatespeed;
        updateTask = Bukkit.getScheduler().runTaskTimer(RidesPlugin.getInstance(), () -> {
            currentRotation += rotationStep;
            tickPositions();
            if (currentRotation>=2*Math.PI*length) stopRide();
        },1l,1l);
        running = true;
        countdownStarted = false;
    }

    public void stopRide() {
        //Cancel movement & reset Positions
        if (updateTask!=null) updateTask.cancel();

        currentRotation = 0.0;

        countdownStarted = false;
        running = false;

        //Eject Players
        for (UUID uuid:riders.keySet()) {
            ejectPlayer(Bukkit.getPlayer(uuid));
        }

        Bukkit.getScheduler().runTaskLater(RidesPlugin.getInstance(), () -> { if (enabled) checkQueue();},10l);
    }




    /**
     * gets the position of a seat at the current point in the ride cycle
     *
     * @param seatNum = the index of the seat to check
     * @return = the location the seat should currently be
     */
    public Location getPosition(int seatNum) {
        int positionNum = seatNum/cartWidth;
        int positionCap = capacity /cartWidth;
        double angle = currentRotation + ((positionNum/(double)positionCap))*2*Math.PI - Math.PI/2;

        Location loc = baseLocation.clone();
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
                    try {
                        radius = Double.parseDouble(values[0]);
                        out = Messages.command_admin_ride_setting_GENERAL_success_blocks.replaceAll("\\{VALUE}",Double.toString(radius));
                    } catch (NumberFormatException e) {
                        out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeDoubBlocks;
                    }
                    break;
                case "ROTATE_SPEED": //integer, ticks per full rotation
                    try {
                        rotatespeed = Integer.parseInt(values[0]);
                        out = Messages.command_admin_ride_setting_GENERAL_success_ticks.replaceAll("\\{VALUE}",Integer.toString(rotatespeed));
                    } catch (NumberFormatException e) {
                        out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeIntTicks;
                    }
                    break;
                case "RIDE_LENGTH": //integer, number of full rotations per ride
                    try {
                        length = Integer.parseInt(values[0]);
                        out = Messages.command_admin_ride_setting_GENERAL_success_cycles.replaceAll("\\{VALUE}",Integer.toString(length));
                    } catch (NumberFormatException e) {
                        out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeIntCycles;
                    }
                    break;

                case "CART_WIDTH": //double, the number of full height cycles per rotation
                    try {
                        cartWidth = Integer.parseInt(values[0]);
                        out = Messages.command_admin_ride_setting_GENERAL_success.replaceAll("\\{VALUE}",Integer.toString(cartWidth));
                    } catch (NumberFormatException e) {
                        out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeInt;
                    }
                    break;
                case "AXIS": //boolean
                    if (Boolean.parseBoolean(values[0])) {
                        axis = true;
                        out = Messages.command_admin_ride_setting_GENERAL_success_axis_T.replaceAll("\\{VALUE}",Boolean.toString(true));
                    } else {
                        axis = false;
                        out = Messages.command_admin_ride_setting_GENERAL_success_axis_F.replaceAll("\\{VALUE}",Boolean.toString(false));
                    }
                    break;
            }
        }


        //If out is still empty, we didn't recognise the option key
        //if this isn't the case, save the changes
        if (out.equals("")) out = Messages.command_admin_ride_setting_GENERAL_fail_notFound;
        else RidesPlugin.getInstance().getConfigHandler().saveRideConfig(createConfig());


        //If enabled re-enable the ride to introduce setting
        if (enabled) enable();

        //return the message
        return out.replaceAll("\\{OPTION}",key).replaceAll("\\{RIDE}", rideID);
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
                baseLocation == null
                        || exitLocation == null
                        || capacity == null
                        || rideID == null
                        || type == null
                        || radius == null
                        || rotatespeed == null
                        || length == null
                        || cartWidth == null
                        || capacity == 0
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
        enabled = true;

        return true;
    }

    /**
     * Disable the ride
     */
    @Override
    public void disable() {

        stopRide();

        despawnSeats();

        for (Player p: queue) {
            removeFromQueue(p);
        }

        enabled = false;
    }

    /**
     *
     * @return a formatted string with the ride info in human readable format
     */
    @Override
    public String getRideInfoStr() {
        String out = super.getRideInfoStr();
        out = out + Messages.command_admin_ride_info_ferrisWheel;

        //leaving these here just incase they get put in the FerrisWheel specific message too
        out = out.replaceAll("\\{ID}", rideID);
        out = out.replaceAll("\\{ENABLED}",Boolean.toString(enabled));
        out = out.replaceAll("\\{PRICE}",Integer.toString(price));
        out = out.replaceAll("\\{RUNNING}",Boolean.toString(running));
        out = out.replaceAll("\\{RIDER_COUNT}",Integer.toString(riders.size()));
        out = out.replaceAll("\\{QUEUE_COUNT}",Integer.toString(queue.size()));
        out = out.replaceAll("\\{START_PLAYERS}",Integer.toString(minStartPlayers));
        out = out.replaceAll("\\{START_DELAY}",Integer.toString(startWaitTime));
        out = out.replaceAll("\\{JOIN_AFTER_START}",Boolean.toString(joinAfterStart));

        if (capacity ==null|| capacity ==0) out = out.replaceAll("\\{CAPACITY}","NOT SET");
        else out = out.replaceAll("\\{CAPACITY}",Integer.toString(capacity));

        String exit,base;
        if (exitLocation ==null) exit = "NOT SET"; else exit = exitLocation.getWorld().getName() + " x"+ exitLocation.getX() + " y"+ exitLocation.getY() + " z" + exitLocation.getZ();
        if (baseLocation ==null) base = "NOT SET"; else base = baseLocation.getWorld().getName() + " x"+ baseLocation.getX() + " y"+ baseLocation.getY() + " z" + baseLocation.getZ();
        out = out.replaceAll("\\{EXIT_LOCATION}",exit);
        out = out.replaceAll("\\{BASE_LOCATION}",base);

        //FerrisWheel specific stuff
        if (radius==null || radius==0)  out = out.replaceAll("\\{RADIUS}","NOT SET");
        else out = out.replaceAll("\\{RADIUS}",Double.toString(radius));

        if (rotatespeed==null || rotatespeed==0) out = out.replaceAll("\\{ROTATE_SPEED}","NOT SET");
        else out = out.replaceAll("\\{ROTATE_SPEED}",Integer.toString(rotatespeed));

        if(length==null||length==0) out = out.replaceAll("\\{RIDE_LENGTH}","NOT SET");
        else out = out.replaceAll("\\{RIDE_LENGTH}",Integer.toString(length));

        out = out.replaceAll("\\{CART_WIDTH}",Integer.toString(cartWidth));
        out = out.replaceAll("\\{AXIS}",Boolean.toString(axis));

        return out;
    }
}
