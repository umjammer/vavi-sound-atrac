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


public class ChannelUnit {

    public int bandsCoded;
    public int numComponents;
    public final float[] prevFrame = new float[Atrac3Decoder.SAMPLES_PER_FRAME];
    public int gcBlkSwitch;
    public final TonalComponent[] components = new TonalComponent[64];
    public final GainBlock[] gainBlock = new GainBlock[2];

    public final float[] spectrum = new float[Atrac3Decoder.SAMPLES_PER_FRAME];
    public final float[] imdctBuf = new float[Atrac3Decoder.SAMPLES_PER_FRAME];

    // qmf delay buffers
    public final float[] delayBuf1 = new float[46];
    public final float[] delayBuf2 = new float[46];
    public final float[] delayBuf3 = new float[46];

    public ChannelUnit() {
        for (int i = 0; i < components.length; i++) {
            components[i] = new TonalComponent();
        }
        for (int i = 0; i < gainBlock.length; i++) {
            gainBlock[i] = new GainBlock();
        }
    }
}
