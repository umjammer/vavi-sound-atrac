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

import java.util.Arrays;

import libatrac9.util.BitReader;


class ScaleFactors {

    public static void read(BitReader reader, Channel channel) {
        Arrays.fill(channel.getScaleFactors(), 0, channel.getScaleFactors().length, 0);

        channel.setScaleFactorCodingMode(reader.readInt(2));
        if (channel.getChannelIndex() == 0) {
            switch (channel.getScaleFactorCodingMode()) {
            case 0:
                readVlcDeltaOffset(reader, channel);
                break;
            case 1:
                readClcOffset(reader, channel);
                break;
            case 2:
                if (channel.getBlock().isFirstInSuperframe()) throw new IllegalArgumentException();
                readVlcDistanceToBaseline(reader, channel, channel.getScaleFactorsPrev(), channel.getBlock().getQuantizationUnitsPrev());
                break;
            case 3:
                if (channel.getBlock().isFirstInSuperframe()) throw new IllegalArgumentException();
                readVlcDeltaOffsetWithBaseline(reader, channel, channel.getScaleFactorsPrev(), channel.getBlock().getQuantizationUnitsPrev());
                break;
            }
        } else {
            switch (channel.getScaleFactorCodingMode()) {
            case 0:
                readVlcDeltaOffset(reader, channel);
                break;
            case 1:
                readVlcDistanceToBaseline(reader, channel, channel.getBlock().getChannels()[0].getScaleFactors(), channel.getBlock().getExtensionUnit());
                break;
            case 2:
                readVlcDeltaOffsetWithBaseline(reader, channel, channel.getBlock().getChannels()[0].getScaleFactors(), channel.getBlock().getExtensionUnit());
                break;
            case 3:
                if (channel.getBlock().isFirstInSuperframe()) throw new IllegalArgumentException();
                readVlcDistanceToBaseline(reader, channel, channel.getScaleFactorsPrev(), channel.getBlock().getQuantizationUnitsPrev());
                break;
            }
        }

        for (int i = 0; i < channel.getBlock().getExtensionUnit(); i++) {
            if (channel.getScaleFactors()[i] < 0 || channel.getScaleFactors()[i] > 31) {
                throw new IllegalArgumentException("Scale factor values are out of range.");
            }
        }

        System.arraycopy(channel.getScaleFactors(), 0, channel.getScaleFactorsPrev(), 0, channel.getScaleFactors().length);
    }

    private static void readClcOffset(BitReader reader, Channel channel) {
        final int maxBits = 5;
        int[] sf = channel.getScaleFactors();
        int bitLength = reader.readInt(2) + 2;
        int baseValue = bitLength < maxBits ? reader.readInt(maxBits) : 0;

        for (int i = 0; i < channel.getBlock().getExtensionUnit(); i++) {
            sf[i] = reader.readInt(bitLength) + baseValue;
        }
    }

    private static void readVlcDeltaOffset(BitReader reader, Channel channel) {
        int weightIndex = reader.readInt(3);
        byte[] weights = ScaleFactorWeights[weightIndex];

        int[] sf = channel.getScaleFactors();
        int baseValue = reader.readInt(5);
        int bitLength = reader.readInt(2) + 3;
        HuffmanCodebook codebook = Tables.HuffmanScaleFactorsUnsigned[bitLength];

        sf[0] = reader.readInt(bitLength);

        for (int i = 1; i < channel.getBlock().getExtensionUnit(); i++) {
            int delta = Unpack.readHuffmanValue(codebook, reader, false);
            sf[i] = (sf[i - 1] + delta) & (codebook.valueMax - 1);
        }

        for (int i = 0; i < channel.getBlock().getExtensionUnit(); i++) {
            sf[i] += baseValue - (weights[i] & 0xff);
        }
    }

    private static void readVlcDistanceToBaseline(BitReader reader, Channel channel, int[] baseline, int baselineLength) {
        int[] sf = channel.getScaleFactors();
        int bitLength = reader.readInt(2) + 2;
        HuffmanCodebook codebook = Tables.HuffmanScaleFactorsSigned[bitLength];
        int unitCount = Math.min(channel.getBlock().getExtensionUnit(), baselineLength);

        for (int i = 0; i < unitCount; i++) {
            int distance = Unpack.readHuffmanValue(codebook, reader, true);
            sf[i] = (baseline[i] + distance) & 31;
        }

        for (int i = unitCount; i < channel.getBlock().getExtensionUnit(); i++) {
            sf[i] = reader.readInt(5);
        }
    }

    private static void readVlcDeltaOffsetWithBaseline(BitReader reader, Channel channel, int[] baseline, int baselineLength) {
        int[] sf = channel.getScaleFactors();
        int baseValue = reader.readOffsetBinary(5, BitReader.OffsetBias.Negative);
        int bitLength = reader.readInt(2) + 1;
        HuffmanCodebook codebook = Tables.HuffmanScaleFactorsUnsigned[bitLength];
        int unitCount = Math.min(channel.getBlock().getExtensionUnit(), baselineLength);

        sf[0] = reader.readInt(bitLength);

        for (int i = 1; i < unitCount; i++) {
            int delta = Unpack.readHuffmanValue(codebook, reader, false);
            sf[i] = (sf[i - 1] + delta) & (codebook.valueMax - 1);
        }

        for (int i = 0; i < unitCount; i++) {
            sf[i] += baseValue + baseline[i];
        }

        for (int i = unitCount; i < channel.getBlock().getExtensionUnit(); i++) {
            sf[i] = reader.readInt(5);
        }
    }

    public static final byte[][] ScaleFactorWeights = {
            new byte[] {
                    0, 0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 3, 2, 3, 3, 4, 4, 4, 4, 4, 4, 5, 5, 6, 6, 7, 7, 8, 10, 12, 12, 12
            }, new byte[] {
                    3, 2, 2, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 2, 3, 3, 4, 5, 7, 10, 10, 10
            }, new byte[] {
                    0, 2, 4, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 8, 9, 12, 12, 12
            }, new byte[] {
                    0, 1, 1, 2, 2, 2, 3, 3, 3, 3, 3, 4, 4, 4, 5, 5, 5, 6, 6, 6, 6, 7, 8, 8, 10, 11, 11, 12, 13, 13, 13, 13
            }, new byte[] {
                    0, 2, 2, 3, 3, 4, 4, 5, 4, 5, 5, 5, 5, 6, 7, 8, 8, 8, 8, 9, 9, 9, 10, 10, 11, 12, 12, 13, 13, 14, 14, 14
            }, new byte[] {
                    1, 1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 4, 4, 5, 6, 7, 7, 9, 11, 11, 11
            }, new byte[] {
                    0, 5, 8, 10, 11, 11, 12, 12, 12, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 12, 12, 12,
                    12, 13, 15, 15, 15
            }, new byte[] {
                    0, 2, 3, 4, 5, 6, 6, 7, 7, 8, 8, 8, 9, 9, 10, 10, 10, 11, 11, 11, 11, 11, 11, 12, 12, 12, 12, 13, 13,
                    15, 15, 15
            }
    };
}
