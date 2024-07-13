/*
 * This file is part of jpcsp.
 *
 * Jpcsp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Jpcsp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */

package jpcsp.media.codec.atrac3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import jpcsp.media.codec.ICodec;
import jpcsp.media.codec.atrac3plus.Atrac3plusDecoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vavi.util.ByteUtil;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static jpcsp.media.codec.atrac3.Atrac3Util.AT3_MAGIC;
import static jpcsp.media.codec.atrac3.Atrac3Util.AT3_PLUS_MAGIC;
import static jpcsp.media.codec.atrac3.Atrac3Util.DATA_CHUNK_MAGIC;
import static jpcsp.media.codec.atrac3.Atrac3Util.FMT_CHUNK_MAGIC;
import static jpcsp.media.codec.atrac3.Atrac3Util.PSP_CODEC_AT3;
import static jpcsp.media.codec.atrac3.Atrac3Util.PSP_CODEC_AT3PLUS;
import static jpcsp.media.codec.atrac3.Atrac3Util.RIFF_MAGIC;
import static vavi.sound.SoundUtil.volume;


@PropsEntity(url = "file:local.properties")
public class CodecTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "at3")
    String at3 = "src/test/resources/sample.at3";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    static long time;
    static double volume;

    static {
        time = System.getProperty("vavi.test", "").equals("ide") ? 1000 * 1000 : 9 * 1000;
        volume = Double.parseDouble(System.getProperty("vavi.test.volume",  "0.2"));
    }

    @Test
    void test1() throws Exception {

        byte[] buffer = Files.newInputStream(Path.of(at3)).readAllBytes();
        int length = buffer.length;

        int inputAddr = 0;

        int channels = 2;
        int codecType = -1;
        int bytesPerFrame = 0;
        int codingMode = 0;
        int dataOffset = 0;
        if (ByteUtil.readLeInt(buffer, inputAddr) == RIFF_MAGIC) {
            int scanOffset = 12;
            while (dataOffset <= 0) {
                int chunkMagic = ByteUtil.readLeInt(buffer,inputAddr + scanOffset);
                int chunkLength = ByteUtil.readLeInt(buffer,inputAddr + scanOffset + 4);
                scanOffset += 8;
Debug.println("@CHUNK: " + Atrac3Util.getStringFromInt32(chunkMagic) + ", offset: " + scanOffset + ", length: " + chunkLength);
                switch (chunkMagic) {
                case FMT_CHUNK_MAGIC:
                    codecType = switch (ByteUtil.readLeShort(buffer, inputAddr + scanOffset) & 0xffff) {
                        case AT3_PLUS_MAGIC -> PSP_CODEC_AT3PLUS;
                        case AT3_MAGIC -> PSP_CODEC_AT3;
                        default -> codecType;
                    };
                    channels = ByteUtil.readLeShort(buffer, inputAddr + scanOffset + 2);
Debug.println("channels: " + channels);
                    bytesPerFrame = ByteUtil.readLeShort(buffer, inputAddr + scanOffset + 12);
Debug.println("bytesPerFrame: " + bytesPerFrame);
                    int extraDataSize = ByteUtil.readLeShort(buffer, inputAddr + scanOffset + 16);
                    if (extraDataSize == 14) {
                        codingMode = ByteUtil.readLeShort(buffer, inputAddr + scanOffset + 18 + 6);
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

        AudioFormat audioFormat = new AudioFormat(44100,
                16,
                channels,
                true,
                false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine mLine = (SourceDataLine) AudioSystem.getLine(info);
        mLine.open(audioFormat);
        mLine.start();
        volume(mLine, volume);

        ICodec codec = switch (codecType) {
            case PSP_CODEC_AT3 -> new Atrac3Decoder();
            case PSP_CODEC_AT3PLUS ->  new Atrac3plusDecoder();
            default -> throw new IllegalArgumentException("not atrac3");
        };
Debug.println("codec: " + codec);
        codec.init(bytesPerFrame, channels, channels, codingMode);

        inputAddr += dataOffset;
Debug.println("inputAddr: " + inputAddr);
        length -= dataOffset;

        ByteBuffer in = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer out = ByteBuffer.allocate(codec.getNumberOfSamples() * 2 * channels).order(ByteOrder.LITTLE_ENDIAN);

        for (int frameNbr = 0; true; frameNbr++) {
            out.clear();
            int result = codec.decode(in, inputAddr, length, out, 0);
            if (result < 0) {
Debug.println(Level.SEVERE, String.format("Frame #%d, result 0x%08X", frameNbr, result));
                break;
            }
            if (result == 0) {
                // End of data
                break;
            }
            int consumedBytes = bytesPerFrame;
            if (result < bytesPerFrame - 2 || result > bytesPerFrame) {
                if (bytesPerFrame == 0) {
                    consumedBytes = result;
                } else {
Debug.println(Level.WARNING, String.format("Frame #%d, result 0x%X, expected 0x%X", frameNbr, result, bytesPerFrame));
                }
            }

            inputAddr += consumedBytes;
            length -= consumedBytes;

            byte[] bytes = new byte[codec.getNumberOfSamples() * 2 * channels];
Debug.println(Level.FINEST, "out: " + bytes.length);
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = out.get(i);
            }
            mLine.write(bytes, 0, bytes.length);
        }

        mLine.drain();
        mLine.close();
    }
}
