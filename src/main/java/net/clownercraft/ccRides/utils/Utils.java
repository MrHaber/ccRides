package net.clownercraft.ccRides.utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    /**
     * Used for filtering tab-complete results based on what the user started typing
     * @param list the full list to filter
     * @param regex regex to filter by
     * @return the filtered list
     */
    public static ArrayList<String> filterList(ArrayList<String> list, String regex) {
        Pattern filter = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        return list.stream()
                .filter(filter.asPredicate()).collect(Collectors.toCollection(ArrayList::new));
    }
}
