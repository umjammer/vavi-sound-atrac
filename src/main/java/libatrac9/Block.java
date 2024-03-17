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

    private final Atrac9Config config;
    private final BlockType blockType;
    private final int blockIndex;
    private final Frame frame;

    private final Channel[] channels;
    private final int channelCount;

    public Atrac9Config getConfig() {
        return config;
    }

    public BlockType getBlockType() {
        return blockType;
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public Frame getFrame() {
        return frame;
    }

    public Channel[] getChannels() {
        return channels;
    }

    public int getChannelCount() {
        return channelCount;
    }

    private boolean firstInSuperframe;
    private boolean reuseBandParams;

    public boolean isFirstInSuperframe() {
        return firstInSuperframe;
    }

    public void setFirstInSuperframe(boolean firstInSuperframe) {
        this.firstInSuperframe = firstInSuperframe;
    }

    public boolean isReuseBandParams() {
        return reuseBandParams;
    }

    public void setReuseBandParams(boolean reuseBandParams) {
        this.reuseBandParams = reuseBandParams;
    }

    private int bandCount;
    private int stereoBand;
    private int extensionBand;
    private int quantizationUnitCount;
    private int stereoQuantizationUnit;
    private int extensionUnit;
    private int quantizationUnitsPrev;

    public int getBandCount() {
        return bandCount;
    }

    public void setBandCount(int bandCount) {
        this.bandCount = bandCount;
    }

    public int getStereoBand() {
        return stereoBand;
    }

    public void setStereoBand(int stereoBand) {
        this.stereoBand = stereoBand;
    }

    public int getExtensionBand() {
        return extensionBand;
    }

    public void setExtensionBand(int extensionBand) {
        this.extensionBand = extensionBand;
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
        return extensionUnit;
    }

    public void setExtensionUnit(int extensionUnit) {
        this.extensionUnit = extensionUnit;
    }

    public int getQuantizationUnitsPrev() {
        return quantizationUnitsPrev;
    }

    public void setQuantizationUnitsPrev(int quantizationUnitsPrev) {
        this.quantizationUnitsPrev = quantizationUnitsPrev;
    }

    private final int[] gradient = new int[31];

    public int getGradientMode() {
        return gradientMode;
    }

    public int[] getGradient() {
        return gradient;
    }

    private int gradientMode;
    private int gradientStartUnit;
    private int gradientStartValue;
    private int gradientEndUnit;
    private int gradientEndValue;
    private int gradientBoundary;

    public void setGradientMode(int gradientMode) {
        this.gradientMode = gradientMode;
    }

    public int getGradientStartUnit() {
        return gradientStartUnit;
    }

    public void setGradientStartUnit(int gradientStartUnit) {
        this.gradientStartUnit = gradientStartUnit;
    }

    public int getGradientStartValue() {
        return gradientStartValue;
    }

    public void setGradientStartValue(int gradientStartValue) {
        this.gradientStartValue = gradientStartValue;
    }

    public int getGradientEndUnit() {
        return gradientEndUnit;
    }

    public void setGradientEndUnit(int gradientEndUnit) {
        this.gradientEndUnit = gradientEndUnit;
    }

    public int getGradientEndValue() {
        return gradientEndValue;
    }

    public void setGradientEndValue(int gradientEndValue) {
        this.gradientEndValue = gradientEndValue;
    }

    public int getGradientBoundary() {
        return gradientBoundary;
    }

    public void setGradientBoundary(int gradientBoundary) {
        this.gradientBoundary = gradientBoundary;
    }

    private int primaryChannelIndex;

    public int getPrimaryChannelIndex() {
        return primaryChannelIndex;
    }

    public void setPrimaryChannelIndex(int primaryChannelIndex) {
        this.primaryChannelIndex = primaryChannelIndex;
    }

    private final int[] jointStereoSigns = new int[30];

    public int[] getJointStereoSigns() {
        return jointStereoSigns;
    }

    private boolean hasJointStereoSigns;

    public boolean hasJointStereoSigns() {
        return hasJointStereoSigns;
    }

    public void setHasJointStereoSigns(boolean hasJointStereoSigns) {
        this.hasJointStereoSigns = hasJointStereoSigns;
    }

    public Channel getPrimaryChannel() {
        return channels[primaryChannelIndex == 0 ? 0 : 1];
    }

    public Channel getSecondaryChannel() {
        return channels[primaryChannelIndex == 0 ? 1 : 0];
    }

    private boolean bandExtensionEnabled;
    private boolean hasExtensionData;
    private int bexDataLength;
    private int bexMode;

    public boolean isBandExtensionEnabled() {
        return bandExtensionEnabled;
    }

    public void setBandExtensionEnabled(boolean bandExtensionEnabled) {
        this.bandExtensionEnabled = bandExtensionEnabled;
    }

    public boolean hasExtensionData() {
        return hasExtensionData;
    }

    public void setHasExtensionData(boolean hasExtensionData) {
        this.hasExtensionData = hasExtensionData;
    }

    public int getBexDataLength() {
        return bexDataLength;
    }

    public void setBexDataLength(int bexDataLength) {
        this.bexDataLength = bexDataLength;
    }

    public int getBexMode() {
        return bexMode;
    }

    public void setBexMode(int bexMode) {
        this.bexMode = bexMode;
    }

    public Block(Frame parentFrame, int blockIndex) {
        frame = parentFrame;
        this.blockIndex = blockIndex;
        config = parentFrame.getConfig();
        blockType = config.getChannelConfig().getBlockTypes()[blockIndex];
        channelCount = BlockTypeToChannelCount(blockType);
        channels = new Channel[channelCount];
        for (int i = 0; i < channelCount; i++) {
            channels[i] = new Channel(this, i);
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
