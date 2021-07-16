package de.dennisguse.opentracks.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UintUtilsTest {

    @Test
    public void diff() {
        assertEquals(0, UintUtils.diff(1, 1, UintUtils.UINT16_MAX));
        assertEquals(1, UintUtils.diff(2, 1, UintUtils.UINT16_MAX));
        assertEquals(3, UintUtils.diff(5, 2, UintUtils.UINT16_MAX));
        assertEquals(65534, UintUtils.diff(1, 2, UintUtils.UINT16_MAX));

        assertEquals(65530, UintUtils.diff(UintUtils.UINT16_MAX, 5, UintUtils.UINT16_MAX));
    }

    @Test
    public void realData() {
        assertEquals(1, UintUtils.diff(381616, 381615, UintUtils.UINT32_MAX));
    }
}