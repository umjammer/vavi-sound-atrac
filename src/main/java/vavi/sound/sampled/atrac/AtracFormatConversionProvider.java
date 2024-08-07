/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.atrac;

import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.FormatConversionProvider;


/**
 * AtracFormatConversionProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 231008 nsano initial version <br>
 */
public class AtracFormatConversionProvider extends FormatConversionProvider {

    @Override
    public AudioFormat.Encoding[] getSourceEncodings() {
        return new AudioFormat.Encoding[] {
                AtracEncoding.ATRAC3, AtracEncoding.ATRAC3PLUS, AtracEncoding.ATRAC_ADVANCED_LOSSLESS,
                AudioFormat.Encoding.PCM_SIGNED
        };
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings() {
        return new AudioFormat.Encoding[] {
                AtracEncoding.ATRAC3, AtracEncoding.ATRAC3PLUS, AtracEncoding.ATRAC_ADVANCED_LOSSLESS,
                AudioFormat.Encoding.PCM_SIGNED
        };
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
            return new AudioFormat.Encoding[] {
                    AtracEncoding.ATRAC3, AtracEncoding.ATRAC3PLUS, AtracEncoding.ATRAC_ADVANCED_LOSSLESS};
        } else if (sourceFormat.getEncoding() instanceof AtracEncoding) {
            return new AudioFormat.Encoding[] {AudioFormat.Encoding.PCM_SIGNED};
        } else {
            return new AudioFormat.Encoding[0];
        }
    }

    @Override
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) &&
                targetEncoding instanceof AtracEncoding) {
            if (sourceFormat.getChannels() > 2 ||
                    sourceFormat.getChannels() <= 0 ||
                    sourceFormat.isBigEndian()) {
                return new AudioFormat[0];
            } else {
                return new AudioFormat[] {
                        new AudioFormat(targetEncoding,
                                sourceFormat.getSampleRate(),
                                AudioSystem.NOT_SPECIFIED,    // sample size in bits
                                sourceFormat.getChannels(),
                                AudioSystem.NOT_SPECIFIED,    // frame size
                                AudioSystem.NOT_SPECIFIED,    // frame rate
                                false)                        // little endian
                };
            }
        } else if (sourceFormat.getEncoding() instanceof AtracEncoding &&
                targetEncoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
            return new AudioFormat[] {
                    new AudioFormat(sourceFormat.getSampleRate(),
                            16,           // sample size in bits
                            sourceFormat.getChannels(),
                            true,                // signed
                            false)                      // little endian (for PCM wav)
            };
        } else {
            return new AudioFormat[0];
        }
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding, AudioInputStream sourceStream) {
        if (isConversionSupported(targetEncoding, sourceStream.getFormat())) {
            AudioFormat[] formats = getTargetFormats(targetEncoding, sourceStream.getFormat());
            if (formats != null && formats.length > 0) {
                AudioFormat sourceFormat = sourceStream.getFormat();
                AudioFormat targetFormat = formats[0];
                if (sourceFormat.equals(targetFormat)) {
                    return sourceStream;
                } else if (sourceFormat.getEncoding() instanceof AtracEncoding &&
                        targetFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
                    try {
                        if (sourceFormat.getEncoding().equals(AtracEncoding.ATRAC_ADVANCED_LOSSLESS)) {
                            return new Atrac9ToPcmAudioInputStream(sourceStream, targetFormat, AudioSystem.NOT_SPECIFIED);
                        } else {
                            return new Atrac3ToPcmAudioInputStream(sourceStream, targetFormat, AudioSystem.NOT_SPECIFIED);
                        }
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);
                    }
                } else if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) &&
                        targetFormat.getEncoding() instanceof AtracEncoding) {
                    throw new IllegalArgumentException("unable to convert " + sourceFormat + " to " + targetFormat);
                } else {
                    throw new IllegalArgumentException("unable to convert " + sourceFormat + " to " + targetFormat.toString());
                }
            } else {
                throw new IllegalArgumentException("target format not found");
            }
        } else {
            throw new IllegalArgumentException("conversion not supported");
        }
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream sourceStream) {
        if (isConversionSupported(targetFormat, sourceStream.getFormat())) {
            AudioFormat[] formats = getTargetFormats(targetFormat.getEncoding(), sourceStream.getFormat());
            if (formats != null && formats.length > 0) {
                AudioFormat sourceFormat = sourceStream.getFormat();
                if (sourceFormat.equals(targetFormat)) {
                    return sourceStream;
                } else if (sourceFormat.getEncoding() instanceof AtracEncoding &&
                        targetFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
                    try {
                        if (sourceFormat.getEncoding().equals(AtracEncoding.ATRAC_ADVANCED_LOSSLESS)) {
                            return new Atrac9ToPcmAudioInputStream(sourceStream, targetFormat, AudioSystem.NOT_SPECIFIED);
                        } else {
                            return new Atrac3ToPcmAudioInputStream(sourceStream, targetFormat, AudioSystem.NOT_SPECIFIED);
                        }
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);
                    }
                } else if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) &&
                        targetFormat.getEncoding() instanceof AtracEncoding) {
                    throw new IllegalArgumentException("unable to convert " + sourceFormat + " to " + targetFormat);
                } else {
                    throw new IllegalArgumentException("unable to convert " + sourceFormat + " to " + targetFormat);
                }
            } else {
                throw new IllegalArgumentException("target format not found");
            }
        } else {
            throw new IllegalArgumentException("conversion not supported");
        }
    }
}
