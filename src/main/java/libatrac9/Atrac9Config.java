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

import libatrac9.util.BitReader;


/**
 * Stores the configuration data needed to decode or encode an ATRAC9 stream.
 */
public class Atrac9Config {

    /**
     * The 4-byte ATRAC9 configuration data.
     */
    private byte[] configData;

    /**
     * A 4-bit value specifying one of 16 sample rates.
     */
    private int sampleRateIndex;
    /**
     * A 3-bit value specifying one of 6 substream channel mappings.
     */
    private int channelConfigIndex;
    /**
     * An 11-bit value containing the average size of a single frame.
     */
    private int frameBytes;
    /**
     * A 2-bit value indicating how many frames are in each superframe.
     */
    private int superframeIndex;

    /**
     * The channel mapping used by the ATRAC9 stream.
     */
    private ChannelConfig channelConfig;
    /**
     * The total number of channels in the ATRAC9 stream.
     */
    private int channelCount;
    /**
     * The sample rate of the ATRAC9 stream.
     */
    private int sampleRate;
    /**
     * Indicates whether the ATRAC9 stream has a {@link #sampleRateIndex} of 8 or above.
     */
    private boolean highSampleRate;

    /**
     * The number of frames in each superframe.
     */
    private int framesPerSuperframe;
    /**
     * The number of samples in one frame as an exponent of 2.
     * {@link #frameSamples} = 2^{@code #frameSamplesPower}.
     */
    private int frameSamplesPower;
    /**
     * The number of samples in one frame.
     */
    private int frameSamples;
    /**
     * The number of bytes in one superframe.
     */
    private int superframeBytes;
    /**
     * The number of samples in one superframe.
     */
    private int superframeSamples;

    public byte[] getConfigData() {
        return configData;
    }

    public int getSampleRateIndex() {
        return sampleRateIndex;
    }

    public int getChannelConfigIndex() {
        return channelConfigIndex;
    }

    public int getFrameBytes() {
        return frameBytes;
    }

    public int getSuperframeIndex() {
        return superframeIndex;
    }

    public libatrac9.ChannelConfig getChannelConfig() {
        return channelConfig;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public boolean isHighSampleRate() {
        return highSampleRate;
    }

    public int getFramesPerSuperframe() {
        return framesPerSuperframe;
    }

    public int getFrameSamplesPower() {
        return frameSamplesPower;
    }

    public int getFrameSamples() {
        return frameSamples;
    }

    public int getSuperframeBytes() {
        return superframeBytes;
    }

    public int getSuperframeSamples() {
        return superframeSamples;
    }

    /**
     * Reads ATRAC9 configuration data and calculates the stream parameters from it.
     *
     *@param configData The processed ATRAC9 configuration.
     */
    public Atrac9Config(byte[] configData) {
        if (configData == null || configData.length != 4) {
            throw new IllegalArgumentException("config data must be 4 bytes long");
        }

        int[] a = new int[1];
        int[] b = new int[1];
        int[] c = new int[1];
        int[] d = new int[1];
        readConfigData(configData, /* out */ a, /* out */ b, /* out */ c, /* out */ d);
        sampleRateIndex = a[0];
        channelConfigIndex = b[0];
        frameBytes = c[0];
        superframeIndex = d[0];
        this.configData = configData;

        framesPerSuperframe = 1 << superframeIndex;
        superframeBytes = frameBytes << superframeIndex;
        channelConfig = Tables.ChannelConfig[channelConfigIndex];

        channelCount = channelConfig.getChannelCount();
        sampleRate = Tables.SampleRates[sampleRateIndex];
        highSampleRate = sampleRateIndex > 7;
        frameSamplesPower = Tables.SamplingRateIndexToFrameSamplesPower[sampleRateIndex];
        frameSamples = 1 << frameSamplesPower;
        superframeSamples = frameSamples * framesPerSuperframe;
    }

    private static void readConfigData(byte[] configData, /* out */ int[] sampleRateIndex, /* out */ int[] channelConfigIndex, /* out */ int[] frameBytes, /* out */ int[] superframeIndex) {
        var reader = new BitReader(configData);

        int header = reader.readInt(8);
        sampleRateIndex[0] = reader.readInt(4);
        channelConfigIndex[0] = reader.readInt(3);
        int validationBit = reader.readInt(1);
        frameBytes[0] = reader.readInt(11) + 1;
        superframeIndex[0] = reader.readInt(2);

        if (header != 0xFE || validationBit != 0) {
            throw new IllegalArgumentException("ATRAC9 config Data is invalid");
        }
    }
}
