package de.dennisguse.opentracks.data;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.UUID;

public class UUIDUtils {

  //issue 552
  //Add a private constructor to hide the implicit public one.
    private void fromBytes(){
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static UUID fromBytes(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        long mostSignificant = byteBuffer.getLong();
        long lestSignificant = byteBuffer.getLong();
        return new UUID(mostSignificant, lestSignificant);
    }
    private void toBytes(){
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    public static byte[] toBytes(@NonNull UUID uuid) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(16);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return byteBuffer.array();
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private void toHex(){
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    public static String toHex(@NonNull UUID uuid) {
        byte[] bytes = toBytes(uuid);

        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;

            hexChars[i * 2] = HEX_ARRAY[v >>> 4];
            hexChars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
