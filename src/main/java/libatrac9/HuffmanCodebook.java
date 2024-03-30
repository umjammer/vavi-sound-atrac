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

import libatrac9.util.Helpers;


class HuffmanCodebook {

    public HuffmanCodebook(short[] codes, byte[] bits, byte valueCountPower) {
        this.codes = codes;
        this.bits = bits;
        if (this.codes == null || this.bits == null) return;

        valueCount = 1 << valueCountPower;
        this.valueCountPower = valueCountPower;
        valueBits = Helpers.log2(codes.length) >>> valueCountPower;
//Debug.println("valueBits: " + valueBits);
        valueMax = 1 << valueBits;

        int max = 0;
        for (byte bitSize : bits) {
            max = Math.max(max, bitSize);
        }

        maxBitSize = max;
        lookup = CreateLookupTable();
    }

    private byte[] CreateLookupTable() {
        if (codes == null || bits == null) return null;

        int tableSize = 1 << maxBitSize;
        var dest = new byte[tableSize];

        for (int i = 0; i < bits.length; i++) {
            if (bits[i] == 0) continue;
            int unusedBits = maxBitSize - bits[i];

            int start = codes[i] << unusedBits;
            int length = 1 << unusedBits;
            int end = start + length;

            for (int j = start; j < end; j++) {
                dest[j] = (byte) i;
            }
        }
        return dest;
    }

    public short[] codes;
    public byte[] bits;
    public byte[] lookup;
    public int valueCount;
    public int valueCountPower;
    public int valueBits;
    public int valueMax;
    public int maxBitSize;

    public short[] getCodes() {
        return codes;
    }

    public byte[] getBits() {
        return bits;
    }

    public byte[] getLookup() {
        return lookup;
    }

    public int getValueCount() {
        return valueCount;
    }

    public int getValueCountPower() {
        return valueCountPower;
    }

    public int getValueBits() {
        return valueBits;
    }

    public int getValueMax() {
        return valueMax;
    }

    public int getMaxBitSize() {
        return maxBitSize;
    }
}

