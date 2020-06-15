package net.clownercraft.ccRides.utils;

import java.text.DecimalFormat;

public class Utils {

    /**
     * @return Format a double to a string, with given decimal places
     * @param val the value to format
     * @param decimalPlaces number of decimal places
     */
    public static String formatDouble(double val, int decimalPlaces){

        String format = "#.";

        for (int i=0;i<decimalPlaces;i++){
            format = format + "#";
        }
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(val);
    }
}
