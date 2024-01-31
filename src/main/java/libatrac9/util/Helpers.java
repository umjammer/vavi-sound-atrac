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


public class Helpers {

    public static short clamp16(int value) {
        if (value > Short.MAX_VALUE)
            return Short.MAX_VALUE;
        if (value < Short.MIN_VALUE)
            return Short.MIN_VALUE;
        return (short) value;
    }

    public static int getNextMultiple(int value, int multiple) {
        if (multiple <= 0)
            return value;

        if (value % multiple == 0)
            return value;

        return value + multiple - value % multiple;
    }

    /**
     * Returns the floor of the base 2 logarithm of a specified number.
     * @param value The number whose logarithm is to be found.
     * @return The floor of the base 2 logarithm of {@code value}
     */
    public static int log2(int value) {
        value |= value >> 1;
        value |= value >> 2;
        value |= value >> 4;
        value |= value >> 8;
        value |= value >> 16;

        return MultiplyDeBruijnBitPosition[(value * 0x07C4ACDD) >> 27];
    }

    private static final int[] MultiplyDeBruijnBitPosition = {
            0, 9, 1, 10, 13, 21, 2, 29, 11, 14, 16, 18, 22, 25, 3, 30,
            8, 12, 20, 28, 15, 17, 24, 7, 19, 27, 23, 6, 26, 5, 4, 31
    };
}

