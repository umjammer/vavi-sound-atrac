/*
 * https://github.com/xXxTheDarkprogramerxXx/PS4_Tools/blob/master/PS4_Tools/Media/Atrac9/Atract9FormatBuilder.cs
 */

package libatrac9;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

import dotnet4j.io.BinaryWriter;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;
import libatrac9.Atrac9.At9Structure;
import vavi.util.ByteUtil;
import vavi.util.Debug;

import static libatrac9.Atrac9Reader.At9DataChunk;
import static libatrac9.Atrac9Reader.At9FactChunk;
import static libatrac9.Atrac9Reader.At9WaveExtensible;
import static libatrac9.Atrac9Reader.MediaSubtypes;
import static libatrac9.Atrac9Reader.RiffParser;
import static libatrac9.Atrac9Reader.WaveDataChunk;
import static libatrac9.Atrac9Reader.WaveFmtChunk;
import static libatrac9.Atrac9Reader.WaveFormatTags;
import static libatrac9.Atrac9Reader.WaveSmplChunk;
import static libatrac9.Utils.clamp;
import static libatrac9.Utils.concat;
import static libatrac9.Utils.createJaggedArray;
import static libatrac9.Utils.divideByRoundUp;
import static libatrac9.Utils.interleave;
import static libatrac9.Utils.interleavedByteToShort;
import static libatrac9.Utils.shortToInterleavedByte;


/**
 * Atract9FormatBuilder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-01-31 nsano initial version <br>
 */
public class Atract9FormatBuilder {

    /** */
    public static class At9Configuration extends Configuration {

    }

    /** */
    public abstract static class AudioReader<TReader extends AudioReader<TReader, TStructure, TConfig>, TStructure, TConfig extends Configuration> implements IAudioReader<TConfig> {

        @Override
        public IAudioFormat readFormat(Stream stream) throws IOException {
            return readStream(stream).audioFormat;
        }

        @Override
        public IAudioFormat readFormat(byte[] file) throws IOException {
            return readByteArray(file).audioFormat;
        }

        @Override
        public AudioData read(Stream stream) throws IOException {
            return readStream(stream).getAudio();
        }

        @Override
        public AudioData read(byte[] file) throws IOException {
            return readByteArray(file).getAudio();
        }

        @Override
        public AudioWithConfig readWithConfig(Stream stream) throws IOException {
            return readStream(stream);
        }

        @Override
        public AudioWithConfig readWithConfig(byte[] file) throws IOException {
            return readByteArray(file);
        }

        public TStructure readMetadata(Stream stream) throws IOException {
            return readStructure(stream, false);
        }

        protected TConfig getConfiguration(TStructure structure) {
            try {
                return getConfigurationClass().getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }

        public abstract TStructure readFile(Stream stream, boolean readAudioData /* = true */) throws IOException;

        protected abstract IAudioFormat toAudioStream(TStructure structure);

        private AudioWithConfig readByteArray(byte[] file) throws IOException {
            try (var stream = new MemoryStream(file)) {
                return readStream(stream);
            }
        }

        private AudioWithConfig readStream(Stream stream) throws IOException {
            TStructure structure = readStructure(stream, true);
            return new AudioWithConfig(toAudioStream(structure), getConfiguration(structure));
        }

        private TStructure readStructure(Stream stream, boolean readAudioData /* = true */) throws IOException {
            if (!stream.canSeek()) {
                throw new UnsupportedOperationException("A seekable stream is required");
            }

            return readFile(stream, readAudioData);
        }
    }

    /** */
    public static class At9Reader extends AudioReader<At9Reader, At9Structure, At9Configuration> {

        @Override
        public Class<At9Configuration> getConfigurationClass() {
            return At9Configuration.class;
        }

        private static void validateAt9File(RiffParser parser) {
            if (!parser.riffChunk.type.equals("WAVE")) {
                throw new IllegalArgumentException("Not a valid WAVE file");
            }

            WaveFmtChunk fmt = parser.getSubChunk("fmt ");
            if (fmt == null) throw new IllegalArgumentException("File must have a valid fmt chunk");
            At9WaveExtensible ext = (At9WaveExtensible) fmt.ext;
            if (ext == null) throw new IllegalArgumentException("File must have a format chunk extension");
            if (parser.getSubChunk("fact") == null)
                throw new IllegalArgumentException("File must have a valid fact chunk");
            if (parser.getSubChunk("data") == null)
                throw new IllegalArgumentException("File must have a valid data chunk");

            if (fmt.channelCount == 0) throw new IllegalArgumentException("Channel count must not be zero");

            if (!ext.subFormat.equals(MediaSubtypes.MediaSubtypeAtrac9))
                throw new IllegalArgumentException("Must contain ATRAC9 data. Has unsupported subFormat " + ext.subFormat);
        }

        @Override
        public At9Structure readFile(Stream stream, boolean readAudioData /* = true */) throws IOException {
            var structure = new At9Structure();
            RiffParser parser = new RiffParser();
            parser.readDataChunk = readAudioData;
            parser.registerSubChunk("fact", At9FactChunk::parseAt9);
            parser.registerSubChunk("data", At9DataChunk::parseAt9);
            parser.formatExtensibleParser = At9WaveExtensible::parseAt9;
            parser.parseRiff(stream);

            validateAt9File(parser);

            WaveFmtChunk fmt = parser.getSubChunk("fmt ");
            var ext = (At9WaveExtensible) fmt.ext;
            At9FactChunk fact = parser.getSubChunk("fact");
            At9DataChunk data = parser.getSubChunk("data");
            WaveSmplChunk smpl = parser.getSubChunk("smpl");

            structure.config = new Atrac9Config(ext.configData);
            structure.sampleCount = fact.sampleCount;
            structure.encoderDelay = fact.encoderDelaySamples;
            structure.version = ext.versionInfo;
            structure.audioData = data.audioData;
            structure.superframeCount = data.frameCount;

            if (Objects.requireNonNullElse(smpl, null).loops != null) {
                structure.loopStart = smpl.loops[0].start - structure.encoderDelay;
                structure.loopEnd = smpl.loops[0].end - structure.encoderDelay;
                structure.looping = structure.loopEnd > structure.loopStart;
            }

            return structure;
        }

