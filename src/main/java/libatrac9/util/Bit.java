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


public class Bit {

    private static int bitReverse32(int value) {
        value = ((value & 0xaaaaaaaa) >> 1) | ((value & 0x55555555) << 1);
        value = ((value & 0xcccccccc) >> 2) | ((value & 0x33333333) << 2);
        value = ((value & 0xf0f0f0f0) >> 4) | ((value & 0x0f0f0f0f) << 4);
        value = ((value & 0xff00ff00) >> 8) | ((value & 0x00ff00ff) << 8);
        return (value >> 16) | (value << 16);
    }

    public static int bitReverse32(int value, int bitCount) {
        return bitReverse32(value) >> (32 - bitCount);
    }

    public static int signExtend32(int value, int bits) {
        int shift = 8 * Integer.BYTES - bits;
        return (value << shift) >> shift;
    }
}
