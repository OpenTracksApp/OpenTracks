package de.dennisguse.opentracks.util;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.UUID;

public class UUIDUtils {

    public static UUID fromBytes(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        long mostSignificant = byteBuffer.getLong();
        long lestSignificant = byteBuffer.getLong();
        return new UUID(mostSignificant, lestSignificant);
    }

    public static byte[] toBytes(@NonNull UUID uuid) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(16);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return byteBuffer.array();
    }
}