        @Override
        protected IAudioFormat toAudioStream(At9Structure structure) {
            return new Atrac9FormatBuilder(structure.audioData, structure.config, structure.sampleCount, structure.encoderDelay)
                    .withLoop(structure.looping, structure.loopStart, structure.loopEnd)
                    .build();
        }
    }

    /** */
    public static class WaveStructure {

        public Collection<Atrac9Reader.RiffSubChunk> riffSubChunks;

        /** The number of channels in the WAVE file. */
        public int channelCount;

        /** The audio sample rate. */
        public int sampleRate;

        /** The number of bits per audio sample. */
        public int bitsPerSample;

        /** The number of samples in the audio file. */
        public int sampleCount;

        /** This flag is set if the file loops. */
        public boolean looping;

        /** The loop start position in samples. */
        public int loopStart;

        /** The loop end position in samples. */
        public int loopEnd;

        public short[][] audioData16;
        public byte[][] audioData8;
    }

    /**
     * The different audio codecs used in Wave files.
     */
    public enum WaveCodec {
        /**
         * 16-bit PCM.
         */
        Pcm16Bit,
        /**
         * 8-bit PCM.
         */
        Pcm8Bit
    }

    /** */
    public static class WaveConfiguration extends Configuration {

        public final WaveCodec codec = WaveCodec.Pcm16Bit;
    }

    /** */
    public static class WaveReader extends AudioReader<WaveReader, WaveStructure, WaveConfiguration> {

        @Override
        public Class<WaveConfiguration> getConfigurationClass() {
            return WaveConfiguration.class;
        }

        @Override
        public WaveStructure readFile(Stream stream, boolean readAudioData /* = true */) throws IOException {
            var structure = new WaveStructure();
            var parser = new RiffParser();
            parser.readDataChunk = readAudioData;

            validateWaveFile(parser);

            WaveFmtChunk fmt = parser.getSubChunk("fmt ");
            WaveDataChunk data = parser.getSubChunk("data");
            WaveSmplChunk smpl = parser.getSubChunk("smpl");

            int bytesPerSample = divideByRoundUp(fmt.bitsPerSample, 8);
            structure.riffSubChunks = parser.getAllSubChunks();
            structure.sampleCount = data.subChunkSize / bytesPerSample / fmt.channelCount;
            structure.sampleRate = fmt.sampleRate;
            structure.bitsPerSample = fmt.bitsPerSample;
            structure.channelCount = fmt.channelCount;

            if (Objects.requireNonNull(Objects.requireNonNull(smpl).loops)[0] != null) {
                structure.loopStart = smpl.loops[0].start;
                structure.loopEnd = smpl.loops[0].end;
                structure.looping = structure.loopEnd > structure.loopStart;
            }

            if (!readAudioData) return structure;

            switch (fmt.bitsPerSample) {
            case 16:
                structure.audioData16 = interleavedByteToShort(data.data, fmt.channelCount);
                break;
            case 8:
                structure.audioData8 = Utils.deInterleave(data.data, bytesPerSample, fmt.channelCount, -1);
                break;
            }
            return structure;
        }

        @Override
        protected IAudioFormat toAudioStream(WaveStructure structure) {
            return switch (structure.bitsPerSample) {
                case 16 -> new Pcm16FormatBuilder(structure.audioData16, structure.sampleRate)
                        .withLoop(structure.looping, structure.loopStart, structure.loopEnd)
                        .build();
                case 8 -> new Pcm8FormatBuilder(structure.audioData8, structure.sampleRate, false)
                        .withLoop(structure.looping, structure.loopStart, structure.loopEnd)
                        .build();
                default -> null;
            };
        }

        private static void validateWaveFile(RiffParser parser) {
            if (!parser.riffChunk.type.equals("WAVE")) {
                throw new IllegalArgumentException("Not a valid WAVE file");
            }

            WaveFmtChunk fmt = parser.getSubChunk("fmt ");
            if (fmt == null) throw new IllegalArgumentException("File must have a valid fmt chunk");
            if (parser.getSubChunk("data") == null)
                throw new IllegalArgumentException("File must have a valid data chunk");

            int bytesPerSample = divideByRoundUp(fmt.bitsPerSample, 8);

            if (fmt.formatTag != 1 /* WaveFormatTags.WaveFormatPcm */ && fmt.formatTag != WaveFormatTags.WaveFormatExtensible)
                throw new IllegalArgumentException("Must contain PCM data. Has unsupported format " + fmt.formatTag);

            if (fmt.bitsPerSample != 16 && fmt.bitsPerSample != 8)
                throw new IllegalArgumentException("Must have 8 or 16 bits per sample, not " + fmt.bitsPerSample + " bits per sample");

            if (fmt.channelCount == 0) throw new IllegalArgumentException("Channel count must not be zero");

            if (fmt.blockAlign != bytesPerSample * fmt.channelCount)
                throw new IllegalArgumentException("File has invalid block alignment");

            if (fmt.ext != null && fmt.ext.subFormat != MediaSubtypes.MediaSubtypePcm)
                throw new IllegalArgumentException("Must contain PCM data. Has unsupported subFormat " + fmt.ext.subFormat);
        }
    }

    /** */
    public static class WaveWriter extends AudioWriter<WaveWriter, WaveConfiguration> {

        @Override
        public Class<WaveConfiguration> getConfigurationClass() {
            return WaveConfiguration.class;
        }

        private Pcm16Format pcm16;
        private Pcm8Format pcm8;
        private IAudioFormat audioFormat;

        private WaveCodec getCodec() {
            return configuration.codec;
        }

        private int getChannelCount() {
            return audioFormat.getChannelCount();
        }

        private int getSampleCount() {
            return audioFormat.getSampleCount();
        }

