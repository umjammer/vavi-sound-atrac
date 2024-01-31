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


class Block {

    private Atrac9Config config;
    private BlockType BlockType;
    private int BlockIndex;
    private Frame Frame;

    private Channel[] Channels;
    private int ChannelCount;

    public Atrac9Config getConfig() {
        return config;
    }

    public BlockType getBlockType() {
        return BlockType;
    }

    public int getBlockIndex() {
        return BlockIndex;
    }

    public Frame getFrame() {
        return Frame;
    }

    public Channel[] getChannels() {
        return Channels;
    }

    public int getChannelCount() {
        return ChannelCount;
    }

    private boolean FirstInSuperframe;
    private boolean ReuseBandParams;

    public boolean isFirstInSuperframe() {
        return FirstInSuperframe;
    }

    public void setFirstInSuperframe(boolean firstInSuperframe) {
        FirstInSuperframe = firstInSuperframe;
    }

    public boolean isReuseBandParams() {
        return ReuseBandParams;
    }

    public void setReuseBandParams(boolean reuseBandParams) {
        ReuseBandParams = reuseBandParams;
    }

    private int BandCount;
    private int StereoBand;
    private int ExtensionBand;
    private int quantizationUnitCount;
    private int stereoQuantizationUnit;
    private int ExtensionUnit;
    private int QuantizationUnitsPrev;

    public int getBandCount() {
        return BandCount;
    }

    public void setBandCount(int bandCount) {
        BandCount = bandCount;
    }

    public int getStereoBand() {
        return StereoBand;
    }

    public void setStereoBand(int stereoBand) {
        StereoBand = stereoBand;
    }

    public int getExtensionBand() {
        return ExtensionBand;
    }

    public void setExtensionBand(int extensionBand) {
        ExtensionBand = extensionBand;
    }

    public int getQuantizationUnitCount() {
        return quantizationUnitCount;
    }

    public void setQuantizationUnitCount(int quantizationUnitCount) {
        this.quantizationUnitCount = quantizationUnitCount;
    }

    public int getStereoQuantizationUnit() {
        return stereoQuantizationUnit;
    }

    public void setStereoQuantizationUnit(int stereoQuantizationUnit) {
        this.stereoQuantizationUnit = stereoQuantizationUnit;
    }

    public int getExtensionUnit() {
        return ExtensionUnit;
    }

    public void setExtensionUnit(int extensionUnit) {
        ExtensionUnit = extensionUnit;
    }

    public int getQuantizationUnitsPrev() {
        return QuantizationUnitsPrev;
    }

    public void setQuantizationUnitsPrev(int quantizationUnitsPrev) {
        QuantizationUnitsPrev = quantizationUnitsPrev;
    }

    private int[] Gradient = new int[31];

    public int getGradientMode() {
        return GradientMode;
    }

    public int[] getGradient() {
        return Gradient;
    }

    private int GradientMode;
    private int GradientStartUnit;
    private int GradientStartValue;
    private int GradientEndUnit;
    private int GradientEndValue;
    private int GradientBoundary;

    public void setGradientMode(int gradientMode) {
        GradientMode = gradientMode;
    }

    public int getGradientStartUnit() {
        return GradientStartUnit;
    }

    public void setGradientStartUnit(int gradientStartUnit) {
        GradientStartUnit = gradientStartUnit;
    }

    public int getGradientStartValue() {
        return GradientStartValue;
    }

    public void setGradientStartValue(int gradientStartValue) {
        GradientStartValue = gradientStartValue;
    }

    public int getGradientEndUnit() {
        return GradientEndUnit;
    }

    public void setGradientEndUnit(int gradientEndUnit) {
        GradientEndUnit = gradientEndUnit;
    }

    public int getGradientEndValue() {
        return GradientEndValue;
    }

    public void setGradientEndValue(int gradientEndValue) {
        GradientEndValue = gradientEndValue;
    }

    public int getGradientBoundary() {
        return GradientBoundary;
    }

    public void setGradientBoundary(int gradientBoundary) {
        GradientBoundary = gradientBoundary;
    }

    private int PrimaryChannelIndex;

    public int getPrimaryChannelIndex() {
        return PrimaryChannelIndex;
    }

    public void setPrimaryChannelIndex(int primaryChannelIndex) {
        PrimaryChannelIndex = primaryChannelIndex;
    }

    private int[] JointStereoSigns = new int[30];

    public int[] getJointStereoSigns() {
        return JointStereoSigns;
    }

    private boolean HasJointStereoSigns;

    public boolean isHasJointStereoSigns() {
        return HasJointStereoSigns;
    }

    public void setHasJointStereoSigns(boolean hasJointStereoSigns) {
        HasJointStereoSigns = hasJointStereoSigns;
    }

    public Channel PrimaryChannel() {
        return Channels[PrimaryChannelIndex == 0 ? 0 : 1];
    }

    public Channel SecondaryChannel() {
        return Channels[PrimaryChannelIndex == 0 ? 1 : 0];
    }

    private boolean BandExtensionEnabled;
    private boolean HasExtensionData;
    private int BexDataLength;
    private int BexMode;

    public boolean isBandExtensionEnabled() {
        return BandExtensionEnabled;
    }

    public void setBandExtensionEnabled(boolean bandExtensionEnabled) {
        BandExtensionEnabled = bandExtensionEnabled;
    }

    public boolean isHasExtensionData() {
        return HasExtensionData;
    }

    public void setHasExtensionData(boolean hasExtensionData) {
        HasExtensionData = hasExtensionData;
    }

    public int getBexDataLength() {
        return BexDataLength;
    }

    public void setBexDataLength(int bexDataLength) {
        BexDataLength = bexDataLength;
    }

    public int getBexMode() {
        return BexMode;
    }

    public void setBexMode(int bexMode) {
        BexMode = bexMode;
    }

    public Block(Frame parentFrame, int blockIndex) {
        Frame = parentFrame;
        BlockIndex = blockIndex;
        config = parentFrame.getConfig();
        BlockType = config.getChannelConfig().getBlockTypes()[blockIndex];
        ChannelCount = BlockTypeToChannelCount(BlockType);
        Channels = new Channel[ChannelCount];
        for (int i = 0; i < ChannelCount; i++) {
            Channels[i] = new Channel(this, i);
        }
    }

    public static int BlockTypeToChannelCount(BlockType blockType) {
        switch (blockType) {
        case Mono:
            return 1;
        case Stereo:
            return 2;
        case LFE:
            return 1;
        default:
            return 0;
        }
    }

    /**
     * An ATRAC9 block (substream) type
     */
    public enum BlockType {
        /**
         * Mono ATRAC9 block
         */
        Mono,
        /**
         * Stereo ATRAC9 block
         */
        Stereo,
        /**
         * Low-frequency effects ATRAC9 block
         */
        LFE
    }
}
