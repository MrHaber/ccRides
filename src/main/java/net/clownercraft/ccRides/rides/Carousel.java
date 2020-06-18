package net.clownercraft.ccRides.rides;

import net.clownercraft.ccRides.config.Messages;
import net.clownercraft.ccRides.RidesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Objects;

public class Carousel extends Ride {
    Double radius;//the radius of the carousel seats
    Integer rotatespeed; //number of ticks per full rotation of the carousel
    Double accelerateLength = 0.3d; //The number of rotations to take to get up to speed/slow down
    Integer length; //number of full rotations per ride.
    Double heightVariation = 0.0; //The maximum change in height while riding (this is +/-)
    Double heightSpeed = 1.0; //How many full sine waves per rotation
    Boolean horseMode = false; //Use horses instead of minecarts

    /*
    Running data
     */
    Double currentRotation = 0.0; //Current rotation of the carousel in radians
    double currRotStep = 0.005; //Current rotation step size, used to allow for acceleration
    BukkitTask updateTask;


    /**
     * Create a Carousel based on an existing config
     * @param conf the config to read
     */
    public Carousel(YamlConfiguration conf) {
        //Set the type to CAROUSEL
        super.type = "CAROUSEL";

        //load generic options
        super.setRideOptions(conf);

        //load carousel specific options
        radius = conf.getDouble("Carousel.Radius");
        rotatespeed = conf.getInt("Carousel.Rotation.Ticks_Per_Full_Rotation");
        length = conf.getInt("Carousel.Rotation.Num_Cycles");
        heightVariation = conf.getDouble("Carousel.Height.Max_Change_±");
        heightSpeed = conf.getDouble("Carousel.Height.Cycles_Per_Rotation");
        horseMode = conf.getBoolean("Carousel.HorseMode");
        accelerateLength = conf.getDouble("Carousel.Rotation.AccelerateLength");

        if (enabled) enable();
    }

    /**
     * Create a new Carousel, with no settings
     * @param name the ID/Name for this ride
     */
    public Carousel(String name) {
        //Setyp basic settings
        super.type = "CAROUSEL";
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
        //RidesPlugin.getInstance().getLogger().info("Starting Carousel " + ID);
        double rotationStep = 2*Math.PI / rotatespeed;
        double rotationAccel;
        if (accelerateLength==0) {
            rotationAccel = rotationStep;
        } else {
            rotationAccel = rotationStep / (2 * rotatespeed * accelerateLength);
        }



        updateTask = Bukkit.getScheduler().runTaskTimer(RidesPlugin.getInstance(), () -> {
            currentRotation += currRotStep;

            if (currentRotation <= Math.PI*2*accelerateLength) {
                currRotStep += rotationAccel;

                if (currRotStep > rotationStep) currRotStep = rotationStep;
            }

            if (currentRotation >= Math.PI*2*(length-accelerateLength)) {
                currRotStep -= rotationAccel;

                if (currRotStep < 0.005) currRotStep = 0.005;
            }

            tickPositions();
            if (currentRotation>=2*Math.PI*length) stopRide();

        }, 1,1);

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
        for (Player p:riders.keySet()) {
            ejectPlayer(p);
        }

        Bukkit.getScheduler().runTaskLater(RidesPlugin.getInstance(), () -> { if (enabled) checkQueue();},10);
    }


    /**
     * Forces the ride to remove all it's vehicles/seats and respawn them.
     */
    @Override
    public void respawnSeats() {
        despawnSeats();

        for (int i = 0; i< capacity; i++){
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
        double angle = currentRotation + (seatNum/(double) capacity)*2*Math.PI;

        Location loc = baseLocation.clone();
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
        out.set("Carousel.Rotation.AccelerateLength",accelerateLength);

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
        out.add("ACCELERATE_LENGTH");
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

                case "RADIUS": //Double, in number of blocks
                    try{
                        radius = Double.parseDouble(values[0]);
                        out = Messages.command_admin_ride_setting_GENERAL_success_blocks.replaceAll("\\{VALUE}",Double.toString(radius));
                    } catch (NumberFormatException e) {
                        out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeDoubBlocks;
                    }
                    break;
                case "ROTATE_SPEED": //integer, ticks per full rotation
                    try{
                        rotatespeed = Integer.parseInt(values[0]);
                        out = Messages.command_admin_ride_setting_GENERAL_success_ticks.replaceAll("\\{VALUE}",Integer.toString(rotatespeed));
                    } catch (NumberFormatException e) {
                        out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeIntTicks;
                    }
                    break;
                case "RIDE_LENGTH": //integer, number of full rotations per ride
                    try{
                        length = Integer.parseInt(values[0]);
                        out = Messages.command_admin_ride_setting_GENERAL_success_cycles.replaceAll("\\{VALUE}",Integer.toString(length));
                    } catch (NumberFormatException e) {
                        out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeIntCycles;
                    }
                    break;
                case "ACCELERATE_LENGTH": //double, number of rotations to take to get up to speed/slow down
                    try{
                        accelerateLength = Double.parseDouble(values[0]);
                        out = Messages.command_admin_ride_setting_GENERAL_success.replaceAll("\\{VALUE}",Double.toString(accelerateLength));

                    } catch (NumberFormatException e) {
                        out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeDoub;
                    }
                    break;
                case "HEIGHT_VAR": //double, the max +/- height variation in blocks
                    try{
                        heightVariation = Double.parseDouble(values[0]);
                        out = Messages.command_admin_ride_setting_GENERAL_success_blocks.replaceAll("\\{VALUE}","±" + heightVariation);
                    } catch (NumberFormatException e) {
                        out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeDoubBlocks;
                    }
                    break;
                case "HEIGHT_SPEED": //double, the number of full height cycles per rotation
                    try{
                        heightSpeed = Double.parseDouble(values[0]);
                        out = Messages.command_admin_ride_setting_GENERAL_success_cycles.replaceAll("\\{VALUE}",Double.toString(heightSpeed));
                    } catch (NumberFormatException e) {
                        out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeDoub;
                    }
                    break;
                case "HORSE_MODE": //boolean
                    horseMode = Boolean.parseBoolean(values[0]);
                    out = Messages.command_admin_ride_setting_GENERAL_success.replaceAll("\\{VALUE}",Boolean.toString(horseMode));
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
                || heightVariation == null
                || heightSpeed == null
                || capacity == 0
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
        out = out + Messages.command_admin_ride_info_carousel;

        //leaving these here just incase they get put in the carousel specific message too
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
        if (exitLocation ==null) exit = "NOT SET"; else exit = Objects.requireNonNull(exitLocation.getWorld()).getName() + " x"+ exitLocation.getX() + " y"+ exitLocation.getY() + " z" + exitLocation.getZ();
        if (baseLocation ==null) base = "NOT SET"; else base = Objects.requireNonNull(baseLocation.getWorld()).getName() + " x"+ baseLocation.getX() + " y"+ baseLocation.getY() + " z" + baseLocation.getZ();
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
        out = out.replaceAll("\\{ACCELERATE_LENGTH}",Double.toString(accelerateLength));


        return out;
    }
}