        private int getSampleRate() {
            return audioFormat.getSampleRate();
        }

        private boolean isLooping() {
            return audioFormat.isLooping();
        }

        private int getLoopStart() {
            return audioFormat.getLoopStart();
        }

        private int LoopEnd() {
            return audioFormat.getLoopEnd();
        }

        @Override
        protected int getFileSize() {
            return 8 + getRiffChunkSize();
        }

        private int getRiffChunkSize() {
            return 4 + 8 + getFmtChunkSize() + 8 + getDataChunkSize()
                    + (isLooping() ? 8 + getSmplChunkSize() : 0);
        }

        private int getFmtChunkSize() {
            return getChannelCount() > 2 ? 40 : 16;
        }

        private int getDataChunkSize() {
            return getChannelCount() * getSampleCount() * getBytesPerSample();
        }

        private int getSmplChunkSize() {
            return 0x3c;
        }

        private int getBitDepth() {
            return configuration.codec == WaveCodec.Pcm16Bit ? 16 : 8;
        }

        private int getBytesPerSample() {
            return divideByRoundUp(getBitDepth(), 8);
        }

        private int getBytesPerSecond() {
            return getSampleRate() * getBytesPerSample() * getChannelCount();
        }

        private int getBlockAlign() {
            return getBytesPerSample() * getChannelCount();
        }

        @Override
        protected void setupWriter(AudioData audio) {
            var parameters = new CodecParameters();
            parameters.progress = configuration.progress;

            switch (getCodec()) {
            case Pcm16Bit:
                pcm16 = audio.getFormat(parameters, Pcm16Format.class);
                audioFormat = pcm16;
                break;
            case Pcm8Bit:
                pcm8 = audio.getFormat(parameters, Pcm8Format.class);
                audioFormat = pcm8;
                break;
            }
        }

        @Override
        protected void writeStream(Stream stream) throws IOException {
            try (BinaryWriter writer = new BinaryWriter(stream)) {
                stream.position(0);
                writeRiffHeader(writer);
                writeFmtChunk(writer);
                writeDataChunk(writer);
                if (isLooping())
                    writeSmplChunk(writer);
Debug.println(Level.FINER, "stream: " + stream.getLength());
            }
        }

        private void writeRiffHeader(BinaryWriter writer) {
            writer.write("RIFF".getBytes()); // write URF8
            writer.write(getRiffChunkSize());
            writer.write("WAVE".getBytes());
        }

        private void writeFmtChunk(BinaryWriter writer) {
            // Every chunk should be 2-byte aligned
            writer.getBaseStream().position(writer.getBaseStream().position() + (writer.getBaseStream().position() & 1));

            writer.write("fmt ".getBytes());
            writer.write(getFmtChunkSize());
            writer.write((short) (getChannelCount() > 2 ? WaveFormatTags.WaveFormatExtensible : WaveFormatTags.WaveFormatPcm));
            writer.write((short) getChannelCount());
            writer.write(getSampleRate());
            writer.write(getBytesPerSecond());
            writer.write((short) getBlockAlign());
            writer.write((short) getBitDepth());

            if (getChannelCount() > 2) {
                writer.write((short) 22);
                writer.write((short) getBitDepth());
                writer.write(getChannelMask(getChannelCount()));
                byte[] b = new byte[8];
                ByteUtil.writeLeUUID(MediaSubtypes.MediaSubtypePcm, b, 0);
                writer.write(b);
            }
        }

        private void writeDataChunk(BinaryWriter writer) {
            writer.getBaseStream().position(writer.getBaseStream().position() + (writer.getBaseStream().position() & 1));

            writer.write("data".getBytes());
            writer.write(getDataChunkSize());

            switch (getCodec()) {
            case Pcm16Bit:
                byte[] audioData = shortToInterleavedByte(pcm16.channels);
                writer.write(audioData, 0, audioData.length);
                break;
            case Pcm8Bit:
                interleave(pcm8.channels, writer.getBaseStream(), getBytesPerSample(), -1);
                break;
            }
        }

        private void writeSmplChunk(BinaryWriter writer) {
            writer.getBaseStream().position(writer.getBaseStream().position() + (writer.getBaseStream().position() & 1));

            writer.write("smpl".getBytes());
            writer.write(getSmplChunkSize());
            for (int i = 0; i < 7; i++)
                writer.write(0);
            writer.write(1);
            for (int i = 0; i < 3; i++)
                writer.write(0);
            writer.write(getLoopStart());
            writer.write(LoopEnd());
            writer.write(0);
            writer.write(0);
        }

        private static int getChannelMask(int channelCount) {
            // Nothing special about these masks. I just choose
            // whatever channel combinations seemed okay.
            return switch (channelCount) {
                case 4 -> 0x0033;
                case 5 -> 0x0133;
                case 6 -> 0x0633;
                case 7 -> 0x01f3;
                case 8 -> 0x06f3;
                default -> (1 << channelCount) - 1;
            };
        }
    }

    /** */
    static class AudioInfo {

        public static final Map<FileType, ContainerType> containers = new HashMap<>();

        static {
            containers.put(FileType.Wave, new ContainerType("WAVE", List.of("wav"), "WAVE Audio File", WaveReader::new, WaveWriter::new));
            containers.put(FileType.Atrac9, new ContainerType("ATRAC9", List.of("at9"), "ATRAC9 Audio File", At9Reader::new, null));
        }

        public static final Map<String, FileType> extensions =
                containers.entrySet().stream().flatMap(x -> x.getValue().extensions.stream().map(y -> new Object[] {y, x.getKey()}))
                        .collect(Collectors.toMap(x -> (String) x[0], x -> (FileType)  x[1]));

        public static FileType getFileTypeFromName(String fileName) {
            String name = Objects.requireNonNullElse(Path.of(fileName).getFileName(), "").toString();
            String extension = name.substring(name.indexOf('.') + 1).toLowerCase();
            FileType fileType = extensions.getOrDefault(extension, null); // TODO null
            return fileType;
        }
    }

