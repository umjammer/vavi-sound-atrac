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
import libatrac9.util.Bit;
import libatrac9.Block.BlockType;


class Unpack {

    public static void unpackFrame(BitReader reader, Frame frame) {
        for (Block block : frame.getBlocks()) {
            unpackBlock(reader, block);
        }
    }

    private static void unpackBlock(BitReader reader, Block block) {
        readBlockHeader(reader, block);

        if (block.getBlockType() == BlockType.LFE) {
            unpackLfeBlock(reader, block);
        } else {
            unpackStandardBlock(reader, block);
        }

        reader.alignPosition(8);
    }

    private static void readBlockHeader(BitReader reader, Block block) {
        boolean firstInSuperframe = block.getFrame().getFrameIndex() == 0;
        block.setFirstInSuperframe(!reader.readBool());
        block.setReuseBandParams(reader.readBool());

        if (block.isFirstInSuperframe() != firstInSuperframe) {
            throw new IllegalArgumentException();
        }

        if (firstInSuperframe && block.isReuseBandParams() && block.getBlockType() != BlockType.LFE) {
            throw new IllegalArgumentException();
        }
    }

    private static void unpackStandardBlock(BitReader reader, Block block) {
        Channel[] channels = block.getChannels();

        if (!block.isReuseBandParams()) {
            readBandParams(reader, block);
        }

        readGradientParams(reader, block);
        BitAllocation.createGradient(block);
        readStereoParams(reader, block);
        readExtensionParams(reader, block);

        for (Channel channel : channels) {
            channel.UpdateCodedUnits();

            ScaleFactors.read(reader, channel);
            BitAllocation.calculateMask(channel);
            BitAllocation.calculatePrecisions(channel);
            calculateSpectrumCodebookIndex(channel);

            readSpectra(reader, channel);
            readSpectraFine(reader, channel);
        }

        block.setQuantizationUnitsPrev(block.isBandExtensionEnabled() ? block.getExtensionUnit() : block.getQuantizationUnitCount());
    }

    private static void readBandParams(BitReader reader, Block block) {
        int minBandCount = Tables.minBandCount(block.getConfig().isHighSampleRate());
        int maxExtensionBand = Tables.maxExtensionBand(block.getConfig().isHighSampleRate());
        block.setBandCount(reader.readInt(4));
        block.setBandCount(block.getBandCount() + minBandCount);
        block.setQuantizationUnitCount(Tables.BandToQuantUnitCount[block.getBandCount()]);
        if (block.getBandCount() < minBandCount || block.getBandCount() >
                Tables.MaxBandCount[block.getConfig().getSampleRateIndex()]) {
            return;
        }

        if (block.getBlockType() == BlockType.Stereo) {
            block.setStereoBand(reader.readInt(4));
            block.setStereoBand(block.getStereoBand() + minBandCount);
            block.setStereoQuantizationUnit(Tables.BandToQuantUnitCount[block.getStereoBand()]);
        } else {
            block.setStereoBand(block.getBandCount());
        }

        block.setBandExtensionEnabled(reader.readBool());
        if (block.isBandExtensionEnabled()) {
            block.setExtensionBand(reader.readInt(4));
            block.setExtensionBand(block.getExtensionBand() + minBandCount);

            if (block.getExtensionBand() < block.getBandCount() || block.getExtensionBand() > maxExtensionBand) {
                throw new IllegalArgumentException();
            }

            block.setExtensionUnit(Tables.BandToQuantUnitCount[block.getExtensionBand()]);
        } else {
            block.setExtensionBand(block.getBandCount());
            block.setExtensionUnit(block.getQuantizationUnitCount());
        }
    }

    private static void readGradientParams(BitReader reader, Block block) {
        block.setGradientMode(reader.readInt(2));
        if (block.getGradientMode() > 0) {
            block.setGradientEndUnit(31);
            block.setGradientEndValue(31);
            block.setGradientStartUnit(reader.readInt(5));
            block.setGradientStartValue(reader.readInt(5));
        } else {
            block.setGradientStartUnit(reader.readInt(6));
            block.setGradientEndUnit(reader.readInt(6) + 1);
            block.setGradientStartValue(reader.readInt(5));
            block.setGradientEndValue(reader.readInt(5));
        }
        block.setGradientBoundary(reader.readInt(4));

        if (block.getGradientBoundary() > block.getQuantizationUnitCount()) {
            throw new IllegalArgumentException();
        }
        if (block.getGradientStartUnit() < 1 || block.getGradientStartUnit() >= 48) {
            throw new IllegalArgumentException();
        }
        if (block.getGradientEndUnit() < 1 || block.getGradientEndUnit() >= 48) {
            throw new IllegalArgumentException();
        }
        if (block.getGradientStartUnit() > block.getGradientEndUnit()) {
            throw new IllegalArgumentException();
        }
        if (block.getGradientStartValue() < 0 || block.getGradientStartValue() >= 32) {
            throw new IllegalArgumentException();
        }
        if (block.getGradientEndValue() < 0 || block.getGradientEndValue() >= 32) {
            throw new IllegalArgumentException();
        }
    }

