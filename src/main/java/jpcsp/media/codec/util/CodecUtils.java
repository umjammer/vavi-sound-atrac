/*
 * This file is part of jpcsp.
 *
 * Jpcsp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Jpcsp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */

package jpcsp.media.codec.util;

import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.lang.System.Logger;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.System.getLogger;


public class CodecUtils {

    private static final Logger logger = getLogger(CodecUtils.class.getName());

    // FLT_EPSILON the minimum positive number such that 1.0 + FLT_EPSILON != 1.0
    public static final float FLT_EPSILON = 1.19209290E-07F;
    public static final float M_SQRT1_2 = 0.707106781186547524401f; // 1/sqrt(2)
    public static final float M_PI = (float) Math.PI;
    public static final float M_SQRT2 = 1.41421356237309504880f; // sqrt(2)

    public static final int[] ff_log2_tab = {
            0, 0, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
            6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
            7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
            7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
            7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
            7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7
    };

    private static short convertSampleFloatToInt16(float sample) {
        return (short) (min(max((int) (sample * 32768f + 0.5f), -32768), 32767) & 0xFFFF);
    }

    public static void writeOutput(float[][] samples, ByteBuffer outputMemory, int outputAddr, int numberOfSamples, int decodedChannels, int outputChannels) {
        ShortBuffer writer = outputMemory.asShortBuffer();
        logger.log(Level.TRACE, "position: " + outputAddr);
        writer.position(outputAddr);
        logger.log(Level.TRACE, "limit: " + (outputAddr + numberOfSamples * outputChannels));
        writer.limit(outputAddr + numberOfSamples * outputChannels);
        switch (outputChannels) {
            case 1:
                for (int i = 0; i < numberOfSamples; i++) {
                    short sample = convertSampleFloatToInt16(samples[0][i]);
                    writer.put(sample);
                }
                break;
            case 2:
                if (decodedChannels == 1) {
                    // Convert decoded mono into output stereo
                    for (int i = 0; i < numberOfSamples; i++) {
                        short sample = convertSampleFloatToInt16(samples[0][i]);
                        writer.put(sample);
                        writer.put(sample);
                    }
                } else {
                    for (int i = 0; i < numberOfSamples; i++) {
                        short lsample = convertSampleFloatToInt16(samples[0][i]);
                        short rsample = convertSampleFloatToInt16(samples[1][i]);
                        writer.put(lsample);
                        writer.put(rsample);
                    }
                }
                break;
        }
    }

    public static int avLog2(int n) {
        if (n == 0) {
            return 0;
        }
        return 31 - Integer.numberOfLeadingZeros(n);
    }

    private static final float log2 = (float) Math.log(2.0);

    public static float log2f(float n) {
        return (float) Math.log(n) / log2;
    }

    public static int lrintf(float n) {
        return (int) Math.rint(n);
    }

    public static float exp2f(float n) {
        return (float) Math.pow(2.0, n);
    }

    public static float sqrtf(float n) {
        return (float) Math.sqrt(n);
    }

    public static float cosf(float n) {
        return (float) Math.cos(n);
    }

    public static float sinf(float n) {
        return (float) Math.sin(n);
    }

    public static float atanf(float n) {
        return (float) Math.atan(n);
    }

    public static float atan2f(float y, float x) {
        return (float) Math.atan2(y, x);
    }

    public static int signExtend(int value, int bits) {
        int shift = Integer.SIZE - bits;
        return (value << shift) >> shift;
    }
}
