package de.dennisguse.opentracks.sensors;

import androidx.annotation.NonNull;

import java.util.Objects;
import java.util.UUID;

public record ServiceMeasurementUUID(UUID serviceUUID, UUID measurementUUID) {
}
