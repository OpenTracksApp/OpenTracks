package de.dennisguse.opentracks.ui.util;
/**
 * @author Aniket Tailor
 * This class provides utility functions for displaying weather information in different units.
 */
public class WeatherUtils {

    /**
     * @author Aniket Tailor
     * @param temperature
     * @return Temperature in celsius
     */
    public static double convertFahrenheitToCelsius(double temperature) {
        double celsius = (temperature - 32) * 5/9;
        return celsius;
    }

    /**
     * @author Aniket Tailor
     * @param temperature
     * @return Temperature in fahrenheit
     */
    public static double convertCelsiusToFahrenheit(double temperature) {
        double fahrenheit = (temperature * 9/5) + 32;
        return fahrenheit;
    }

    /**
     * @author Aniket Tailor
     * @param temperature
     * @return Temperature in string format
     */
    public static String formatTemperature(double temperature) {
        return String.format("%.1f°C", temperature);
    }
}
