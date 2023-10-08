/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.atrac3;

import jpcsp.media.codec.ICodec;
import jpcsp.media.codec.atrac3.Atrac3Decoder;
import org.junit.jupiter.api.Test;


class Test1 {

    @Test
    //@EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test1() throws Exception {
        Test1.class.getResourceAsStream("sample.at3");
        ICodec codec = new Atrac3Decoder();
        codec.
    }
}

/* */
