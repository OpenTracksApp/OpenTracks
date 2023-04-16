package de.dennisguse.opentracks.ui.util;

public class SpeedConversionUtil {
    // These Function converts KM to Miles and vice-versa.
    public static double convertKmToMiles(double kilometers) {
        double miles = kilometers / 1.60934;
        return miles;
    }
    public static double convertMilesToKm(double miles) {
        double kilometers = miles * 1.60934;
        return kilometers;
    }
}
