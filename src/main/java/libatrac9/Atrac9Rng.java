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

/**
 * An Xorshift RNG used by the ATRAC9 codec
 */
class Atrac9Rng {

    private short _stateA;
    private short _stateB;
    private short _stateC;
    private short _stateD;

    public Atrac9Rng(short seed) {
        int startValue = 0x4D93 * (seed ^ (seed >> 14));

        _stateA = (short) (3 - startValue);
        _stateB = (short) (2 - startValue);
        _stateC = (short) (1 - startValue);
        _stateD = (short) (0 - startValue);
    }

    public short Next() {
        short t = (short) (_stateD ^ (_stateD << 5));
        _stateD = _stateC;
        _stateC = _stateB;
        _stateB = _stateA;
        _stateA = (short) (t ^ _stateA ^ ((t ^ (_stateA >> 5)) >> 4));
        return _stateA;
    }
}
