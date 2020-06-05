package net.clownercraft.ccRides;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class PlaceholderProvider extends PlaceholderExpansion {
    private RidesPlugin plugin;

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
        //%ccRides_[placeholder]%
        //TODO Decide on some placeholders

//        //%ccTokens_balance%
//        if (identifier.equals("balance")) {
//            try{
//                int bal = TokenEconomy.getBalance(player.getUniqueId());
//                return Integer.toString(bal);
//            } catch (Exception e) {
//                e.printStackTrace();
//                return "Error";
//            }
//
//        }

        return null;
    }

}
