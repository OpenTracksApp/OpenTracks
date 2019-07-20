package com.google.android.apps.mytracks.content.sensor;

public final class SensorDataSet {

    public static final float DATA_UNAVAILABLE = Integer.MAX_VALUE;

    //TODO It might be necessary to consider: sensor if sensor was connected as well.

    private float heartRate;
    private float cadence;
    private float power;
    private float batteryLevel;
    private long creationTimestamp;

    public SensorDataSet(float heartRate, float cadence, float power, float batteryLevel, long creationTimestamp) {
        this.heartRate = heartRate;
        this.cadence = cadence;
        this.power = power;
        this.batteryLevel = batteryLevel;
        this.creationTimestamp = creationTimestamp;
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

    public SensorDataSet(float heartRate) {
        this(heartRate, DATA_UNAVAILABLE, DATA_UNAVAILABLE, DATA_UNAVAILABLE, System.currentTimeMillis());
    }

    public boolean hasHeartRate() {
        return heartRate != DATA_UNAVAILABLE;
    }

    public float getHeartRate() {
        return heartRate;
    }

    public boolean hasCadence() {
        return cadence != DATA_UNAVAILABLE;
    }

    public float getCadence() {
        return cadence;
    }

    public boolean hasPower() {
        return power != DATA_UNAVAILABLE;
    }

    public float getPower() {
        return power;
    }

    public long getCreationTime() {
        return creationTimestamp;
    }

    /**
     * Is the data recent considering the current time.
     * @param maxAge the maximal age in milliseconds.
     */
    public boolean isRecent(long maxAge) {
        return creationTimestamp + maxAge > System.currentTimeMillis();
    }

    public boolean hasBatteryLevel() {
        return batteryLevel != DATA_UNAVAILABLE;
    }

    public float getBatteryLevel() {
        return batteryLevel;
    }
}