    /** */
    public static class Pcm8Codec {

        public static byte[] encode(short[] array) {
            var output = new byte[array.length];

            for (int i = 0; i < array.length; i++) {
                output[i] = (byte) ((array[i] + Short.MAX_VALUE + 1) >> 8);
            }

            return output;
        }

        public static short[] decode(byte[] array) {
            var output = new short[array.length];

            for (int i = 0; i < array.length; i++) {
                output[i] = (short) ((array[i] - 0x80) << 8);
            }

            return output;
        }

        public static byte[] encodeSigned(short[] array) {
            var output = new byte[array.length];

            for (int i = 0; i < array.length; i++) {
                output[i] = (byte) (array[i] >> 8);
            }

            return output;
        }

        public static short[] decodeSigned(byte[] array) {
            var output = new short[array.length];

            for (int i = 0; i < array.length; i++) {
                output[i] = (short) (array[i] << 8);
            }

            return output;
        }
    }

    /** */
    public static class Pcm8Format extends AudioFormatBase<Pcm8Format, Pcm8FormatBuilder, CodecParameters> {

        public final byte[][] channels;

        public boolean isSigned() {
            return false;
        }

        public Pcm8Format() {
            channels = new byte[0][];
        }

        public Pcm8Format(byte[][] channels, int sampleRate) {
            this(new Pcm8FormatBuilder(channels, sampleRate, false));
        }

        private Pcm8Format(Pcm8FormatBuilder b) {
            super(b);
            channels = b.channels;
        }

        @Override
        public Pcm16Format toPcm16() {
            var channels = new short[channelCount][];

            for (int i = 0; i < channelCount; i++) {
                channels[i] = isSigned() ? Pcm8Codec.decodeSigned(this.channels[i]) : Pcm8Codec.decode(this.channels[i]);
            }

            return new Pcm16FormatBuilder(channels, sampleRate)
                    .withLoop(looping, getLoopStart(), getLoopEnd())
                    .withTracks(tracks)
                    .build();
        }

        @Override
        public Pcm8Format encodeFromPcm16(Pcm16Format pcm16) {
            var channels = new byte[pcm16.channelCount][];

            for (int i = 0; i < pcm16.channelCount; i++) {
                channels[i] = isSigned() ? Pcm8Codec.encodeSigned(pcm16.channels[i]) : Pcm8Codec.encode(pcm16.channels[i]);
            }

            return new Pcm8FormatBuilder(channels, pcm16.sampleRate, isSigned())
                    .withLoop(pcm16.looping, pcm16.getLoopStart(), pcm16.getLoopEnd())
                    .withTracks(pcm16.tracks)
                    .build();
        }

        @Override
        protected Pcm8Format addInternal(Pcm8Format pcm8) {
            Pcm8FormatBuilder copy = getCloneBuilder();
            copy.channels = concat(byte[].class, channels, pcm8.channels);
            return copy.build();
        }

        @Override
        protected Pcm8Format getChannelsInternal(int[] channelRange) {
            var channels = new ArrayList<byte[]>();

            for (int i : channelRange) {
                if (i < 0 || i >= this.channels.length)
                    throw new IllegalArgumentException(String.format("channelRange: Channel %d does not exist.", i));
                channels.add(this.channels[i]);
            }

            Pcm8FormatBuilder copy = getCloneBuilder();
            copy.channels = channels.toArray(byte[][]::new);
            return copy.build();
        }

        public static Pcm8FormatBuilder getBuilder(byte[][] channels, int sampleRate) {
            return new Pcm8FormatBuilder(channels, sampleRate, false);
        }

        @Override
        public Pcm8FormatBuilder getCloneBuilder() {
            return getCloneBuilderBase(new Pcm8FormatBuilder(channels, sampleRate, false));
        }
    }

    /** */
    public static class Pcm8SignedFormat extends Pcm8Format {

        @Override
        public boolean isSigned() {
            return true;
        }

        public Pcm8SignedFormat() {
        }

        public Pcm8SignedFormat(byte[][] channels, int sampleRate) {
            super(new Pcm8FormatBuilder(channels, sampleRate, false));
        }

        private Pcm8SignedFormat(Pcm8FormatBuilder b) {
            super(b);
        }
    }

    /** */
    public static class Pcm8FormatBuilder extends AudioFormatBaseBuilder<Pcm8Format, Pcm8FormatBuilder, CodecParameters> {

        public byte[][] channels;
        public boolean signed;

        @Override
        public int getChannelCount() {
            return channels.length;
        }

        public Pcm8FormatBuilder(byte[][] channels, int sampleRate, boolean signed /* = false */) {
            if (channels == null || channels.length < 1)
                throw new IllegalArgumentException("channels parameter cannot be empty or null");

            this.channels = channels;
            sampleCount = Objects.requireNonNull(this.channels[0]).length;
            this.sampleRate = sampleRate;
            this.signed = signed;

            for (byte[] channel : this.channels) {
                if (channel == null)
                    throw new IllegalArgumentException("All provided channels must be non-null");

                if (channel.length != sampleCount)
                    throw new IllegalArgumentException("All channels must have the same sample count");
            }
        }

        @Override
        public Pcm8Format build() {
            return signed ? new Pcm8SignedFormat(this) : new Pcm8Format(this);
        }
    }

    /** */
    public enum FileType {
        Unknown,
        Wave,
        Dsp,
        Brstm,
        Bcstm,
        Bfstm,
        Idsp,
        Hps,
        Adx,
        Hca,
        Genh,
        Atrac9
    }

    /** */
    public static class ContainerType {

        public final String displayName;
        public final List<String> extensions;
        public final String description;
        public final Supplier<IAudioReader<?>> getReader;
        public final Supplier<IAudioWriter<?>> getWriter;

