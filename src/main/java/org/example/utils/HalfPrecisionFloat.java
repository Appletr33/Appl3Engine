package org.example.utils;

public class HalfPrecisionFloat {

    // Convert a 16-bit half-precision float to a 32-bit float
    public static float halfToFloat(short half) {
        int s = (half >> 15) & 0x00000001; // sign
        int e = (half >> 10) & 0x0000001F; // exponent
        int f = half & 0x000003FF; // fraction

        if (e == 0) {
            if (f == 0) {
                return (s == 1) ? -0.0f : 0.0f; // Signed zero
            } else {
                // Subnormal number
                return (float) ((s == 1 ? -1 : 1) * Math.pow(2, -14) * (f / 1024.0));
            }
        } else if (e == 31) {
            if (f == 0) {
                return (s == 1) ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY; // Signed infinity
            } else {
                return Float.NaN; // Not-a-Number
            }
        } else {
            // Normalized number
            return (float) ((s == 1 ? -1 : 1) * Math.pow(2, e - 15) * (1 + f / 1024.0));
        }
    }

    // Convert a 32-bit float to a 16-bit half-precision float
    public static short floatToHalf(float value) {
        int bits = Float.floatToIntBits(value);
        int sign = (bits >> 31) & 0x1;
        int exponent = ((bits >> 23) & 0xFF) - 127;
        int fraction = bits & 0x7FFFFF;

        if (exponent < -14) {
            // Subnormal number
            return (short) (sign << 15);
        } else if (exponent > 15) {
            // Overflow to infinity
            return (short) ((sign << 15) | (0x1F << 10));
        } else {
            // Normalized number
            int halfExponent = exponent + 15;
            int halfFraction = fraction >> 13;
            return (short) ((sign << 15) | (halfExponent << 10) | halfFraction);
        }
    }
}
