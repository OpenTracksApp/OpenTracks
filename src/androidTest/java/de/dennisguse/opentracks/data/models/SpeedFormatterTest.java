package de.dennisguse.opentracks.data.models;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import de.dennisguse.opentracks.settings.UnitSystem;

//TODO Use make parametrized tests?
@RunWith(AndroidJUnit4.class)
public class SpeedFormatterTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void testGetSpeedParts_pace_metric() {
        SpeedFormatter formatter = SpeedFormatter.Builder()
                .setDecimalCount(2)
                .setUnit(UnitSystem.METRIC)
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
                .setUnit(UnitSystem.IMPERIAL_FEET)
                .setReportSpeedOrPace(false)
                .build(context);

        assertEquals("8:02", formatter.getSpeedParts(Speed.of(3.34)).first);
        assertEquals("min/mi", formatter.getSpeedParts(Speed.zero()).second);
    }

    @Test
    public void testGetSpeedParts_pace_nautical() {
        SpeedFormatter formatter = SpeedFormatter.Builder()
                .setDecimalCount(2)
                .setUnit(UnitSystem.NAUTICAL_IMPERIAL)
                .setReportSpeedOrPace(false)
                .build(context);

        assertEquals("9:14", formatter.getSpeedParts(Speed.of(3.34)).first);
        assertEquals("min/NM", formatter.getSpeedParts(Speed.zero()).second);
    }

    @Test
    public void testGetSpeedParts_speed_metric() {
        SpeedFormatter formatter = SpeedFormatter.Builder()
                .setDecimalCount(2)
                .setUnit(UnitSystem.METRIC)
                .setReportSpeedOrPace(true)
                .build(context);

        assertEquals("11.9", formatter.getSpeedParts(Speed.of(3.31)).first);
        assertEquals("km/h", formatter.getSpeedParts(Speed.zero()).second);
    }

    @Test
    public void testGetSpeedParts_speed_imperial() {
        SpeedFormatter formatter = SpeedFormatter.Builder()
                .setDecimalCount(2)
                .setUnit(UnitSystem.IMPERIAL_FEET)
                .setReportSpeedOrPace(true)
                .build(context);

        assertEquals("7.5", formatter.getSpeedParts(Speed.of(3.34)).first);
        assertEquals("mph", formatter.getSpeedParts(Speed.zero()).second);
    }

    @Test
    public void testGetSpeedParts_speed_nautical() {
        SpeedFormatter formatter = SpeedFormatter.Builder()
                .setDecimalCount(2)
                .setUnit(UnitSystem.NAUTICAL_IMPERIAL)
                .setReportSpeedOrPace(true)
                .build(context);

        assertEquals("6.5", formatter.getSpeedParts(Speed.of(3.34)).first);
        assertEquals("kn", formatter.getSpeedParts(Speed.zero()).second);
    }

    @Test
    public void testFormatSpeed() {
        SpeedFormatter formatter = SpeedFormatter.Builder()
                .setDecimalCount(2)
                .setUnit(UnitSystem.METRIC)
                .setReportSpeedOrPace(false)
                .build(context);

        assertEquals("4:59 min/km", formatter.formatSpeed(Speed.of(3.34)));
    }
}