        public ContainerType(String displayName, List<String> extensions, String description, Supplier<IAudioReader<?>> getReader, Supplier<IAudioWriter<?>> getWriter) {
            this.displayName = displayName;
            this.extensions = extensions;
            this.description = description;
            this.getReader = getReader;
            this.getWriter = getWriter;
        }
    }

    /** */
    public interface IAudioReader<TConfig extends Configuration> {

        Class<TConfig> getConfigurationClass();

        IAudioFormat readFormat(Stream stream) throws IOException;

        IAudioFormat readFormat(byte[] file) throws IOException;

        AudioData read(Stream stream) throws IOException;

        AudioData read(byte[] file) throws IOException;

        AudioWithConfig readWithConfig(Stream stream) throws IOException;

        AudioWithConfig readWithConfig(byte[] file) throws IOException;
    }

    /** */
    public interface IAudioWriter<TConfig extends Configuration> {

        Class<TConfig> getConfigurationClass();

        void writeToStream(IAudioFormat audio, Stream stream, TConfig configuration /* = null */) throws IOException;

        byte[] getFile(IAudioFormat audio, TConfig configuration /* = null */) throws IOException;

        void writeToStream(AudioData audio, Stream stream, TConfig configuration /* = null */) throws IOException;
    }

    /** */
    public static abstract class AudioWriter<TWriter extends AudioWriter<TWriter, TConfig>, TConfig extends Configuration> implements IAudioWriter<TConfig> {

        @Override
        public byte[] getFile(IAudioFormat audio, TConfig configuration /* = null */) throws IOException {
            return getByteArray(new AudioData(audio), configuration);
        }

        @Override
        public void writeToStream(IAudioFormat audio, Stream stream, TConfig configuration /* = null */) throws IOException {
            writeStream(new AudioData(audio), stream, configuration);
        }

        public byte[] getFile(AudioData audio, TConfig configuration /* = null */) throws IOException {
            return getByteArray(audio, configuration);
        }

        @Override
        public void writeToStream(AudioData audio, Stream stream, TConfig configuration /* = null */) throws IOException {
            writeStream(audio, stream, configuration);
        }

        protected AudioData audioData;
        public TConfig configuration;
        public TConfig getConfiguration() {
            try {
                return getConfigurationClass().getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }
        protected abstract int getFileSize();

        protected abstract void setupWriter(AudioData audio);

        protected abstract void writeStream(Stream stream) throws IOException;

        private byte[] getByteArray(AudioData audio, TConfig configuration /* = null */) throws IOException {
            this.configuration = Objects.requireNonNullElse(configuration, getConfiguration());
            setupWriter(audio);

            MemoryStream stream;
            byte[] file = null;

            if (getFileSize() == -1) {
                stream = new MemoryStream(0);
            } else {
                file = new byte[getFileSize()];
                stream = new MemoryStream();
            }

            writeStream(stream);

            return getFileSize() == -1 ? stream.toArray() : file;
        }

        private void writeStream(AudioData audio, Stream stream, TConfig configuration /* = null */) throws IOException {
            this.configuration = Objects.requireNonNullElse(configuration, getConfiguration());
            setupWriter(audio);
//Debug.println("stream: " + stream.getLength());
//Debug.println("fileSize: " + getFileSize());
            if (stream.getLength() != getFileSize() && getFileSize() != -1) {
                try {
                    stream.setLength(getFileSize());
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Stream is too small.", ex);
                }
            }

            writeStream(stream);
        }
    }

    /** */
    public static class AudioData {

        private final Map<Class<?>, IAudioFormat> formats = new HashMap<>();

        private void addFormat(IAudioFormat format) {
            formats.put(format.getClass(), format);
        }

        public AudioData(IAudioFormat audioFormat) {
            addFormat(audioFormat);
        }

        public <T extends IAudioFormat> T getFormat(CodecParameters configuration /* = null */, Class<T> c) {
            var format = getAudioFormat(c);

            if (format != null) {
                return format;
            }

            createPcm16(configuration);
            createFormat(configuration, c);

            return getAudioFormat(c);
        }

        public Collection<IAudioFormat> getAllFormats() {
            return formats.values();
        }

        public Set<Class<?>> listAvailableFormats() {
            return formats.keySet();
        }

        public void setLoop(boolean loop, int loopStart, int loopEnd) {
            formats.replaceAll((f, v) -> formats.get(f).withLoop(loop, loopStart, loopEnd));
        }

        public void setLoop(boolean loop) {
            formats.replaceAll((f, v) -> formats.get(f).withLoop(loop));
        }

        public static AudioData combine(AudioData... audio) {
            if (audio == null || audio.length <= 0 || Arrays.stream(audio).allMatch(Objects::isNull))
                throw new IllegalArgumentException("Audio cannot be null, empty, or have any null elements");

            List<Class<?>> commonTypes = Arrays.stream(audio)
                    .flatMap(x -> x.listAvailableFormats().stream())
                    .distinct()
                    .toList();

            Class<?> formatToUse;

            if (commonTypes.isEmpty() || commonTypes.size() == 1 && commonTypes.contains(Pcm16Format.class)) {
                formatToUse = Pcm16Format.class;
                for (AudioData a : audio) {
                    a.createPcm16(null);
                }
            } else {
                formatToUse = commonTypes.stream().filter(x -> x != Pcm16Format.class).findFirst().get();
            }

            IAudioFormat[] combined = new IAudioFormat[] { audio[0].formats.get(formatToUse) };

            Arrays.stream(audio).map(x -> x.formats.get(formatToUse)).skip(1).forEach(format -> {
                if (!combined[0].tryAdd(format, /* out */ combined)) {
                    throw new IllegalArgumentException("Audio streams cannot be added together");
                }
            });

            return new AudioData(combined[0]);
        }

        @SuppressWarnings("unchecked")
        private <T extends IAudioFormat> T getAudioFormat(Class<T> c) {
            IAudioFormat format = formats.get(c);

            return (T) format;
        }

