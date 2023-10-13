/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.atrac3;


import javax.sound.sampled.AudioFormat;


/**
 * Encodings used by the ATRAC audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 231008 nsano initial version <br>
 */
public class Atrac3Encoding extends AudioFormat.Encoding {

    public static final int AT3_MAGIC = 0x0270; // "AT3"
    public static final int AT3_PLUS_MAGIC = 0xFFFE; // "AT3PLUS"

    /** Specifies any ALAC encoded data. */
    public static final Atrac3Encoding ATRAC = new Atrac3Encoding("ATRAC", 0); // TODO
    public static final Atrac3Encoding ATRAC3 = new Atrac3Encoding("ATRAC3", AT3_MAGIC);
    public static final Atrac3Encoding ATRAC3PLUS = new Atrac3Encoding("ATRAC3plus", AT3_PLUS_MAGIC);
    public static final Atrac3Encoding ATRAC_ADVANCED_LOSSLESS = new Atrac3Encoding("ATRAC Advanced Lossless", 0); // TODO

    public final int magic;

    /**
     * Constructs a new encoding.
     *
     * @param name Name of the ALAC encoding.
     */
    public Atrac3Encoding(String name, int magic) {
        super(name);
        this.magic = magic;
    }
}

/* */
