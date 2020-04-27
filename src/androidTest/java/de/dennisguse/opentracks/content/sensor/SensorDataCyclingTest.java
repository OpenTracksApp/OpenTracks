package de.dennisguse.opentracks.content.sensor;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.dennisguse.opentracks.util.UintUtils;

@RunWith(AndroidJUnit4.class)
public class SensorDataCyclingTest {

    @Test
    public void compute_cadence() {
        // given
        SensorDataCycling.Cadence previous = new SensorDataCycling.Cadence("sensorAddress", "sensorName", 1, 1024); // 1s
        SensorDataCycling.Cadence current = new SensorDataCycling.Cadence("sensorAddress", "sensorName", 2, 2048); // 2s

        // when
        current.compute(previous);

        // then
        Assert.assertEquals(60, current.getCadence_rpm(), 0.01);
    }

    @Test
    public void compute_cadence_sameCount() {
        // given
        SensorDataCycling.Cadence previous = new SensorDataCycling.Cadence("sensorAddress", "sensorName", 1, 1024);
        SensorDataCycling.Cadence current = new SensorDataCycling.Cadence("sensorAddress", "sensorName", 1, 2048);

        // when
        current.compute(previous);

        // then
        Assert.assertEquals(0, current.getCadence_rpm(), 0.01);
    }


    @Test
    public void compute_cadence_sameTime() {
        // given
        SensorDataCycling.Cadence previous = new SensorDataCycling.Cadence("sensorAddress", "sensorName", 1, 1024);
        SensorDataCycling.Cadence current = new SensorDataCycling.Cadence("sensorAddress", "sensorName", 2, 1024);

        // when
        current.compute(previous);

        // then
        Assert.assertFalse(current.hasCadence_rpm());
    }

    @Test
    public void compute_cadence_rollOverTime() {
        // given
        SensorDataCycling.Cadence previous = new SensorDataCycling.Cadence("sensorAddress", "sensorName", 1, UintUtils.UINT16_MAX - 1024);
        SensorDataCycling.Cadence current = new SensorDataCycling.Cadence("sensorAddress", "sensorName", 2, 0);

        // when
        current.compute(previous);

        // then
        Assert.assertEquals(60, current.getCadence_rpm(), 0.01);
    }

    @Test
    public void compute_cadence_rollOverCount() {
        // given
        SensorDataCycling.Cadence previous = new SensorDataCycling.Cadence("sensorAddress", "sensorName", UintUtils.UINT32_MAX - 1, 1024);
        SensorDataCycling.Cadence current = new SensorDataCycling.Cadence("sensorAddress", "sensorName", 0, 2048);

        // when
        current.compute(previous);

        // then
        Assert.assertEquals(60, current.getCadence_rpm(), 0.01);
    }

    @Test
    public void compute_speed_rollOverCount() {
        // given
        SensorDataCycling.Speed previous = new SensorDataCycling.Speed("sensorAddress", "sensorName", UintUtils.UINT16_MAX - 1, 1024);
        SensorDataCycling.Speed current = new SensorDataCycling.Speed("sensorAddress", "sensorName", 0, 2048);

        // when
        current.compute(previous, 2000);

        // then
        Assert.assertEquals(2, current.getSpeed_mps(), 0.01);
    }
}