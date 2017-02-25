/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 * Modified by Brian_Entei
 *
 *
 *
 *
 *
 *
 *
 *
 */

package com.gmail.br45entei.util.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/** Writes text to a character-output stream, buffering characters so as to
 * provide for the efficient writing of single characters, arrays, and strings.
 *
 * <p>
 * The buffer size may be specified, or the default size may be accepted.
 * The default is large enough for most purposes.
 *
 * <p>
 * A newLine() method is provided, which uses the platform's own notion of
 * line separator as defined by the system property <tt>line.separator</tt>.
 * Not all platforms use the newline character ('\n') to terminate lines.
 * Calling this method to terminate each output line is therefore preferred to
 * writing a newline character directly. */

public class UnlockedBufferedWriter extends UnlockedWriter {
	
	private UnlockedOutputStreamWriter	out;
	
	private char						cb[];
	private final int					nChars;
	
	private int							nextChar;
	
	private static int					defaultCharBufferSize	= 8192;
	
	/** Line separator string. This is the value of the line.separator
	 * property at the moment that the stream was created. */
	private final String				lineSeparator;
	
	/** @return The underlying OutputStream for this Writer */
	@Override
	public final OutputStream getOutputStream() {
		return this.out.getOutputStream();
	}
	
	/** Creates a buffered character-output stream that uses a default-sized
	 * output buffer.
	 *
	 * @param out A Writer */
	public UnlockedBufferedWriter(UnlockedOutputStreamWriter out) {
		this(out, defaultCharBufferSize);
	}
	
	/** Creates a new buffered character-output stream that uses an output
	 * buffer of the given size.
	 *
	 * @param out A Writer
	 * @param sz Output-buffer size, a positive integer
	 *
	 * @exception IllegalArgumentException If {@code sz <= 0} */
	public UnlockedBufferedWriter(UnlockedOutputStreamWriter out, int sz) {
		super(out);
		if(sz <= 0) throw new IllegalArgumentException("Buffer size <= 0");
		this.out = out;
		this.cb = new char[sz];
		this.nChars = sz;
		this.nextChar = 0;
		
		this.lineSeparator = System.getProperty("line.separator");//java.security.AccessController.doPrivileged(new sun.security.action.GetPropertyAction("line.separator"));
	}
	
	/** Checks to make sure that the stream has not been closed */
	private void ensureOpen() throws IOException {
		if(this.out == null) throw new IOException("Stream closed");
	}
	
	/** Flushes the output buffer to the underlying character stream, without
	 * flushing the stream itself. This method is non-private only so that it
	 * may be invoked by PrintStream. */
	private void flushBuffer1() throws IOException {
		synchronized(this.lock) {
			ensureOpen();
			if(this.nextChar == 0) return;
			this.out.write(this.cb, 0, this.nextChar);
			this.nextChar = 0;
		}
	}
	
	/** Writes a single character.
	 *
	 * @exception IOException If an I/O error occurs */
	@Override
	public void write(int c) throws IOException {
		synchronized(this.lock) {
			ensureOpen();
			if(this.nextChar >= this.nChars) flushBuffer1();
			this.cb[this.nextChar++] = (char) c;
		}
	}
	
	/** Our own little min method, to avoid loading java.lang.Math if we've run
	 * out of file descriptors and we're trying to print a stack trace. */
	private static final int min(int a, int b) {
		if(a < b) return a;
		return b;
	}
	
	/** Writes a portion of an array of characters.
	 *
	 * <p>
	 * Ordinarily this method stores characters from the given array into
	 * this stream's buffer, flushing the buffer to the underlying stream as
	 * needed. If the requested length is at least as large as the buffer,
	 * however, then this method will flush the buffer and write the characters
	 * directly to the underlying stream. Thus redundant
	 * <code>BufferedWriter</code>s will not copy data unnecessarily.
	 *
	 * @param cbuf A character array
	 * @param off Offset from which to start reading characters
	 * @param len Number of characters to write
	 *
	 * @exception IOException If an I/O error occurs */
	@Override
	public void write(char cbuf[], int off, int len) throws IOException {
		synchronized(this.lock) {
			ensureOpen();
			if((off < 0) || (off > cbuf.length) || (len < 0) || ((off + len) > cbuf.length) || ((off + len) < 0)) {
				throw new IndexOutOfBoundsException();
			} else if(len == 0) {
				return;
			}
			
			if(len >= this.nChars) {
				/* If the request length exceeds the size of the output buffer,
				   flush the buffer and then write the data directly.  In this
				   way buffered streams will cascade harmlessly. */
				flushBuffer1();
				this.out.write(cbuf, off, len);
				return;
			}
			
			int b = off, t = off + len;
			while(b < t) {
				int d = min(this.nChars - this.nextChar, t - b);
				System.arraycopy(cbuf, b, this.cb, this.nextChar, d);
				b += d;
				this.nextChar += d;
				if(this.nextChar >= this.nChars) flushBuffer1();
			}
		}
	}
	
	/** Writes a portion of a String.
	 *
	 * <p>
	 * If the value of the <tt>len</tt> parameter is negative then no
	 * characters are written. This is contrary to the specification of this
	 * method in the {@linkplain java.io.Writer#write(java.lang.String,int,int)
	 * superclass}, which requires that an {@link IndexOutOfBoundsException} be
	 * thrown.
	 *
	 * @param s String to be written
	 * @param off Offset from which to start reading characters
	 * @param len Number of characters to be written
	 *
	 * @exception IOException If an I/O error occurs */
	@Override
	public void write(String s, int off, int len) throws IOException {
		synchronized(this.lock) {
			ensureOpen();
			
			int b = off, t = off + len;
			while(b < t) {
				int d = min(this.nChars - this.nextChar, t - b);
				s.getChars(b, b + d, this.cb, this.nextChar);
				b += d;
				this.nextChar += d;
				if(this.nextChar >= this.nChars) flushBuffer1();
			}
		}
	}
	
	/** Writes a line separator. The line separator string is defined by the
	 * system property <tt>line.separator</tt>, and is not necessarily a single
	 * newline ('\n') character.
	 *
	 * @exception IOException If an I/O error occurs */
	public void newLine() throws IOException {
		write(this.lineSeparator);
	}
	
	/** Flushes the stream.
	 *
	 * @exception IOException If an I/O error occurs */
	@Override
	public void flush() throws IOException {
		synchronized(this.lock) {
			flushBuffer1();
			this.out.flush();
		}
	}
	
	@Override
	public void close() throws IOException {
		synchronized(this.lock) {
			if(this.out == null) {
				return;
			}
			try(Writer w = this.out) {
				flushBuffer1();
			} finally {
				this.out = null;
				this.cb = null;
			}
		}
	}
	
}
