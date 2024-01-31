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

import libatrac9.Block.BlockType;


class Stereo {

    public static void applyIntensityStereo(Block block) {
        if (block.getBlockType() != BlockType.Stereo) return;

        int totalUnits = block.getQuantizationUnitCount();
        int stereoUnits = block.getStereoQuantizationUnit();
        if (stereoUnits >= totalUnits) return;

        Channel source = block.PrimaryChannel();
        Channel dest = block.SecondaryChannel();

        for (int i = stereoUnits; i < totalUnits; i++) {
            int sign = block.getJointStereoSigns()[i];
            for (int sb = Tables.QuantUnitToCoeffIndex[i]; sb < Tables.QuantUnitToCoeffIndex[i + 1]; sb++) {
                if (sign > 0) {
                    dest.getSpectra()[sb] = -source.getSpectra()[sb];
                } else {
                    dest.getSpectra()[sb] = source.getSpectra()[sb];
                }
            }
        }
    }
}

