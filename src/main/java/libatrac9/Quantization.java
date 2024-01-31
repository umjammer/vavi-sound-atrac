/*
 * MIT License
 *
 * Copyright (c) 2018 Alex Barney
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package libatrac9;

import java.util.Arrays;


class Quantization {

    public static void dequantizeSpectra(Block block) {
        for (Channel channel : block.getChannels()) {
            Arrays.fill(channel.getSpectra(), 0, channel.getSpectra().length, 0);

            for (int i = 0; i < channel.getCodedQuantUnits(); i++) {
                dequantizeQuantUnit(channel, i);
            }
        }
    }

    private static void dequantizeQuantUnit(Channel channel, int band) {
        int subBandIndex = Tables.QuantUnitToCoeffIndex[band];
        int subBandCount = Tables.QuantUnitToCoeffCount[band];
        double stepSize = Tables.QuantizerStepSize[channel.getPrecisions()[band]];
        double stepSizeFine = Tables.QuantizerFineStepSize[channel.getPrecisionsFine()[band]];

        for (int sb = 0; sb < subBandCount; sb++) {
            double coarse = channel.getQuantizedSpectra()[subBandIndex + sb] * stepSize;
            double fine = channel.getQuantizedSpectraFine()[subBandIndex + sb] * stepSizeFine;
            channel.getSpectra()[subBandIndex + sb] = coarse + fine;
        }
    }

    public static void scaleSpectrum(Block block) {
        for (Channel channel : block.getChannels()) {
            scaleSpectrum(channel);
        }
    }

    private static void scaleSpectrum(Channel channel) {
        int quantUnitCount = channel.getBlock().getQuantizationUnitCount();
        double[] spectra = channel.getSpectra();

        for (int i = 0; i < quantUnitCount; i++) {
            for (int sb = Tables.QuantUnitToCoeffIndex[i]; sb < Tables.QuantUnitToCoeffIndex[i + 1]; sb++) {
                spectra[sb] *= Tables.SpectrumScale[channel.getScaleFactors()[i]];
            }
        }
    }
}
