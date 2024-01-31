/*
 * https://github.com/xXxTheDarkprogramerxXx/PS4_Tools/blob/master/PS4_Tools/Main.cs
 */

package libatrac9;

import java.io.IOException;

import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;
import libatrac9.Atract9FormatBuilder.At9Reader;
import libatrac9.Atract9FormatBuilder.Atrac9FormatBuilder;
import libatrac9.Atract9FormatBuilder.AudioData;
import libatrac9.Atract9FormatBuilder.AudioInfo;
import libatrac9.Atract9FormatBuilder.FileType;
import libatrac9.Atract9FormatBuilder.IAudioFormat;


/**
 * Atrac9.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-01-31 nsano initial version <br>
 */
public class Atrac9 {

    /**
     * Atract 9 Structure class
     */
    public static class At9Structure {

        public Atrac9Config config;
        public byte[][] audioData;
        public int sampleCount;
        public int version;
        public int encoderDelay;
        public int superframeCount;

        public boolean looping;
        public int loopStart;
        public int loopEnd;
    }

    /**
     * Allows users to load an at9 for decoding and returns the wav as a byte array
     * @param at9file Location of at9 file
     * @return Byte Array of Wav file
     */
    public static byte[] loadAt9(String at9file) throws IOException {
        // Byte array holder for return vars
        byte[] array;

        try (Stream stream = new FileStream(at9file, FileMode.Open, FileAccess.Read)) {
            At9Reader reader = new At9Reader();
            At9Structure structure = reader.readFile(stream, true);
            IAudioFormat format = new Atrac9FormatBuilder(structure.audioData, structure.config, structure.sampleCount, structure.encoderDelay)
                    .withLoop(structure.looping, structure.loopStart, structure.loopEnd)
                    .build();
            //now we have the atrac9 format now we need to play it somehow
            AudioData AudioData = new AudioData(format);
            MemoryStream SongStream = new MemoryStream();
            AudioInfo.containers.get(FileType.Wave).getWriter.get().writeToStream(AudioData, SongStream, null, null);

            // Uncomment this if you need to test but this definitely works
            //System.out.println(at9file + ".wav", SongStream.ToArray());

            array = SongStream.toArray();
        }

        return array;
    }

    /**
     * Allows user to load an at9 to At9Structure
     * @param at9file Location of at9 file
     * @return At9Structure
     */
    public static At9Structure load_At9(String at9file) throws IOException {
        At9Structure at9;

        try (Stream stream = new FileStream(at9file, FileMode.Open, FileAccess.Read)) {
            At9Reader reader = new At9Reader();
            at9 = reader.readFile(stream, true);
        }

        return at9;
    }
}

