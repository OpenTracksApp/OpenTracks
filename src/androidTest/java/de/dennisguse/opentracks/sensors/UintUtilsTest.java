package de.dennisguse.opentracks.sensors;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UintUtilsTest {

    @Test
    public void diff() {
        assertEquals(0, UintUtils.diff(1, 1, UintUtils.UINT16_MAX));
        assertEquals(1, UintUtils.diff(2, 1, UintUtils.UINT16_MAX));
        assertEquals(3, UintUtils.diff(5, 2, UintUtils.UINT16_MAX));
        assertEquals(65535, UintUtils.diff(1, 2, UintUtils.UINT16_MAX));     /* unsigned 16 arithmetic is modulo UINT16_MAX + 1, not modulo UINT16_MAX */
        assertEquals(65530, UintUtils.diff(UintUtils.UINT16_MAX, 5, UintUtils.UINT16_MAX));

        /*   Test modulo arithmetic for arguments that are out of range */

        if (false) {   /*  false means UintUtils.diff() throws if arguments out of range */
            assertEquals(0, UintUtils.diff(65537, 1, UintUtils.UINT16_MAX));
            assertEquals(0, UintUtils.diff(1, 65537, UintUtils.UINT16_MAX));
            assertEquals(0, UintUtils.diff(65537, 65537, UintUtils.UINT16_MAX));
            assertEquals(0, UintUtils.diff(-65535, 1, UintUtils.UINT16_MAX));
            assertEquals(0, UintUtils.diff(1, -65535, UintUtils.UINT16_MAX));
            assertEquals(0, UintUtils.diff(-65535, -65535, UintUtils.UINT16_MAX));
            assertEquals(0, UintUtils.diff(-65535, 65537, UintUtils.UINT16_MAX));
            assertEquals(0, UintUtils.diff(65537, -65535, UintUtils.UINT16_MAX));
        }

        /*  The following tests are the above, but for 32-bit unsigned. */

        assertEquals(0, UintUtils.diff(1, 1, UintUtils.UINT32_MAX));
        assertEquals(1, UintUtils.diff(2, 1, UintUtils.UINT32_MAX));
        assertEquals(3, UintUtils.diff(5, 2, UintUtils.UINT32_MAX));
        assertEquals(4294967295L, UintUtils.diff(1, 2, UintUtils.UINT32_MAX));
        assertEquals(4294967290L, UintUtils.diff(UintUtils.UINT32_MAX, 5, UintUtils.UINT32_MAX));

        if (false) { /*  false means UintUtils.diff() throws if arguments out of range */
            assertEquals(0, UintUtils.diff(4294967297L, 1, UintUtils.UINT32_MAX));
            assertEquals(0, UintUtils.diff(1, 4294967297L, UintUtils.UINT32_MAX));
            assertEquals(0, UintUtils.diff(4294967297L, 4294967297L, UintUtils.UINT32_MAX));
            assertEquals(0, UintUtils.diff(-4294967295L, 1, UintUtils.UINT32_MAX));
            assertEquals(0, UintUtils.diff(1, -4294967295L, UintUtils.UINT32_MAX));
            assertEquals(0, UintUtils.diff(-4294967295L, -4294967295L, UintUtils.UINT32_MAX));
            assertEquals(0, UintUtils.diff(-4294967295L, 4294967297L, UintUtils.UINT32_MAX));
            assertEquals(0, UintUtils.diff(65537, -4294967297L, UintUtils.UINT32_MAX));
        }
    }

    @Test
    public void realData() {
        assertEquals(1, UintUtils.diff(381616, 381615, UintUtils.UINT32_MAX));
    }
}
