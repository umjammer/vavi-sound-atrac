/*
 * https://github.com/xXxTheDarkprogramerxXx/PS4_Tools/blob/master/PS4_Tools/Media/Atrac9/Atrac9Reader.cs
 */

package libatrac9;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.logging.Level;

import dotnet4j.io.BinaryReader;
import dotnet4j.io.Stream;
import vavi.util.ByteUtil;
import vavi.util.Debug;

import static libatrac9.Utils.deInterleave;
import static libatrac9.Utils.divideByRoundUp;


/**
 * Atrac9Reader.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-02-02 nsano initial version <br>
 */
public class Atrac9Reader {

    public static class RiffChunk {

        public String chunkId;
        public int size;
        public String type;

        public static RiffChunk parse(BinaryReader reader) throws IOException {
            var chunk = new RiffChunk();
            chunk.chunkId = Utils.readUTF8String(reader, 4);
            chunk.size = reader.readInt32();
            chunk.type = Utils.readUTF8String(reader, 4);

            if (!chunk.chunkId.equals("RIFF")) {
                throw new IllegalArgumentException("Not a valid RIFF file");
            }

Debug.println(Level.FINER, chunk);
            return chunk;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", RiffChunk.class.getSimpleName() + "[", "]")
                    .add("chunkId='" + chunkId + "'")
                    .add("size=" + size)
                    .add("type='" + type + "'")
                    .toString();
        }
    }

    public static class RiffSubChunk {

        public String subChunkId;
        public int subChunkSize;
        public byte[] extra;

        public RiffSubChunk(BinaryReader reader) throws IOException {
            subChunkId = Utils.readUTF8String(reader, 4);
            subChunkSize = reader.readInt32();
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", RiffSubChunk.class.getSimpleName() + "[", "]")
                    .add("subChunkId='" + subChunkId + "'")
                    .add("subChunkSize=" + subChunkSize)
                    .add("extra=" + Arrays.toString(extra))
                    .toString();
        }
    }

    public static class WaveFormatExtensible {

        public int size;
        public int validBitsPerSample;

        public int getSamplesPerBlock() {
            return validBitsPerSample;
        }

        public void setSamplesPerBlock(int value) {
            validBitsPerSample = value;
        }

        public int channelMask;
        public UUID subFormat;
        public byte[] extra;

        protected WaveFormatExtensible(BinaryReader reader) throws IOException {
            size = reader.readInt16();

            validBitsPerSample = reader.readInt16();
            channelMask = reader.readUInt32();
            subFormat = ByteUtil.readLeUUID(reader.readBytes(16), 0);
        }

