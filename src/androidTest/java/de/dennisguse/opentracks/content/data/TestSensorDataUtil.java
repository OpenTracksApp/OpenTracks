package de.dennisguse.opentracks.content.data;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.data.models.Altitude;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Position;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.TrackPoint;

public class TestSensorDataUtil {

    private final List<TrackPoint> trackPointList = new ArrayList<>();
    private final List<SensorData> sensorDataList = new ArrayList<>();

    public void add(Instant time, Float hr, Float cadence, Float power, TrackPoint.Type type) {
        sensorDataList.add(new TestSensorDataUtil.SensorData(time, hr, cadence, power, type));
        int i = trackPointList.size() + 1;

        TrackPoint tp = new TrackPoint(type,
                new Position(
                        time,
                        TestDataUtil.INITIAL_LATITUDE + (double) i / 10000.0,
                        TestDataUtil.INITIAL_LONGITUDE - (double) i / 10000.0,
                        Distance.of(1f),
                        Altitude.WGS84.of(1f),
                        null,
                        null,
                        Speed.of(5f + (i / 10f))
                )
        );
        if (hr != null) {
            tp.setHeartRate(hr);
        }
        if (cadence != null) {
            tp.setCadence(cadence);
        }
        if (power != null) {
            tp.setPower(power);
        }
        tp.setAltitudeGain(3f);
        tp.setAltitudeLoss(3f);
        trackPointList.add(tp);
    }

    public List<TrackPoint> getTrackPointList() {
        return this.trackPointList;
    }

    public SensorDataStats computeStats() {
        if (sensorDataList.size() <= 1) {
            return null;
        }

        SensorDataStats stats = new SensorDataStats();
        long timeElapsed;
        long movingTime = 0;
        SensorData dataPrev = sensorDataList.get(0);
        stats.maxHr = dataPrev.hr;
        stats.maxCadence = dataPrev.cadence;
        SensorData dataCurrent;
        for (int i = 1; i < sensorDataList.size(); i++) {
            dataCurrent = sensorDataList.get(i);
            if (dataPrev.type != TrackPoint.Type.SEGMENT_START_MANUAL) {
                timeElapsed = dataCurrent.type != TrackPoint.Type.SEGMENT_START_MANUAL ? dataCurrent.time.getEpochSecond() - dataPrev.time.getEpochSecond() : 0;
                stats.avgHr += (dataPrev.hr * timeElapsed);
                stats.maxHr = Math.max(dataPrev.hr, stats.maxHr);
                stats.avgCadence += (dataPrev.cadence * timeElapsed);
                stats.maxCadence = Math.max(dataPrev.cadence, stats.maxCadence);
                stats.avgPower += (dataPrev.power * timeElapsed);

                movingTime += timeElapsed;
            }
            dataPrev = dataCurrent;
        }

        stats.avgHr /= movingTime;
        stats.avgCadence /= movingTime;
        stats.avgPower /= movingTime;

        return stats;
    }

    private static class SensorData {
        final Instant time;
        final float hr;
        final float cadence;
        final float power;
        final TrackPoint.Type type;

        public SensorData(Instant time, Float hr, Float cadence, Float power, TrackPoint.Type type) {
            this.time = time;
            this.hr = hr == null ? 0 : hr;
            this.cadence = cadence == null ? 0 : cadence;
            this.power = power == null ? 0 : power;
            this.type = type;
        }
    }

    public static class SensorDataStats {
        public float avgHr = 0f;
        public float maxHr = 0f;
        public float avgCadence = 0f;
        public float maxCadence = 0f;
        public float avgPower = 0f;
    }
}
