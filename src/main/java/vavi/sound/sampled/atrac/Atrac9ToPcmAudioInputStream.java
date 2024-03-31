/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.atrac;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import libatrac9.Atrac9Config;
import libatrac9.Atrac9Decoder;
import libatrac9.decoder.Atrac9.At9Structure;
import libatrac9.decoder.Atrac9.At9WaveExtensible;
import libatrac9.decoder.Atrac9.fact;
import libatrac9.decoder.Atrac9.smpl;
import vavi.io.LittleEndianDataInputStream;
import vavi.io.OutputEngine;
import vavi.io.OutputEngineInputStream;
import vavi.util.Debug;
import vavi.util.win32.Chunk;
import vavi.util.win32.WAVE;
import vavi.util.win32.WAVE.fmt;

import static libatrac9.decoder.Utils.createJaggedArray;
import static libatrac9.decoder.Utils.divideByRoundUp;
import static libatrac9.decoder.Utils.shortToInterleavedByte;


/**
 * Converts an ATRAC9 bitstream into a PCM 16bits/sample audio stream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 240330 nsano initial version <br>
 */
class Atrac9ToPcmAudioInputStream extends AudioInputStream {

    /**
     * Constructor.
     *
     * @param in     the underlying input stream.
     * @param format the target format of this stream's audio data.
     * @param length the length in sample frames of the data in this stream.
     */
    public Atrac9ToPcmAudioInputStream(AudioInputStream in, AudioFormat format, long length) throws IOException {
        super(new OutputEngineInputStream(new Atrac9OutputEngine(in)), format, length);
    }

    /** decoding input stream */
    private static class Atrac9OutputEngine implements OutputEngine {

        /** */
        At9Structure structure;

        /** */
        Atrac9Decoder decoder;

        /** */
        DataOutputStream out;

        /** */
        LittleEndianDataInputStream in;

        /** */
        Atrac9OutputEngine(AudioInputStream in) throws IOException {
            this.in = new LittleEndianDataInputStream(in);

            Map<String, Object> context = new HashMap<>();
            context.put(WAVE.CHUNK_PARSE_STRICT_KEY, true);
            context.put(WAVE.MULTIPART_CHUNK_PARSE_STRICT_KEY, true);
            context.put(WAVE.WAVE_DATA_NOT_LOAD_KEY, true);
            context.put(WAVE.CHUNK_SUB_CHUNK_KEY_BASE + fact.class.getSimpleName(), fact.class);
            context.put(WAVE.CHUNK_SUB_CHUNK_KEY_BASE + smpl.class.getSimpleName(), smpl.class);
            WAVE wave = Chunk.readFrom(in, WAVE.class, context);
            fmt fmt = wave.findChildOf(WAVE.fmt.class);

            structure = new At9Structure();

            At9WaveExtensible ext = new At9WaveExtensible(fmt.getExtended());
            fact fact = wave.findChildOf(fact.class);
            WAVE.data data = wave.findChildOf(WAVE.data.class);
            smpl smpl = null;
            try {
                smpl = wave.findChildOf(smpl.class);
            } catch (IllegalArgumentException e) {
Debug.println(Level.FINER, "no smpl chunk");
            }

            structure.config = new Atrac9Config(ext.configData);
            structure.sampleCount = fact.sampleCount;
            structure.encoderDelay = ((fact.at9) fact.u).encoderDelaySamples;
            structure.version = ext.versionInfo;

            var config = new Atrac9Config(ext.configData);
            int frameCount = divideByRoundUp(fact.sampleCount + ((fact.at9) fact.u).encoderDelaySamples, config.getSuperframeSamples());
            int dataSize = frameCount * config.getSuperframeBytes();
            structure.superframeCount = frameCount;

            if (Objects.requireNonNullElse(smpl, null).loops != null) {
                structure.loopStart = smpl.loops[0].start - structure.encoderDelay;
                structure.loopEnd = smpl.loops[0].end - structure.encoderDelay;
                structure.looping = structure.loopEnd > structure.loopStart;
            }

            decoder = new Atrac9Decoder();
            decoder.initialize(config.getConfigData());

            outputCount = frameCount;
            int inputSize = dataSize / outputCount;
            int outputSize = inputSize;
            interleaveSize = config.getSuperframeBytes();

            inBlockCount = divideByRoundUp(inputSize, interleaveSize);
            outBlockCount = divideByRoundUp(outputSize, interleaveSize);
            lastInputInterleaveSize = inputSize - (inBlockCount - 1) * interleaveSize;
            lastOutputInterleaveSize = outputSize - (outBlockCount - 1) * interleaveSize;
            blocksToCopy = Math.min(inBlockCount, outBlockCount);

            pcmBuffer = createJaggedArray(short[][].class, config.getChannelCount(), config.getSuperframeSamples());
Debug.println(Level.FINER, "array: pcmBuffer, " + pcmBuffer.length + " x " + pcmBuffer[0].length);
        }

        @Override
        public void initialize(OutputStream out) throws IOException {
            if (this.out != null) {
                throw new IOException("Already initialized");
            } else {
                this.out = new DataOutputStream(out);
            }
        }

        int outputCount;
        int interleaveSize;
        int inBlockCount;
        int outBlockCount;
        int lastInputInterleaveSize;
        int lastOutputInterleaveSize;
        int blocksToCopy;

        private int b = 0;
        byte[] output;
        byte[] pcm;

        short[][] pcmBuffer;

        @Override
        public void execute() throws IOException {
            if (out == null) {
                throw new IOException("Not yet initialized");
            } else {
                if (b++ < blocksToCopy) {
                    int currentInputInterleaveSize = b == inBlockCount - 1 ? lastInputInterleaveSize : interleaveSize;
                    int currentOutputInterleaveSize = b == outBlockCount - 1 ? lastOutputInterleaveSize : interleaveSize;
                    int bytesToCopy = Math.min(currentInputInterleaveSize, currentOutputInterleaveSize);

                    for (int o = 0; o < outputCount; o++) {
                        int l = Math.min(bytesToCopy, in.available());
                        if (l > 0) {
                            if (output == null) output = new byte[bytesToCopy];
                            in.readFully(output, 0, l);
                            if (bytesToCopy < currentInputInterleaveSize) {
                                in.skipBytes(currentInputInterleaveSize - bytesToCopy);
                            }

                            decoder.decode(l < bytesToCopy ? Arrays.copyOfRange(output, 0, l) : output, pcmBuffer);

                            // TODO structure.encoderDelay, see Atract9FormatBuilder.Atrac9Format#copyBuffer()
                            if (pcm == null) pcm = new byte[pcmBuffer.length * pcmBuffer[0].length * 2];
                            byte[] audioData = shortToInterleavedByte(pcmBuffer, pcm);
                            out.write(audioData, 0, pcmBuffer.length * pcmBuffer[0].length * 2);
                        }
                    }
                } else {
                    out.close();
                }
            }
        }

        @Override
        public void finish() throws IOException {
        }
    }
}
