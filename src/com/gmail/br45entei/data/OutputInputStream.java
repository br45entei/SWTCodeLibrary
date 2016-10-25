package com.gmail.br45entei.data;

import java.io.IOException;
import java.io.OutputStream;

/** @author Brian_Entei */
public class OutputInputStream extends OutputStream {
	private final DisposableByteArrayOutputStream data;
	
	/** A new OutputInputStream */
	public OutputInputStream() {
		this.data = new DisposableByteArrayOutputStream();
	}
	
	/** @return A new ByteArrayInputStream using the data from this output
	 *         stream */
	public final DisposableByteArrayInputStream asInputStream() {
		return this.data.asInputStream();
	}
	
	/** @return The next available byte from the internal buffer, or -1 if it is
	 *         empty or the end of the buffer was reached. */
	public final int read() {
		return this.data.read();
	}
	
	/** Writes the specified byte to this output stream. The general
	 * contract for <code>write</code> is that one byte is written
	 * to the output stream. The byte to be written is the eight
	 * low-order bits of the argument <code>b</code>. The 24
	 * high-order bits of <code>b</code> are ignored.
	 * <p>
	 * Subclasses of <code>OutputStream</code> must provide an
	 * implementation for this method.
	 *
	 * @param b the <code>byte</code>.
	 * @exception IOException if an I/O error occurs. In particular,
	 *                an <code>IOException</code> may be thrown if the
	 *                output stream has been closed. */
	@Override
	public final void write(int b) throws IOException {
		this.data.write(b);
	}
	
	/** Writes <code>b.length</code> bytes from the specified byte array
	 * to this output stream. The general contract for <code>write(b)</code>
	 * is that it should have exactly the same effect as the call
	 * <code>write(b, 0, b.length)</code>.
	 *
	 * @param b the data.
	 * @exception IOException if an I/O error occurs.
	 * @see java.io.OutputStream#write(byte[], int, int) */
	@Override
	public void write(byte b[]) throws IOException {
		write(b, 0, b.length);
	}
	
	/** Writes <code>len</code> bytes from the specified byte array
	 * starting at offset <code>off</code> to this output stream.
	 * The general contract for <code>write(b, off, len)</code> is that
	 * some of the bytes in the array <code>b</code> are written to the
	 * output stream in order; element <code>b[off]</code> is the first
	 * byte written and <code>b[off+len-1]</code> is the last byte written
	 * by this operation.
	 * <p>
	 * The <code>write</code> method of <code>OutputStream</code> calls
	 * the write method of one argument on each of the bytes to be
	 * written out. Subclasses are encouraged to override this method and
	 * provide a more efficient implementation.
	 * <p>
	 * If <code>b</code> is <code>null</code>, a
	 * <code>NullPointerException</code> is thrown.
	 * <p>
	 * If <code>off</code> is negative, or <code>len</code> is negative, or
	 * <code>off+len</code> is greater than the length of the array
	 * <code>b</code>, then an <tt>IndexOutOfBoundsException</tt> is thrown.
	 *
	 * @param b the data.
	 * @param off the start offset in the data.
	 * @param len the number of bytes to write.
	 * @exception IOException if an I/O error occurs. In particular,
	 *                an <code>IOException</code> is thrown if the output
	 *                stream is closed. */
	@Override
	public void write(byte b[], int off, int len) throws IOException {
		this.data.write(b, off, len);
	}
	
}
