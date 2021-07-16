package de.dennisguse.opentracks.content.sensor;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.util.UintUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

@RunWith(AndroidJUnit4.class)
public class SensorDataCyclingTest {

    @Test
    public void compute_cadence_1() {
        // given
        SensorDataCycling.Cadence previous = new SensorDataCycling.Cadence("sensorAddress", "sensorName", 1, 1024); // 1s
        SensorDataCycling.Cadence current = new SensorDataCycling.Cadence("sensorAddress", "sensorName", 2, 2048); // 2s

        // when
        current.compute(previous);

        // then
        assertEquals(60, current.getValue(), 0.01);
    }

    @Test
    public void compute_cadence_2() {
        // given
        SensorDataCycling.Cadence previous = new SensorDataCycling.Cadence("sensorAddress", "sensorName", 1, 6184);
        SensorDataCycling.Cadence current = new SensorDataCycling.Cadence("sensorAddress", "sensorName", 2, 8016);

        // when
        current.compute(previous);

        // then
        assertEquals(33.53, current.getValue(), 0.01);
    }

    @Test
    public void compute_cadence_sameCount() {
        // given
        SensorDataCycling.Cadence previous = new SensorDataCycling.Cadence("sensorAddress", "sensorName", 1, 1024);
        SensorDataCycling.Cadence current = new SensorDataCycling.Cadence("sensorAddress", "sensorName", 1, 2048);

        // when
        current.compute(previous);

        // then
        assertEquals(0, current.getValue(), 0.01);
    }


    @Test
    public void compute_cadence_sameTime() {
        // given
        SensorDataCycling.Cadence previous = new SensorDataCycling.Cadence("sensorAddress", "sensorName", 1, 1024);
        SensorDataCycling.Cadence current = new SensorDataCycling.Cadence("sensorAddress", "sensorName", 2, 1024);

        // when
        current.compute(previous);

        // then
        assertFalse(current.hasValue());
    }

    @Test
    public void compute_cadence_rollOverTime() {
        // given
        SensorDataCycling.Cadence previous = new SensorDataCycling.Cadence("sensorAddress", "sensorName", 1, UintUtils.UINT16_MAX - 1024);
        SensorDataCycling.Cadence current = new SensorDataCycling.Cadence("sensorAddress", "sensorName", 2, 0);

        // when
        current.compute(previous);

        // then
        assertEquals(60, current.getValue(), 0.01);
    }

    @Test
    public void compute_cadence_rollOverCount() {
        // given
        SensorDataCycling.Cadence previous = new SensorDataCycling.Cadence("sensorAddress", "sensorName", UintUtils.UINT32_MAX - 1, 1024);
        SensorDataCycling.Cadence current = new SensorDataCycling.Cadence("sensorAddress", "sensorName", 0, 2048);

        // when
        current.compute(previous);

        // then
        assertEquals(60, current.getValue(), 0.01);
    }

    @Test
    public void compute_speed() {
        // given
        SensorDataCycling.DistanceSpeed previous = new SensorDataCycling.DistanceSpeed("sensorAddress", "sensorName", 1, 6184);
        SensorDataCycling.DistanceSpeed current = new SensorDataCycling.DistanceSpeed("sensorAddress", "sensorName", 2, 8016);

        // when
        current.compute(previous, Distance.ofMM(2150));

        // then
        assertEquals(2.15, current.getValue().getDistance().toM(), 0.01);
        assertEquals(1.20, current.getValue().getSpeed().toMPS(), 0.01);
    }

    @Test
    public void compute_speed_rollOverCount() {
        // given
        SensorDataCycling.DistanceSpeed previous = new SensorDataCycling.DistanceSpeed("sensorAddress", "sensorName", UintUtils.UINT32_MAX - 1, 1024);
        SensorDataCycling.DistanceSpeed current = new SensorDataCycling.DistanceSpeed("sensorAddress", "sensorName", 0, 2048);

        // when
        current.compute(previous, Distance.ofMM(2000));

        // then
        assertEquals(2, current.getValue().getDistance().toM(), 0.01);
        assertEquals(2, current.getValue().getSpeed().toMPS(), 0.01);
    }

    @Test
    public void equals_speed_with_no_data() {
        // given
        SensorDataCycling.DistanceSpeed previous = new SensorDataCycling.DistanceSpeed("sensorAddress");
        SensorDataCycling.DistanceSpeed current = new SensorDataCycling.DistanceSpeed("sensorAddress", "sensorName", 0, 2048);

        // when
        previous.toString();

        // then
        assertNotEquals(previous, current);
        assertNotEquals(previous, previous);
    }

    @Test
    public void equals_cadence_with_no_data() {
        // given
        SensorDataCycling.Cadence previous = new SensorDataCycling.Cadence("sensorAddress");
        SensorDataCycling.Cadence current = new SensorDataCycling.Cadence("sensorAddress", "sensorName", 0, 2048);

        // when
        previous.toString();

        // then
        assertNotEquals(previous, current);
        assertNotEquals(previous, previous);
    }
}