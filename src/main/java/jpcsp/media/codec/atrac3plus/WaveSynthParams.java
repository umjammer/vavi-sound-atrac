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

package jpcsp.media.codec.atrac3plus;

import static jpcsp.media.codec.atrac3plus.Atrac3plusDecoder.ATRAC3P_SUBBANDS;


public class WaveSynthParams {

    /** < 1 - tones info present */
    boolean tonesPresent;
    /** < 1 - low range, 0 - high range */
    int amplitudeMode;
    /** < number of PQF bands with tones */
    int numToneBands;
    /** < 1 - subband-wise tone sharing flags */
    final boolean[] toneSharing = new boolean[ATRAC3P_SUBBANDS];
    /** < 1 - subband-wise tone channel swapping */
    final boolean[] toneMaster = new boolean[ATRAC3P_SUBBANDS];
    /** < 1 - subband-wise 180 degrees phase shifting */
    final boolean[] phaseShift = new boolean[ATRAC3P_SUBBANDS];
    /** < total sum of tones in this unit */
    int tonesIndex;
    final WaveParam[] waves = new WaveParam[48];

    public WaveSynthParams() {
        for (int i = 0; i < waves.length; i++) {
            waves[i] = new WaveParam();
        }
    }
}
