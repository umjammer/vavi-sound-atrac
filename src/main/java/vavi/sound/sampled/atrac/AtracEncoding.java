/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.atrac;


import javax.sound.sampled.AudioFormat;


/**
 * Encodings used by the ATRAC audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 231008 nsano initial version <br>
 */
public class AtracEncoding extends AudioFormat.Encoding {

    public static final int AT3_MAGIC = 0x0270; // "AT3"
    public static final int AT3_PLUS_MAGIC = 0xFFFE; // "AT3PLUS"

    /** Specifies any ALAC encoded data. */
    public static final AtracEncoding ATRAC = new AtracEncoding("ATRAC", 0); // TODO
    public static final AtracEncoding ATRAC3 = new AtracEncoding("ATRAC3", AT3_MAGIC);
    public static final AtracEncoding ATRAC3PLUS = new AtracEncoding("ATRAC3plus", AT3_PLUS_MAGIC);
    public static final AtracEncoding ATRAC_ADVANCED_LOSSLESS = new AtracEncoding("ATRAC Advanced Lossless", 0); // TODO

    public final int magic;

    /**
     * Constructs a new encoding.
     *
     * @param name Name of the ALAC encoding.
     */
    public AtracEncoding(String name, int magic) {
        super(name);
        this.magic = magic;
    }
}
