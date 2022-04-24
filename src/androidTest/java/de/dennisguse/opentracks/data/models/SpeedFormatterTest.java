package de.dennisguse.opentracks.data.models;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SpeedFormatterTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void testGetSpeedParts_pace_metric() {
        SpeedFormatter formatter = SpeedFormatter.Builder()
                .setDecimalCount(2)
                .setMetricUnits(true)
                .setReportSpeedOrPace(false)
                .build(context);

        assertEquals("4:59", formatter.getSpeedParts(Speed.of(3.34)).first);
        assertEquals("5:00", formatter.getSpeedParts(Speed.of(3.33)).first);

        assertEquals("min/km", formatter.getSpeedParts(Speed.zero()).second);
    }

    @Test
    public void testGetSpeedParts_pace_imperial() {
        SpeedFormatter formatter = SpeedFormatter.Builder()
                .setDecimalCount(2)
                .setMetricUnits(false)
                .setReportSpeedOrPace(false)
                .build(context);

//        assertEquals("TODO", formatter.getSpeedParts(Speed.of(3.34)).first);
//        assertEquals("TODO", formatter.getSpeedParts(Speed.of(3.33)).first);

        assertEquals("min/mi", formatter.getSpeedParts(Speed.zero()).second);
    }

    @Test
    public void testGetSpeedParts_speed_metric() {
        SpeedFormatter formatter = SpeedFormatter.Builder()
                .setDecimalCount(2)
                .setMetricUnits(true)
                .setReportSpeedOrPace(true)
                .build(context);

        assertEquals("11.9", formatter.getSpeedParts(Speed.of(3.31)).first);
        assertEquals("km/h", formatter.getSpeedParts(Speed.zero()).second);
    }

    @Test
    public void testGetSpeedParts_speed_imperial() {
        SpeedFormatter formatter = SpeedFormatter.Builder()
                .setDecimalCount(2)
                .setMetricUnits(false)
                .setReportSpeedOrPace(true)
                .build(context);

        assertEquals("7.5", formatter.getSpeedParts(Speed.of(3.34)).first);
        assertEquals("mph", formatter.getSpeedParts(Speed.zero()).second);
    }

    @Test
    public void testFormatSpeed() {
        SpeedFormatter formatter = SpeedFormatter.Builder()
                .setDecimalCount(2)
                .setMetricUnits(true)
                .setReportSpeedOrPace(false)
                .build(context);

        assertEquals("4:59 min/km", formatter.formatSpeed(Speed.of(3.34)));
    }
}