/*
 * https://github.com/korlibs-archive/korau-atrac3plus/blob/581183e01edab4b90a86b46ad6998a20f213e367/korau-atrac3plus/common/src/main/kotlin/com/soywiz/korau/format/atrac3plus/util/Atrac3PlusUtil.kt
 */

package jpcsp.media.codec.atrac3;

import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.lang.System.Logger;

import static java.lang.System.getLogger;


public class Atrac3Util {

    static final Logger logger = getLogger(Atrac3Util.class.getName());

    public static final int PSP_CODEC_AT3PLUS = 0x00001000;
    public static final int PSP_CODEC_AT3 = 0x00001001;
    public static final int PSP_CODEC_MP3 = 0x00001002;
    public static final int PSP_CODEC_AAC = 0x00001003;

    public static final int AT3_MAGIC = 0x0270; // "AT3"
    public static final int AT3_PLUS_MAGIC = 0xFFFE; // "AT3PLUS"
    public static final int RIFF_MAGIC = 0x46464952; // "RIFF"
    public static final int WAVE_MAGIC = 0x45564157; // "WAVE"
    public static final int FMT_CHUNK_MAGIC = 0x20746D66; // "FMT "
    protected static final int FACT_CHUNK_MAGIC = 0x74636166; // "FACT"
    protected static final int SMPL_CHUNK_MAGIC = 0x6C706D73; // "SMPL"
    public static final int DATA_CHUNK_MAGIC = 0x61746164; // "DATA"

    private static final int ATRAC3_CONTEXT_READ_SIZE_OFFSET = 160;
    private static final int ATRAC3_CONTEXT_REQUIRED_SIZE_OFFSET = 164;
    private static final int ATRAC3_CONTEXT_DECODE_RESULT_OFFSET = 188;

    public static final int PSP_ATRAC_ALLDATA_IS_ON_MEMORY = -1;
    public static final int PSP_ATRAC_NONLOOP_STREAM_DATA_IS_ON_MEMORY = -2;
    public static final int PSP_ATRAC_LOOP_STREAM_DATA_IS_ON_MEMORY = -3;

    protected static final int PSP_ATRAC_STATUS_NONLOOP_STREAM_DATA = 0;
    protected static final int PSP_ATRAC_STATUS_LOOP_STREAM_DATA = 1;

    public static final int ATRAC_HEADER_HASH_LENGTH = 512;

    public static final int atracDecodeDelay = 2300; // Microseconds, based on PSP tests

    public final static int ERROR_ATRAC_UNKNOWN_FORMAT = 0x80630006;
    public final static int ERROR_ATRAC_INVALID_SIZE = 0x80630011;

    protected static String getStringFromInt32(int n) {
        char c1 = (char) ((n) & 0xFF);
        char c2 = (char) ((n >> 8) & 0xFF);
        char c3 = (char) ((n >> 16) & 0xFF);
        char c4 = (char) ((n >> 24) & 0xFF);

        return String.format("%c%c%c%c", c1, c2, c3, c4);
    }

