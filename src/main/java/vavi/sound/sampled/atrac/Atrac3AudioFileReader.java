/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.atrac;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.logging.Level;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

import vavi.util.ByteUtil;
import vavi.util.Debug;

import static vavi.sound.sampled.atrac.AtracEncoding.ATRAC3;
import static vavi.sound.sampled.atrac.AtracEncoding.ATRAC3PLUS;


/**
 * Provider for Opus audio file reading services. This implementation can parse
 * the format information from Opus audio file, and can produce audio input
 * streams from files of this type.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 231008 nsano initial version <br>
 */
public class Atrac3AudioFileReader extends AudioFileReader {

    @Override
    public AudioFileFormat getAudioFileFormat(File file) throws UnsupportedAudioFileException, IOException {
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            return getAudioFileFormat(new BufferedInputStream(inputStream), (int) file.length());
        }
    }

    @Override
    public AudioFileFormat getAudioFileFormat(URL url) throws UnsupportedAudioFileException, IOException {
        try (InputStream inputStream = url.openStream()) {
            return getAudioFileFormat(inputStream instanceof BufferedInputStream ? inputStream : new BufferedInputStream(inputStream));
        }
    }

    @Override
    public AudioFileFormat getAudioFileFormat(InputStream stream) throws UnsupportedAudioFileException, IOException {
        return getAudioFileFormat(stream, AudioSystem.NOT_SPECIFIED);
    }

    /**
     * Return the AudioFileFormat from the given InputStream. Implementation.
     *
     * @param bitStream
     * @param mediaLength
     * @return an AudioInputStream object based on the audio file data contained
     * in the input stream.
     * @throws UnsupportedAudioFileException if the File does not point to a
     *                                       valid audio file data recognized by the system.
     * @throws IOException                   if an I/O exception occurs.
     */
    protected AudioFileFormat getAudioFileFormat(InputStream bitStream, int mediaLength) throws UnsupportedAudioFileException, IOException {
Debug.println(Level.FINE, "enter available: " + bitStream.available());
        if (!bitStream.markSupported()) {
            throw new IllegalArgumentException("must be mark supported");
        }
        AudioFormat format;
        try {
            bitStream.mark(28);
            DataInputStream dis = new DataInputStream(bitStream);
            byte[] b = new byte[28];
            dis.readFully(b);
            int codec = ByteUtil.readLeShort(b, 20) & 0xffff;
            if (!new String(b, 0, 4).equals("RIFF") ||
                    !new String(b, 8, 4).equals("WAVE") ||
                    !new String(b, 12, 4).equals("fmt ") || // TODO depends fixed chunk order
                    !(codec == ATRAC3.magic || codec == ATRAC3PLUS.magic)
            ) {
                throw new UnsupportedAudioFileException("not ATRAC3/ATRAC3plus");
            }
            int channels = ByteUtil.readLeShort(b, 22) & 0xffff;
            int sampleRate = ByteUtil.readLeInt(b, 24);
            AudioFormat.Encoding encoding = switch (codec) {
                case AtracEncoding.AT3_MAGIC -> ATRAC3;
                case AtracEncoding.AT3_PLUS_MAGIC -> ATRAC3PLUS;
                default -> throw new UnsupportedAudioFileException("codec: " + codec);
            };
            format = new AudioFormat(encoding,
                    sampleRate,
                    AudioSystem.NOT_SPECIFIED,
                    channels,
                    AudioSystem.NOT_SPECIFIED,
                    AudioSystem.NOT_SPECIFIED,
                    true);
        } catch (IOException | IllegalArgumentException e) {
            throw (UnsupportedAudioFileException) new UnsupportedAudioFileException(e.getMessage()).initCause(e);
        } finally {
            try {
                bitStream.reset();
            } catch (IOException e) {
                Debug.println(e.toString());
            }
Debug.println(Level.FINE, "finally available: " + bitStream.available());
        }
        return new AudioFileFormat(AtracFileFormatType.ATRAC, format, AudioSystem.NOT_SPECIFIED);
    }

    @Override
    public AudioInputStream getAudioInputStream(File file) throws UnsupportedAudioFileException, IOException {
        InputStream inputStream = Files.newInputStream(file.toPath());
        return getAudioInputStream(new BufferedInputStream(inputStream), (int) file.length());
    }

    @Override
    public AudioInputStream getAudioInputStream(URL url) throws UnsupportedAudioFileException, IOException {
        InputStream inputStream = url.openStream();
        return getAudioInputStream(inputStream instanceof BufferedInputStream ? inputStream : new BufferedInputStream(inputStream));
    }

    @Override
    public AudioInputStream getAudioInputStream(InputStream stream) throws UnsupportedAudioFileException, IOException {
        return getAudioInputStream(stream, AudioSystem.NOT_SPECIFIED);
    }

    /**
     * Obtains an audio input stream from the input stream provided. The stream
     * must point to valid audio file data.
     *
     * @param inputStream the input stream from which the AudioInputStream
     *                    should be constructed.
     * @param mediaLength
     * @return an AudioInputStream object based on the audio file data contained
     * in the input stream.
     * @throws UnsupportedAudioFileException if the File does not point to a
     *                                       valid audio file data recognized by the system.
     * @throws IOException                   if an I/O exception occurs.
     */
    protected AudioInputStream getAudioInputStream(InputStream inputStream, int mediaLength) throws UnsupportedAudioFileException, IOException {
        AudioFileFormat audioFileFormat = getAudioFileFormat(inputStream, mediaLength);
        return new AudioInputStream(inputStream, audioFileFormat.getFormat(), audioFileFormat.getFrameLength());
    }
}
