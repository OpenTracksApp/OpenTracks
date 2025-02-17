package de.dennisguse.opentracks.sensors.sensorData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;

import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.sensors.BluetoothHandlerCyclingCadence;
import de.dennisguse.opentracks.sensors.BluetoothHandlerCyclingDistanceSpeed;
import de.dennisguse.opentracks.sensors.UintUtils;

@RunWith(AndroidJUnit4.class)
public class SensorDataCyclingTest {

    @Test
    public void compute_cadence_1() {
        AggregatorCyclingCadence current = new AggregatorCyclingCadence("", "");

        // when
        current.add(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingCadence.CrankData(1, 1024)));
        current.add(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingCadence.CrankData(2, 2048)));

        // then
        assertEquals(60, current.getAggregatedValue(Instant.MIN).getRPM(), 0.01);
    }

    @Test
    public void compute_cadence_2() {
        AggregatorCyclingCadence current = new AggregatorCyclingCadence("", "");

        // when
        current.add(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingCadence.CrankData(1, 6184)));
        current.add(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingCadence.CrankData(2, 8016)));

        // then
        assertEquals(33.53, current.getAggregatedValue(Instant.MIN).getRPM(), 0.01);
    }

    @Test
    public void compute_cadence_sameCount() {
        AggregatorCyclingCadence current = new AggregatorCyclingCadence("", "");

        // when
        current.add(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingCadence.CrankData(1, 1024)));
        current.add(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingCadence.CrankData(1, 2048)));

        // then
        assertEquals(Cadence.of(0), current.getAggregatedValue(Instant.MIN));
    }


    @Test
    public void compute_cadence_sameTime() {
        AggregatorCyclingCadence current = new AggregatorCyclingCadence("", "");

        // when
        current.add(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingCadence.CrankData(1, 1024)));
        current.add(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingCadence.CrankData(2, 1024)));

        // then
        assertFalse(current.hasReceivedData()); //TODO Cadence should be 0?
    }

    @Test
    public void compute_cadence_rollOverTime() {
        AggregatorCyclingCadence current = new AggregatorCyclingCadence("", "");

        // when
        current.add(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingCadence.CrankData(1, UintUtils.UINT16_MAX - 1024)));
        current.add(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingCadence.CrankData(2, 0)));

        // then
        assertEquals(60, current.getAggregatedValue(Instant.MIN).getRPM(), 0.01);
    }

    @Test
    @Deprecated
    public void compute_cadence_rollOverCount() {
        AggregatorCyclingCadence current = new AggregatorCyclingCadence("", "");

        // when
        current.add(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingCadence.CrankData(UintUtils.UINT32_MAX - 1, 1024)));
        current.add(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingCadence.CrankData(0, 2048)));

        // then
        // TODO See #953
//        assertEquals(60, current.getValue().getRPM(), 0.01);
        assertEquals(Cadence.of(0), current.getAggregatedValue(Instant.MIN));
    }

    @Test
    public void compute_speed() {
        AggregatorCyclingDistanceSpeed current = new AggregatorCyclingDistanceSpeed("", "");
        current.setWheelCircumference(Distance.ofMM(2150));

        // when
        current.add(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingDistanceSpeed.WheelData(1, 6184)));
        current.add(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingDistanceSpeed.WheelData(2, 8016)));

        // then
        assertEquals(2.15, current.getAggregatedValue(Instant.MIN).distance().toM(), 0.01);
        assertEquals(1.20, current.getAggregatedValue(Instant.MIN).speed().toMPS(), 0.01);
    }

    @Test
    @Deprecated
    public void compute_speed_rollOverCount() {
        AggregatorCyclingDistanceSpeed current = new AggregatorCyclingDistanceSpeed("", "");
        current.setWheelCircumference(Distance.ofMM(2000));

        // when
        current.add(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingDistanceSpeed.WheelData(UintUtils.UINT32_MAX - 1, 1024)));
        current.add(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingDistanceSpeed.WheelData(0, 2048)));


        // then
        // TODO See #953
//        assertEquals(2, current.getValue().getDistance().toM(), 0.01);
//        assertEquals(2, current.getValue().getSpeed().toMPS(), 0.01);
        assertEquals(new AggregatorCyclingDistanceSpeed.Data(Distance.of(0), Distance.of(0), Speed.of(0)), current.getAggregatedValue(Instant.MIN));
    }
}