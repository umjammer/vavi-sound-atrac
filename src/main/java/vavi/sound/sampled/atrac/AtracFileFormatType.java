/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.atrac;

import javax.sound.sampled.AudioFileFormat;


/**
 * FileFormatTypes used by ATRAC audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 231008 nsano initial version <br>
 */
public class AtracFileFormatType extends AudioFileFormat.Type {

    /**
     * Specifies an ATRAC (RIFF/WAVE) file.
     */
    public static final AudioFileFormat.Type ATRAC = new AtracFileFormatType("ATRAC3", "at3");
    public static final AudioFileFormat.Type ATRAC9 = new AtracFileFormatType("ATRAC Advanced Lossless", "at9");

    /**
     * Constructs a file type.
     *
     * @param name      the name of the ATRAC File Format.
     * @param extension the file extension for this ATRAC File Format.
     */
    public AtracFileFormatType(String name, String extension) {
        super(name, extension);
    }
}

/* */
