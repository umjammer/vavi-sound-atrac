/*
 * https://github.com/xXxTheDarkprogramerxXx/PS4_Tools/blob/master/PS4_Tools/Main.cs
 */

package libatrac9.decoder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.UUID;

import libatrac9.Atrac9Config;
import vavi.io.LittleEndianDataInputStream;
import vavi.util.ByteUtil;
import vavi.util.win32.Chunk;
import vavi.util.win32.WAVE;
import vavi.util.win32.WAVE.data;


/**
 * Atrac9.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-03-29 nsano initial version <br>
 */
public class Atrac9 {

    /**
     * Atract 9 Structure class
     */
    public static class At9Structure {

        public Atrac9Config config;
        public int sampleCount;
        public int version;
        public int encoderDelay;
        public int superframeCount;

        public boolean looping;
        public int loopStart;
        public int loopEnd;

        @Override
        public String toString() {
            return new StringJoiner(", ", At9Structure.class.getSimpleName() + "[", "]")
                    .add("config=" + config)
                    .add("sampleCount=" + sampleCount)
                    .add("version=" + version)
                    .add("encoderDelay=" + encoderDelay)
                    .add("superframeCount=" + superframeCount)
                    .add("looping=" + looping)
                    .add("loopStart=" + loopStart)
                    .add("loopEnd=" + loopEnd)
                    .toString();
        }
    }

    /** WAVE.fmt.ext */
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

        public WaveFormatExtensible(byte[] b) throws IOException {
            size = b.length;

            validBitsPerSample = ByteUtil.readLeShort(b, 0);
            channelMask = ByteUtil.readLeInt(b, 2);
            subFormat = ByteUtil.readLeUUID(b, 6);
        }
    }

    /** WAVE.fmt.ext for AT9 */
    public static class At9WaveExtensible extends WaveFormatExtensible {

        public int versionInfo;
        public byte[] configData;
        public int reserved;

        public At9WaveExtensible(byte[] b) throws IOException {
            super(b);

            versionInfo = ByteUtil.readLeInt(b, 22);
            configData = Arrays.copyOfRange(b, 26, 30);
            reserved = ByteUtil.readLeInt(b, 30);
        }
    }

    /** WAVE.fact for AT9 */
    public static class fact extends WAVE.fact {

        static abstract class union {
            public abstract void setData(LittleEndianDataInputStream ledis) throws IOException;
        }

        public static class at3 extends union {

            public int atracEndSample;

            @Override
            public void setData(LittleEndianDataInputStream ledis) throws IOException {
                atracEndSample = ledis.readInt();
            }

            @Override
            public String toString() {
                return new StringJoiner(", ", at3.class.getSimpleName() + "[", "]")
                        .add("atracEndSample=" + atracEndSample)
                        .toString();
            }
        }

        public static class at9 extends union {

            public int inputOverlapDelaySamples;
            public int encoderDelaySamples;

            @Override
            public void setData(LittleEndianDataInputStream ledis) throws IOException {
                inputOverlapDelaySamples = ledis.readInt();
                encoderDelaySamples = ledis.readInt();
            }

            @Override
            public String toString() {
                return new StringJoiner(", ", at9.class.getSimpleName() + "[", "]")
                        .add("inputOverlapDelaySamples=" + inputOverlapDelaySamples)
                        .add("encoderDelaySamples=" + encoderDelaySamples)
                        .toString();
            }
        }

        public union u;

        @Override
        public void setData(InputStream is) throws IOException {
            LittleEndianDataInputStream ledis = new LittleEndianDataInputStream(is);
            super.setData(is);
            if (getLength() == 8) {
                u = new at3();
                u.setData(ledis);
            } else if (getLength() == 12) {
                u = new at9();
                u.setData(ledis);
            } else {
                throw new IllegalArgumentException("unsupported fact length: " + getLength());
            }
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", fact.class.getSimpleName() + "[", "]")
                    .add("fileSize=" + sampleCount)
                    .add(u.toString())
                    .toString();
        }
    }

    public static class smpl extends Chunk {

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

        @Override
        public void setData(InputStream is) throws IOException {
            LittleEndianDataInputStream ledis = new LittleEndianDataInputStream(is);
            manufacturer = ledis.readInt();
            product = ledis.readInt();
            samplePeriod = ledis.readInt();
            midiUnityNote = ledis.readInt();
            midiPitchFraction = ledis.readInt();
            smpteFormat = ledis.readInt();
            smpteOffset = ledis.readInt();
            sampleLoops = ledis.readInt();
            samplerData = ledis.readInt();
            loops = new SampleLoop[sampleLoops];

            for (int i = 0; i < sampleLoops; i++) {
                loops[i] = new SampleLoop();
                loops[i].cuePointId = ledis.readInt();
                loops[i].type = ledis.readInt();
                loops[i].start = ledis.readInt();
                loops[i].end = ledis.readInt();
                loops[i].fraction = ledis.readInt();
                loops[i].playCount = ledis.readInt();
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
}

