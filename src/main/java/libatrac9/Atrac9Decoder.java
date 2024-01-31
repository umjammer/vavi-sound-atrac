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
import libatrac9.util.Helpers;


/**
 * Decodes an ATRAC9 stream into 16-bit PCM.
 */
public class Atrac9Decoder {

    /**
     * The config data for the current ATRAC9 stream.
     */
    private Atrac9Config config;

    private Frame frame;
    private BitReader reader;

    public Atrac9Config getConfig() {
        return config;
    }

    public void setConfig(Atrac9Config config) {
        this.config = config;
    }

    public Frame getFrame() {
        return frame;
    }

    public void setFrame(Frame frame) {
        this.frame = frame;
    }

    public BitReader getReader() {
        return reader;
    }

    public void setReader(BitReader reader) {
        this.reader = reader;
    }

    private boolean initialized;

    /**
     * Sets up the decoder to decode an ATRAC9 stream based on the information in {@code configData}.
     * @param configData A 4-byte value containing information about the ATRAC9 stream.
     */
    public void initialize(byte[] configData) {
        config = new Atrac9Config(configData);
        frame = new Frame(config);
        reader = new BitReader(null);
        initialized = true;
    }

    /**
     * Decodes one superframe of ATRAC9 data.
     * @param atrac9Data The ATRAC9 data to decode. The array must be at least
     * {@link #config}.{@link Atrac9Config#getSuperframeBytes()} bytes long.
     * @param pcmOut A buffer that the decoded PCM data will be placed in.
     * The array must have dimensions of at least [{@link #config}.{@link Atrac9Config#getChannelCount()}]
     * [{@link #config}.{@link Atrac9Config#getSuperframeSamples()}].
     */
    public void decode(byte[] atrac9Data, short[][] pcmOut) {
        if (!initialized) throw new IllegalStateException("Decoder must be initialized before decoding.");

        validateDecodeBuffers(atrac9Data, pcmOut);
        reader.setBuffer(atrac9Data);
        decodeSuperFrame(pcmOut);
    }

    private void validateDecodeBuffers(byte[] atrac9Buffer, short[][] pcmBuffer) {
        if (atrac9Buffer == null) throw new NullPointerException("atrac9Buffer");
        if (pcmBuffer == null) throw new NullPointerException("pcmBuffer");

        if (atrac9Buffer.length < config.getSuperframeBytes()) {
            throw new IllegalArgumentException("ATRAC9 buffer is too small");
        }

        if (pcmBuffer.length < config.getChannelCount()) {
            throw new IllegalArgumentException("PCM buffer is too small");
        }

        for (int i = 0; i < config.getChannelCount(); i++) {
            if (pcmBuffer[i] != null && pcmBuffer[i].length < config.getSuperframeSamples()) {
                throw new IllegalArgumentException("PCM buffer is too small");
            }
        }
    }

    private void decodeSuperFrame(short[][] pcmOut) {
        for (int i = 0; i < config.getFramesPerSuperframe(); i++) {
            frame.setFrameIndex(i);
            decodeFrame(reader, frame);
            pcmFloatToShort(pcmOut, i * config.getFrameSamples());
            reader.alignPosition(8);
        }
    }

    private void pcmFloatToShort(short[][] pcmOut, int start) {
        int endSample = start + config.getFrameSamples();
        int channelNum = 0;
        for (Block block : frame.getBlocks()) {
            for (Channel channel : block.getChannels()) {
                double[] pcmSrc = channel.getPcm();
                short[] pcmDest = pcmOut[channelNum++];
                for (int d = 0, s = start; s < endSample; d++, s++) {
                    double sample = pcmSrc[d];
                    // Not using Math.Round because it's ~20x slower on 64-bit
                    int roundedSample = (int) Math.floor(sample + 0.5);
                    pcmDest[s] = Helpers.clamp16(roundedSample);
                }
            }
        }
    }

    private static void decodeFrame(BitReader reader, Frame frame) {
        Unpack.unpackFrame(reader, frame);

        for (Block block : frame.getBlocks()) {
            Quantization.dequantizeSpectra(block);
            Stereo.applyIntensityStereo(block);
            Quantization.scaleSpectrum(block);
            BandExtension.applyBandExtension(block);
            imdctBlock(block);
        }
    }

    private static void imdctBlock(Block block) {
        for (Channel channel : block.getChannels()) {
            channel.getMdct().runImdct(channel.getSpectra(), channel.getPcm());
        }
    }
}

