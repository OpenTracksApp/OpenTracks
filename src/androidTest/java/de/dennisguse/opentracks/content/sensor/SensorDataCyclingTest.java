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
        SensorDataCycling.Cadence previous = new SensorDataCycling.Cadence(1, 1024); // 1s
        SensorDataCycling.Cadence current = new SensorDataCycling.Cadence(2, 2048); // 2s

        // when
        current.compute(previous);

        // then
        Assert.assertEquals(57.2519, current.getCadence_rpm(), 0.01);
    }

    @Test
    public void compute_cadence_sameCount() {
        // given
        SensorDataCycling.Cadence previous = new SensorDataCycling.Cadence(1, 1024);
        SensorDataCycling.Cadence current = new SensorDataCycling.Cadence(1, 2048);

        // when
        current.compute(previous);

        // then
        Assert.assertEquals(0, current.getCadence_rpm(), 0.01);
    }


    @Test
    public void compute_cadence_sameTime() {
        // given
        SensorDataCycling.Cadence previous = new SensorDataCycling.Cadence(1, 1024);
        SensorDataCycling.Cadence current = new SensorDataCycling.Cadence(2, 1024);

        // when
        current.compute(previous);

        // then
        Assert.assertFalse(current.hasCadence_rpm());
    }

    @Test
    public void compute_cadence_rollOverTime() {
        // given
        SensorDataCycling.Cadence previous = new SensorDataCycling.Cadence(1, UintUtils.UINT16_MAX - 1024);
        SensorDataCycling.Cadence current = new SensorDataCycling.Cadence(2, 0);

        // when
        current.compute(previous);

        // then
        Assert.assertEquals(57.2519, current.getCadence_rpm(), 0.01);
    }

    @Test
    public void compute_cadence_rollOverCount() {
        // given
        SensorDataCycling.Cadence previous = new SensorDataCycling.Cadence(UintUtils.UINT32_MAX - 1, 1024);
        SensorDataCycling.Cadence current = new SensorDataCycling.Cadence(0, 2048);

        // when
        current.compute(previous);

        // then
        Assert.assertEquals(57.2519, current.getCadence_rpm(), 0.01);
    }
}