/*
 * This file is part of jpcsp.
 *
 * Jpcsp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Jpcsp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */

package jpcsp.media.codec.atrac3;

import jpcsp.media.codec.atrac3plus.Atrac;
import jpcsp.media.codec.util.BitReader;
import jpcsp.media.codec.util.FFT;


public class Context {

    public BitReader br;
    public int codingMode;
    public final ChannelUnit[] units = new ChannelUnit[2];
    public int channels;
    public int outputChannels;
    public int blockAlign;
    // joint-stereo related variables
    final int[] matrixCoeffIndexPrev = new int[4];
    final int[] matrixCoeffIndexNow = new int[4];
    final int[] matrixCoeffIndexNext = new int[4];
    final int[] weightingDelay = new int[6];
    // data buffers
    public final float[] tempBuf = new float[1070];

    public Atrac gaincCtx;
    public FFT mdctCtx;

    public final float[][] samples = new float[2][Atrac3Decoder.SAMPLES_PER_FRAME];
}
