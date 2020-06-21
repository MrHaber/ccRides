package net.clownercraft.ccRides.rides;

import net.clownercraft.ccRides.config.Messages;
import net.clownercraft.ccRides.RidesPlugin;
import net.clownercraft.ccRides.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Chairswing extends Ride {
    Double radius;//the radius of the chairswing seats
    Integer rotatespeed; //number of ticks per full rotation of the chairswing (once spinning)
    Integer length; //number of full rotations per ride.
    Double chainHeight = 5.0; //The length of the chains supporting the carts
    Double accelerateLength = 1d; //Number of rotations to accelerate to full speed
    Boolean showLeads = true; //Enable/Disable decorative leads to look like chains supporting each seat
    Double MaxSwingAngle = Math.toRadians(30.0); //Swing angle in radians

    /*
    Running data
     */
    Double currentRotation = 0.0; //Current rotation of the chairswing in radians
    double currRotStep = 0.005; //Current rotation step size, used to allow for acceleration
    Double currentSwing = 0.0; //Current swing angle of the chairswing in radians
    BukkitTask updateTask;

    ArrayList<Entity> leadStarts = new ArrayList<>();
    ArrayList<Entity> leadEnds = new ArrayList<>();



    /**
     * Create a Chairswing based on an existing config
     * @param conf the config to read
     */
    public Chairswing(YamlConfiguration conf) {
        //Set the type to CHAIRSWING
        super.type = "CHAIRSWING";

        //load generic options
        super.setRideOptions(conf);

        //load chairswing specific options
        radius = conf.getDouble("Chairswing.Radius");
        chainHeight = conf.getDouble("Chairswing.ChainHeight");
        accelerateLength = conf.getDouble("Chairswing.Rotation.AccelerateRotations");
        rotatespeed = conf.getInt("Chairswing.Rotation.TicksPerFullRotation");
        length = conf.getInt("Chairswing.Rotation.RideCycles");
        showLeads = conf.getBoolean("Chairswing.ShowLeads");
        MaxSwingAngle = conf.getDouble("Chairswing.MaxSwingAngle");

        if (enabled) enable();
    }

    /**
     * Create a new Chairswing, with no settings
     * @param name the ID/Name for this ride
     */
    public Chairswing(String name) {
        //Setyp basic settings
        super.type = "CHAIRSWING";
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
        //RidesPlugin.getInstance().getLogger().info("Starting Chairswing " + ID);
        double MainRotationStep = 2*Math.PI / rotatespeed;
        double rotationAccel,swingstep;
        if (accelerateLength==0) {
            rotationAccel = MainRotationStep;
            swingstep = MaxSwingAngle;
        } else {
            rotationAccel = MainRotationStep / (2 * rotatespeed * accelerateLength);
            swingstep = MaxSwingAngle / (2 * rotatespeed * accelerateLength);
        }

        updateTask = Bukkit.getScheduler().runTaskTimer(RidesPlugin.getInstance(), () -> {
            currentRotation += currRotStep;

            if (currentRotation <= Math.PI*2*accelerateLength) {
                currRotStep += rotationAccel;
                currentSwing += swingstep;

                if (currentSwing > MaxSwingAngle) currentSwing = MaxSwingAngle;
                if (currRotStep > MainRotationStep) currRotStep = MainRotationStep;
            }

            if (currentRotation >= Math.PI*2*(length-accelerateLength)) {
                currRotStep -= rotationAccel;
                currentSwing -= (1.3 * swingstep);

                if (currentSwing < 0) currentSwing = 0d;
                if (currRotStep < 0.005) currRotStep = 0.005;
            }

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
        currRotStep = 0.005;
        currentSwing = 0.0;

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
        double angle = currentRotation + (seatNum/(double) capacity)*2*Math.PI;

        Location locTop = baseLocation.clone();
        double x1,y1,z1;

        x1 = radius * Math.cos(angle);
        z1 = radius * Math.sin(angle);
        y1 = 0;

        locTop.add(x1,y1,z1);

        Location locSeat = locTop.clone();

        double x2,y2,z2,c;
        c = Math.sqrt(chainHeight*chainHeight - (chainHeight * Math.cos(currentSwing))*(chainHeight * Math.cos(currentSwing)));
        x2 = c * Math.cos(angle);
        y2 = -1 * chainHeight * Math.cos(currentSwing);
        z2 = c * Math.sin(angle);

        locSeat.add(x2,y2,z2);

        locSeat.setPitch((float) Math.toDegrees(currentSwing));
        locSeat.setYaw((float) Math.toDegrees(angle));

        return locSeat;
    }
    /**
     * gets the position of a lead at the current point in the ride cycle
     *
     * @param seatNum = the index of the seat to check
     * @return = the location the lead should currently be
     */
    public Location getLeadPosition(int seatNum) {
        double angle = currentRotation + (seatNum/(double) capacity)*2*Math.PI;

        Location locTop = baseLocation.clone();
        double x1,y1,z1;

        x1 = radius * Math.cos(angle);
        z1 = radius * Math.sin(angle);
        y1 = 0;

        locTop.add(x1,y1,z1);
        locTop.setYaw((float) Math.toDegrees(angle));


        return locTop;
    }

    /**
     * Triggers the ride to set the position of all it's vehicles,
     */
    @Override
    public void tickPositions() {
        for (int i=0;i<seats.size();i++) {
            Vehicle v = seats.get(i);
            Location loc = getPosition(i);
            //Teleport cart
            teleportWithPassenger(v,loc);

            if (showLeads) {
                Entity e = leadEnds.get(i);
                teleportWithPassenger(e,loc);
            }
        }
        if (showLeads){
            for (int i=0;i<leadStarts.size();i++) {
                Entity e = leadStarts.get(i);
                Location loc = getLeadPosition(i);
                //Teleport cart
                teleportWithPassenger(e,loc);
            }
        }

    }

    /**
     * Forces the ride to remove all it's vehicles/seats and respawn them.
     */
    @Override
    public void respawnSeats() {
        super.respawnSeats();

        if (showLeads) {
            for (int i = 0; i < capacity; i++) {
                Location LocLeadE = getPosition(i);
                Location LocLeadS = getLeadPosition(i);

                //remove any minecarts within a few blocks of each location in case there's duplicates in the world.
                for (Entity e : LocLeadE.getWorld().getNearbyEntities(LocLeadE, 3, 3, 3)) {
                    if (e.getType().equals(EntityType.PARROT)
                            && !leadEnds.contains(e)) e.remove();
                }
                for (Entity e : LocLeadS.getWorld().getNearbyEntities(LocLeadS, 3, 3, 3)) {
                    if (e.getType().equals(EntityType.PARROT)
                            && !leadStarts.contains(e)) e.remove();
                }
                //Spawn new lead
                Parrot leadStart = (Parrot) LocLeadS.getWorld().spawnEntity(LocLeadS, EntityType.PARROT);
                Parrot leadEnd = (Parrot) LocLeadE.getWorld().spawnEntity(LocLeadE, EntityType.PARROT);

                //Make carts invulnerable, not affected by gravity and have no velocity

                leadStart.setInvulnerable(true);
                leadStart.setGravity(false);
                leadStart.setAI(false);
                leadStart.setVelocity(new Vector(0, 0, 0));
                leadStart.setSilent(true);
                leadStart.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,Integer.MAX_VALUE,1,false,false));
                leadStart.setCustomName("Chairswing Parrot");
                leadStart.setCustomNameVisible(false);

                leadEnd.setInvulnerable(true);
                leadEnd.setGravity(false);
                leadEnd.setAI(false);
                leadEnd.setVelocity(new Vector(0, 0, 0));
                leadEnd.setSilent(true);
                leadEnd.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,Integer.MAX_VALUE,1,false,false));
                leadEnd.setCustomName("Chairswing Parrot");
                leadEnd.setCustomNameVisible(false);

                leadEnd.setLeashHolder(leadStart);


                leadStarts.add(leadStart);
                leadEnds.add(leadEnd);
            }
        }
    }

    /**
     * despawns all this rides minecarts, to prevent duplicates appearing after a shutdown
     */
    @Override
    public void despawnSeats() {
        super.despawnSeats();

        for (Entity e : leadStarts) {
            e.remove();
        }
        for (Entity e : leadEnds) {
            e.remove();
        }

        leadStarts.clear();
        leadEnds.clear();
    }

    /**
     * @return A FileConfiguration containing all of this Chairswing's settings
     */
    @Override
    public FileConfiguration createConfig() {
        //Get generic options
        FileConfiguration out = super.createConfig();

        //Add Chairswing Specific Options
        try{

            out.set("Chairswing.Radius",radius);
            out.set("Chairswing.ChainHeight",chainHeight);
            out.set("Chairswing.Rotation.AccelerateRotations",accelerateLength);
            out.set("Chairswing.Rotation.TicksPerFullRotation",rotatespeed);
            out.set("Chairswing.Rotation.RideCycles",length);
            out.set("Chairswing.ShowLeads",showLeads);
            out.set("Chairswing.MaxSwingAngle",MaxSwingAngle);

        } catch (NullPointerException ignored) {}

        return out;
    }

    /**
     * Get the list of options you can set
     * @return A string list containing all options.
     */
    @Override
    public List<String> getConfigOptions() {
        //Get Default Options
        List<String> out = super.getConfigOptions();
        //Add Chairswing Specific Options
        out.add("RADIUS");
        out.add("ROTATE_SPEED");
        out.add("RIDE_LENGTH");
        out.add("CHAIN_HEIGHT");
        out.add("ACCELERATE_LENGTH");
        out.add("SHOW_LEADS");
        out.add("MAX_SWING_ANGLE");
        return out;
    }

    /**
     * @param key    the key of the setting
     *               Chairswing Options are:
     *                  RADIUS, ROTATE_SPEED, RIDE_LENGTH, CHAIN_HEIGHT,
     *                  ACCELERATE_LENGTH, SHOW_LEADS
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

            //The setting wasn't one of the defaults, so let's set chairswing specific ones.
            switch (key) {

                case "RADIUS": //integer, in number of blocks
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
                case "CHAIN_HEIGHT":
                    try{
                        chainHeight = Double.parseDouble(values[0]);
                        out = Messages.command_admin_ride_setting_GENERAL_success.replaceAll("\\{VALUE}",Double.toString(chainHeight));

                    } catch (NumberFormatException e) {
                        out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeDoub;
                    }
                    break;
                case "ACCELERATE_LENGTH":
                    try{
                        accelerateLength = Double.parseDouble(values[0]);
                        out = Messages.command_admin_ride_setting_GENERAL_success.replaceAll("\\{VALUE}",Double.toString(accelerateLength));

                    } catch (NumberFormatException e) {
                        out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeDoub;
                    }
                    break;
                case "SHOW_LEADS":
                    try{
                        showLeads = Boolean.parseBoolean(values[0]);
                        out = Messages.command_admin_ride_setting_GENERAL_success.replaceAll("\\{VALUE}",Boolean.toString(showLeads));

                    } catch (NumberFormatException e) {
                        out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeBool;
                    }
                    break;
                case "MAX_SWING_ANGLE":
                    try{
                        MaxSwingAngle = Math.toRadians(Double.parseDouble(values[0]));
                        out = Messages.command_admin_ride_setting_GENERAL_success.replaceAll("\\{VALUE}", Utils.formatDouble(Math.toDegrees(MaxSwingAngle),3));

                    } catch (NumberFormatException e) {
                        out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeDoub;
                    }
                    break;
            }
        }


        //If out is still empty, we didn't recognise the option key
        //if this isn't the case, save the changes
        if (out.equals("")) out = Messages.command_admin_ride_setting_GENERAL_fail_notFound;
        else RidesPlugin.getInstance().getConfigHandler().saveRideConfig(createConfig());


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
                        || capacity == 0
                        || radius == 0
                        || rotatespeed == 0
                        || length == 0
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
        out = out + Messages.command_admin_ride_info_chairswing;
        //leaving these here just incase they get put in the chairswing specific message too
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

        //Chairswing specific stuff
        if (radius==null || radius==0)  out = out.replaceAll("\\{RADIUS}","NOT SET");
        else out = out.replaceAll("\\{RADIUS}",Double.toString(radius));

        if (rotatespeed==null || rotatespeed==0) out = out.replaceAll("\\{ROTATE_SPEED}","NOT SET");
        else out = out.replaceAll("\\{ROTATE_SPEED}",Integer.toString(rotatespeed));

        if(length==null||length==0) out = out.replaceAll("\\{RIDE_LENGTH}","NOT SET");
        else out = out.replaceAll("\\{RIDE_LENGTH}",Integer.toString(length));

        if(accelerateLength==null) out = out.replaceAll("\\{ACCELERATE_LENGTH}","NOT SET");
        else out = out.replaceAll("\\{ACCELERATE_LENGTH}",Double.toString(accelerateLength));

        if(MaxSwingAngle==null) out = out.replaceAll("\\{MAX_SWING_ANGLE}","NOT SET");
        else out = out.replaceAll("\\{MAX_SWING_ANGLE}",Utils.formatDouble(Math.toDegrees(MaxSwingAngle),3));

        if(chainHeight==null) out = out.replaceAll("\\{CHAIN_HEIGHT}","NOT SET");
        else out = out.replaceAll("\\{CHAIN_HEIGHT}",Double.toString(chainHeight));

        out = out.replaceAll("\\{SHOW_LEADS}",Boolean.toString(showLeads));


        return out;
    }
}
