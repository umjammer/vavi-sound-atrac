/*
 * MIT License
 *
 * Copyright (c) 2018 Alex Barney
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package libatrac9.util;

import java.util.ArrayList;
import java.util.List;


public class Mdct {

    private int mdctBits;

    private int mdctSize;

    private double scale;

    public int getMdctBits() {
        return mdctBits;
    }

    public int getMdctSize() {
        return mdctSize;
    }

    public double getScale() {
        return scale;
    }

    private static final Object tableLock = new Object();
    private static int _tableBits = -1;
    private static final List<double[]> sinTables = new ArrayList<>();
    private static final List<double[]> cosTables = new ArrayList<>();
    private static final List<int[]> shuffleTables = new ArrayList<>();

    private final double[] imdctPrevious;
    private final double[] imdctWindow;

    private final double[] scratchMdct;
    private final double[] scratchDct;

    public Mdct(int mdctBits, double[] window, double scale /* = 1 */) {
        setTables(mdctBits);

        this.mdctBits = mdctBits;
        mdctSize = 1 << mdctBits;
        this.scale = scale;

        if (window.length < mdctSize) {
            throw new IllegalArgumentException("Window must be as long as the MDCT size.: window");
        }

        imdctPrevious = new double[mdctSize];
        scratchMdct = new double[mdctSize];
        scratchDct = new double[mdctSize];
        imdctWindow = window;
    }

    private static void setTables(int maxBits) {
        synchronized (tableLock) {
            if (maxBits > _tableBits) {
                for (int i = _tableBits + 1; i <= maxBits; i++) {
                    double[][] sin = new double[1][];
                    double[][] cos = new double[1][];
                    generateTrigTables(i, /* out */ sin, /* out */ cos);
                    sinTables.add(sin[0]);
                    cosTables.add(cos[0]);
                    shuffleTables.add(generateShuffleTable(i));
                }
                _tableBits = maxBits;
            }
//Debug.println("sinTables: " + sinTables.size() + ", " + sinTables);
//Debug.println("cosTables: " + cosTables.size() + ", " + cosTables);
        }
    }

    public void runImdct(double[] input, double[] output) {
        if (input.length < mdctSize) {
            throw new IllegalArgumentException("Input must be as long as the MDCT size.: input");
        }

        if (output.length < mdctSize) {
            throw new IllegalArgumentException("Output must be as long as the MDCT size.: output");
        }

        int size = mdctSize;
        int half = size / 2;
        double[] dctOut = scratchMdct;

        dct4(input, dctOut);

        for (int i = 0; i < half; i++) {
            output[i] = imdctWindow[i] * dctOut[i + half] + imdctPrevious[i];
            output[i + half] = imdctWindow[i + half] * -dctOut[size - 1 - i] - imdctPrevious[i + half];
            imdctPrevious[i] = imdctWindow[size - 1 - i] * -dctOut[half - i - 1];
            imdctPrevious[i + half] = imdctWindow[half - i - 1] * dctOut[i];
        }
    }

    /**
     * Does a Type-4 DCT.
     * @param input The input array containing the time or frequency-domain samples
     * @param output The output array that will contain the transformed time or frequency-domain samples
     */
    private void dct4(double[] input, double[] output) {
        int[] shuffleTable = shuffleTables.get(mdctBits);
        double[] sinTable = sinTables.get(mdctBits);
        double[] cosTable = cosTables.get(mdctBits);
        double[] dctTemp = scratchDct;
//Debug.println("mdctBits: " + mdctBits);

        int size = mdctSize;
        int lastIndex = size - 1;
        int halfSize = size / 2;

        for (int i = 0; i < halfSize; i++) {
            int i2 = i * 2;
            double a = input[i2];
            double b = input[lastIndex - i2];
            double sin = sinTable[i];
            double cos = cosTable[i];
            dctTemp[i2] = a * cos + b * sin;
            dctTemp[i2 + 1] = a * sin - b * cos;
        }
        int stageCount = mdctBits - 1;

        for (int stage = 0; stage < stageCount; stage++) {
            int blockCount = 1 << stage;
            int blockSizeBits = stageCount - stage;
            int blockHalfSizeBits = blockSizeBits - 1;
            int blockSize = 1 << blockSizeBits;
            int blockHalfSize = 1 << blockHalfSizeBits;
            sinTable = sinTables.get(blockHalfSizeBits);
            cosTable = cosTables.get(blockHalfSizeBits);

            for (int block = 0; block < blockCount; block++) {
                for (int i = 0; i < blockHalfSize; i++) {
                    int frontPos = (block * blockSize + i) * 2;
                    int backPos = frontPos + blockSize;
                    double a = dctTemp[frontPos] - dctTemp[backPos];
                    double b = dctTemp[frontPos + 1] - dctTemp[backPos + 1];
                    double sin = sinTable[i];
                    double cos = cosTable[i];
                    dctTemp[frontPos] += dctTemp[backPos];
                    dctTemp[frontPos + 1] += dctTemp[backPos + 1];
                    dctTemp[backPos] = a * cos + b * sin;
                    dctTemp[backPos + 1] = a * sin - b * cos;
                }
            }
        }

        for (int i = 0; i < mdctSize; i++) {
            output[i] = dctTemp[shuffleTable[i]] * scale;
        }
    }

    private static void generateTrigTables(int sizeBits, /* out */ double[][] sin, /* out */ double[][] cos) {
        int size = 1 << sizeBits;
        sin[0] = new double[size];
        cos[0] = new double[size];

        for (int i = 0; i < size; i++) {
            double value = Math.PI * (4 * i + 1) / (4 * size);
            sin[0][i] = Math.sin(value);
            cos[0][i] = Math.cos(value);
        }
    }

    private static int[] generateShuffleTable(int sizeBits) {
        int size = 1 << sizeBits;
        var table = new int[size];

        for (int i = 0; i < size; i++) {
            table[i] = Bit.bitReverse32(i ^ (i / 2), sizeBits);
        }

        return table;
    }
}