    private static void readStereoParams(BitReader reader, Block block) {
        if (block.getBlockType() != BlockType.Stereo) return;

        block.setPrimaryChannelIndex(reader.readInt(1));
        block.setHasJointStereoSigns(reader.readBool());
        if (block.isHasJointStereoSigns()) {
            for (int i = block.getStereoQuantizationUnit(); i < block.getQuantizationUnitCount(); i++) {
                block.getJointStereoSigns()[i] = reader.readInt(1);
            }
        } else {
            Arrays.fill(block.getJointStereoSigns(), 0, block.getJointStereoSigns().length, 0);
        }
    }

    private static void readExtensionParams(BitReader reader, Block block) {
        // ReSharper disable once RedundantAssignment
        int[] bexBand = new int[1];
        if (block.isBandExtensionEnabled()) {
            int[] dummy1 = new int[1];
            int[] dummy2 = new int[1];
            BandExtension.getBexBandInfo(/* out */ bexBand, /* out */ dummy1, /* out */ dummy2, block.getQuantizationUnitCount());
            if (block.getBlockType() == BlockType.Stereo) {
                readHeader(reader, bexBand[0], block.getChannels()[1]);
            } else {
                reader.position += 1;
            }
        }
        block.setHasExtensionData(reader.readBool());

        if (!block.isHasExtensionData()) return;
        if (!block.isBandExtensionEnabled()) {
            block.setBexMode(reader.readInt(2));
            block.setBexDataLength(reader.readInt(5));
            reader.position += block.getBexDataLength();
            return;
        }

        readHeader(reader, bexBand[0], block.getChannels()[0]);

        block.setBexDataLength(reader.readInt(5));
        if (block.getBexDataLength() <= 0) return;
        int bexDataEnd = reader.position + block.getBexDataLength();

        readData(reader, bexBand[0], block.getChannels()[0]);

        if (block.getBlockType() == BlockType.Stereo) {
            readData(reader, bexBand[0], block.getChannels()[1]);
        }

        // Make sure we didn't read too many bits
        if (reader.position > bexDataEnd) {
            throw new IllegalArgumentException();
        }
    }

    private static void readHeader(BitReader reader, int bexBand, Channel channel) {
        int bexMode = reader.readInt(2);
        channel.setBexMode(bexBand > 2 ? bexMode : 4);
        channel.setBexValueCount(BandExtension.BexEncodedValueCounts[channel.getBexMode()][bexBand]);
    }

    private static void readData(BitReader reader, int bexBand, Channel channel) {
        for (int i = 0; i < channel.getBexValueCount(); i++) {
            int dataLength = BandExtension.BexDataLengths[channel.getBexMode()][bexBand][i];
            channel.getBexValues()[i] = reader.readInt(dataLength);
        }
    }

    private static void calculateSpectrumCodebookIndex(Channel channel) {
        Arrays.fill(channel.getCodebookSet(), 0, channel.getCodebookSet().length, 0);
        int quantUnits = channel.getCodedQuantUnits();
        int[] sf = channel.getScaleFactors();

        if (quantUnits <= 1) return;
        if (channel.getConfig().isHighSampleRate()) return;

        // Temporarily setting this value allows for simpler code by
        // making the last value a non-special case.
        int originalScaleTmp = sf[quantUnits];
        sf[quantUnits] = sf[quantUnits - 1];

        int avg = 0;
        if (quantUnits > 12) {
            for (int i = 0; i < 12; i++) {
                avg += sf[i];
            }
            avg = (avg + 6) / 12;
        }

        for (int i = 8; i < quantUnits; i++) {
            int prevSf = sf[i - 1];
            int nextSf = sf[i + 1];
            int minSf = Math.min(prevSf, nextSf);
            if (sf[i] - minSf >= 3 || sf[i] - prevSf + sf[i] - nextSf >= 3) {
                channel.getCodebookSet()[i] = 1;
            }
        }

        for (int i = 12; i < quantUnits; i++) {
            if (channel.getCodebookSet()[i] == 0) {
                int minSf = Math.min(sf[i - 1], sf[i + 1]);
                if (sf[i] - minSf >= 2 && sf[i] >= avg - (Tables.QuantUnitToCoeffCount[i] == 16 ? 1 : 0)) {
                    channel.getCodebookSet()[i] = 1;
                }
            }
        }

        sf[quantUnits] = originalScaleTmp;
    }

