/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package libatrac9;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vavi.util.Debug;
import vavi.util.StringUtil;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static vavi.sound.SoundUtil.volume;


/**
 * Test2.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-02-03 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class Test2 {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "at9")
    String at9 = "src/test/resources/snd0.at9";

    Path tmp = Path.of("tmp");

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

        if (!Files.exists(tmp)) {
            Files.createDirectories(tmp);
        }
    }

    static long time;
    static double volume;

    static {
        time = System.getProperty("vavi.test", "").equals("ide") ? 1000 * 1000 : 9 * 1000;
        volume = Double.parseDouble(System.getProperty("vavi.test.volume",  "0.2"));
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test1() throws Exception {

        byte[] pcm = Atrac9.loadAt9(at9);
Debug.println("wave\n" + StringUtil.getDump(pcm, 128));

        Path out = tmp.resolve("out.wav");
        Files.write(out, pcm);

        AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(out)));
        Clip clip = AudioSystem.getClip();
        CountDownLatch cdl = new CountDownLatch(1);
        clip.addLineListener(e -> {
            Debug.println(e.getType());
            if (e.getType() == LineEvent.Type.STOP) cdl.countDown();
        });
        clip.open(ais);
        volume(clip, volume);
        clip.start();
        cdl.await();
        clip.drain();
        clip.stop();
        clip.close();
    }
}
