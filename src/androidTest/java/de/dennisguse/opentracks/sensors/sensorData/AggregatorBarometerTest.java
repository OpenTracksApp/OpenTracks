package de.dennisguse.opentracks.sensors.sensorData;

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;

import de.dennisguse.opentracks.data.models.AtmosphericPressure;

public class AggregatorBarometerTest {

    private static void addSensorValue(AggregatorBarometer aggregatorBarometer, float[] values) {
        for (float f : values) {
            aggregatorBarometer.add(new Raw<>(Instant.MIN, AtmosphericPressure.ofHPA(f)));
        }
    }

    @Test
    public void getAltitudeGainLoss_downhill() {
        // given
        AggregatorBarometer subject = new AggregatorBarometer("test", null);

        // then
        addSensorValue(subject, new float[]{1015f, 1015.01f, 1015.02f, 1015.03f, 1015.04f, 1015.05f, 1015.06f, 1015.07f, 1015.08f, 1015.09f, 1015.10f, 1015.11f, 1015.12f, 1015.13f, 1018f, 1018.1f, 1018.1f, 1018.1f, 1018.1f});

        // then
        Assert.assertEquals(0f, subject.aggregatedValue.gain_m(), 0.01);
        Assert.assertEquals(15f, subject.aggregatedValue.loss_m(), 0.01);
    }
}