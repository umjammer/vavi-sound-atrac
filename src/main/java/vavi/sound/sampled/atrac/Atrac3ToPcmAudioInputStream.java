/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.atrac;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import jpcsp.media.codec.ICodec;
import jpcsp.media.codec.atrac3.Atrac3Decoder;
import jpcsp.media.codec.atrac3plus.Atrac3plusDecoder;
import vavi.io.OutputEngine;
import vavi.io.OutputEngineInputStream;
import vavi.util.ByteUtil;
import vavi.util.Debug;


/**
 * Converts an ATRAC bitstream into a PCM 16bits/sample audio stream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 231008 nsano initial version <br>
 */
class Atrac3ToPcmAudioInputStream extends AudioInputStream {

    /**
     * Constructor.
     *
     * @param in     the underlying input stream.
     * @param format the target format of this stream's audio data.
     * @param length the length in sample frames of the data in this stream.
     */
    public Atrac3ToPcmAudioInputStream(AudioInputStream in, AudioFormat format, long length) throws IOException {
        super(new OutputEngineInputStream(new Atrac3OutputEngine(in)), format, length);
    }

    /** */
    private static class Atrac3OutputEngine implements OutputEngine {

        public static final int PSP_CODEC_AT3PLUS = 0x00001000;
        public static final int PSP_CODEC_AT3 = 0x00001001;

        public static final int RIFF_MAGIC = 0x46464952; // "RIFF"
        public static final int FMT_CHUNK_MAGIC = 0x20746D66; // "FMT "
        public static final int DATA_CHUNK_MAGIC = 0x61746164; // "DATA"

        /** */
        private ByteBuffer in;

        /** */
        private DataOutputStream out;

        /** */
        private final ICodec decoder;

        /** */
        private int inputAddr;

        /** */
        private int length;
        /** */
        private int channels = 2;
        /** */
        private int bytesPerFrame = 0;

        /** */
        public Atrac3OutputEngine(AudioInputStream in) throws IOException {
            byte[] inBuf = in.readAllBytes();
            this.in = ByteBuffer.wrap(inBuf).order(ByteOrder.LITTLE_ENDIAN);
            this.length = this.in.capacity();

            int codecType = -1;
            int codingMode = 0;
            int dataOffset = 0;
            if (ByteUtil.readLeInt(inBuf, inputAddr) == RIFF_MAGIC) {
                int scanOffset = 12;
                while (dataOffset <= 0) {
                    int chunkMagic = ByteUtil.readLeInt(inBuf,inputAddr + scanOffset);
                    int chunkLength = ByteUtil.readLeInt(inBuf,inputAddr + scanOffset + 4);
                    scanOffset += 8;
byte[] m = new byte[4];
ByteUtil.writeLeInt(chunkMagic, m);
Debug.printf(Level.FINER, "@CHUNK: %c%c%c%c, offset: %d, length: %d", m[0], m[1], m[2], m[3], scanOffset, chunkLength);
                    switch (chunkMagic) {
                    case FMT_CHUNK_MAGIC:
                        codecType = switch (ByteUtil.readLeShort(inBuf, inputAddr + scanOffset) & 0xffff) {
                            case AtracEncoding.AT3_PLUS_MAGIC -> PSP_CODEC_AT3PLUS;
                            case AtracEncoding.AT3_MAGIC -> PSP_CODEC_AT3;
                            default -> codecType;
                        };
                        channels = ByteUtil.readLeShort(inBuf, inputAddr + scanOffset + 2);
Debug.println(Level.FINER, "channels: " + channels);
                        bytesPerFrame = ByteUtil.readLeShort(inBuf, inputAddr + scanOffset + 12);
Debug.println(Level.FINER, "bytesPerFrame: " + bytesPerFrame);
                        int extraDataSize = ByteUtil.readLeShort(inBuf, inputAddr + scanOffset + 16);
                        if (extraDataSize == 14) {
                            codingMode = ByteUtil.readLeShort(inBuf, inputAddr + scanOffset + 18 + 6);
                        }
                        break;
                    case DATA_CHUNK_MAGIC:
                        dataOffset = scanOffset;
                        break;
                    }
                    scanOffset += chunkLength;
                }
            } else {
                throw new IllegalArgumentException("not in RIFF format");
            }

            this.decoder = switch (codecType) {
                case PSP_CODEC_AT3 -> new Atrac3Decoder();
                case PSP_CODEC_AT3PLUS ->  new Atrac3plusDecoder();
                default -> throw new IllegalArgumentException("not atrac3");
            };
Debug.println(Level.FINER, "codec: " + this.decoder);
            this.decoder.init(bytesPerFrame, channels, channels, codingMode);

            this.inputAddr += dataOffset;
Debug.println(Level.FINER, "inputAddr: " + inputAddr);
            this.length -= dataOffset;
        }

        @Override
        public void initialize(OutputStream out) throws IOException {
            if (this.out != null) {
                throw new IOException("Already initialized");
            } else {
                this.out = new DataOutputStream(out);
            }
        }

        /** */
        private int frameNbr;

        @Override
        public void execute() throws IOException {
            if (out == null) {
                throw new IOException("Not yet initialized");
            } else {
                ByteBuffer outBuf = ByteBuffer.allocate(decoder.getNumberOfSamples() * 2 * channels).order(ByteOrder.LITTLE_ENDIAN);
                int result = decoder.decode(in, inputAddr, length, outBuf, 0);
                if (result < 0) {
Debug.printf(Level.WARNING, "Frame #%d, result 0x%X", frameNbr, result);
                    throw new IllegalStateException(String.format("Frame #%d, result 0x%08X", frameNbr, result));
                }
                if (result == 0) {
Debug.printf(Level.FINER, "Frame #%d, EOF", frameNbr);
                    out.close();
                    return;
                }
                int consumedBytes = bytesPerFrame;
                if (result < bytesPerFrame - 2 || result > bytesPerFrame) {
                    if (bytesPerFrame == 0) {
                        consumedBytes = result;
                    } else {
Debug.printf(Level.WARNING, "Frame #%d, result 0x%X, expected 0x%X", frameNbr, result, bytesPerFrame);
                    }
                }

                inputAddr += consumedBytes;
                length -= consumedBytes;

                out.write(outBuf.array(), 0, decoder.getNumberOfSamples() * 2 * channels);

                frameNbr++;
            }
        }

        @Override
        public void finish() throws IOException {
        }
    }
}
