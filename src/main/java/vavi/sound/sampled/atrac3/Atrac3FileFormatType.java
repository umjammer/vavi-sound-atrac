/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.atrac3;

import javax.sound.sampled.AudioFileFormat;


/**
 * FileFormatTypes used by ATRAC audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 231008 nsano initial version <br>
 */
public class Atrac3FileFormatType extends AudioFileFormat.Type {

    /**
     * Specifies an ATRAC (RIFF/WAVE) file.
     */
    public static final AudioFileFormat.Type ATRAC = new Atrac3FileFormatType("ATRAC3", "at3");

    /**
     * Constructs a file type.
     *
     * @param name      the name of the ATRAC File Format.
     * @param extension the file extension for this ATRAC File Format.
     */
    public Atrac3FileFormatType(String name, String extension) {
        super(name, extension);
    }
}

/* */
