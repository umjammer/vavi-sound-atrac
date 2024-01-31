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
        Codes = codes;
        Bits = bits;
        if (Codes == null || Bits == null) return;

        ValueCount = 1 << valueCountPower;
        ValueCountPower = valueCountPower;
        ValueBits = Helpers.log2(codes.length) >> valueCountPower;
        ValueMax = 1 << ValueBits;

        int max = 0;
        for (byte bitSize : bits) {
            max = Math.max(max, bitSize);
        }

        MaxBitSize = max;
        Lookup = CreateLookupTable();
    }

    private byte[] CreateLookupTable() {
        if (Codes == null || Bits == null) return null;

        int tableSize = 1 << MaxBitSize;
        var dest = new byte[tableSize];

        for (int i = 0; i < Bits.length; i++) {
            if (Bits[i] == 0) continue;
            int unusedBits = MaxBitSize - Bits[i];

            int start = Codes[i] << unusedBits;
            int length = 1 << unusedBits;
            int end = start + length;

            for (int j = start; j < end; j++) {
                dest[j] = (byte) i;
            }
        }
        return dest;
    }

    public short[] Codes;
    public byte[] Bits;
    public byte[] Lookup;
    public int ValueCount;
    public int ValueCountPower;
    public int ValueBits;
    public int ValueMax;
    public int MaxBitSize;

    public short[] getCodes() {
        return Codes;
    }

    public byte[] getBits() {
        return Bits;
    }

    public byte[] getLookup() {
        return Lookup;
    }

    public int getValueCount() {
        return ValueCount;
    }

    public int getValueCountPower() {
        return ValueCountPower;
    }

    public int getValueBits() {
        return ValueBits;
    }

    public int getValueMax() {
        return ValueMax;
    }

    public int getMaxBitSize() {
        return MaxBitSize;
    }
}

