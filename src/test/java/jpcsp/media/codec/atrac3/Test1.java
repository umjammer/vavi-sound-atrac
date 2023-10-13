/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
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
import vavi.util.Debug;
import vavi.util.StringUtil;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static vavi.sound.SoundUtil.volume;
import static vavix.util.DelayedWorker.later;


/**
 * Test1.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 231008 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
class Test1 {

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

        byte[] s = Files.newInputStream(Path.of(at3)).readAllBytes();
Debug.println("\n" + StringUtil.getDump(s, 64));
        var ainfo = new Atrac3Util.AtracFileInfo();
        var mem = ByteBuffer.wrap(s).order(ByteOrder.LITTLE_ENDIAN);
Debug.println("native: " + ByteOrder.nativeOrder() + ", mem: " + mem.order());
        int r = Atrac3Util.analyzeRiffFile(mem, 0, s.length, ainfo);
Debug.println(ainfo);
Debug.println("mem: " + mem.position());

Debug.println("codec: " + switch (r) {
 case Atrac3Util.PSP_CODEC_AT3 -> "ATRAC3";
 case Atrac3Util.PSP_CODEC_AT3PLUS ->  "ATRAC3plus";
 default -> "not ATRAC3";
});
        ICodec decoder = switch (r) {
            case Atrac3Util.PSP_CODEC_AT3 -> new Atrac3Decoder();
            case Atrac3Util.PSP_CODEC_AT3PLUS -> new Atrac3plusDecoder();
            default -> throw new IllegalArgumentException("not atrac3");
        };

        decoder.init(ainfo.atracBytesPerFrame, ainfo.atracChannels, ainfo.atracChannels, 0);

        mem.position(ainfo.inputFileDataOffset);
Debug.println("mem: " + mem.position());

        AudioFormat outAudioFormat = new AudioFormat(
                ainfo.atracSampleRate,
                16,
                ainfo.atracChannels,
                true,
                false);
Debug.println("OUT: " + outAudioFormat);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, outAudioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(outAudioFormat);
        line.addLineListener(ev -> Debug.println(ev.getType()));
        line.start();

        volume(line, volume);

        ByteBuffer out = ByteBuffer.allocate(decoder.getNumberOfSamples() * Short.BYTES * ainfo.atracChannels).order(ByteOrder.LITTLE_ENDIAN);
        var ipos = ainfo.inputFileDataOffset;
        while (!later(time).come()) {
            out.clear();
            var res = decoder.decode(mem, ipos, ainfo.atracBytesPerFrame, out, 0);
Debug.println(Level.FINER, res + ", " + out);
            if (res < 0) {
Debug.println(Level.WARNING, "res: " + res);
                break;
            }
            if (res == 0) break;

            int consumedBytes = ainfo.atracBytesPerFrame;
            if (res < ainfo.atracBytesPerFrame - 2 || res > ainfo.atracBytesPerFrame) {
                if (ainfo.atracBytesPerFrame == 0) {
                    consumedBytes = res;
                } else {
Debug.printf(Level.WARNING, "result 0x%X, expected 0x%X", res, ainfo.atracBytesPerFrame);
                }
            }

            ipos += consumedBytes;

Debug.println(Level.FINER, "position: " + out.position() + ", limit: " + out.limit() + ", endian: " + out.order());
            line.write(out.array(), 0, decoder.getNumberOfSamples() * Short.BYTES * ainfo.atracChannels);
        }

        line.drain();
        line.stop();
        line.close();
    }
}

/* */
