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

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

/** An UnlockedOutputStreamWriter is a bridge from character streams to byte
 * streams:
 * Characters written to it are encoded into bytes using a specified {@link
 * java.nio.charset.Charset charset}. The charset that it uses
 * may be specified by name or may be given explicitly, or the platform's
 * default charset may be accepted.
 *
 * <p>
 * Each invocation of a write() method causes the encoding converter to be
 * invoked on the given character(s). The resulting bytes are accumulated in a
 * buffer before being written to the underlying output stream. The size of
 * this buffer may be specified, but by default it is large enough for most
 * purposes. Note that the characters passed to the write() methods are not
 * buffered.
 *
 * <p>
 * For top efficiency, consider wrapping an UnlockedOutputStreamWriter within a
 * BufferedWriter so as to avoid frequent converter invocations. For example:
 *
 * <pre>
 * Writer out = new UnlockedBufferedWriter(new UnlockedOutputStreamWriter(System.out));
 * </pre>
 *
 * <p>
 * A <i>surrogate pair</i> is a character represented by a sequence of two
 * <tt>char</tt> values: A <i>high</i> surrogate in the range '&#92;uD800' to
 * '&#92;uDBFF' followed by a <i>low</i> surrogate in the range '&#92;uDC00' to
 * '&#92;uDFFF'.
 *
 * <p>
 * A <i>malformed surrogate element</i> is a high surrogate that is not
 * followed by a low surrogate or a low surrogate that is not preceded by a
 * high surrogate.
 *
 * <p>
 * This class always replaces malformed surrogate elements and unmappable
 * character sequences with the charset's default <i>substitution sequence</i>.
 * The {@linkplain java.nio.charset.CharsetEncoder} class should be used when
 * more
 * control over the encoding process is required.
 *
 * @see UnlockedBufferedWriter
 * @see OutputStream
 * @see java.nio.charset.Charset */
public class UnlockedOutputStreamWriter extends UnlockedWriter {
	
	private final OutputStream out;
	
	/** @return The underlying OutputStream for this Writer */
	@Override
	public final OutputStream getOutputStream() {
		return this.out;
	}
	
	/** Creates an UnlockedOutputStreamWriter that uses the named charset.
	 *
	 * @param out
	 *            An OutputStream
	 *
	 * @param charsetName
	 *            The name of a supported
	 *            {@link java.nio.charset.Charset charset}
	 *
	 * @exception UnsupportedEncodingException
	 *                If the named encoding is not supported */
	public UnlockedOutputStreamWriter(OutputStream out, String charsetName) throws UnsupportedEncodingException {
		super(out, charsetName);
		this.out = out;
	}
	
	/** Creates an UnlockedOutputStreamWriter that uses the default character
	 * encoding.
	 *
	 * @param out An OutputStream */
	public UnlockedOutputStreamWriter(OutputStream out) {
		super(out);
		this.out = out;
	}
	
	/** Creates an UnlockedOutputStreamWriter that uses the given charset.
	 *
	 * @param out
	 *            An OutputStream
	 *
	 * @param cs
	 *            A charset
	 *
	 * @since 1.4
	 * @spec JSR-51 */
	public UnlockedOutputStreamWriter(OutputStream out, Charset cs) {
		super(out, cs);
		this.out = out;
	}
	
	/** Creates an UnlockedOutputStreamWriter that uses the given charset
	 * encoder.
	 *
	 * @param out
	 *            An OutputStream
	 *
	 * @param enc
	 *            A charset encoder */
	public UnlockedOutputStreamWriter(OutputStream out, CharsetEncoder enc) {
		super(out, enc);
		this.out = out;
	}
	
}
