/*
 * Copyright 2001-2005 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.gmail.br45entei.util.writer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;

import sun.nio.cs.HistoricallyNamedCharset;

@SuppressWarnings("javadoc")
public class StreamEncoder extends Writer {
	
	private static final int		DEFAULT_BYTE_BUFFER_SIZE	= 8192;
	
	private volatile boolean		isOpen						= true;
	protected volatile OutputStream	secondaryOut				= null;
	
	public final void setSecondaryOut(OutputStream out) {
		this.secondaryOut = out;
	}
	
	private void ensureOpen() throws IOException {
		if(!this.isOpen) throw new IOException("Stream closed");
	}
	
	// Factories for java.io.OutputStreamWriter
	public static StreamEncoder forOutputStreamWriter(OutputStream out, Object lock, String charsetName) throws UnsupportedEncodingException {
		String csn = charsetName;
		if(csn == null) csn = Charset.defaultCharset().name();
		try {
			if(Charset.isSupported(csn)) return new StreamEncoder(out, lock, Charset.forName(csn));
		} catch(IllegalCharsetNameException x) {
		}
		throw new UnsupportedEncodingException(csn);
	}
	
	public static StreamEncoder forOutputStreamWriter(OutputStream out, Object lock, Charset cs) {
		return new StreamEncoder(out, lock, cs);
	}
	
	public static StreamEncoder forOutputStreamWriter(OutputStream out, Object lock, CharsetEncoder enc) {
		return new StreamEncoder(out, lock, enc);
	}
	
	// Factory for java.nio.channels.Channels.newWriter
	
	public static StreamEncoder forEncoder(WritableByteChannel ch, CharsetEncoder enc, int minBufferCap) {
		return new StreamEncoder(ch, enc, minBufferCap);
	}
	
	// -- Public methods corresponding to those in OutputStreamWriter --
	
	// All synchronization and state/argument checking is done in these public
	// methods; the concrete stream-encoder subclasses defined below need not
	// do any such checking.
	
	public String getEncoding() {
		if(isOpen()) return encodingName();
		return null;
	}
	
	public void flushBuffer() throws IOException {
		synchronized(this.lock) {
			if(isOpen()) implFlushBuffer();
			else
				throw new IOException("Stream closed");
		}
	}
	
	@Override
	public void write(int c) throws IOException {
		char cbuf[] = new char[1];
		cbuf[0] = (char) c;
		write(cbuf, 0, 1);
	}
	
	@Override
	public void write(char cbuf[], int off, int len) throws IOException {
		synchronized(this.lock) {
			ensureOpen();
			if((off < 0) || (off > cbuf.length) || (len < 0) || ((off + len) > cbuf.length) || ((off + len) < 0)) {
				throw new IndexOutOfBoundsException();
			} else if(len == 0) {
				return;
			}
			implWrite(cbuf, off, len);
		}
	}
	
	@Override
	public void write(String str, int off, int len) throws IOException {
		/* Check the len before creating a char buffer */
		if(len < 0) throw new IndexOutOfBoundsException();
		char cbuf[] = new char[len];
		str.getChars(off, off + len, cbuf, 0);
		write(cbuf, 0, len);
	}
	
	@Override
	public void flush() throws IOException {
		synchronized(this.lock) {
			ensureOpen();
			implFlush();
		}
	}
	
	@Override
	public void close() throws IOException {
		synchronized(this.lock) {
			if(!this.isOpen) return;
			implClose();
			this.isOpen = false;
		}
	}
	
	private boolean isOpen() {
		return this.isOpen;
	}
	
	// -- Charset-based stream encoder impl --
	
	private final Charset			cs;
	private final CharsetEncoder	encoder;
	private ByteBuffer				bb;
	
	// Exactly one of these is non-null
	private final OutputStream		out;
	private WritableByteChannel		ch;
	
	// Leftover first char in a surrogate pair
	private boolean					haveLeftoverChar	= false;
	private char					leftoverChar;
	private CharBuffer				lcb					= null;
	
	private StreamEncoder(OutputStream out, Object lock, Charset cs) {
		this(out, lock, cs.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE));
	}
	
	@SuppressWarnings("unused")
	private StreamEncoder(OutputStream out, Object lock, CharsetEncoder enc) {
		super(lock);
		this.out = out;
		this.ch = null;
		this.cs = enc.charset();
		this.encoder = enc;
		
		// This path disabled until direct buffers are faster
		if(false && out instanceof FileOutputStream) {
			this.ch = ((FileOutputStream) out).getChannel();
			if(this.ch != null) this.bb = ByteBuffer.allocateDirect(DEFAULT_BYTE_BUFFER_SIZE);
		}
		if(this.ch == null) {
			this.bb = ByteBuffer.allocate(DEFAULT_BYTE_BUFFER_SIZE);
		}
	}
	
	private StreamEncoder(WritableByteChannel ch, CharsetEncoder enc, int mbc) {
		this.out = null;
		this.ch = ch;
		this.cs = enc.charset();
		this.encoder = enc;
		this.bb = ByteBuffer.allocate(mbc < 0 ? DEFAULT_BYTE_BUFFER_SIZE : mbc);
	}
	
	private void writeBytes() throws IOException {
		this.bb.flip();
		int lim = this.bb.limit();
		int pos = this.bb.position();
		assert (pos <= lim);
		int rem = (pos <= lim ? lim - pos : 0);
		
		if(rem > 0) {
			if(this.ch != null) {
				if(this.ch.write(this.bb) != rem) assert false : rem;
			} else {
				this.out.write(this.bb.array(), this.bb.arrayOffset() + pos, rem);
			}
			if(this.secondaryOut != null) {
				try {
					this.secondaryOut.write(this.bb.array(), this.bb.arrayOffset() + pos, rem);
				} catch(Throwable ignored) {
				}
			}
		}
		this.bb.clear();
	}
	
	private void flushLeftoverChar(CharBuffer cb, boolean endOfInput) throws IOException {
		if(!this.haveLeftoverChar && !endOfInput) return;
		if(this.lcb == null) this.lcb = CharBuffer.allocate(2);
		else
			this.lcb.clear();
		if(this.haveLeftoverChar) this.lcb.put(this.leftoverChar);
		if((cb != null) && cb.hasRemaining()) this.lcb.put(cb.get());
		this.lcb.flip();
		while(this.lcb.hasRemaining() || endOfInput) {
			CoderResult cr = this.encoder.encode(this.lcb, this.bb, endOfInput);
			if(cr.isUnderflow()) {
				if(this.lcb.hasRemaining()) {
					this.leftoverChar = this.lcb.get();
					if(cb != null && cb.hasRemaining()) flushLeftoverChar(cb, endOfInput);
					return;
				}
				break;
			}
			if(cr.isOverflow()) {
				assert this.bb.position() > 0;
				writeBytes();
				continue;
			}
			cr.throwException();
		}
		this.haveLeftoverChar = false;
	}
	
	void implWrite(char cbuf[], int off, int len) throws IOException {
		CharBuffer cb = CharBuffer.wrap(cbuf, off, len);
		
		if(this.haveLeftoverChar) flushLeftoverChar(cb, false);
		
		while(cb.hasRemaining()) {
			CoderResult cr = this.encoder.encode(cb, this.bb, false);
			if(cr.isUnderflow()) {
				assert (cb.remaining() <= 1) : cb.remaining();
				if(cb.remaining() == 1) {
					this.haveLeftoverChar = true;
					this.leftoverChar = cb.get();
				}
				break;
			}
			if(cr.isOverflow()) {
				assert this.bb.position() > 0;
				writeBytes();
				continue;
			}
			cr.throwException();
		}
	}
	
	void implFlushBuffer() throws IOException {
		if(this.bb.position() > 0) writeBytes();
	}
	
	void implFlush() throws IOException {
		implFlushBuffer();
		if(this.out != null) this.out.flush();
		if(this.secondaryOut != null) {
			try {
				this.secondaryOut.flush();
			} catch(Throwable ignored) {
			}
		}
	}
	
	void implClose() throws IOException {
		flushLeftoverChar(null, true);
		try {
			for(;;) {
				CoderResult cr = this.encoder.flush(this.bb);
				if(cr.isUnderflow()) break;
				if(cr.isOverflow()) {
					assert this.bb.position() > 0;
					writeBytes();
					continue;
				}
				cr.throwException();
			}
			
			if(this.bb.position() > 0) writeBytes();
			if(this.ch != null) this.ch.close();
			else
				this.out.close();
		} catch(IOException x) {
			this.encoder.reset();
			throw x;
		}
	}
	
	String encodingName() {
		return((this.cs instanceof HistoricallyNamedCharset) ? ((HistoricallyNamedCharset) this.cs).historicalName() : this.cs.name());
	}
	
}