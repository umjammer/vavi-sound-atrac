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


/** Parameters of a single sine wave */
public class WaveParam {

    /** wave frequency index */
    int freqIndex;
    /** quantized amplitude scale factor */
    int ampSf;
    /** quantized amplitude index */
    int ampIndex;
    /** quantized phase index */
    int phaseIndex;

    public void clear() {
        freqIndex = 0;
        ampSf = 0;
        ampIndex = 0;
        phaseIndex = 0;
    }
}
