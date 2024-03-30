/*
 * https://github.com/xXxTheDarkprogramerxXx/PS4_Tools/blob/master/PS4_Tools/Util/Utils.cs
 */

package libatrac9.decoder;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.logging.Level;

import vavi.io.LittleEndianDataInputStream;
import vavi.io.LittleEndianDataOutputStream;
import vavi.util.Debug;


/**
 * Utils.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-03-29 nsano initial version <br>
 */
public class Utils {

    private Utils() {
    }

    public static int divideByRoundUp(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }

    public static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }

    @SuppressWarnings("unchecked")
    public static <T> T createJaggedArray(Class<T> c, int... lengths) {
        return (T) initializeJaggedArray(c.getComponentType(), 0, lengths);
    }

    private static Object initializeJaggedArray(Class<?> type, int index, int... lengths) {
Debug.println(Level.FINER, "array: " + type + ", length: " + lengths[index]);
        Object array = Array.newInstance(type, lengths[index]);

        if (!type.isArray()) return array;
        Class<?> elementType = type.getComponentType();

        for (int i = 0; i < lengths[index]; i++) {
Debug.println(Level.FINER, " sub array: index[" + i  + "]: length: " + lengths[index + 1]);
            Array.set(array, i, initializeJaggedArray(elementType, index + 1, lengths));
        }

        return array;
    }

    public static byte[] shortToInterleavedByte(short[][] input) {
        int inputCount = input.length;
        int length = input[0].length;
        var output = new byte[inputCount * length * 2];

        for (int i = 0; i < length; i++) {
            for (int j = 0; j < inputCount; j++) {
                int offset = (i * inputCount + j) * 2;
                output[offset] = (byte) input[j][i];
                output[offset + 1] = (byte) (input[j][i] >> 8);
            }
        }

        return output;
    }

    @SuppressWarnings("unchecked")
    static <T> T[] concatArray(Class<T> c, T[] first, T[] second) {
        T[] result = (T[]) Array.newInstance(c, first.length + second.length);
        int i = 0;
        for (T element : first) result[i++] = element;
        for (T element : second) result[i++] = element;
        return result;
    }
}
