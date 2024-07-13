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

package libatrac9.util;

import vavi.util.ByteUtil;


public class BitReader {

    private byte[] buffer;

    private int lengthBits;

    private int position;

    public byte[] getBuffer() {
        return buffer;
    }

    public int getLengthBits() {
        return lengthBits;
    }

    public void setLengthBits(int lengthBits) {
        this.lengthBits = lengthBits;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    private int remaining() {
        return lengthBits - position;
    }

    public BitReader(byte[] buffer) {
        setBuffer(buffer);
    }

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
        lengthBits = this.buffer != null ? this.buffer.length * 8 : 0;
        position = 0;
    }

    public int readInt(int bitCount) {
        int value = peekInt(bitCount);
        position += bitCount;
        return value;
    }

    public int readSignedInt(int bitCount) {
        int value = peekInt(bitCount);
        position += bitCount;
        return Bit.signExtend32(value, bitCount);
    }

    public boolean readBool() {
        return readInt(1) == 1;
    }

    public int readOffsetBinary(int bitCount, OffsetBias bias) {
        int offset = (1 << (bitCount - 1)) - bias.ordinal();
        int value = peekInt(bitCount) - offset;
        position += bitCount;
        return value;
    }

    public void alignPosition(int multiple) {
        position = Helpers.getNextMultiple(position, multiple);
    }

    public int peekInt(int bitCount) {
        assert bitCount >= 0 && bitCount <= 32;

        if (bitCount > remaining()) {
            if (position >= lengthBits) return 0;

            int extraBits = bitCount - remaining();
            return peekIntFallback(remaining()) << extraBits;
        }

        int byteIndex = position / 8;
        int bitIndex = position % 8;

        if (bitCount <= 9 && remaining() >= 16) {
//            int value = buffer[byteIndex] << 8 | buffer[byteIndex + 1];
            int value = ByteUtil.readBeShort(buffer, byteIndex) & 0xffff;
            value &= 0xFFFF >> bitIndex;
            value >>>= 16 - bitCount - bitIndex;
            return value;
        }

        if (bitCount <= 17 && remaining() >= 24) {
//            int value = buffer[byteIndex] << 16 | buffer[byteIndex + 1] << 8 | buffer[byteIndex + 2];
            int value = ByteUtil.readBe24(buffer, byteIndex);
            value &= 0xFFFF_FF >> bitIndex;
            value >>>= 24 - bitCount - bitIndex;
            return value;
        }

        if (bitCount <= 25 && remaining() >= 32) {
//            int value = buffer[byteIndex] << 24 | buffer[byteIndex + 1] << 16 | buffer[byteIndex + 2] << 8 | buffer[byteIndex + 3];
            int value = ByteUtil.readBeInt(buffer, byteIndex);
            value &= 0xFFFF_FFFF >> bitIndex;
            value >>>= 32 - bitCount - bitIndex;
            return value;
        }
        return peekIntFallback(bitCount);
    }

    private int peekIntFallback(int bitCount) {
        int value = 0;
        int byteIndex = position / 8;
        int bitIndex = position % 8;

        while (bitCount > 0) {
            if (bitIndex >= 8) {
                bitIndex = 0;
                byteIndex++;
            }

            int bitsToRead = Math.min(bitCount, 8 - bitIndex);
            int mask = 0xFF >> bitIndex;
            int currentByte = (mask & buffer[byteIndex]) >> (8 - bitIndex - bitsToRead);

            value = (value << bitsToRead) | currentByte;
            bitIndex += bitsToRead;
            bitCount -= bitsToRead;
        }
        return value;
    }

    /**
     * Specifies the bias of an offset binary value. A positive bias can represent one more
     * positive value than negative value, and a negative bias can represent one more
     * negative value than positive value.
     * <remarks>Example:
     * A 4-bit offset binary value with a positive bias can store
     * the values 8 through -7 inclusive.
     * A 4-bit offset binary value with a positive bias can store
     * the values 7 through -8 inclusive.</remarks>
     */
    public enum OffsetBias {
        Negative
    }
}