        public static WaveFormatExtensible parse(RiffParser parser, BinaryReader reader) {
            try {
                return new WaveFormatExtensible(reader);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public static class WaveFormatTags {

        public static final int WaveFormatPcm = 0x0001;
        public static final int WaveFormatExtensible = 0xFFFE;
    }

    public static class WaveFmtChunk extends RiffSubChunk {

        public int formatTag;
        public int channelCount;
        public int sampleRate;
        public int avgBytesPerSec;
        public int blockAlign;
        public int bitsPerSample;
        public WaveFormatExtensible ext;

        protected WaveFmtChunk(RiffParser parser, BinaryReader reader) throws IOException {
            super(reader);
            formatTag = reader.readUInt16() & 0xffff;
            channelCount = reader.readInt16() & 0xffff;
            sampleRate = reader.readInt32();
            avgBytesPerSec = reader.readInt32();
            blockAlign = reader.readInt16() & 0xffff;
            bitsPerSample = reader.readInt16() & 0xffff;

//Debug.printf("formatTag: %04x, formatExtensibleParser: %s", formatTag, parser.formatExtensibleParser);
            if (formatTag == WaveFormatTags.WaveFormatExtensible && parser.formatExtensibleParser != null) {
                long startOffset = reader.getBaseStream().position() + 2;
                ext = parser.formatExtensibleParser.apply(parser, reader);

                long endOffset = startOffset + ext.size;
                int remainingBytes = (int) Math.max(endOffset - reader.getBaseStream().position(), 0);
                ext.extra = reader.readBytes(remainingBytes);
            }
        }

        public static WaveFmtChunk parse(RiffParser parser, BinaryReader reader) {
            try {
                return new WaveFmtChunk(parser, reader);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public static class WaveSmplChunk extends RiffSubChunk {

        public int manufacturer;
        public int product;
        public int samplePeriod;
        public int midiUnityNote;
        public int midiPitchFraction;
        public int smpteFormat;
        public int smpteOffset;
        public int sampleLoops;
        public int samplerData;
        public SampleLoop[] loops;

        protected WaveSmplChunk(BinaryReader reader) throws IOException {
            super(reader);
            manufacturer = reader.readInt32();
            product = reader.readInt32();
            samplePeriod = reader.readInt32();
            midiUnityNote = reader.readInt32();
            midiPitchFraction = reader.readInt32();
            smpteFormat = reader.readInt32();
            smpteOffset = reader.readInt32();
            sampleLoops = reader.readInt32();
            samplerData = reader.readInt32();
            loops = new SampleLoop[sampleLoops];

            for (int i = 0; i < sampleLoops; i++) {
                loops[i] = new SampleLoop();
                loops[i].cuePointId = reader.readInt32();
                loops[i].type = reader.readInt32();
                loops[i].start = reader.readInt32();
                loops[i].end = reader.readInt32();
                loops[i].fraction = reader.readInt32();
                loops[i].playCount = reader.readInt32();
            }
        }

        public static WaveSmplChunk parse(RiffParser parser, BinaryReader reader) {
            try {
                return new WaveSmplChunk(reader);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public static class SampleLoop {

        public int cuePointId;
        public int type;
        public int start;
        public int end;
        public int fraction;
        public int playCount;
    }

    public static class WaveFactChunk extends RiffSubChunk {

        public int sampleCount;

        protected WaveFactChunk(BinaryReader reader) throws IOException {
            super(reader);
            sampleCount = reader.readInt32();
        }

        public static WaveFactChunk parse(RiffParser parser, BinaryReader reader) {
            try {
                return new WaveFactChunk(reader);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public static class WaveDataChunk extends RiffSubChunk {

        public byte[] data;

        protected WaveDataChunk(RiffParser parser, BinaryReader reader) throws IOException {
            super(reader);
            if (parser.readDataChunk) {
                data = reader.readBytes(subChunkSize);
            }
        }

        public static WaveDataChunk parse(RiffParser parser, BinaryReader reader) {
            try {
                return new WaveDataChunk(parser, reader);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public static class RiffParser {

        public RiffChunk riffChunk;
        public boolean readDataChunk = true;
        private final Map<String, RiffSubChunk> subChunks = new HashMap<>();

        private final Map<String, BiFunction<RiffParser, BinaryReader, RiffSubChunk>> registeredSubChunks;

        {
            registeredSubChunks = new HashMap<>();
            registeredSubChunks.put("fmt ", WaveFmtChunk::parse);
            registeredSubChunks.put("smpl", WaveSmplChunk::parse);
            registeredSubChunks.put("fact", WaveFactChunk::parse);
            registeredSubChunks.put("data", WaveDataChunk::parse);
        }

        public BiFunction<RiffParser, BinaryReader, WaveFormatExtensible> formatExtensibleParser = WaveFormatExtensible::parse;

        public void registerSubChunk(String id, BiFunction<RiffParser, BinaryReader, RiffSubChunk> subChunkReader) {
            if (id.length() != 4) {
                throw new UnsupportedOperationException("Subchunk ID must be 4 characters long");
            }

            registeredSubChunks.put(id, subChunkReader);
        }

        public void parseRiff(Stream file) throws IOException {
            try (BinaryReader reader = new BinaryReader(file)) {
                riffChunk = RiffChunk.parse(reader);
                subChunks.clear();

                // Size is counted from after the ChunkSize field, not the RiffType field
                long startOffset = reader.getBaseStream().position() - 4;
                long endOffset = startOffset + riffChunk.size;

                // Make sure 8 bytes are available for the subchunk header
                while (reader.getBaseStream().position() + 8 < endOffset) {
                    RiffSubChunk subChunk = parseSubChunk(reader);
                    subChunks.put(subChunk.subChunkId, subChunk);
                }
Debug.println(Level.FINER, subChunks);
            }
        }

        public Collection<RiffSubChunk> getAllSubChunks() {
            return subChunks.values();
        }

        @SuppressWarnings("unchecked")
        public <T extends RiffSubChunk> T getSubChunk(String id) {
//Debug.println(Level.FINER, "[" + id + "], " + subChunks);
            RiffSubChunk chunk = subChunks.get(id);
//Debug.println(Level.FINER, chunk);
            return (T) chunk;
        }

        private RiffSubChunk parseSubChunk(BinaryReader reader) throws IOException {
            String id = Utils.readUTF8String(reader, 4);
            reader.getBaseStream().position(reader.getBaseStream().position() - 4);
            long startOffset = reader.getBaseStream().position() + 8;
            BiFunction<RiffParser, BinaryReader, RiffSubChunk> parser = registeredSubChunks.get(id);
//Debug.println(Level.FINER, parser != null ? parser.getClass().getName() : "parser is null");
            RiffSubChunk subChunk = parser != null ? parser.apply(this, reader) : new RiffSubChunk(reader);

            long endOffset = startOffset + subChunk.subChunkSize;
            int remainingBytes = (int) Math.max(endOffset - reader.getBaseStream().position(), 0);
            subChunk.extra = reader.readBytes(remainingBytes);

            reader.getBaseStream().position(endOffset + (endOffset & 1)); // Subchunks are 2-byte aligned
Debug.println(Level.FINER, subChunk);
            return subChunk;
        }
    }

    static class At9FactChunk extends WaveFactChunk {

        public int inputOverlapDelaySamples;
        public int encoderDelaySamples;

        protected At9FactChunk(BinaryReader reader) throws IOException {
            super(reader);

            inputOverlapDelaySamples = reader.readInt32();
            encoderDelaySamples = reader.readInt32();
        }

        public static At9FactChunk parseAt9(RiffParser parser, BinaryReader reader) {
            try {
                return new At9FactChunk(reader);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    static class At9WaveExtensible extends WaveFormatExtensible {

        public int versionInfo;
        public byte[] configData;
        public int reserved;

        protected At9WaveExtensible(BinaryReader reader) throws IOException {
            super(reader);

            versionInfo = reader.readInt32();
            configData = reader.readBytes(4);
            reserved = reader.readInt32();
        }

        public static At9WaveExtensible parseAt9(RiffParser parser, BinaryReader reader) {
            try {
                return new At9WaveExtensible(reader);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public static class MediaSubtypes {

        public static final UUID MediaSubtypePcm = UUID.fromString("00000001-0000-0010-8000-00AA00389B71");
        public static final UUID MediaSubtypeAtrac9 = UUID.fromString("47E142D2-36BA-4d8d-88FC-61654F8C836C");
    }

    static class At9DataChunk extends RiffSubChunk {

        public int frameCount;
        public byte[][] audioData;

        public At9DataChunk(RiffParser parser, BinaryReader reader) throws IOException {
            super(reader);

            // Do not trust the blockAlign field in the fmt chunk to equal the superframe size.
            // Some AT9 files have an invalid number in there.
            // Calculate the size try the ATRAC9 DataConfig instead.

Debug.println(Level.FINER, parser.<WaveFmtChunk>getSubChunk("fmt "));
            At9WaveExtensible ext = (At9WaveExtensible) Objects.requireNonNull(parser.<WaveFmtChunk>getSubChunk("fmt ")).ext;
            if (ext == null)
                throw new IllegalArgumentException("fmt chunk must come before data chunk");

            At9FactChunk fact = parser.getSubChunk("fact");
            if (fact == null)
                throw new IllegalArgumentException("fact chunk must come before data chunk");

            var config = new Atrac9Config(ext.configData);
            frameCount = divideByRoundUp(fact.sampleCount + fact.encoderDelaySamples, config.getSuperframeSamples());
            int dataSize = frameCount * config.getSuperframeBytes();

            if (dataSize > reader.getBaseStream().getLength() - reader.getBaseStream().position()) {
                throw new IllegalArgumentException("Required AT9 length is greater than the number of bytes remaining in the file.");
            }

            audioData = deInterleave(reader.getBaseStream(), dataSize, config.getSuperframeBytes(), frameCount, -1);
        }

        public static At9DataChunk parseAt9(RiffParser parser, BinaryReader reader) {
            try {
                return new At9DataChunk(parser, reader);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
