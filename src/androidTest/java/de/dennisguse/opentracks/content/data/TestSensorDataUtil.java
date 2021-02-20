package de.dennisguse.opentracks.content.data;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TestSensorDataUtil {

    private List<TrackPoint> trackPointList = new ArrayList<>();
    private List<SensorData> sensorDataList = new ArrayList<>();

    public void add(Instant time, Float hr, Float cadence, Float power, TrackPoint.Type type) {
        sensorDataList.add(new TestSensorDataUtil.SensorData(time, hr, cadence, power, type));
        TrackPoint tp = new TrackPoint(type);
        int i = trackPointList.size() + 1;
        tp.setLatitude(TestDataUtil.INITIAL_LATITUDE + (double) i / 10000.0);
        tp.setLongitude(TestDataUtil.INITIAL_LONGITUDE - (double) i / 10000.0);
        tp.setHeartRate_bpm(hr);
        tp.setCyclingCadence_rpm(cadence);
        tp.setPower(power);
        tp.setAccuracy(1f);
        tp.setAltitude(1f);
        tp.setTime(time);
        tp.setSpeed(5f + (i / 10f));
        tp.setElevationGain(3f);
        tp.setElevationLoss(3f);
        trackPointList.add(tp);
    }

    public List<TrackPoint> getTrackPointList() {
        return this.trackPointList;
    }

    public SensorDataStats computeStats() {
        if (sensorDataList == null || sensorDataList.size() <= 1) {
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
                stats.maxHr = dataPrev.hr > stats.maxHr ? dataPrev.hr : stats.maxHr;
                stats.avgCadence += (dataPrev.cadence * timeElapsed);
                stats.maxCadence = dataPrev.cadence > stats.maxCadence ? dataPrev.cadence : stats.maxCadence;
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
        Instant time;
        float hr;
        float cadence;
        float power;
        TrackPoint.Type type;

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
