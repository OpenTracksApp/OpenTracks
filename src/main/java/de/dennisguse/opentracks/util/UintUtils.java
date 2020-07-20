package de.dennisguse.opentracks.util;

public class UintUtils {

    public static final int UINT16_MAX = 0xFFFF;
    public static final long UINT32_MAX = 0xFFFFFFFFL;

    private UintUtils() {
    }

    /**
     * Computes a - b for UINT with overflow (b < a).
     *
     * @return diff or -1 (invalid)
     */
    public static long diff(long a, long b, final long UINT_MAX) {
        if (a < 0 || b < 0) {
            return -1;
        }
        if (a > UINT_MAX || b > UINT_MAX) {
            return -1;
        }

        if (a >= b) {
            return a - b;
        }

        return (UINT_MAX - b) + a;
    }
}
