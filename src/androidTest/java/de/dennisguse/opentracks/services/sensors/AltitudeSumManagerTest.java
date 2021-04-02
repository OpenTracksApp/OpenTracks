package de.dennisguse.opentracks.services.sensors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * NOTE: Test data is completely artificial.
 */
public class AltitudeSumManagerTest {

    private final AltitudeSumManager subject = new AltitudeSumManager();

    private static void addSensorValue(AltitudeSumManager altitudeSumManager, float[] values) {
        for (float f : values) {
            altitudeSumManager.onSensorValueChanged(f);
        }
    }

    @Before
    public void setUp() {
        subject.reset();
    }

    @Test
    public void getAltitudeGainLoss_downhill() {
        // given
        subject.setConnected(true);

        // then
        addSensorValue(subject, new float[]{1015f, 1015.01f, 1015.02f, 1015.03f, 1015.04f, 1015.05f, 1015.06f, 1015.07f, 1015.08f, 1015.09f, 1015.10f, 1015.11f, 1015.12f, 1015.13f, 1015.14f, 1015.15f});

        // then
        Assert.assertEquals(0f, subject.getAltitudeGain_m(), 0.01);
        Assert.assertEquals(48.0, subject.getAltitudeLoss_m(), 0.01);
    }

    @Test
    public void sensorUnavailable() {
        // given
        subject.setConnected(false);

        // then
        subject.onSensorValueChanged(999f);

        // then
        Assert.assertNull(subject.getAltitudeGain_m());
        Assert.assertNull(subject.getAltitudeLoss_m());
    }
}