    public static int analyzeRiffFile(ByteBuffer mem, int addr, int length, AtracFileInfo info) {
        int result = ERROR_ATRAC_UNKNOWN_FORMAT;

logger.log(Level.DEBUG, "addr: " + addr);
        mem.position(addr);
        int bufferSize = length;
        info.atracEndSample = -1;
        info.numLoops = 0;
        info.inputFileDataOffset = 0;

        if (bufferSize < 12) {
            logger.log(Level.ERROR, String.format("Atrac buffer too small %d", bufferSize));
            return ERROR_ATRAC_INVALID_SIZE;
        }

        // RIFF file format:
        // Offset 0: 'RIFF'
        // Offset 4: file length - 8
        // Offset 8: 'WAVE'
        int magic = mem.getInt();
        info.inputFileSize = mem.getInt() + 8;
        int WAVEMagic = mem.getInt();
        if (magic != RIFF_MAGIC || WAVEMagic != WAVE_MAGIC) {
logger.log(Level.INFO, "constant RIFF/WAVE format: " + getStringFromInt32(RIFF_MAGIC) + ", " + getStringFromInt32(WAVE_MAGIC));
logger.log(Level.ERROR, "Not a RIFF/WAVE format: " + getStringFromInt32(magic) + ", " + getStringFromInt32(WAVEMagic));
            return ERROR_ATRAC_UNKNOWN_FORMAT;
        }

        info.inputDataSize = info.inputFileSize;
logger.log(Level.TRACE, String.format("FileSize %d", info.inputFileSize));
        bufferSize -= 12;

        boolean foundData = false;
        while (bufferSize >= 8 && !foundData) {
            int chunkMagic = mem.getInt();
            int chunkSize = mem.getInt();
            bufferSize -= 8;
logger.log(Level.TRACE, "@CHUNK: " + getStringFromInt32(chunkMagic) + ", offset: " + mem.position() + ", length: " + chunkSize);

            int currentAddr = mem.position();

            switch (chunkMagic) {
            case DATA_CHUNK_MAGIC:
                foundData = true;
                // Offset of the data chunk in the input file
                info.inputFileDataOffset = mem.position() - addr;
                info.inputDataSize = chunkSize;
logger.log(Level.TRACE, String.format("DATA Chunk: data offset=%d, data size=%d", info.inputFileDataOffset, info.inputDataSize));
                break;
            case FMT_CHUNK_MAGIC: {
                if (chunkSize >= 16) {
                    int compressionCode = mem.getShort() & 0xffff;
                    info.atracChannels = mem.getShort() & 0xffff;
                    info.atracSampleRate = mem.getInt();
                    info.atracBitrate = mem.getInt();
                    info.atracBytesPerFrame = mem.getShort() & 0xffff;
                    int hiBytesPerSample = mem.getShort() & 0xffff;
                    int extraDataSize = mem.getShort() & 0xffff;
                    if (extraDataSize == 14) {
                        mem.getInt(); // +4
                        mem.getShort(); // +2
                        info.atracCodingMode = mem.getShort() & 0xffff;
                    }
if (logger.isLoggable(Level.TRACE)) {
    logger.log(Level.TRACE, String.format("WAVE format: magic=0x%08X('%s'), chunkSize=%d, compressionCode=0x%04X, channels=%d, sampleRate=%d, bitrate=%d, bytesPerFrame=0x%X, hiBytesPerSample=%d, codingMode=%d", chunkMagic, getStringFromInt32(chunkMagic), chunkSize, compressionCode, info.atracChannels, info.atracSampleRate, info.atracBitrate, info.atracBytesPerFrame, hiBytesPerSample, info.atracCodingMode));
    // Display rest of chunk as debug information
    StringBuilder restChunk = new StringBuilder();
    for (int i = 16; i < chunkSize; i++) {
        int b = mem.get() & 0xff;
        restChunk.append(String.format(" %02X", b));
    }
    if (!restChunk.isEmpty()) {
        logger.log(Level.TRACE, String.format("Additional chunk data:%s", restChunk));
    }
}

                    if (compressionCode == AT3_MAGIC) {
                        result = PSP_CODEC_AT3;
                    } else if (compressionCode == AT3_PLUS_MAGIC) {
                        result = PSP_CODEC_AT3PLUS;
                    } else {
                        return ERROR_ATRAC_UNKNOWN_FORMAT;
                    }
                }
                break;
            }
            case FACT_CHUNK_MAGIC: {
                if (chunkSize >= 8) {
                    info.atracEndSample = mem.getInt();
                    if (info.atracEndSample > 0) {
                        info.atracEndSample -= 1;
                    }
                    if (chunkSize >= 12) {
                        // Is the value at offset 4 ignored?
                        mem.getInt(); // +4 (↑+4)
                        info.atracSampleOffset = mem.getInt(); // The loop samples are offset by this value
                    } else {
                        info.atracSampleOffset = mem.getInt(); // The loop samples are offset by this value
                    }
logger.log(Level.TRACE, String.format("FACT Chunk: chunkSize=%d, endSample=0x%X, sampleOffset=0x%X", chunkSize, info.atracEndSample, info.atracSampleOffset));
                }
                break;
            }
            case SMPL_CHUNK_MAGIC: {
                if (chunkSize >= 36) {
                    mem.getLong(); // +8
                    mem.getLong(); // +8
                    mem.getLong(); // +8
                    mem.getInt(); // +4
                    int checkNumLoops = mem.getInt();
                    if (chunkSize >= 36 + checkNumLoops * 24) {
                        info.numLoops = checkNumLoops;
                        info.loops = new LoopInfo[info.numLoops];
                        int loopInfoAddr = mem.position() + 36;
                        for (int i = 0; i < info.numLoops; i++) {
                            LoopInfo loop = new LoopInfo();
                            info.loops[i] = loop;
                            loop.cuePointID = mem.getInt();
                            loop.type = mem.getInt();
                            loop.startSample = mem.getInt() - info.atracSampleOffset;
                            loop.endSample = mem.getInt() - info.atracSampleOffset;
                            loop.fraction = mem.getInt();
                            loop.playCount = mem.getInt();

logger.log(Level.TRACE, String.format("Loop #%d: %s", i, loop));
                            loopInfoAddr += 24;
                        }
                        // TODO Second buffer processing disabled because still incomplete
                        //isSecondBufferNeeded = true;
                    }
                }
                break;
            }
            }

            if (chunkSize > bufferSize) {
                break;
            }

            mem.position(currentAddr + chunkSize);
            bufferSize -= chunkSize;
        }

        if (info.loops != null) {
            // If a loop end is past the atrac end, assume the atrac end
            for (LoopInfo loop : info.loops) {
                if (loop.endSample > info.atracEndSample) {
                    loop.endSample = info.atracEndSample;
                }
            }
        }

        return result;
    }

