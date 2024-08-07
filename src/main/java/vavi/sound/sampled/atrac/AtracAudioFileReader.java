/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.atrac;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

import libatrac9.decoder.Atrac9.WaveFormatExtensible;
import libatrac9.decoder.Atrac9.fact;
import libatrac9.decoder.Atrac9.smpl;
import vavi.util.Debug;
import vavi.util.win32.Chunk;
import vavi.util.win32.WAVE;
import vavi.util.win32.WAVE.fmt;

import static vavi.sound.sampled.atrac.AtracEncoding.ATRAC3;
import static vavi.sound.sampled.atrac.AtracEncoding.ATRAC3PLUS;
import static vavi.sound.sampled.atrac.AtracEncoding.ATRAC_ADVANCED_LOSSLESS;


/**
 * Provider for Opus audio file reading services. This implementation can parse
 * the format information from Opus audio file, and can produce audio input
 * streams from files of this type.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 231008 nsano initial version <br>
 */
public class AtracAudioFileReader extends AudioFileReader {

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
     * @param bitStream   input to decode
     * @param mediaLength unused
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
        float sampleRate;
        int channels;
        Encoding encoding;
        try {
            bitStream.mark(512);
            Map<String, Object> context = new HashMap<>();
            context.put(WAVE.CHUNK_PARSE_STRICT_KEY, true);
            context.put(WAVE.MULTIPART_CHUNK_PARSE_STRICT_KEY, true);
            context.put(WAVE.WAVE_DATA_NOT_LOAD_KEY, true);
            context.put(WAVE.CHUNK_SUB_CHUNK_KEY_BASE + fact.class.getSimpleName(), fact.class);
            context.put(WAVE.CHUNK_SUB_CHUNK_KEY_BASE + smpl.class.getSimpleName(), smpl.class);
            WAVE wave = Chunk.readFrom(bitStream, WAVE.class, context);
            fmt fmt = wave.findChildOf(WAVE.fmt.class);
            int formatCode = fmt.getFormatId();
            Debug.println(Level.FINER, "formatCode: " + formatCode);
            sampleRate = fmt.getSamplingRate();
            channels = fmt.getNumberChannels();
            encoding = switch (formatCode) {
                case AtracEncoding.AT3_MAGIC -> ATRAC3;
                case AtracEncoding.WAVE_FORMAT_EXTENSIBLE -> {
                    if (fmt.getExtended() == null) throw new IllegalArgumentException("no fmt.extension");
                    var wavext = new WaveFormatExtensible(fmt.getExtended());
                    Debug.println(Level.FINER, "subFormat: " + wavext.subFormat);
                    if (wavext.subFormat.equals(ATRAC_ADVANCED_LOSSLESS.guid)) {
                        yield ATRAC_ADVANCED_LOSSLESS;
                    }
                    if (wavext.subFormat.equals(ATRAC3PLUS.guid)) {
                        yield ATRAC3PLUS;
                    } else {
                        throw new UnsupportedAudioFileException("guid: " + wavext.subFormat);
                    }
                }
                default -> throw new UnsupportedAudioFileException("formatCode: " + formatCode);
            };
            format = new AudioFormat(encoding,
                    sampleRate,
                    AudioSystem.NOT_SPECIFIED,
                    channels,
                    AudioSystem.NOT_SPECIFIED,
                    AudioSystem.NOT_SPECIFIED,
                    true);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            Debug.println(Level.FINE, e);
            Debug.printStackTrace(Level.FINER, e);
            throw (UnsupportedAudioFileException) new UnsupportedAudioFileException(e.getMessage()).initCause(e);
        } finally {
            try {
                bitStream.reset();
            } catch (IOException e) {
                if (Debug.isLoggable(Level.FINEST))
                    Debug.printStackTrace(e);
                else
                    Debug.println(Level.FINE, e);
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
     * @param mediaLength inputStream length
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