        protected <T extends IAudioFormat> T newAudioFormat(Class<T> c) {
            try {
                return c.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }

        private <T extends IAudioFormat> void createFormat(CodecParameters configuration /* = null */, Class<T> c) {
            var pcm = this.getAudioFormat(Pcm16Format.class);
            addFormat(newAudioFormat(c).encodeFromPcm16(pcm, configuration));
        }

        private void createPcm16(CodecParameters configuration /* = null */) {
            if (this.getAudioFormat(Pcm16Format.class) == null) {
                addFormat(formats.values().stream().findFirst().get().toPcm16(configuration));
            }
        }
    }

    /** */
    public static class Configuration {

        public IProgressReport progress;
        /**
         * If {@code true}, trims the output file length to the set loopEnd.
         * If {@code false} or if the {@link IAudioFormat} does not loop,
         * the output file is not trimmed.
         * Default is <c>true</c>.
         */
        public boolean trimFile = true;
    }

    /** */
    public static class AudioWithConfig {

        public AudioWithConfig(IAudioFormat audioFormat, Configuration configuration) {
            this.audioFormat = audioFormat;
            this.configuration = configuration;
        }

        public final IAudioFormat audioFormat;

        public AudioData getAudio() {
            return new AudioData(audioFormat);
        }

        public final Configuration configuration;
    }

    /** */
    public interface IProgressReport {

        /**
         * Sets the current value of the {@link IProgressReport} to {@code value}.
         *
         * @param value The value to set.
         */
        void report(int value);

        /**
         * Adds <{@code value"/> to the current value of the {@link IProgressReport}.
         *
         * @param value The amount to add.
         */
        void reportAdd(int value);

        /**
         * Sets the maximum value of the {@link IProgressReport} to {@code value}.
         *
         * @param value The maximum value to set.
         */
        void setTotal(int value);

        /**
         * Logs a message to the {@link IProgressReport} object.
         *
         * @param message The message to output.
         */
        void logMessage(String message);
    }

    /** */
    public static class CodecParameters {

        public IProgressReport progress;
        public int sampleCount = -1;

        public CodecParameters() {
        }

        protected CodecParameters(CodecParameters source) {
            if (source == null) return;
            progress = source.progress;
            sampleCount = source.sampleCount;
        }
    }

    /** */
    public static class Pcm16FormatBuilder extends AudioFormatBaseBuilder<Pcm16Format, Pcm16FormatBuilder, CodecParameters> {

        public short[][] channels;

        @Override
        public int getChannelCount() {
            return channels.length;
        }

        public Pcm16FormatBuilder(short[][] channels, int sampleRate) {
            if (channels == null || channels.length < 1)
                throw new IllegalArgumentException("channels parameter cannot be empty or null");

            this.channels = channels;
            sampleCount = Objects.requireNonNull(this.channels[0]).length;
            this.sampleRate = sampleRate;

            for (short[] channel : this.channels) {
                if (channel == null)
                    throw new IllegalArgumentException("All provided channels must be non-null");

                if (channel.length != sampleCount)
                    throw new IllegalArgumentException("All channels must have the same sample count");
            }
        }

        @Override
        public Pcm16Format build() {
            return new Pcm16Format(this);
        }
    }

    /**
     * A 16-bit PCM audio stream.
     * The stream can contain any number of individual channels.
     */
    public static class Pcm16Format extends AudioFormatBase<Pcm16Format, Pcm16FormatBuilder, CodecParameters> {

        public final short[][] channels;

        public Pcm16Format() {
            channels = new short[0][];
        }

        public Pcm16Format(short[][] channels, int sampleRate) {
            this(new Pcm16FormatBuilder(channels, sampleRate));
        }

        private Pcm16Format(Pcm16FormatBuilder b) {
            super(b);
            channels = b.channels;
        }

        @Override
        public Pcm16Format toPcm16() {
            return getCloneBuilder().build();
        }

        @Override
        public Pcm16Format encodeFromPcm16(Pcm16Format pcm16) {
            return pcm16.getCloneBuilder().build();
        }

        @Override
        protected Pcm16Format addInternal(Pcm16Format pcm16) {
            Pcm16FormatBuilder copy = getCloneBuilder();
            copy.channels = concat(short[].class, channels, pcm16.channels);
            return copy.build();
        }

        @Override
        protected Pcm16Format getChannelsInternal(int[] channelRange) {
            var channels = new ArrayList<short[]>();

            for (int i : channelRange) {
                if (i < 0 || i >= this.channels.length)
                    throw new IllegalArgumentException("Channel {i} does not exist.");
                channels.add(this.channels[i]);
            }

            Pcm16FormatBuilder copy = getCloneBuilder();
            copy.channels = channels.toArray(short[][]::new);
            return copy.build();
        }

        public static Pcm16FormatBuilder getBuilder(short[][] channels, int sampleRate) {
            return new Pcm16FormatBuilder(channels, sampleRate);
        }

        @Override
        public Pcm16FormatBuilder getCloneBuilder() {
            return getCloneBuilderBase(new Pcm16FormatBuilder(channels, sampleRate));
        }
    }

    /** */
    public interface IAudioFormat {

        int getSampleCount();
        int getSampleRate();
        int getChannelCount();
        int getLoopStart();
        int getLoopEnd();
        boolean isLooping();

        IAudioFormat withLoop(boolean loop, int loopStart, int loopEnd);

        IAudioFormat withLoop(boolean loop);

        Pcm16Format toPcm16();

        Pcm16Format toPcm16(CodecParameters config);

        IAudioFormat encodeFromPcm16(Pcm16Format pcm16);

        IAudioFormat encodeFromPcm16(Pcm16Format pcm16, CodecParameters config);

        IAudioFormat getChannels(int... channelRange);

        boolean tryAdd(IAudioFormat format, /* out */ IAudioFormat[] result);
    }

    /**
     * Defines an audio track in an audio stream.
     * Each track is composed of one or two channels.
     */
    public static class AudioTrack {

        public AudioTrack(int channelCount, int channelLeft, int channelRight, int panning, int volume) {
            this.channelCount = channelCount;
            this.channelLeft = channelLeft;
            this.channelRight = channelRight;
            this.panning = panning;
            this.volume = volume;
        }

