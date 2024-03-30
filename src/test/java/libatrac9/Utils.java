/*
 * https://github.com/xXxTheDarkprogramerxXx/PS4_Tools/blob/master/PS4_Tools/Util/Utils.cs
 */

package libatrac9;


import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.logging.Level;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.Stream;
import vavi.util.Debug;


/**
 * Utils.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-02-02 nsano initial version <br>
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

    public static String readUTF8String(Object stream, int length) throws IOException {
        byte[] array = ((BinaryReader) stream).readBytes(length - 1 + 1);
        return new String(array);
    }

    public static short[][] interleavedByteToShort(byte[] input, int outputCount) {
        int itemCount = input.length / 2 / outputCount;
        var output = new short[outputCount][];
        for (int i = 0; i < outputCount; i++) {
            output[i] = new short[itemCount];
        }

        for (int i = 0; i < itemCount; i++) {
            for (int o = 0; o < outputCount; o++) {
                int offset = (i * outputCount + o) * 2;
                output[o][i] = (short) (input[offset] | (input[offset + 1] << 8));
            }
        }

        return output;
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

    public static byte[][] deInterleave(byte[] input, int interleaveSize, int outputCount, int outputSize /* = -1 */) {
        if (input.length % outputCount != 0)
            throw new IndexOutOfBoundsException(
                    "The input array length " + input.length + " must be divisible by the number of outputs.");

        int inputSize = input.length / outputCount;
        if (outputSize == -1)
            outputSize = inputSize;

        int inBlockCount = divideByRoundUp(inputSize, interleaveSize);
        int outBlockCount = divideByRoundUp(outputSize, interleaveSize);
        int lastInputInterleaveSize = inputSize - (inBlockCount - 1) * interleaveSize;
        int lastOutputInterleaveSize = outputSize - (outBlockCount - 1) * interleaveSize;
        int blocksToCopy = Math.min(inBlockCount, outBlockCount);

        var outputs = (byte[][]) Array.newInstance(byte.class, outputCount, outputCount);

        for (int b = 0; b < blocksToCopy; b++) {
            int currentInputInterleaveSize = b == inBlockCount - 1 ? lastInputInterleaveSize : interleaveSize;
            int currentOutputInterleaveSize = b == outBlockCount - 1 ? lastOutputInterleaveSize : interleaveSize;
            int bytesToCopy = Math.min(currentInputInterleaveSize, currentOutputInterleaveSize);

            for (int o = 0; o < outputCount; o++) {
                System.arraycopy(input, interleaveSize * b * outputCount + currentInputInterleaveSize * o, outputs[o],
                        interleaveSize * b, bytesToCopy);
            }
        }

        return outputs;
    }

    public static void interleave(byte[][] inputs, Stream output, int interleaveSize, int outputSize /* = -1 */) {
        int inputSize = inputs[0].length;
        if (outputSize == -1)
            outputSize = inputSize;

        if (Arrays.stream(inputs).anyMatch(x -> x.length != inputSize))
            throw new IndexOutOfBoundsException("Inputs must be of equal length");

        int inputCount = inputs.length;
        int inBlockCount = divideByRoundUp(inputSize, interleaveSize);
        int outBlockCount = divideByRoundUp(outputSize, interleaveSize);
        int lastInputInterleaveSize = inputSize - (inBlockCount - 1) * interleaveSize;
        int lastOutputInterleaveSize = outputSize - (outBlockCount - 1) * interleaveSize;
        int blocksToCopy = Math.min(inBlockCount, outBlockCount);

        for (int b = 0; b < blocksToCopy; b++) {
            int currentInputInterleaveSize = b == inBlockCount - 1 ? lastInputInterleaveSize : interleaveSize;
            int currentOutputInterleaveSize = b == outBlockCount - 1 ? lastOutputInterleaveSize : interleaveSize;
            int bytesToCopy = Math.min(currentInputInterleaveSize, currentOutputInterleaveSize);

            for (byte[] input : inputs) {
                output.write(input, interleaveSize * b, bytesToCopy);
                if (bytesToCopy < currentOutputInterleaveSize) {
                    output.position(output.position() + currentOutputInterleaveSize - bytesToCopy);
                }
            }
        }

        // Simply setting the position past the end of the stream doesn't expand the stream,
        // so we do that manually if necessary
        output.setLength(Math.max(outputSize * inputCount, (int) output.getLength()));
    }

    public static byte[][] deInterleave(Stream input, int length, int interleaveSize, int outputCount, int outputSize /* = -1 */) {
        if (input.canSeek()) {
            long remainingLength = input.getLength() - input.position();
            if (remainingLength < length) {
                throw new IndexOutOfBoundsException(
                        "Specified length is greater than the number of bytes remaining in the Stream: length: " + length);
            }
        }

        if (length % outputCount != 0)
            throw new IndexOutOfBoundsException(
                    "The input length (" + length + ") must be divisible by the number of outputs.");

        int inputSize = length / outputCount;
        if (outputSize == -1)
            outputSize = inputSize;

        int inBlockCount = divideByRoundUp(inputSize, interleaveSize);
        int outBlockCount = divideByRoundUp(outputSize, interleaveSize);
        int lastInputInterleaveSize = inputSize - (inBlockCount - 1) * interleaveSize;
        int lastOutputInterleaveSize = outputSize - (outBlockCount - 1) * interleaveSize;
        int blocksToCopy = Math.min(inBlockCount, outBlockCount);

        var outputs = new byte[outputCount][];
        for (int i = 0; i < outputCount; i++) {
            outputs[i] = new byte[outputSize];
        }

        for (int b = 0; b < blocksToCopy; b++) {
            int currentInputInterleaveSize = b == inBlockCount - 1 ? lastInputInterleaveSize : interleaveSize;
            int currentOutputInterleaveSize = b == outBlockCount - 1 ? lastOutputInterleaveSize : interleaveSize;
            int bytesToCopy = Math.min(currentInputInterleaveSize, currentOutputInterleaveSize);

            for (int o = 0; o < outputCount; o++) {
                input.read(outputs[o], interleaveSize * b, bytesToCopy);
                if (bytesToCopy < currentInputInterleaveSize) {
                    input.position(input.position() + currentInputInterleaveSize - bytesToCopy);
                }
            }
        }

        return outputs;
    }

    public static <T> T[] concat(Class<T> c, T[] first, T[] second) {
        if (first == null) throw new NullPointerException("first");
        if (second == null) throw new NullPointerException("second");
        return concatArray(c, first, second);
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
