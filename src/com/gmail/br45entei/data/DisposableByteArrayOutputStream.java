package com.gmail.br45entei.data;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** @author Brian_Entei */
public class DisposableByteArrayOutputStream extends ByteArrayOutputStream {
	
	private volatile int	pos			= 0;
	private volatile int	markedPos	= 0;
	
	/** @return A ByteArrayInputStream using this ByteArrayOutputStream's byte[]
	 *         array. */
	public final DisposableByteArrayInputStream asInputStream() {
		return new DisposableByteArrayInputStream(this.buf, 0, this.count);
	}
	
	/** Retrieves this ByteArrayOutputStream's byte[] array, then calls
	 * {@link #dispose()} and returns the retrieved bytes.
	 * 
	 * @return The bytes. */
	public final byte[] getBytesAndDispose() {
		byte[] buf = this.toByteArray();
		this.dispose();
		return buf;
	}
	
	private static final byte[] copyOf(byte[] original, int newLength, int startPos) {
		final byte[] copy = new byte[newLength];
		System.arraycopy(original, startPos, copy, 0, Math.min(original.length, newLength));
		return copy;
	}
	
	/** @return The next available byte from the internal buffer, or -1 if it is
	 *         empty or the end of the buffer was reached. */
	public final int read() {
		if(this.isEmpty() || this.pos >= this.size()) {
			this.pos = this.size();
			return -1;
		}
		return this.buf[this.pos++];
	}
	
	/** @return The rest of the available bytes read from this
	 *         ByteArrayOutputStream's internal buffer. */
	public final byte[] readAvailable() {
		byte[] data = new byte[this.available()];
		if(data.length == 0) {
			return data;
		}
		int read = read(data, 0, data.length);
		final byte[] rtrn = new byte[read];
		System.arraycopy(data, 0, rtrn, 0, read);
		return rtrn;
	}
	
	/** @return The number of bytes remaining that can be read using
	 *         {@link #read()}. */
	public final int available() {
		if(this.isEmpty() || this.pos >= this.size()) {
			return 0;
		}
		return this.size() - this.pos;
	}
	
	/** Marks the current position of the internal counter used by
	 * {@link #read()}. */
	public final void markRead() {
		this.markedPos = this.pos;
	}
	
	/** @return True, as mark is supported for 'simple' byte buffer
	 *         operations */
	@SuppressWarnings("static-method")
	public final boolean markSupported() {
		return true;
	}
	
	/** Resets the current position of the internal counter used by
	 * {@link #read()} to the last marked position, or 0 if it was never
	 * marked. */
	public final void resetRead() {
		if(this.markedPos >= this.size()) {
			this.markedPos = 0;
		}
		this.pos = this.markedPos;
	}
	
	/** Calls {@link #resetRead()} and then calls {@link #reset()}. */
	public final void resetAll() {
		this.resetRead();
		this.reset();
	}
	
	/** Copied code from {@link InputStream#read(byte[])}
	 *
	 * @param b the buffer into which the data is read.
	 * @return the total number of bytes read into the buffer, or
	 *         <code>-1</code> if there is no more data because the end of
	 *         the stream has been reached.
	 * @exception NullPointerException if <code>b</code> is <code>null</code>.
	 * @see java.io.InputStream#read(byte[], int, int) */
	public int read(byte b[]) {
		return read(b, 0, b.length);
	}
	
	/** Copied code from {@link InputStream#read(byte[], int, int)}
	 * 
	 * @param b the buffer into which the data is read.
	 * @param off the start offset in array <code>b</code> at which the data is
	 *            written.
	 * @param len the maximum number of bytes to read.
	 * @return the total number of bytes read into the buffer, or
	 *         <code>-1</code> if there is no more data because the end of
	 *         the stream has been reached.
	 * @exception NullPointerException If <code>b</code> is <code>null</code>.
	 * @exception IndexOutOfBoundsException If <code>off</code> is negative,
	 *                <code>len</code> is negative, or <code>len</code> is
	 *                greater than <code>b.length - off</code>
	 * @see InputStream#read(byte[], int, int) */
	public int read(byte b[], int off, int len) {
		if(b == null) {
			throw new NullPointerException();
		} else if(off < 0 || len < 0 || len > b.length - off) {
			throw new IndexOutOfBoundsException();
		} else if(len == 0) {
			return 0;
		}
		int c = this.read();
		if(c == -1) {
			return -1;
		}
		b[off] = (byte) c;
		int i = 1;
		for(; i < len; i++) {
			c = this.read();
			if(c == -1) {
				break;
			}
			b[off + i] = (byte) c;
		}
		return i;
	}
	
	/** This method resets the marked position used by {@link #markRead()} and
	 * {@link #resetRead()}
	 * 
	 * @return The first byte that was in this ByteArrayOutputStream's internal
	 *         buffer before removing it. */
	public final int readAndRemoveFirstByte() {
		if(this.isEmpty()) {
			return -1;
		}
		final int data = this.buf[0] & 0xFF;//conversion from byte to int using '& 255'.
		this.buf = copyOf(this.buf, this.buf.length - 1, 1);
		this.pos = 0;
		this.markedPos = 0;
		this.count = this.buf.length;
		return data;
	}
	
	/** @return True if this ByteArrayOutputStream's byte[] array's size is
	 *         0. */
	public final boolean isEmpty() {
		return this.buf.length == 0;
	}
	
	/** Wipes this ByteArrayOutputStream's byte[] array and resets the
	 * counter. */
	public final synchronized void dispose() {
		this.buf = new byte[0];
		this.count = 0;
		this.pos = 0;
		this.markedPos = 0;
		System.gc();
	}
	
	/** Calls {@link #dispose()}.
	 * 
	 * @see java.io.ByteArrayOutputStream#close() */
	@Override
	public final void close() {
		this.dispose();
	}
	
}