        public AudioTrack(int channelCount, int channelLeft, int channelRight) {
            this.channelCount = channelCount;
            this.channelLeft = channelLeft;
            this.channelRight = channelRight;
        }

        public AudioTrack() {
        }

        /**
         * The volume of the track. Ranges from
         * 0 to 127 (0x7f).
         */
        public int volume = 0x7f;

        /**
         * The panning of the track. Ranges from
         * 0 (Completely to the left) to 127 (0x7f)
         * (Completely to the right) with the center
         * at 64 (0x40).
         */
        public int panning = 0x40;

        /**
         * The number of channels in the track.
         * If <c>1</c>, only {@link #channelLeft}
         * will be used for the mono track.
         * If <c>2</c>, both {@link #channelLeft}
         * and {@link #channelRight} will be used.
         */
        public int channelCount;

        /**
         * The zero-based ID of the left channel in a stereo
         * track, or the only channel in a mono track.
         */
        public int channelLeft;

        /**
         * The zero-based ID of the right channel in
         * a stereo track.
         */
        public int channelRight;

        public int surroundPanning;
        public int flags;

        public static List<AudioTrack> getDefaultTrackList(int channelCount) {
            List<AudioTrack> result = new ArrayList<>();
            int trackCount = divideByRoundUp(channelCount, 2);
            for (int i = 0; i < trackCount; i++) {
                int trackChannelCount = Math.min(channelCount - i * 2, 2);
                AudioTrack audioTrack = new AudioTrack();
                audioTrack.channelCount = trackChannelCount;
                audioTrack.channelLeft = i * 2;
                audioTrack.channelRight = trackChannelCount >= 2 ? i * 2 + 1 : 0;
                result.add(audioTrack);
            }
            return result;
        }
    }

    /** */
    public static abstract class AudioFormatBase<TFormat extends AudioFormatBase<TFormat, TBuilder, TConfig>, TBuilder extends AudioFormatBaseBuilder<TFormat, TBuilder, TConfig>, TConfig extends CodecParameters> implements IAudioFormat {

        Class<TConfig> c;

        private List<AudioTrack> _tracks;
        public int sampleRate;
        public int channelCount;

        @Override
        public int getSampleRate() { return sampleRate; }
        @Override
        public int getChannelCount() { return channelCount; }
        @Override
        public boolean isLooping() { return looping; }

        public int unalignedSampleCount;
        public int unalignedLoopStart;
        public int unalignedLoopEnd;

        @Override
        public int getSampleCount() {
            return unalignedSampleCount;
        }

        @Override
        public int getLoopStart() {
            return unalignedLoopStart;
        }

        @Override
        public int getLoopEnd() {
            return unalignedLoopEnd;
        }

        public boolean looping;
        public List<AudioTrack> tracks;

        @Override
        public abstract Pcm16Format toPcm16();

        @Override
        public Pcm16Format toPcm16(CodecParameters config) {
            return toPcm16();
        }

        @Override
        public abstract TFormat encodeFromPcm16(Pcm16Format pcm16);

        @Override
        public TFormat encodeFromPcm16(Pcm16Format pcm16, CodecParameters config) {
            return encodeFromPcm16(pcm16);
        }

        protected AudioFormatBase() {
        }

        protected AudioFormatBase(TBuilder builder) {
            unalignedSampleCount = builder.sampleCount;
            sampleRate = builder.sampleRate;
            channelCount = builder.getChannelCount();
            looping = builder.looping;
            unalignedLoopStart = builder.loopStart;
            unalignedLoopEnd = builder.loopEnd;
            _tracks = builder.tracks;
            tracks = _tracks != null && !_tracks.isEmpty() ? _tracks : AudioTrack.getDefaultTrackList(channelCount);
        }

        @Override
        public TFormat getChannels(int... channelRange) {
            if (channelRange == null)
                throw new NullPointerException("channelRange");

            return getChannelsInternal(channelRange);
        }

        protected abstract TFormat getChannelsInternal(int[] channelRange);

        @Override
        public TFormat withLoop(boolean loop) {
            return getCloneBuilder().withLoop(loop).build();
        }

