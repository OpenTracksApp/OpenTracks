package de.dennisguse.opentracks.data.models;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DistanceFormatterTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void testFormatDistance() {
        DistanceFormatter formatter = DistanceFormatter.Builder()
                .setDecimalCount(2)
                .build(context);

        // A large number in metric
        assertEquals("5.00 km", formatter.formatDistance(Distance.of(5000), true));
        // A large number in imperial
        assertEquals("3.11 mi", formatter.formatDistance(Distance.of(5000), false));
        // A small number in metric
        assertEquals("100.00 m", formatter.formatDistance(Distance.of(100), true));
        // A small number in imperial
        assertEquals("328.08 ft", formatter.formatDistance(Distance.of(100), false));
    }
}