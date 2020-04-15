package de.dennisguse.opentracks.content.sensor;

import androidx.annotation.NonNull;

public final class SensorDataSet {

    public static final float DATA_UNAVAILABLE = Float.NaN;

    //TODO It might be necessary to consider: if sensor was connected as well.
    private String sensorName;
    private String sensorAddress;
    private float heartRate;
    private float cadence;
    private float power;
    private float batteryLevel;
    private long time;

    public SensorDataSet(float heartRate, float cadence, float power, float batteryLevel, long time) {
        this.heartRate = heartRate;
        this.cadence = cadence;
        this.power = power;
        this.batteryLevel = batteryLevel;
        this.time = time;
    }

    public SensorDataSet(float heartRate, float cadence, float power, float batteryLevel) {
        this(heartRate, cadence, power, batteryLevel, System.currentTimeMillis());
    }

    public SensorDataSet(float heartRate, float cadence, float power) {
        this(heartRate, cadence, power, DATA_UNAVAILABLE, System.currentTimeMillis());
    }

    public SensorDataSet(float heartRate, float cadence) {
        this(heartRate, cadence, DATA_UNAVAILABLE, DATA_UNAVAILABLE, System.currentTimeMillis());
    }

    public SensorDataSet(float heartRate, String sensorName, String sensorAddress) {
        this(heartRate, DATA_UNAVAILABLE, DATA_UNAVAILABLE, DATA_UNAVAILABLE, System.currentTimeMillis());
        this.sensorName = sensorName;
        this.sensorAddress = sensorAddress;
    }

    public boolean hasHeartRate() {
        return !Float.isNaN(heartRate) && heartRate > 0;
    }

    public float getHeartRate() {
        return heartRate;
    }

    public boolean hasCadence() {
        return !Float.isNaN(cadence);
    }

    public float getCadence() {
        return cadence;
    }

    public boolean hasPower() {
        return !Float.isNaN(power);
    }

    public float getPower() {
        return power;
    }

    public long getTime() {
        return time;
    }

    /**
     * Is the data recent considering the current time.
     *
     * @param maxAge the maximal age in milliseconds.
     */
    public boolean isRecent(long maxAge) {
        return time + maxAge > System.currentTimeMillis();
    }

    public boolean hasBatteryLevel() {
        return !Float.isNaN(batteryLevel);
    }

    public float getBatteryLevel() {
        return batteryLevel;
    }

    public String getSensorName() {
        return sensorName;
    }

    public String getSensorAddress() {
        return sensorAddress;
    }

    @NonNull
    @Override
    public String toString() {
        return "time=" + getTime() + " sensor=" + getSensorAddress() + " heart=" + getHeartRate();
    }
}