        @Override
        public TFormat withLoop(boolean loop, int loopStart, int loopEnd) {
            return
                    getCloneBuilder().withLoop(loop, loopStart, loopEnd).build();
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean tryAdd(IAudioFormat format, /* out */ IAudioFormat[] result) {
            result[0] = null;
            var castFormat = (TFormat) format;
            if (castFormat == null) return false;
            try {
                result[0] = add(castFormat);
            } catch (Exception e) {
                return false;
            }
            return true;
        }

        public TFormat add(TFormat format) {
            if (format.unalignedSampleCount != unalignedSampleCount) {
                throw new IllegalArgumentException("Only audio streams of the same length can be added to each other.");
            }

            return addInternal(format);
        }

        protected abstract TFormat addInternal(TFormat format);

        public abstract TBuilder getCloneBuilder();

        protected TBuilder getCloneBuilderBase(TBuilder builder) {
            builder.sampleCount = unalignedSampleCount;
            builder.sampleRate = sampleRate;
            builder.looping = looping;
            builder.loopStart = unalignedLoopStart;
            builder.loopEnd = unalignedLoopEnd;
            builder.tracks = _tracks;
            return builder;
        }

        @SuppressWarnings("unchecked")
        private TConfig getDerivedParameters(CodecParameters param) {
            if (param == null) return null;
            var config = (TConfig) param;
            if (config != null) return config;

            try {
                TConfig tConfig = c.getDeclaredConstructor().newInstance();
                tConfig.sampleCount = param.sampleCount;
                tConfig.progress = param.progress;
                return tConfig;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** */
    public static abstract class AudioFormatBaseBuilder<TFormat extends AudioFormatBase<TFormat, TBuilder, TConfig>, TBuilder extends AudioFormatBaseBuilder<TFormat, TBuilder, TConfig>, TConfig extends CodecParameters> {

        public abstract int getChannelCount();

        protected boolean looping;
        protected int loopStart;
        protected int loopEnd;
        protected int sampleCount;
        protected int sampleRate;
        protected List<AudioTrack> tracks;

        public abstract TFormat build();

        @SuppressWarnings("unchecked")
        public TBuilder withLoop(boolean loop, int loopStart, int loopEnd) {
            if (!loop) {
                return withLoop(false);
            }

            if (loopStart < 0 || loopStart > sampleCount) {
                throw new IndexOutOfBoundsException("loopStart: Loop points must be less than the number of samples and non-negative.");
            }

            if (loopEnd < 0 || loopEnd > sampleCount) {
                throw new IndexOutOfBoundsException("loopEnd: Loop points must be less than the number of samples and non-negative.");
            }

            if (loopEnd < loopStart) {
                throw new IndexOutOfBoundsException("loopEnd: The loop end must be greater than the loop start");
            }

            looping = true;
            this.loopStart = loopStart;
            this.loopEnd = loopEnd;

            return (TBuilder) this;
        }

        @SuppressWarnings("unchecked")
        public TBuilder withLoop(boolean loop) {
            looping = loop;
            loopStart = 0;
            loopEnd = loop ? sampleCount : 0;
            return (TBuilder) this;
        }

        @SuppressWarnings("unchecked")
        public TBuilder withTracks(List<AudioTrack> tracks) {
            this.tracks = Objects.requireNonNull(tracks);
            return (TBuilder) this;
        }
    }

    /** */
    public static class Atrac9Parameters extends CodecParameters {

        public Atrac9Parameters() {
        }

        public Atrac9Parameters(CodecParameters source) {
            super(source);
        }
    }

    /** */
    public static class Atrac9Format extends AudioFormatBase<Atrac9Format, Atrac9FormatBuilder, Atrac9Parameters> {

        public final byte[][] audioData;
        public final Atrac9Config config;
        public final int encoderDelay;

        private Atrac9Format(Atrac9FormatBuilder b) {
            super(b);
            audioData = b.audioData;
            config = b.config;
            encoderDelay = b.encoderDelay;
        }

        @Override
        public Pcm16Format toPcm16() {
            return toPcm16(null);
        }

        @Override
        public Pcm16Format toPcm16(CodecParameters config) {
            short[][] audio = decode(config);

            return new Pcm16FormatBuilder(audio, sampleRate)
                    .withLoop(looping, unalignedLoopStart, unalignedLoopEnd)
                    .build();
        }

        private short[][] decode(CodecParameters parameters) {
            IProgressReport progress = Objects.requireNonNull(parameters).progress;
            if (progress != null) progress.setTotal(audioData.length);

            var decoder = new Atrac9Decoder();
            decoder.initialize(config.getConfigData());
            Atrac9Config config = decoder.getConfig();
Debug.println(Level.FINER, "array: short[][], " + config.getChannelCount() + " x " + getSampleCount());
            var pcmOut = createJaggedArray(short[][].class, config.getChannelCount(), getSampleCount());
Debug.println(Level.FINER, "array: pcmOut, " + pcmOut.length + " x " + pcmOut[0].length);
            var pcmBuffer = createJaggedArray(short[][].class, config.getChannelCount(), config.getSuperframeSamples());
Debug.println(Level.FINER, "array: pcmBuffer, " + pcmOut.length + " x " + pcmBuffer[0].length);

            for (int i = 0; i < audioData.length; i++) {
                decoder.decode(audioData[i], pcmBuffer);
                copyBuffer(pcmBuffer, pcmOut, encoderDelay, i);
                if (progress != null) progress.reportAdd(1);
            }
            return pcmOut;
        }

        private static void copyBuffer(short[][] bufferIn, short[][] bufferOut, int startIndex, int bufferIndex) {
            if (bufferIn == null || bufferOut == null || bufferIn.length == 0 || bufferOut.length == 0) {
                throw new IllegalArgumentException(
                        "{nameof(bufferIn)} and {nameof(bufferOut)} must be non-null with a length greater than 0");
            }

            int bufferLength = bufferIn[0].length;
            int outLength = bufferOut[0].length;

            int currentIndex = bufferIndex * bufferLength - startIndex;
            int remainingElements = Math.min(outLength - currentIndex, outLength);
            int srcStart = clamp(0 - currentIndex, 0, bufferLength);
            int destStart = Math.max(currentIndex, 0);

            int length = Math.min(bufferLength - srcStart, remainingElements);
            if (length <= 0) return;

            for (int c = 0; c < bufferOut.length; c++) {
                System.arraycopy(bufferIn[c], srcStart, bufferOut[c], destStart, length);
            }
        }

        @Override
        public Atrac9Format encodeFromPcm16(Pcm16Format pcm16) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Atrac9FormatBuilder getCloneBuilder() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Atrac9Format addInternal(Atrac9Format format) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Atrac9Format getChannelsInternal(int[] channelRange) {
            throw new UnsupportedOperationException();
        }
    }

    /** */
    public static class Atrac9FormatBuilder extends AudioFormatBaseBuilder<Atrac9Format, Atrac9FormatBuilder, Atrac9Parameters> {

        public Atrac9Config config;
        public final byte[][] audioData;

        @Override
        public int getChannelCount() {
            return config.getChannelCount();
        }

        public int encoderDelay;

        public Atrac9FormatBuilder(byte[][] audioData, Atrac9Config config, int sampleCount, int encoderDelay) {
            this.audioData = audioData;
            if (audioData == null) {
                throw new NullPointerException("audioData");
            }
            this.config = config;
            if (this.config == null) {
                throw new NullPointerException("config");
            }
            sampleRate = config.getSampleRate();
            this.sampleCount = sampleCount;
            this.encoderDelay = encoderDelay;
        }

        @Override
        public Atrac9Format build() {
            return new Atrac9Format(this);
        }
    }
}