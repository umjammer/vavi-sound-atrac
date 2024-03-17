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


class BitAllocation {

    public static void createGradient(Block block) {
        int valueCount = block.getGradientEndValue() - block.getGradientStartValue();
        int unitCount = block.getGradientEndUnit() - block.getGradientStartUnit();

        for (int i = 0; i < block.getGradientEndUnit(); i++) {
            block.getGradient()[i] = block.getGradientStartValue();
        }

        for (int i = block.getGradientEndUnit(); i <= block.getQuantizationUnitCount(); i++) {
            block.getGradient()[i] = block.getGradientEndValue();
        }
        if (unitCount <= 0) return;
        if (valueCount == 0) return;

        byte[] curve = Tables.GradientCurves[unitCount - 1];
        if (valueCount <= 0) {
            double scale = (-valueCount - 1) / 31.0;
            int baseVal = block.getGradientStartValue() - 1;
            for (int i = block.getGradientStartUnit(); i < block.getGradientEndUnit(); i++) {
                block.getGradient()[i] = baseVal - (int) (curve[i - block.getGradientStartUnit()] * scale);
            }
        } else {
            double scale = (valueCount - 1) / 31.0;
            int baseVal = block.getGradientStartValue() + 1;
            for (int i = block.getGradientStartUnit(); i < block.getGradientEndUnit(); i++) {
                block.getGradient()[i] = baseVal + (int) (curve[i - block.getGradientStartUnit()] * scale);
            }
        }
    }

    public static void calculateMask(Channel channel) {
        Arrays.fill(channel.getPrecisionMask(), 0, channel.getPrecisionMask().length, 0);
        for (int i = 1; i < channel.getBlock().getQuantizationUnitCount(); i++) {
            int delta = channel.getScaleFactors()[i] - channel.getScaleFactors()[i - 1];
            if (delta > 1) {
                channel.getPrecisionMask()[i] += Math.min(delta - 1, 5);
            } else if (delta < -1) {
                channel.getPrecisionMask()[i - 1] += Math.min(delta * -1 - 1, 5);
            }
        }
    }

    public static void calculatePrecisions(Channel channel) {
        Block block = channel.getBlock();

        if (block.getGradientMode() != 0) {
            for (int i = 0; i < block.getQuantizationUnitCount(); i++) {
                channel.getPrecisions()[i] = channel.getScaleFactors()[i] + channel.getPrecisionMask()[i] - block.getGradient()[i];
                if (channel.getPrecisions()[i] > 0) {
                    switch (block.getGradientMode()) {
                    case 1:
                        channel.getPrecisions()[i] /= 2;
                        break;
                    case 2:
                        channel.getPrecisions()[i] = 3 * channel.getPrecisions()[i] / 8;
                        break;
                    case 3:
                        channel.getPrecisions()[i] /= 4;
                        break;
                    }
                }
            }
        } else {
            for (int i = 0; i < block.getQuantizationUnitCount(); i++) {
                channel.getPrecisions()[i] = channel.getScaleFactors()[i] - block.getGradient()[i];
            }
        }

        for (int i = 0; i < block.getQuantizationUnitCount(); i++) {
            if (channel.getPrecisions()[i] < 1) {
                channel.getPrecisions()[i] = 1;
            }
        }

        for (int i = 0; i < block.getGradientBoundary(); i++) {
            channel.getPrecisions()[i]++;
        }

        for (int i = 0; i < block.getQuantizationUnitCount(); i++) {
            channel.getPrecisionsFine()[i] = 0;
            if (channel.getPrecisions()[i] > 15) {
                channel.getPrecisionsFine()[i] = channel.getPrecisions()[i] - 15;
                channel.getPrecisions()[i] = 15;
            }
        }
    }

    public static byte[][] generateGradientCurves() {
        byte[] main = {
                1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 4, 4, 5, 5, 6, 7, 8, 9, 10, 11, 12, 13, 15,
                16, 18, 19, 20, 21, 22, 23, 24, 25, 26, 26, 27, 27, 28, 28, 28, 29, 29, 29, 29, 30, 30, 30, 30
        };
        var curves = new byte[main.length][];

        for (int length = 1; length <= main.length; length++) {
            curves[length - 1] = new byte[length];
            for (int i = 0; i < length; i++) {
                curves[length - 1][i] = main[i * main.length / length];
            }
        }
        return curves;
    }
}

