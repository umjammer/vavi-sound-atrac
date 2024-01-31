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

import libatrac9.util.Mdct;


class Channel {

    private Atrac9Config config;
    private int channelIndex;

    public boolean IsPrimary() {
        return block.getPrimaryChannelIndex() == channelIndex;
    }

    private Block block;
    private Mdct mdct;

    public Atrac9Config getConfig() {
        return config;
    }

    public int getChannelIndex() {
        return channelIndex;
    }

    public Block getBlock() {
        return block;
    }

    public Mdct getMdct() {
        return mdct;
    }

    private double[] pcm = new double[256];
    private double[] spectra = new double[256];

    public double[] getPcm() {
        return pcm;
    }

    public double[] getSpectra() {
        return spectra;
    }

    private int codedQuantUnits;
    private int scaleFactorCodingMode;

    public int getCodedQuantUnits() {
        return codedQuantUnits;
    }

    public void setCodedQuantUnits(int codedQuantUnits) {
        this.codedQuantUnits = codedQuantUnits;
    }

    public int getScaleFactorCodingMode() {
        return scaleFactorCodingMode;
    }

    public void setScaleFactorCodingMode(int scaleFactorCodingMode) {
        this.scaleFactorCodingMode = scaleFactorCodingMode;
    }

    private int[] scaleFactors = new int[31];
    private int[] scaleFactorsPrev = new int[31];

    private int[] precisions = new int[30];
    private int[] precisionsFine = new int[30];
    private int[] precisionMask = new int[30];

    private int[] spectraValuesBuffer = new int[16];
    private int[] codebookSet = new int[30];

    private int[] quantizedSpectra = new int[256];
    private int[] quantizedSpectraFine = new int[256];

    public int[] getScaleFactors() {
        return scaleFactors;
    }

    public int[] getScaleFactorsPrev() {
        return scaleFactorsPrev;
    }

    public int[] getPrecisions() {
        return precisions;
    }

    public int[] getPrecisionsFine() {
        return precisionsFine;
    }

    public int[] getPrecisionMask() {
        return precisionMask;
    }

    public int[] getSpectraValuesBuffer() {
        return spectraValuesBuffer;
    }

    public int[] getCodebookSet() {
        return codebookSet;
    }

    public int[] getQuantizedSpectra() {
        return quantizedSpectra;
    }

    public int[] getQuantizedSpectraFine() {
        return quantizedSpectraFine;
    }

    public int[] getBexValues() {
        return bexValues;
    }

    public double[] getBexScales() {
        return bexScales;
    }

    private int bexMode;
    private int bexValueCount;
    private int[] bexValues = new int[4];
    private double[] bexScales = new double[6];
    private Atrac9Rng rng;

    public int getBexMode() {
        return bexMode;
    }

    public void setBexMode(int bexMode) {
        this.bexMode = bexMode;
    }

    public int getBexValueCount() {
        return bexValueCount;
    }

    public void setBexValueCount(int bexValueCount) {
        this.bexValueCount = bexValueCount;
    }

    public Atrac9Rng getRng() {
        return rng;
    }

    public void setRng(Atrac9Rng rng) {
        this.rng = rng;
    }

    public Channel(Block parentBlock, int channelIndex) {
        block = parentBlock;
        this.channelIndex = channelIndex;
        config = parentBlock.getConfig();
        mdct = new Mdct(config.getFrameSamplesPower(), Tables.ImdctWindow[config.getFrameSamplesPower() - 6], 1);
    }

    public void UpdateCodedUnits() {
        codedQuantUnits = IsPrimary() ? block.getQuantizationUnitCount() : block.getStereoQuantizationUnit();
    }
}