    /** */
    public static class LoopInfo {

        protected int cuePointID;
        protected int type;
        protected int startSample;
        protected int endSample;
        protected int fraction;
        protected int playCount;

        @Override
        public String toString() {
            return String.format("LoopInfo[cuePointID %d, type %d, startSample 0x%X, endSample 0x%X, fraction %d, playCount %d]", cuePointID, type, startSample, endSample, fraction, playCount);
        }
    }

    /** */
    public static class AtracFileInfo {

        public int atracBitrate = 64;
        public int atracChannels = 2;
        public int atracSampleRate = 0xAC44;
        public int atracBytesPerFrame = 0x0230;
        public int atracEndSample;
        public int atracSampleOffset;
        public int atracCodingMode;
        public int inputFileDataOffset;
        public int inputFileSize;
        public int inputDataSize;

        public int loopNum;
        public int numLoops;
        public LoopInfo[] loops;

        @Override
        public String toString() {
            return "AtracFileInfo{" +
                    "atracBitrate=" + atracBitrate +
                    ", atracChannels=" + atracChannels +
                    ", atracSampleRate=" + atracSampleRate +
                    ", atracBytesPerFrame=" + atracBytesPerFrame +
                    ", atracEndSample=" + atracEndSample +
                    ", atracSampleOffset=" + atracSampleOffset +
                    ", atracCodingMode=" + atracCodingMode +
                    ", inputFileDataOffset=" + inputFileDataOffset +
                    ", inputFileSize=" + inputFileSize +
                    ", inputDataSize=" + inputDataSize +
                    ", loopNum=" + loopNum +
                    ", numLoops=" + numLoops +
                    ", loops=" + Arrays.toString(loops) +
                    '}';
        }
    }
}