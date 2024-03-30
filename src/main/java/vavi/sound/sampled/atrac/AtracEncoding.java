/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.atrac;


import java.util.UUID;
import javax.sound.sampled.AudioFormat;


/**
 * Encodings used by the ATRAC audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 231008 nsano initial version <br>
 */
public class AtracEncoding extends AudioFormat.Encoding {

    public static final int AT3_MAGIC = 0x0270; // "AT3"
    public static final int WAVE_FORMAT_EXTENSIBLE = 0xFFFE; // "AT3PLUS", "AT9"

    /** Specifies any ALAC encoded data. */
    public static final AtracEncoding ATRAC = new AtracEncoding("ATRAC", 0); // TODO
    public static final AtracEncoding ATRAC3 = new AtracEncoding("ATRAC3", AT3_MAGIC);
    public static final AtracEncoding ATRAC3PLUS = new AtracEncoding("ATRAC3plus", WAVE_FORMAT_EXTENSIBLE, UUID.fromString("e923aabf-cb58-4471-a119-fffa01e4ce62"));
    /** AT9 */
    public static final AtracEncoding ATRAC_ADVANCED_LOSSLESS = new AtracEncoding("ATRAC Advanced Lossless", WAVE_FORMAT_EXTENSIBLE, UUID.fromString("47E142D2-36BA-4d8d-88FC-61654F8C836C"));

    public final int magic;
    public final UUID guid;

    /**
     * Constructs a new encoding.
     *
     * @param name Name of the ALAC encoding.
     */
    public AtracEncoding(String name, int magic) {
        this(name, magic, null);
    }

    public AtracEncoding(String name, int magic, UUID guid) {
        super(name);
        this.magic = magic;
        this.guid = guid;
    }
}
