package de.dennisguse.opentracks.sensors;

import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import de.dennisguse.opentracks.data.models.AtmosphericPressure;
import de.dennisguse.opentracks.sensors.driver.BarometerInternal;

/**
 * NOTE: Test data is completely artificial.
 */
@RunWith(MockitoJUnitRunner.class)
public class AltitudeSumManagerTest {

    private static void addSensorValue(AltitudeSumManager altitudeSumManager, float[] values) {
        for (float f : values) {
            altitudeSumManager.onSensorValueChanged(AtmosphericPressure.ofHPA(f));
        }
    }

    @Mock
    BarometerInternal driver;

    @Test
    public void getAltitudeGainLoss_downhill() {
        // given
        AltitudeSumManager subject = new AltitudeSumManager(driver);

        when(driver.isConnected()).thenReturn(true);

        // then
        addSensorValue(subject, new float[]{1015f, 1015.01f, 1015.02f, 1015.03f, 1015.04f, 1015.05f, 1015.06f, 1015.07f, 1015.08f, 1015.09f, 1015.10f, 1015.11f, 1015.12f, 1015.13f, 1018f, 1018.1f, 1018.1f, 1018.1f, 1018.1f});

        // then
        Assert.assertEquals(0f, subject.getAltitudeGain_m(), 0.01);
        Assert.assertEquals(15f, subject.getAltitudeLoss_m(), 0.01);
    }

    @Test
    public void sensorUnavailable() {
        // given
        AltitudeSumManager subject = new AltitudeSumManager(driver);

        when(driver.isConnected()).thenReturn(true);

        // then
        subject.onSensorValueChanged(AtmosphericPressure.ofHPA(999f));

        // then
        Assert.assertNull(subject.getAltitudeGain_m());
        Assert.assertNull(subject.getAltitudeLoss_m());
    }
}