    private static void readSpectra(BitReader reader, Channel channel) {
        int[] values = channel.getSpectraValuesBuffer();
        Arrays.fill(channel.getQuantizedSpectra(), 0, channel.getQuantizedSpectra().length, 0);
        int maxHuffPrecision = Tables.maxHuffPrecision(channel.getConfig().isHighSampleRate());

        for (int i = 0; i < channel.getCodedQuantUnits(); i++) {
            int subbandCount = Tables.QuantUnitToCoeffCount[i];
            int precision = channel.getPrecisions()[i] + 1;
            if (precision <= maxHuffPrecision) {
                HuffmanCodebook huff = Tables.HuffmanSpectrum[channel.getCodebookSet()[i]][precision][Tables.QuantUnitToCodebookIndex[i]];
                int groupCount = subbandCount >> huff.ValueCountPower;
                for (int j = 0; j < groupCount; j++) {
                    values[j] = readHuffmanValue(huff, reader, false);
                }

                decodeHuffmanValues(channel.getQuantizedSpectra(), Tables.QuantUnitToCoeffIndex[i], subbandCount, huff, values);
            } else {
                int subbandIndex = Tables.QuantUnitToCoeffIndex[i];
                for (int j = subbandIndex; j < Tables.QuantUnitToCoeffIndex[i + 1]; j++) {
                    channel.getQuantizedSpectra()[j] = reader.readSignedInt(precision);
                }
            }
        }
    }

    private static void readSpectraFine(BitReader reader, Channel channel) {
        Arrays.fill(channel.getQuantizedSpectraFine(), 0, channel.getQuantizedSpectraFine().length, 0);

        for (int i = 0; i < channel.getCodedQuantUnits(); i++) {
            if (channel.getPrecisionsFine()[i] > 0) {
                int overflowBits = channel.getPrecisionsFine()[i] + 1;
                int startSubband = Tables.QuantUnitToCoeffIndex[i];
                int endSubband = Tables.QuantUnitToCoeffIndex[i + 1];

                for (int j = startSubband; j < endSubband; j++) {
                    channel.getQuantizedSpectraFine()[j] = reader.readSignedInt(overflowBits);
                }
            }
        }
    }

    private static void decodeHuffmanValues(int[] spectrum, int index, int bandCount, HuffmanCodebook huff, int[] values) {
        int valueCount = bandCount >> huff.ValueCountPower;
        int mask = (1 << huff.ValueBits) - 1;

        for (int i = 0; i < valueCount; i++) {
            int value = values[i];
            for (int j = 0; j < huff.ValueCount; j++) {
                spectrum[index++] = Bit.signExtend32(value & mask, huff.ValueBits);
                value >>= huff.ValueBits;
            }
        }
    }

    public static int readHuffmanValue(HuffmanCodebook huff, BitReader reader, boolean signed /* = false */) {
        int code = reader.peekInt(huff.MaxBitSize);
        byte value = huff.Lookup[code];
        int bits = huff.Bits[value];
        reader.position += bits;
        return signed ? Bit.signExtend32(value, huff.ValueBits) : value;
    }

    private static void unpackLfeBlock(BitReader reader, Block block) {
        Channel channel = block.getChannels()[0];
        block.setQuantizationUnitCount(2);

        decodeLfeScaleFactors(reader, channel);
        calculateLfePrecision(channel);
        channel.setCodedQuantUnits(block.getQuantizationUnitCount());
        readLfeSpectra(reader, channel);
    }

    private static void decodeLfeScaleFactors(BitReader reader, Channel channel) {
        Arrays.fill(channel.getScaleFactors(), 0, channel.getScaleFactors().length, 0);
        for (int i = 0; i < channel.getBlock().getQuantizationUnitCount(); i++) {
            channel.getScaleFactors()[i] = reader.readInt(5);
        }
    }

    private static void calculateLfePrecision(Channel channel) {
        Block block = channel.getBlock();
        int precision = block.isReuseBandParams() ? 8 : 4;
        for (int i = 0; i < block.getQuantizationUnitCount(); i++) {
            channel.getPrecisions()[i] = precision;
            channel.getPrecisionsFine()[i] = 0;
        }
    }

    private static void readLfeSpectra(BitReader reader, Channel channel) {
        Arrays.fill(channel.getQuantizedSpectra(), 0, channel.getQuantizedSpectra().length, 0);

        for (int i = 0; i < channel.getCodedQuantUnits(); i++) {
            if (channel.getPrecisions()[i] <= 0) continue;

            int precision = channel.getPrecisions()[i] + 1;
            for (int j = Tables.QuantUnitToCoeffIndex[i]; j < Tables.QuantUnitToCoeffIndex[i + 1]; j++) {
                channel.getQuantizedSpectra()[j] = reader.readSignedInt(precision);
            }
        }
    }
}
