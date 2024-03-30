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

package libatrac9;

import java.lang.reflect.Array;
import java.util.function.Function;

import libatrac9.Block.BlockType;

import static libatrac9.HuffmanCodebooks.HuffmanScaleFactorsABits;
import static libatrac9.HuffmanCodebooks.HuffmanScaleFactorsACodes;
import static libatrac9.HuffmanCodebooks.HuffmanScaleFactorsBBits;
import static libatrac9.HuffmanCodebooks.HuffmanScaleFactorsBCodes;
import static libatrac9.HuffmanCodebooks.HuffmanScaleFactorsGroupSizes;
import static libatrac9.HuffmanCodebooks.HuffmanSpectrumABits;
import static libatrac9.HuffmanCodebooks.HuffmanSpectrumACodes;
import static libatrac9.HuffmanCodebooks.HuffmanSpectrumAGroupSizes;
import static libatrac9.HuffmanCodebooks.HuffmanSpectrumBBits;
import static libatrac9.HuffmanCodebooks.HuffmanSpectrumBCodes;
import static libatrac9.HuffmanCodebooks.HuffmanSpectrumBGroupSizes;
import static libatrac9.HuffmanCodebooks.generateHuffmanCodebooks;


class Tables {

    public static int maxHuffPrecision(boolean highSampleRate) {
        return highSampleRate ? 1 : 7;
    }

    public static int minBandCount(boolean highSampleRate) {
        return highSampleRate ? 1 : 3;
    }

    public static int maxExtensionBand(boolean highSampleRate) {
        return highSampleRate ? 16 : 18;
    }

    public static final int[] SampleRates = {
            11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000,
            44100, 48000, 64000, 88200, 96000, 128000, 176400, 192000
    };

    public static final byte[] SamplingRateIndexToFrameSamplesPower = {6, 6, 7, 7, 7, 8, 8, 8, 6, 6, 7, 7, 7, 8, 8, 8};

    // From sampling rate index
    public static final byte[] MaxBandCount = {8, 8, 12, 12, 12, 18, 18, 18, 8, 8, 12, 12, 12, 16, 16, 16};
    public static final byte[] BandToQuantUnitCount = {0, 4, 8, 10, 12, 13, 14, 15, 16, 18, 20, 21, 22, 23, 24, 25, 26, 28, 30};

    public static final byte[] QuantUnitToCoeffCount = {
            2, 2, 2, 2, 2, 2, 2, 2, 4, 4, 4, 4, 8, 8, 8,
            8, 8, 8, 8, 8, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16
    };

    public static final short[] QuantUnitToCoeffIndex = {
            0, 2, 4, 6, 8, 10, 12, 14, 16, 20, 24, 28, 32, 40, 48, 56,
            64, 72, 80, 88, 96, 112, 128, 144, 160, 176, 192, 208, 224, 240, 256
    };

    public static final byte[] QuantUnitToCodebookIndex = {
            0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2,
            2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3
    };

    public static final ChannelConfig[] ChannelConfig = {
            new ChannelConfig(BlockType.Mono),
            new ChannelConfig(BlockType.Mono, BlockType.Mono),
            new ChannelConfig(BlockType.Stereo),
            new ChannelConfig(BlockType.Stereo, BlockType.Mono, BlockType.LFE, BlockType.Stereo),
            new ChannelConfig(BlockType.Stereo, BlockType.Mono, BlockType.LFE, BlockType.Stereo, BlockType.Stereo),
            new ChannelConfig(BlockType.Stereo, BlockType.Stereo)
    };

    public static final HuffmanCodebook[] HuffmanScaleFactorsUnsigned =
            generateHuffmanCodebooks(HuffmanScaleFactorsACodes, HuffmanScaleFactorsABits, HuffmanScaleFactorsGroupSizes);

    public static final HuffmanCodebook[] HuffmanScaleFactorsSigned =
            generateHuffmanCodebooks(HuffmanScaleFactorsBCodes, HuffmanScaleFactorsBBits, HuffmanScaleFactorsGroupSizes);

    public static final HuffmanCodebook[][][] HuffmanSpectrum = {
            generateHuffmanCodebooks(HuffmanSpectrumACodes, HuffmanSpectrumABits, HuffmanSpectrumAGroupSizes),
            generateHuffmanCodebooks(HuffmanSpectrumBCodes, HuffmanSpectrumBBits, HuffmanSpectrumBGroupSizes)
    };

    public static final double[][] ImdctWindow = {generateImdctWindow(6), generateImdctWindow(7), generateImdctWindow(8)};

    public static final Double[] SpectrumScale = generate(32, Tables::spectrumScaleFunction, Double.class);
    public static final Double[] QuantizerStepSize = generate(16, Tables::quantizerStepSizeFunction, Double.class);
    public static final Double[] QuantizerFineStepSize = generate(16, Tables::quantizerFineStepSizeFunction, Double.class);

    public static final byte[][] GradientCurves = BitAllocation.generateGradientCurves();

    private static double quantizerStepSizeFunction(int x) {
        return 2.0 / ((1 << (x + 1)) - 1);
    }

    private static double quantizerFineStepSizeFunction(int x) {
        return quantizerStepSizeFunction(x) / 0xffff /* ushort.MaxValue */;
    }

    private static double spectrumScaleFunction(int x) {
        return Math.pow(2, x - 15);
    }

    private static double[] generateImdctWindow(int frameSizePower) {
        int frameSize = 1 << frameSizePower;
        var output = new double[frameSize];

        double[] a1 = generateMdctWindow(frameSizePower);

        for (int i = 0; i < frameSize; i++) {
            output[i] = a1[i] / (a1[frameSize - 1 - i] * a1[frameSize - 1 - i] + a1[i] * a1[i]);
        }
        return output;
    }

    private static double[] generateMdctWindow(int frameSizePower) {
        int frameSize = 1 << frameSizePower;
        var output = new double[frameSize];

        for (int i = 0; i < frameSize; i++) {
            output[i] = (Math.sin(((i + 0.5) / frameSize - 0.5) * Math.PI) + 1.0) * 0.5;
        }

        return output;
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] generate(int count, Function<Integer, T> elementGenerator, Class<T> c) {
        T[] table = (T[]) Array.newInstance(c, count);
        for (int i = 0; i < count; i++) {
            table[i] = elementGenerator.apply(i);
        }
        return table;
    }
}
