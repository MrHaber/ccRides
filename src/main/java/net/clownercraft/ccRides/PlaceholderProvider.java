package net.clownercraft.ccRides;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.clownercraft.ccRides.config.Messages;
import net.clownercraft.ccRides.rides.Ride;
import org.bukkit.entity.Player;

public class PlaceholderProvider extends PlaceholderExpansion {
    private final RidesPlugin plugin;

    public PlaceholderProvider(RidesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public String getIdentifier() {
        return "ccRides";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier){
        if(player == null){
            return "";
        }

        //%ccRides_[rideID]_enabled%
        //%ccRides_[rideID]_running%
        //%ccRides_[rideID]_maxRiders%
        //%ccRides_[rideID]_numRiders%
        //%ccRides_[rideID]_numQueue%
        //%ccRides_[rideID]_price%

        String[] strings = identifier.split("_");
        String RideID = strings[0];
        String placeholder = strings[1];

        if (plugin.getConfigHandler().rides.containsKey(RideID)) {
            String out = "";
            Ride ride = plugin.getConfigHandler().rides.get(RideID);
            switch (placeholder) {
                case "enabled":
                    if (ride.isEnabled()) out = Messages.placeholder_enabled;
                    else out = Messages.placeholder_disabled;
                    break;
                case "running":
                    if (ride.isRunning()) out = Messages.placeholder_running;
                    else if (ride.isCountdownStarted()) out = Messages.placeholder_startingSoon;
                    else out = Messages.placeholder_waiting;
                    break;
                case "maxRiders":
                    out = Integer.toString(ride.getCapacity());
                    break;
                case "numRiders":
                    out = Integer.toString(ride.getNumRiders());
                    break;
                case "numQueue":
                    out = Integer.toString(ride.getNumQueue());
                    break;
                case "price":
                    out = Integer.toString(ride.getPrice());
                    break;
            }
            return out;
        } else {
            return Messages.placeholder_rideNotExist.replaceAll("\\{RIDE}",RideID);
        }
    }

}
