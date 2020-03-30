package de.dennisguse.opentracks.services.sensors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * NOTE: Test data is completely artificial.
 */
public class ElevationSumManagerTest {

    private ElevationSumManager elevationSumManager = new ElevationSumManager();

    private static void addSensorValue(ElevationSumManager elevationSumManager, float[] values) {
        for (float f : values) {
            elevationSumManager.onSensorValueChanged(f);
        }
    }

    @Before
    public void setUp() {
        elevationSumManager.reset();
    }

    @Test
    public void getElevationGainLoss_downhill() {
        // then
        addSensorValue(elevationSumManager, new float[]{1015f, 1015.01f, 1015.02f, 1015.03f, 1015.04f, 1015.05f, 1015.06f, 1015.07f, 1015.08f, 1015.09f, 1015.10f, 1015.11f, 1015.12f, 1015.13f, 1015.14f, 1015.15f});

        // then
        Assert.assertEquals(0f, elevationSumManager.getElevationGain_m(), 0.01);
        Assert.assertEquals(-1.08, elevationSumManager.getElevationLoss_m(), 0.01);
    }
}