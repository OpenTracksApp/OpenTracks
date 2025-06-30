package de.dennisguse.opentracks.io.file.exporter;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FITv2TrackExporterTest {

    private final byte FIT_PROTOCOL_VERSION = 2 << 4 + 0;
    private final short FIT_PROFILE_VERSION = 21 * 1000 + 158;

    private long checksum;
    private final byte FILE_HEADER_SIZE = 14;

    @Test
    public void run() {
        ByteBuffer littleEndian = ByteBuffer
                .allocate(FILE_HEADER_SIZE)
                .order(ByteOrder.LITTLE_ENDIAN);

        littleEndian.put(0, FILE_HEADER_SIZE);
        littleEndian.put(1, FIT_PROTOCOL_VERSION);

        // Profile version: 1513
        littleEndian.putShort(2, FIT_PROFILE_VERSION);

        // Data size: 32bit
        littleEndian.putInt(4, 0); // We do not calculate the data size.

        // .FIT
        littleEndian.putChar(8, '.');
        littleEndian.putChar(9, 'F');
        littleEndian.putChar(10, 'I');
        littleEndian.putChar(11, 'T');

        // CRC (not implemented)
        littleEndian.put(12, (byte) 0);
        littleEndian.put(13, (byte) 0);

        var a = littleEndian.array();
        littleEndian.array();
    }
}