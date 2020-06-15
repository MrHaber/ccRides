package net.clownercraft.ccRides.rides;

import net.clownercraft.ccRides.config.Messages;
import net.clownercraft.ccRides.RidesPlugin;
import net.clownercraft.ccRides.utils.Utils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Jets extends Ride {
    Double armLength;//the radius of the jets seats
    double centerRadius = 0;
    Integer rotatespeed; //number of ticks per full rotation of the jets
    Double accelerateLength = 0.3d;
    Integer length; //number of full rotations per ride.

    Double angleMax = Math.toRadians(30.0); //The maximum angle of each 'jet' in radians
    Double angleSpeed = Math.toRadians(0.2); //The angle change per tick in radians

    boolean showLeads = true;
    boolean showBanners = true;

    Material evenDecoration = Material.RED_BANNER;
    Material oddDecoration = Material.BLUE_BANNER;

    /*
    Running data
     */
    Double currentRotation = 0.0; //Current rotation of the jets in radians
    double currRotStep = 0.005; //Current rotation step size, used to allow for acceleration
    ArrayList<Double> seatAngles = new ArrayList<>();
    BukkitTask updateTask;
    boolean canAdjustHeight = false;

    /*
    Extra Entities
     */
    ArrayList<Entity> leashHolders = new ArrayList<>();
    ArrayList<Entity> leashEnds = new ArrayList<>();
    ArrayList<ArmorStand> decoration1 = new ArrayList<>();
    ArrayList<ArmorStand> decoration2 = new ArrayList<>();


    /**
     * Create a Jets based on an existing config
     * @param conf the config to read
     */
    public Jets(YamlConfiguration conf) {
        //Set the type to JETS
        super.type = "JETS";

        //load generic options
        super.setRideOptions(conf);

        //load jets specific options
        armLength = conf.getDouble("Jets.ArmLength");
        centerRadius = conf.getDouble("Jets.CentreRadius");
        rotatespeed = conf.getInt("Jets.Rotation.Ticks_Per_Full_Rotation");
        length = conf.getInt("Jets.Rotation.Num_Cycles");
        angleMax = conf.getDouble("Jets.SeatAngles.Maximum");
        angleSpeed = conf.getDouble("Jets.SeatAngles.ChangePerSecond");
        showLeads = conf.getBoolean("Jets.Decoration.ShowLeads");
        showBanners = conf.getBoolean("Jets.Decoration.ShowBanners");
        oddDecoration = Material.getMaterial(Objects.requireNonNull(conf.getString("Jets.Decoration.OddMaterial", oddDecoration.name())));
        evenDecoration = Material.getMaterial(Objects.requireNonNull(conf.getString("Jets.Decoration.EvenMaterial", evenDecoration.name())));
        accelerateLength = conf.getDouble("Jets.Rotation.AccelerateLength");

        if (enabled) enable();
    }

    /**
     * Create a new Jets, with no settings
     * @param name the ID/Name for this ride
     */
    public Jets(String name) {
        //Setyp basic settings
        super.type = "JETS";
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
        //RidesPlugin.getInstance().getLogger().info("Starting Jets " + ID);
        double rotationStep = 2*Math.PI / rotatespeed;

        double rotationAccel,lowerSpeed;
        if (accelerateLength<=0.05) {
            rotationAccel = rotationStep;
            lowerSpeed = angleMax;
        } else {
            rotationAccel = rotationStep / (2 * rotatespeed * accelerateLength);
            lowerSpeed = angleMax / (1.75 * accelerateLength * rotatespeed);
        }

        updateTask = Bukkit.getScheduler().runTaskTimer(RidesPlugin.getInstance(), () -> {
            currentRotation += currRotStep;

            if (currentRotation <= Math.PI*2*accelerateLength) {
                currRotStep += rotationAccel;

                if (currRotStep > rotationStep) currRotStep = rotationStep;
            }

            if (currentRotation >= Math.PI*2*(length-accelerateLength)) {
                if (canAdjustHeight) canAdjustHeight = false;
                currRotStep -= rotationAccel;

                //lower all seats on slow down
                for (int i=0;i<seatAngles.size();i++) {
                    double angle = seatAngles.get(i);
                    angle -= lowerSpeed;
                    if (angle<0) angle = 0;
                    seatAngles.set(i,angle);
                }


                if (currRotStep < 0.005) currRotStep = 0.005;
            }

            tickPositions();
            if (currentRotation>=2*Math.PI*length) stopRide();

        },1l,1l);
        running = true;
        canAdjustHeight = true;
        countdownStarted = false;
    }

    public void stopRide() {
        //Cancel movement & reset Positions
        if (updateTask!=null) updateTask.cancel();

        currentRotation = 0.0;
        for (int i=0;i<seatAngles.size();i++) {
            seatAngles.set(i,0.0);
        }

        tickPositions();

        countdownStarted = false;
        running = false;

        //Eject Players
        for (Player p:riders.keySet()) {
            ejectPlayer(p);
        }


        Bukkit.getScheduler().runTaskLater(RidesPlugin.getInstance(), () -> { if (enabled) checkQueue();},10l);
    }

    /**
     * Put a player onto the ride
     * Will add to the queue if the ride is full or already running
     * Will do nothing if the ride is disabled.
     *
     * @param player the player to add
     */
    @Override
    public void addPlayer(Player player) {
        super.addPlayer(player);

        //Tell player how to control height and set their slot to the mid slot
        if (riders.containsKey(player)) {
            player.getInventory().setHeldItemSlot(4);
            Bukkit.getScheduler().runTaskLater(
                    RidesPlugin.getInstance(),()->{
                        player.spigot().sendMessage(
                                ChatMessageType.ACTION_BAR,
                                TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',Messages.ride_jets_controlMessage)));
            },20);
        }

    }

    /**
     * Triggers the ride to set the position of all it's vehicles,
     */
    @Override
    public void tickPositions() {
        super.tickPositions();
        for (int i=0;i<seats.size();i++) {
            Location loc = getPosition(i);
            if (showLeads) {
                teleportWithPassenger(leashEnds.get(i),loc.clone().subtract(0,1,0));
                teleportWithPassenger(leashHolders.get(i), getCentrePosition(i));
            }
            if (showBanners) {
                Location standLoc = getDecorPos(i).clone().subtract(0,0.25,0);
                teleportWithPassenger(decoration1.get(i),standLoc);
                teleportWithPassenger(decoration2.get(i),standLoc);
                decoration1.get(i).setRotation(standLoc.getYaw(),0);
                decoration2.get(i).setRotation(standLoc.getYaw()+180,0);
            }

        }

    }

    /**
     * Forces the ride to remove all it's vehicles/seats and respawn them.
     */
    @Override
    public void respawnSeats() {
        seatAngles.clear();
        for (int i = 0; i< capacity; i++){
            seatAngles.add(0.0);
        }

        super.respawnSeats();


        for (int i = 0; i< capacity; i++){
            Location loc2 = getPosition(i);


            //If leads enabled, spawn lead end entity
            if (showLeads) {
                Location leadLoc = loc2.clone().subtract(0,1,0);
                Location leadHoldLoc = getCentrePosition(i);

                for (Entity e:leadHoldLoc.getWorld().getNearbyEntities(leadHoldLoc,3,3,3)) {
                    if (e.getType().equals(EntityType.PARROT)
                            && !leashEnds.contains(e) && !leashHolders.contains(e)) e.remove();
                }

                Parrot leadHold = (Parrot) leadHoldLoc.getWorld().spawnEntity(leadHoldLoc,EntityType.PARROT);
                leadHold.setInvulnerable(true);
                leadHold.setGravity(false);
                leadHold.setAI(false);
                leadHold.setVelocity(new Vector(0, 0, 0));
                leadHold.setSilent(true);
                leadHold.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,Integer.MAX_VALUE,1,false,false));
                leadHold.setCustomName("Jets Parrot");
                leadHold.setCustomNameVisible(false);

                leashHolders.add(leadHold);

                for (Entity e:leadLoc.getWorld().getNearbyEntities(leadLoc,3,3,3)) {
                    if (e.getType().equals(EntityType.PARROT)
                            && !leashEnds.contains(e) && !leashHolders.contains(e)) e.remove();
                }

                Parrot leadEnd = (Parrot) leadLoc.getWorld().spawnEntity(leadLoc,EntityType.PARROT);
                leadEnd.setInvulnerable(true);
                leadEnd.setGravity(false);
                leadEnd.setAI(false);
                leadEnd.setVelocity(new Vector(0, 0, 0));
                leadEnd.setSilent(true);
                leadEnd.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,Integer.MAX_VALUE,1,false,false));
                leadEnd.setCustomName("Jets Parrot");
                leadEnd.setCustomNameVisible(false);

                leadEnd.setLeashHolder(leadHold);
                leashEnds.add(leadEnd);


            }

            //if banner decoration enabled, spawn armor stands
            if (showBanners) {
                ArmorStand stand1,stand2;
                Location standloc = getDecorPos(i).clone().subtract(0,0.25,0);
                for (Entity e:standloc.getWorld().getNearbyEntities(loc2,3,3,3)) {
                    if (e.getType().equals(EntityType.ARMOR_STAND)
                            && !decoration1.contains(e) && !decoration2.contains(e)) e.remove();
                }

                stand1 = (ArmorStand) standloc.getWorld().spawnEntity(standloc,EntityType.ARMOR_STAND);
                stand2 = (ArmorStand) standloc.getWorld().spawnEntity(standloc,EntityType.ARMOR_STAND);


                stand1.setGravity(false);
                stand1.setInvulnerable(true);
                stand1.setVisible(false);
                stand1.setSmall(true);
                if (i%2==0) stand1.getEquipment().setHelmet(new ItemStack(evenDecoration));
                else stand1.getEquipment().setHelmet(new ItemStack(oddDecoration));
                stand1.setRotation(standloc.getYaw(),0);
                stand1.setHeadPose(new EulerAngle(Math.toRadians(-95),0,0));

                stand2.setGravity(false);
                stand2.setInvulnerable(true);
                stand2.setVisible(false);
                stand2.setSmall(true);
                if (i%2==0) stand2.getEquipment().setHelmet(new ItemStack(evenDecoration));
                else stand2.getEquipment().setHelmet(new ItemStack(oddDecoration));
                stand2.setRotation(standloc.getYaw()+180,0);
                stand2.setHeadPose(new EulerAngle(Math.toRadians(-95),0,0));

                decoration1.add(stand1);
                decoration2.add(stand2);
            }

        }

    }

    /**
     * despawns all this rides minecarts, to prevent duplicates appearing after a shutdown
     */
    @Override
    public void despawnSeats() {
        super.despawnSeats();

        for (Entity e:leashHolders) {
            e.remove();
        }
        leashHolders.clear();

        for (Entity e:leashEnds) {
            e.remove();
        }
        leashEnds.clear();

        for (ArmorStand e:decoration1) {
            e.remove();
        }
        decoration1.clear();

        for (ArmorStand e:decoration2) {
            e.remove();
        }
        decoration2.clear();

    }

    /**
     * gets the position of a seat at the current point in the ride cycle
     *
     * @param seatNum = the index of the seat to check
     * @return = the location the seat should currently be
     */
    public Location getPosition(int seatNum) {
        double angle = currentRotation + (seatNum/(double) capacity)*2*Math.PI;
        double seatAngle = 0;
        if (seatAngles.size()>seatNum) {
            seatAngle = seatAngles.get(seatNum);
        }

        Location locSeat = getCentrePosition(seatNum).clone();

        double x2,y2,z2,c;
        c = Math.sqrt(armLength * armLength - (armLength * Math.sin(seatAngle))*(armLength * Math.sin(seatAngle)));
        x2 = c * Math.cos(angle);
        y2 = armLength * Math.sin(seatAngle);
        z2 = c * Math.sin(angle);

        locSeat.add(x2,y2,z2);

        locSeat.setPitch(0);
        locSeat.setYaw((float) Math.toDegrees(angle)+90);

        return locSeat;
    }

    /**
     * gets the position of the central point for that seat.
     * ie. seats leash holder and the point of rotation for vertical movement
     *
     * @param seatNum = the index of the seat to check
     * @return = the location the seat should currently be
     */
    public Location getCentrePosition(int seatNum) {
        double angle = currentRotation + (seatNum/(double) capacity)*2*Math.PI;

        Location locTop = baseLocation.clone();
        double x1,y1,z1;

        x1 = centerRadius * Math.cos(angle);
        z1 = centerRadius * Math.sin(angle);
        y1 = 0;

        locTop.add(x1,y1,z1);
        locTop.setYaw((float) Math.toDegrees(angle));


        return locTop;
    }

    /**
     * gets the position of a seats decoration armor stands
     *
     * @param seatNum = the index of the seat to check
     * @return = the location the seat should currently be
     */
    public Location getDecorPos(int seatNum) {
        double angle = currentRotation + (seatNum/(double) capacity)*2*Math.PI;
        angle -= 0.3/ armLength;

        double seatAngle = 0;
        if (seatAngles.size()>seatNum) {
            seatAngle = seatAngles.get(seatNum);
        }

        Location locTop = getCentrePosition(seatNum).clone();

        Location locSeat = locTop.clone();

        double x2,y2,z2,c;
        c = Math.sqrt(armLength * armLength - (armLength * Math.sin(seatAngle))*(armLength * Math.sin(seatAngle)));
        x2 = c * Math.cos(angle);
        y2 = armLength * Math.sin(seatAngle);
        z2 = c * Math.sin(angle);

        locSeat.add(x2,y2,z2);

        locSeat.setPitch(0);
        locSeat.setYaw((float) Math.toDegrees(angle)+90);

        return locSeat;
    }

    /**
     * @return A FileConfiguration containing all of this Jets's settings
     */
    @Override
    public FileConfiguration createConfig() {
        //Get generic options
        FileConfiguration out = super.createConfig();

        //Add Jets Specific Options
        try{
            out.set("Jets.ArmLength", armLength);
            out.set("Jets.CentreRadius",centerRadius);
            out.set("Jets.Rotation.Ticks_Per_Full_Rotation",rotatespeed);
            out.set("Jets.Rotation.Num_Cycles",length);
            out.set("Jets.SeatAngles.Maximum",angleMax);
            out.set("Jets.SeatAngles.ChangePerSecond",angleSpeed);
            out.set("Jets.Decoration.ShowLeads",showLeads);
            out.set("Jets.Decoration.ShowBanners",showBanners);
            out.set("Jets.Decoration.OddMaterial",oddDecoration.name());
            out.set("Jets.Decoration.EvenMaterial",evenDecoration.name());
            out.set("Jets.Rotation.AccelerateLength",accelerateLength);

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
        //Add Jets Specific Options
        out.add("ARM_LENGTH");
        out.add("CENTER_RADIUS");
        out.add("ROTATE_SPEED");
        out.add("RIDE_LENGTH");
        out.add("ACCELERATE_LENGTH");
        out.add("ANGLE_MAX");
        out.add("ANGLE_STEP");
        out.add("SHOW_LEADS");
        out.add("SHOW_BANNERS");
        out.add("DECOR_MATERIAL_ODD");
        out.add("DECOR_MATERIAL_EVEN");


        return out;
    }

    /**
     * @param key    the key of the setting
     *               Jets Options are:
     *                  RADIUS, ROTATE_SPEED, RIDE_LENGTH, ANGLE_MAX, ANGLE_SPEED,
     *                  SHOW_LEADS, SHOW_BANNERS, CONTROL_MATERIAL
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

            //The setting wasn't one of the defaults, so let's set jets specific ones.
            switch (key) {

                case "ARM_LENGTH": //integer, in number of blocks
                    try{
                        armLength = Double.parseDouble(values[0]);
                        out = Messages.command_admin_ride_setting_GENERAL_success_blocks.replaceAll("\\{VALUE}",Double.toString(armLength));
                    } catch (NumberFormatException e) {
                        out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeDoubBlocks;
                    }
                    break;
                case "CENTER_RADIUS": //integer, in number of blocks
                    try{
                        centerRadius = Double.parseDouble(values[0]);
                        out = Messages.command_admin_ride_setting_GENERAL_success_blocks.replaceAll("\\{VALUE}",Double.toString(centerRadius));
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
                case "ANGLE_MAX": //double, the max +/- height variation in blocks
                    try{
                        angleMax = Math.toRadians(Double.parseDouble(values[0]));
                        out = Messages.command_admin_ride_setting_GENERAL_success_degree.replaceAll("\\{VALUE}",Utils.formatDouble(Math.toDegrees(angleMax),3));
                    } catch (NumberFormatException e) {
                        out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeDoubDegrees;
                    }
                    break;
                case "ANGLE_STEP": //double, the number of full height cycles per rotation
                    try{
                        angleSpeed = Math.toRadians(Double.parseDouble(values[0]));
                        out = Messages.command_admin_ride_setting_GENERAL_success_degree.replaceAll("\\{VALUE}",Utils.formatDouble(Math.toDegrees(angleSpeed),3));
                    } catch (NumberFormatException e) {
                        out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeDoubDegrees;
                    }
                    break;
                case "SHOW_LEADS": //boolean
                    showLeads = Boolean.parseBoolean(values[0]);
                    out = Messages.command_admin_ride_setting_GENERAL_success.replaceAll("\\{VALUE}",Boolean.toString(showLeads));
                    break;
                case "SHOW_BANNERS": //boolean
                    showBanners = Boolean.parseBoolean(values[0]);
                    out = Messages.command_admin_ride_setting_GENERAL_success.replaceAll("\\{VALUE}",Boolean.toString(showBanners));
                    break;
                case "ACCELERATE_LENGTH":
                    try{
                        accelerateLength = Double.parseDouble(values[0]);
                        out = Messages.command_admin_ride_setting_GENERAL_success.replaceAll("\\{VALUE}",Double.toString(accelerateLength));

                    } catch (NumberFormatException e) {
                        out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeDoub;
                    }
                    break;
                case "DECOR_MATERIAL_ODD":
                    Material matO = Material.getMaterial(values[0]);
                    if (matO != null && matO != Material.AIR) {
                        oddDecoration = matO;
                        out = Messages.command_admin_ride_setting_GENERAL_success.replaceAll("\\{VALUE}",matO.name());
                    } else {
                        out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeMaterial;
                    }
                    break;
                case "DECOR_MATERIAL_EVEN":
                    Material matE = Material.getMaterial(values[0]);
                    if (matE != null && matE != Material.AIR) {
                        evenDecoration = matE;
                        out = Messages.command_admin_ride_setting_GENERAL_success.replaceAll("\\{VALUE}",matE.name());
                    } else {
                        out = Messages.command_admin_ride_setting_GENERAL_fail_mustBeMaterial;
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
                || armLength == null
                || rotatespeed == null
                || length == null
                || angleMax == null
                || angleSpeed == null
                || capacity == 0
                || armLength == 0
                || rotatespeed == 0
                || length == 0
                || angleSpeed == 0
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
        out = out + Messages.command_admin_ride_info_jets;

        //leaving these here just incase they get put in the jets specific message too
        out = out.replaceAll("\\{ID}", rideID)
                .replaceAll("\\{ENABLED}",Boolean.toString(enabled))
                .replaceAll("\\{PRICE}",Integer.toString(price))
                .replaceAll("\\{RUNNING}",Boolean.toString(running))
                .replaceAll("\\{RIDER_COUNT}",Integer.toString(riders.size()))
                .replaceAll("\\{QUEUE_COUNT}",Integer.toString(queue.size()))
                .replaceAll("\\{START_PLAYERS}",Integer.toString(minStartPlayers))
                .replaceAll("\\{START_DELAY}",Integer.toString(startWaitTime))
                .replaceAll("\\{JOIN_AFTER_START}",Boolean.toString(joinAfterStart));

        if (capacity ==null|| capacity ==0) out = out.replaceAll("\\{CAPACITY}","NOT SET");
        else out = out.replaceAll("\\{CAPACITY}",Integer.toString(capacity));

        String exit,base;
        if (exitLocation ==null) exit = "NOT SET"; else exit = exitLocation.getWorld().getName() + " x"+ exitLocation.getX() + " y"+ exitLocation.getY() + " z" + exitLocation.getZ();
        if (baseLocation ==null) base = "NOT SET"; else base = baseLocation.getWorld().getName() + " x"+ baseLocation.getX() + " y"+ baseLocation.getY() + " z" + baseLocation.getZ();
        out = out.replaceAll("\\{EXIT_LOCATION}",exit)
                .replaceAll("\\{BASE_LOCATION}",base);

        //Jets specific stuff
        if (armLength ==null || armLength ==0)  out = out.replaceAll("\\{ARM_LENGTH}","NOT SET");
            else out = out.replaceAll("\\{ARM_LENGTH}",Double.toString(armLength));

        if (rotatespeed==null || rotatespeed==0) out = out.replaceAll("\\{ROTATE_SPEED}","NOT SET");
        else out = out.replaceAll("\\{ROTATE_SPEED}",Integer.toString(rotatespeed));

        if(length==null||length==0) out = out.replaceAll("\\{RIDE_LENGTH}","NOT SET");
        else out = out.replaceAll("\\{RIDE_LENGTH}",Integer.toString(length));

        if(angleSpeed==null||angleSpeed==0) out = out.replaceAll("\\{ANGLE_STEP}","NOT SET");
        else out = out.replaceAll("\\{ANGLE_STEP}", Utils.formatDouble(Math.toDegrees(angleSpeed),3));

        out = out.replaceAll("\\{ANGLE_MAX}",Utils.formatDouble(Math.toDegrees(angleMax),3))
                .replaceAll("\\{CENTER_RADIUS}",Double.toString(centerRadius))
                .replaceAll("\\{SHOW_LEADS}",Boolean.toString(showLeads))
                .replaceAll("\\{SHOW_BANNERS}",Boolean.toString(showBanners))
                .replaceAll("\\{ACCELERATE_LENGTH}",Double.toString(accelerateLength))
                .replaceAll("\\{DECOR_MATERIAL_ODD}",oddDecoration.name())
                .replaceAll("\\{DECOR_MATERIAL_EVEN}",evenDecoration.name());

        return out;
    }


    /*    EVENT LISTENERS     */

    /**
     * Detect player changing their hotbar slot to control the seat height.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMouseScroll(PlayerItemHeldEvent event) {
        if (riders.containsKey(event.getPlayer())){

            int oldslot = event.getPreviousSlot();
            int newslot = event.getNewSlot();
            int jump = newslot-oldslot;
            event.setCancelled(true);

            if (canAdjustHeight) {
                if (oldslot==8 && newslot == 0) {
                    jump = 1;
                } else if (oldslot==0 && newslot == 8) {
                    jump = -1;
                }
                int seatnum = riders.get(event.getPlayer());
                double angle = seatAngles.get(seatnum);
                angle += jump * angleSpeed;
                if (angle>angleMax) angle = angleMax;
                if (angle<0) angle = 0;

                seatAngles.set(seatnum,angle);
            }

        }
    }


    /**
     * Prevent players taking/swapping items from armor stands.
     */
    @EventHandler
    public void onStandEdit(PlayerInteractEntityEvent e) {
        if (e.getRightClicked() instanceof ArmorStand
            && (decoration1.contains((ArmorStand) e.getRightClicked()) || decoration2.contains((ArmorStand) e.getRightClicked()))) {
            e.setCancelled(true);
        }
    }
}
