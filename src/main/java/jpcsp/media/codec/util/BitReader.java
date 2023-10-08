/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

/*
 * This file is part of jpcsp.
 *
 * Jpcsp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Jpcsp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */

package jpcsp.media.codec.util;


import java.nio.ByteBuffer;


public class BitReader implements IBitReader {
	private final ByteBuffer mem;
	private int addr;
	private int initialAddr;
	private int initialSize;
	private int size;
	private int bits;
	private int value;
	private int direction;

	public BitReader(ByteBuffer mem, int addr, int size) {
		this.mem = mem;
		this.addr = addr;
		this.size = size;
		initialAddr = addr;
		initialSize = size;
		bits = 0;
		direction = 1;
	}

	@Override
	public boolean readBool() {
		return read1() != 0;
	}

	protected int nextAddr() {
		size--;
		return addr + direction;
	}

	protected int previousAddr() {
		size++;
		return addr - direction;
	}

	@Override
	public int read1() {
		if (bits <= 0) {
			value = mem.get(addr) & 0xff;
			addr = nextAddr();
			bits = 8;
		}
		int bit = value >> 7;
		bits--;
		value = (value << 1) & 0xFF;

		return bit;
	}

	@Override
	public int read(int n) {
		int read;
		if (n <= bits) {
			read = value >> (8 - n);
			bits -= n;
			value = (value << n) & 0xFF;
		} else {
			read = 0;
			for (; n > 0; n--) {
				read = (read << 1) + read1();
			}
		}

		return read;
	}

	public int readByte() {
		if (bits == 8) {
			bits = 0;
			return value;
		}
		if (bits > 0) {
			skip(bits);
		}
		int read = mem.get(addr) & 0xff;
		addr = nextAddr();

		return read;
	}

	public int getBitsLeft() {
		return (size << 3) + bits;
	}

	public int getBytesRead() {
		int bytesRead = addr - initialAddr;
		if (bits == 8) {
			bytesRead--;
		}

		return bytesRead;
	}

	public int getBitsRead() {
		return (addr - initialAddr) * 8 - bits;
	}

	@Override
	public int peek(int n) {
		int read = read(n);
		skip(-n);
		return read;
	}

	@Override
	public void skip(int n) {
		bits -= n;
		if (n >= 0) {
			while (bits < 0) {
				addr = nextAddr();
				bits += 8;
			}
		} else {
			while (bits > 8) {
				addr = previousAddr();
				bits -= 8;
			}
		}

		if (bits > 0) {
			value = mem.get(addr - direction) & 0xff;
			value = (value << (8 - bits)) & 0xFF;
		}
	}

	public void seek(int n) {
		addr = initialAddr + n;
		size = initialSize - n;
		bits = 0;
	}

	public void setDirection(int direction) {
		this.direction = direction;
		bits = 0;
	}

	public void byteAlign() {
		if (bits > 0 && bits < 8) {
			skip(bits);
		}
	}

	@Override
	public int getReadAddr() {
		if (bits == 8) {
			return addr - direction;
		}
		return addr;
	}

	@Override
	public String toString() {
		return String.format("BitReader addr=0x%08X, bits=%d, size=0x%X, bits read %d", addr, bits, size, getBitsRead());
	}
}
