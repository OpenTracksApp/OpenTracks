package de.dennisguse.opentracks.sensors;

import androidx.annotation.NonNull;

import java.util.Objects;
import java.util.UUID;

public class ServiceMeasurementUUID {

    private final UUID serviceUUID;
    private final UUID measurementUUID;

    public ServiceMeasurementUUID(UUID serviceUUID, UUID measurementUUID) {
        this.serviceUUID = serviceUUID;
        this.measurementUUID = measurementUUID;
    }

    public UUID getServiceUUID() {
        return serviceUUID;
    }

    public UUID getMeasurementUUID() {
        return measurementUUID;
    }

    @NonNull
    @Override
    public String toString() {
        return "ServiceMeasurementUUID{" +
                "serviceUUID=" + serviceUUID +
                ", measurementUUID=" + measurementUUID +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceMeasurementUUID that = (ServiceMeasurementUUID) o;
        return Objects.equals(serviceUUID, that.serviceUUID) && Objects.equals(measurementUUID, that.measurementUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceUUID, measurementUUID);
    }
}
