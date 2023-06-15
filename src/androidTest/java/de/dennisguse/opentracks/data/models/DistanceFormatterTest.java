package de.dennisguse.opentracks.data.models;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import de.dennisguse.opentracks.settings.UnitSystem;

//TODO Parametrized tests
@RunWith(AndroidJUnit4.class)
public class DistanceFormatterTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void testFormatDistance_metric() {
        DistanceFormatter formatter = DistanceFormatter.Builder()
                .setDecimalCount(2)
                .setUnit(UnitSystem.METRIC)
                .build(context);

        // A large number in metric
        assertEquals("5.00 km", formatter.formatDistance(Distance.of(5000)));
        // A small number in metric
        assertEquals("100.00 m", formatter.formatDistance(Distance.of(100)));
    }

    @Test
    public void testFormatDistance_imperial() {
        DistanceFormatter formatter = DistanceFormatter.Builder()
                .setDecimalCount(2)
                .setUnit(UnitSystem.IMPERIAL_FEET)
                .build(context);

        // A large number in imperial
        assertEquals("3.11 mi", formatter.formatDistance(Distance.of(5000)));
        // A small number in imperial
        assertEquals("328.08 ft", formatter.formatDistance(Distance.of(100)));
    }

    @Test
    public void testFormatDistance_nautical() {
        DistanceFormatter formatter = DistanceFormatter.Builder()
                .setDecimalCount(2)
                .setUnit(UnitSystem.NAUTICAL_IMPERIAL)
                .build(context);

        // A large number in nautical
        assertEquals("2.70 NM", formatter.formatDistance(Distance.of(5000)));
        // A small number in nautical
        assertEquals("328.08 ft", formatter.formatDistance(Distance.of(100)));
    }
}