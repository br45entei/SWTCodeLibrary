/*
 * Copyright (c) 1996, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Formatter;
import java.util.Locale;
import java.util.Objects;

/** @author Brian_Entei */
public class DualPrintWriter extends UnlockedWriter {
	
	/** The underlying character-output stream of this
	 * <code>PrintWriter</code>.
	 *
	 * @since 1.2 */
	protected volatile UnlockedWriter out;
	protected final OutputStream underlyingOut;
	
	/** @return The underlying OutputStream for this Writer */
	@Override
	public final OutputStream getOutputStream() {
		return this.underlyingOut;
	}
	
	@Override
	public final void setSecondaryOut(OutputStream out) {
		this.out.setSecondaryOut(out);
	}
	
	private final boolean autoFlush;
	private volatile boolean trouble = false;
	private Formatter formatter;
	private PrintStream psOut = null;
	
	/** @return The object used to synchronize operations on this stream. For
	 *         efficiency, a character-stream object may use an object other than
	 *         itself to protect critical sections. A subclass should therefore use
	 *         the object in this field rather than <tt>this</tt> or a synchronized
	 *         method. */
	public final Object getSynchronizationLock() {
		return this.lock;
	}
	
	/** Line separator string. This is the value of the line.separator
	 * property at the moment that the stream was created. */
	private volatile String lineSeparator;
	
	/** @return This DualPrintWriter's current line separator string */
	public final String getLineSeparator() {
		return this.lineSeparator;
	}
	
	/** @param lineSeparator The new line separator to use
	 * @return This DualPrintWriter */
	public final DualPrintWriter setLineSeparator(String lineSeparator) {
		if(lineSeparator == null || lineSeparator.isEmpty()) {
			throw new IllegalArgumentException("lineSeperator mustn't be null or empty!");
		}
		this.lineSeparator = lineSeparator;
		return this;
	}
	
	/** Returns a charset object for the given charset name.
	 * 
	 * @throws NullPointerException if the charset name is null
	 * @throws UnsupportedEncodingException if the charset is not supported */
	private static Charset toCharset(String csn) throws UnsupportedEncodingException {
		Objects.requireNonNull(csn, "charsetName");
		try {
			return Charset.forName(csn);
		} catch(IllegalCharsetNameException | UnsupportedCharsetException unused) {
			// UnsupportedEncodingException should be thrown
			throw new UnsupportedEncodingException(csn);
		}
	}
	
	/** Creates a new DualPrintWriter, without automatic line flushing.
	 *
	 * @param out A character-output stream */
	public DualPrintWriter(UnlockedWriter out) {
		this(out, false);
	}
	
	/** Creates a new DualPrintWriter.
	 *
	 * @param out A character-output stream
	 * @param autoFlush A boolean; if true, the <tt>println</tt>,
	 *            <tt>printf</tt>, or <tt>format</tt> methods will
	 *            flush the output buffer */
	public DualPrintWriter(UnlockedWriter out, boolean autoFlush) {
		super(out);
		this.out = out;
		this.underlyingOut = out.getOutputStream();
		this.autoFlush = autoFlush;
		this.lineSeparator = System.getProperty("line.separator");//java.security.AccessController.doPrivileged(new sun.security.action.GetPropertyAction("line.separator"));
	}
	
	/** Creates a new DualPrintWriter, without automatic line flushing, from an
	 * existing OutputStream. This convenience constructor creates the
	 * necessary intermediate OutputStreamWriter, which will convert characters
	 * into bytes using the default character encoding.
	 *
	 * @param out An output stream
	 *
	 * @see java.io.OutputStreamWriter#OutputStreamWriter(java.io.OutputStream) */
	public DualPrintWriter(OutputStream out) {
		this(out, false);
	}
	
	/** Creates a new DualPrintWriter from an existing OutputStream. This
	 * convenience constructor creates the necessary intermediate
	 * OutputStreamWriter, which will convert characters into bytes using the
	 * default character encoding.
	 *
	 * @param out An output stream
	 * @param autoFlush A boolean; if true, the <tt>println</tt>,
	 *            <tt>printf</tt>, or <tt>format</tt> methods will
	 *            flush the output buffer
	 *
	 * @see java.io.OutputStreamWriter#OutputStreamWriter(java.io.OutputStream) */
	@SuppressWarnings("resource")
	public DualPrintWriter(OutputStream out, boolean autoFlush) {
		this(new UnlockedBufferedWriter(new UnlockedOutputStreamWriter(out)), autoFlush);
		
		// save print stream for error propagation
		if(out instanceof java.io.PrintStream) {
			this.psOut = (PrintStream) out;
		}
	}
	
	/** Creates a new DualPrintWriter, without automatic line flushing, with the
	 * specified file name. This convenience constructor creates the necessary
	 * intermediate {@link java.io.OutputStreamWriter OutputStreamWriter},
	 * which will encode characters using the {@linkplain
	 * java.nio.charset.Charset#defaultCharset() default charset} for this
	 * instance of the Java virtual machine.
	 *
	 * @param fileName
	 *            The name of the file to use as the destination of this writer.
	 *            If the file exists then it will be truncated to zero size;
	 *            otherwise, a new file will be created. The output will be
	 *            written to the file and is buffered.
	 *
	 * @throws FileNotFoundException
	 *             If the given string does not denote an existing, writable
	 *             regular file and a new regular file of that name cannot be
	 *             created, or if some other error occurs while opening or
	 *             creating the file
	 *
	 * @throws SecurityException
	 *             If a security manager is present and {@link
	 * 			SecurityManager#checkWrite checkWrite(fileName)} denies write
	 *             access to the file
	 *
	 * @since 1.5 */
	@SuppressWarnings("resource")
	public DualPrintWriter(String fileName) throws FileNotFoundException {
		this(new UnlockedBufferedWriter(new UnlockedOutputStreamWriter(new FileOutputStream(fileName))), false);
	}
	
	/* Private constructor */
	@SuppressWarnings("resource")
	private DualPrintWriter(Charset charset, File file) throws FileNotFoundException {
		this(new UnlockedBufferedWriter(new UnlockedOutputStreamWriter(new FileOutputStream(file), charset)), false);
	}
	
	/** Creates a new DualPrintWriter, without automatic line flushing, with the
	 * specified file name and charset. This convenience constructor creates
	 * the necessary intermediate {@link java.io.OutputStreamWriter
	 * OutputStreamWriter}, which will encode characters using the provided
	 * charset.
	 *
	 * @param fileName
	 *            The name of the file to use as the destination of this writer.
	 *            If the file exists then it will be truncated to zero size;
	 *            otherwise, a new file will be created. The output will be
	 *            written to the file and is buffered.
	 *
	 * @param csn
	 *            The name of a supported {@linkplain java.nio.charset.Charset
	 *            charset}
	 *
	 * @throws FileNotFoundException
	 *             If the given string does not denote an existing, writable
	 *             regular file and a new regular file of that name cannot be
	 *             created, or if some other error occurs while opening or
	 *             creating the file
	 *
	 * @throws SecurityException
	 *             If a security manager is present and {@link
	 * 			SecurityManager#checkWrite checkWrite(fileName)} denies write
	 *             access to the file
	 *
	 * @throws UnsupportedEncodingException
	 *             If the named charset is not supported
	 *
	 * @since 1.5 */
	public DualPrintWriter(String fileName, String csn) throws FileNotFoundException, UnsupportedEncodingException {
		this(toCharset(csn), new File(fileName));
	}
	
	/** Creates a new DualPrintWriter, without automatic line flushing, with the
	 * specified file. This convenience constructor creates the necessary
	 * intermediate {@link java.io.OutputStreamWriter OutputStreamWriter},
	 * which will encode characters using the {@linkplain
	 * java.nio.charset.Charset#defaultCharset() default charset} for this
	 * instance of the Java virtual machine.
	 *
	 * @param file
	 *            The file to use as the destination of this writer. If the file
	 *            exists then it will be truncated to zero size; otherwise, a
	 *            new
	 *            file will be created. The output will be written to the file
	 *            and is buffered.
	 *
	 * @throws FileNotFoundException
	 *             If the given file object does not denote an existing,
	 *             writable
	 *             regular file and a new regular file of that name cannot be
	 *             created, or if some other error occurs while opening or
	 *             creating the file
	 *
	 * @throws SecurityException
	 *             If a security manager is present and {@link
	 * 			SecurityManager#checkWrite checkWrite(file.getPath())}
	 *             denies write access to the file
	 *
	 * @since 1.5 */
	@SuppressWarnings("resource")
	public DualPrintWriter(File file) throws FileNotFoundException {
		this(new UnlockedBufferedWriter(new UnlockedOutputStreamWriter(new FileOutputStream(file))), false);
	}
	
	/** Creates a new DualPrintWriter, without automatic line flushing, with the
	 * specified file and charset. This convenience constructor creates the
	 * necessary intermediate {@link java.io.OutputStreamWriter
	 * OutputStreamWriter}, which will encode characters using the provided
	 * charset.
	 *
	 * @param file
	 *            The file to use as the destination of this writer. If the file
	 *            exists then it will be truncated to zero size; otherwise, a
	 *            new
	 *            file will be created. The output will be written to the file
	 *            and is buffered.
	 *
	 * @param csn
	 *            The name of a supported {@linkplain java.nio.charset.Charset
	 *            charset}
	 *
	 * @throws FileNotFoundException
	 *             If the given file object does not denote an existing,
	 *             writable
	 *             regular file and a new regular file of that name cannot be
	 *             created, or if some other error occurs while opening or
	 *             creating the file
	 *
	 * @throws SecurityException
	 *             If a security manager is present and {@link
	 * 			SecurityManager#checkWrite checkWrite(file.getPath())}
	 *             denies write access to the file
	 *
	 * @throws UnsupportedEncodingException
	 *             If the named charset is not supported
	 *
	 * @since 1.5 */
	public DualPrintWriter(File file, String csn) throws FileNotFoundException, UnsupportedEncodingException {
		this(toCharset(csn), file);
	}
	
	/** Checks to make sure that the stream has not been closed */
	private void ensureOpen() throws IOException {
		if(this.out == null) throw new IOException("Stream closed");
	}
	
	/** Flushes the stream.
	 *
	 * @exception IOException If an I/O error occurs */
	public final void directFlush() throws IOException {
		this.out.flush();
	}
	
	/** Flushes the stream.
	 * 
	 * @see #checkError() */
	@Override
	public void flush() {
		try {
			synchronized(this.lock) {
				ensureOpen();
				this.out.flush();
			}
		} catch(IOException x) {
			this.trouble = true;
		}
	}
	
	/** Closes the stream and releases any system resources associated
	 * with it. Closing a previously closed stream has no effect.
	 *
	 * @see #checkError() */
	@Override
	public void close() {
		try {
			synchronized(this.lock) {
				if(this.out == null) return;
				this.out.close();
				this.out = null;
			}
		} catch(IOException x) {
			this.trouble = true;
		}
	}
	
	/** @return This stream's error state <b><u>WITHOUT ANY FLUSHING OMFG</u></b> */
	public final boolean getTrouble() {
		return this.trouble;
	}
	
	/** Flushes the stream if it's not closed and checks its error state.
	 *
	 * @return <code>true</code> if the print stream has encountered an error,
	 *         either on the underlying output stream or during a format
	 *         conversion. */
	public boolean checkError() {
		if(this.out != null) {
			flush();
		}
		if(this.out instanceof DualPrintWriter) {
			DualPrintWriter pw = (DualPrintWriter) this.out;
			return pw.checkError();
		} else if(this.psOut != null) {
			return this.psOut.checkError();
		}
		return this.trouble;
	}
	
	/** Indicates that an error has occurred.
	 *
	 * <p>
	 * This method will cause subsequent invocations of {@link
	 * #checkError()} to return <tt>true</tt> until {@link
	 * #clearError()} is invoked. */
	protected void setError() {
		this.trouble = true;
	}
	
	/** Clears the error state of this stream.
	 *
	 * <p>
	 * This method will cause subsequent invocations of {@link
	 * #checkError()} to return <tt>false</tt> until another write
	 * operation fails and invokes {@link #setError()}.
	 *
	 * @since 1.6 */
	protected void clearError() {
		this.trouble = false;
	}
	
	/*
	 * Exception-catching, synchronized output operations,
	 * which also implement the write() methods of Writer
	 */
	
	/** Writes a single character.
	 * 
	 * @param c int specifying a character to be written. */
	@Override
	public void write(int c) {
		try {
			synchronized(this.lock) {
				ensureOpen();
				this.out.write(c);
			}
		} catch(InterruptedIOException x) {
			Thread.currentThread().interrupt();
		} catch(IOException x) {
			this.trouble = true;
		}
	}
	
	/** Writes A Portion of an array of characters.
	 * 
	 * @param buf Array of characters
	 * @param off Offset from which to start writing characters
	 * @param len Number of characters to write */
	@Override
	public void write(char buf[], int off, int len) {
		try {
			synchronized(this.lock) {
				ensureOpen();
				this.out.write(buf, off, len);
			}
		} catch(InterruptedIOException x) {
			Thread.currentThread().interrupt();
		} catch(IOException x) {
			this.trouble = true;
		}
	}
	
	/** Writes an array of characters. This method cannot be inherited from the
	 * Writer class because it must suppress I/O exceptions.
	 * 
	 * @param buf Array of characters to be written */
	@Override
	public void write(char buf[]) {
		write(buf, 0, buf.length);
	}
	
	/** Writes a portion of a string.
	 * 
	 * @param s A String
	 * @param off Offset from which to start writing characters
	 * @param len Number of characters to write */
	@Override
	public void write(String s, int off, int len) {
		try {
			synchronized(this.lock) {
				ensureOpen();
				this.out.write(s, off, len);
			}
		} catch(InterruptedIOException x) {
			Thread.currentThread().interrupt();
		} catch(IOException x) {
			this.trouble = true;
		}
	}
	
	/** Writes a string. This method cannot be inherited from the Writer class
	 * because it must suppress I/O exceptions.
	 * 
	 * @param s String to be written */
	@Override
	public void write(String s) {
		write(s, 0, s.length());
	}
	
	private void newLine() {
		try {
			synchronized(this.lock) {
				ensureOpen();
				this.out.write(this.lineSeparator);
				if(this.autoFlush) this.out.flush();
			}
		} catch(InterruptedIOException x) {
			Thread.currentThread().interrupt();
		} catch(IOException x) {
			this.trouble = true;
		}
	}
	
	/* Methods that do not terminate lines */
	
	/** Prints a boolean value. The string produced by <code>{@link
	 * java.lang.String#valueOf(boolean)}</code> is translated into bytes
	 * according to the platform's default character encoding, and these bytes
	 * are written in exactly the manner of the <code>{@link
	 * #write(int)}</code> method.
	 *
	 * @param b The <code>boolean</code> to be printed */
	public void print(boolean b) {
		write(b ? "true" : "false");
	}
	
	/** Prints a character. The character is translated into one or more bytes
	 * according to the platform's default character encoding, and these bytes
	 * are written in exactly the manner of the <code>{@link
	 * #write(int)}</code> method.
	 *
	 * @param c The <code>char</code> to be printed */
	public void print(char c) {
		write(c);
	}
	
	/** Prints an integer. The string produced by <code>{@link
	 * java.lang.String#valueOf(int)}</code> is translated into bytes according
	 * to the platform's default character encoding, and these bytes are
	 * written in exactly the manner of the <code>{@link #write(int)}</code>
	 * method.
	 *
	 * @param i The <code>int</code> to be printed
	 * @see java.lang.Integer#toString(int) */
	public void print(int i) {
		write(String.valueOf(i));
	}
	
	/** Prints a long integer. The string produced by <code>{@link
	 * java.lang.String#valueOf(long)}</code> is translated into bytes
	 * according to the platform's default character encoding, and these bytes
	 * are written in exactly the manner of the <code>{@link #write(int)}</code>
	 * method.
	 *
	 * @param l The <code>long</code> to be printed
	 * @see java.lang.Long#toString(long) */
	public void print(long l) {
		write(String.valueOf(l));
	}
	
	/** Prints a floating-point number. The string produced by <code>{@link
	 * java.lang.String#valueOf(float)}</code> is translated into bytes
	 * according to the platform's default character encoding, and these bytes
	 * are written in exactly the manner of the <code>{@link #write(int)}</code>
	 * method.
	 *
	 * @param f The <code>float</code> to be printed
	 * @see java.lang.Float#toString(float) */
	public void print(float f) {
		write(String.valueOf(f));
	}
	
	/** Prints a double-precision floating-point number. The string produced by
	 * <code>{@link java.lang.String#valueOf(double)}</code> is translated into
	 * bytes according to the platform's default character encoding, and these
	 * bytes are written in exactly the manner of the <code>{@link
	 * #write(int)}</code> method.
	 *
	 * @param d The <code>double</code> to be printed
	 * @see java.lang.Double#toString(double) */
	public void print(double d) {
		write(String.valueOf(d));
	}
	
	/** Prints an array of characters. The characters are converted into bytes
	 * according to the platform's default character encoding, and these bytes
	 * are written in exactly the manner of the <code>{@link #write(int)}</code>
	 * method.
	 *
	 * @param s The array of chars to be printed
	 *
	 * @throws NullPointerException If <code>s</code> is <code>null</code> */
	public void print(char s[]) {
		write(s);
	}
	
	/** Prints a string. If the argument is <code>null</code> then the string
	 * <code>"null"</code> is printed. Otherwise, the string's characters are
	 * converted into bytes according to the platform's default character
	 * encoding, and these bytes are written in exactly the manner of the
	 * <code>{@link #write(int)}</code> method.
	 *
	 * @param s The <code>String</code> to be printed */
	public void print(String s) {
		if(s == null) {
			s = "null";
		}
		write(s);
	}
	
	/** Prints an object. The string produced by the <code>{@link
	 * java.lang.String#valueOf(Object)}</code> method is translated into bytes
	 * according to the platform's default character encoding, and these bytes
	 * are written in exactly the manner of the <code>{@link #write(int)}</code>
	 * method.
	 *
	 * @param obj The <code>Object</code> to be printed
	 * @see java.lang.Object#toString() */
	public void print(Object obj) {
		write(String.valueOf(obj));
	}
	
	/* Methods that do terminate lines */
	
	/** Terminates the current line by writing the line separator string. The
	 * line separator string is defined by the system property
	 * <code>line.separator</code>, and is not necessarily a single newline
	 * character (<code>'\n'</code>). */
	public void println() {
		newLine();
	}
	
	/** Prints a boolean value and then terminates the line. This method behaves
	 * as though it invokes <code>{@link #print(boolean)}</code> and then
	 * <code>{@link #println()}</code>.
	 *
	 * @param x the <code>boolean</code> value to be printed */
	public void println(boolean x) {
		synchronized(this.lock) {
			print(x);
			println();
		}
	}
	
	/** Prints a character and then terminates the line. This method behaves as
	 * though it invokes <code>{@link #print(char)}</code> and then <code>{@link
	 * #println()}</code>.
	 *
	 * @param x the <code>char</code> value to be printed */
	public void println(char x) {
		synchronized(this.lock) {
			print(x);
			println();
		}
	}
	
	/** Prints an integer and then terminates the line. This method behaves as
	 * though it invokes <code>{@link #print(int)}</code> and then <code>{@link
	 * #println()}</code>.
	 *
	 * @param x the <code>int</code> value to be printed */
	public void println(int x) {
		synchronized(this.lock) {
			print(x);
			println();
		}
	}
	
	/** Prints a long integer and then terminates the line. This method behaves
	 * as though it invokes <code>{@link #print(long)}</code> and then
	 * <code>{@link #println()}</code>.
	 *
	 * @param x the <code>long</code> value to be printed */
	public void println(long x) {
		synchronized(this.lock) {
			print(x);
			println();
		}
	}
	
	/** Prints a floating-point number and then terminates the line. This method
	 * behaves as though it invokes <code>{@link #print(float)}</code> and then
	 * <code>{@link #println()}</code>.
	 *
	 * @param x the <code>float</code> value to be printed */
	public void println(float x) {
		synchronized(this.lock) {
			print(x);
			println();
		}
	}
	
	/** Prints a double-precision floating-point number and then terminates the
	 * line. This method behaves as though it invokes <code>{@link
	 * #print(double)}</code> and then <code>{@link #println()}</code>.
	 *
	 * @param x the <code>double</code> value to be printed */
	public void println(double x) {
		synchronized(this.lock) {
			print(x);
			println();
		}
	}
	
	/** Prints an array of characters and then terminates the line. This method
	 * behaves as though it invokes <code>{@link #print(char[])}</code> and then
	 * <code>{@link #println()}</code>.
	 *
	 * @param x the array of <code>char</code> values to be printed */
	public void println(char x[]) {
		synchronized(this.lock) {
			print(x);
			println();
		}
	}
	
	/** Prints a String and then terminates the line. This method behaves as
	 * though it invokes <code>{@link #print(String)}</code> and then
	 * <code>{@link #println()}</code>.
	 *
	 * @param x the <code>String</code> value to be printed */
	public void println(String x) {
		synchronized(this.lock) {
			print(x);
			println();
		}
	}
	
	/** Prints an Object and then terminates the line. This method calls
	 * at first String.valueOf(x) to get the printed object's string value,
	 * then behaves as
	 * though it invokes <code>{@link #print(String)}</code> and then
	 * <code>{@link #println()}</code>.
	 *
	 * @param x The <code>Object</code> to be printed. */
	public void println(Object x) {
		String s = String.valueOf(x);
		synchronized(this.lock) {
			print(s);
			println();
		}
	}
	
	/** A convenience method to write a formatted string to this writer using
	 * the specified format string and arguments. If automatic flushing is
	 * enabled, calls to this method will flush the output buffer.
	 *
	 * <p>
	 * An invocation of this method of the form <tt>out.printf(format,
	 * args)</tt> behaves in exactly the same way as the invocation
	 *
	 * <pre>
	 * out.format(format, args)
	 * </pre>
	 *
	 * @param format
	 *            A format string as described in <a
	 *            href="../util/Formatter.html#syntax">Format string syntax</a>.
	 *
	 * @param args
	 *            Arguments referenced by the format specifiers in the format
	 *            string. If there are more arguments than format specifiers,
	 *            the
	 *            extra arguments are ignored. The number of arguments is
	 *            variable and may be zero. The maximum number of arguments is
	 *            limited by the maximum dimension of a Java array as defined by
	 *            <cite>The Java&trade; Virtual Machine Specification</cite>.
	 *            The behaviour on a
	 *            <tt>null</tt> argument depends on the <a
	 *            href="../util/Formatter.html#syntax">conversion</a>.
	 *
	 * @throws java.util.IllegalFormatException
	 *             If a format string contains an illegal syntax, a format
	 *             specifier that is incompatible with the given arguments,
	 *             insufficient arguments given the format string, or other
	 *             illegal conditions. For specification of all possible
	 *             formatting errors, see the <a
	 *             href="../util/Formatter.html#detail">Details</a> section of
	 *             the
	 *             formatter class specification.
	 *
	 * @throws NullPointerException
	 *             If the <tt>format</tt> is <tt>null</tt>
	 *
	 * @return This writer
	 *
	 * @since 1.5 */
	public DualPrintWriter printf(String format, Object... args) {
		return format(format, args);
	}
	
	/** A convenience method to write a formatted string to this writer using
	 * the specified format string and arguments. If automatic flushing is
	 * enabled, calls to this method will flush the output buffer.
	 *
	 * <p>
	 * An invocation of this method of the form <tt>out.printf(l, format,
	 * args)</tt> behaves in exactly the same way as the invocation
	 *
	 * <pre>
	 * out.format(l, format, args)
	 * </pre>
	 *
	 * @param l
	 *            The {@linkplain java.util.Locale locale} to apply during
	 *            formatting. If <tt>l</tt> is <tt>null</tt> then no
	 *            localization
	 *            is applied.
	 *
	 * @param format
	 *            A format string as described in <a
	 *            href="../util/Formatter.html#syntax">Format string syntax</a>.
	 *
	 * @param args
	 *            Arguments referenced by the format specifiers in the format
	 *            string. If there are more arguments than format specifiers,
	 *            the
	 *            extra arguments are ignored. The number of arguments is
	 *            variable and may be zero. The maximum number of arguments is
	 *            limited by the maximum dimension of a Java array as defined by
	 *            <cite>The Java&trade; Virtual Machine Specification</cite>.
	 *            The behaviour on a
	 *            <tt>null</tt> argument depends on the <a
	 *            href="../util/Formatter.html#syntax">conversion</a>.
	 *
	 * @throws java.util.IllegalFormatException
	 *             If a format string contains an illegal syntax, a format
	 *             specifier that is incompatible with the given arguments,
	 *             insufficient arguments given the format string, or other
	 *             illegal conditions. For specification of all possible
	 *             formatting errors, see the <a
	 *             href="../util/Formatter.html#detail">Details</a> section of
	 *             the
	 *             formatter class specification.
	 *
	 * @throws NullPointerException
	 *             If the <tt>format</tt> is <tt>null</tt>
	 *
	 * @return This writer
	 *
	 * @since 1.5 */
	public DualPrintWriter printf(Locale l, String format, Object... args) {
		return format(l, format, args);
	}
	
	/** Writes a formatted string to this writer using the specified format
	 * string and arguments. If automatic flushing is enabled, calls to this
	 * method will flush the output buffer.
	 *
	 * <p>
	 * The locale always used is the one returned by {@link
	 * java.util.Locale#getDefault() Locale.getDefault()}, regardless of any
	 * previous invocations of other formatting methods on this object.
	 *
	 * @param format
	 *            A format string as described in <a
	 *            href="../util/Formatter.html#syntax">Format string syntax</a>.
	 *
	 * @param args
	 *            Arguments referenced by the format specifiers in the format
	 *            string. If there are more arguments than format specifiers,
	 *            the
	 *            extra arguments are ignored. The number of arguments is
	 *            variable and may be zero. The maximum number of arguments is
	 *            limited by the maximum dimension of a Java array as defined by
	 *            <cite>The Java&trade; Virtual Machine Specification</cite>.
	 *            The behaviour on a
	 *            <tt>null</tt> argument depends on the <a
	 *            href="../util/Formatter.html#syntax">conversion</a>.
	 *
	 * @throws java.util.IllegalFormatException
	 *             If a format string contains an illegal syntax, a format
	 *             specifier that is incompatible with the given arguments,
	 *             insufficient arguments given the format string, or other
	 *             illegal conditions. For specification of all possible
	 *             formatting errors, see the <a
	 *             href="../util/Formatter.html#detail">Details</a> section of
	 *             the
	 *             Formatter class specification.
	 *
	 * @throws NullPointerException
	 *             If the <tt>format</tt> is <tt>null</tt>
	 *
	 * @return This writer
	 *
	 * @since 1.5 */
	public DualPrintWriter format(String format, Object... args) {
		try {
			synchronized(this.lock) {
				ensureOpen();
				if((this.formatter == null) || (this.formatter.locale() != Locale.getDefault())) this.formatter = new Formatter(this);
				this.formatter.format(Locale.getDefault(), format, args);
				if(this.autoFlush) this.out.flush();
			}
		} catch(InterruptedIOException x) {
			Thread.currentThread().interrupt();
		} catch(IOException x) {
			this.trouble = true;
		}
		return this;
	}
	
	/** Writes a formatted string to this writer using the specified format
	 * string and arguments. If automatic flushing is enabled, calls to this
	 * method will flush the output buffer.
	 *
	 * @param l
	 *            The {@linkplain java.util.Locale locale} to apply during
	 *            formatting. If <tt>l</tt> is <tt>null</tt> then no
	 *            localization
	 *            is applied.
	 *
	 * @param format
	 *            A format string as described in <a
	 *            href="../util/Formatter.html#syntax">Format string syntax</a>.
	 *
	 * @param args
	 *            Arguments referenced by the format specifiers in the format
	 *            string. If there are more arguments than format specifiers,
	 *            the
	 *            extra arguments are ignored. The number of arguments is
	 *            variable and may be zero. The maximum number of arguments is
	 *            limited by the maximum dimension of a Java array as defined by
	 *            <cite>The Java&trade; Virtual Machine Specification</cite>.
	 *            The behaviour on a
	 *            <tt>null</tt> argument depends on the <a
	 *            href="../util/Formatter.html#syntax">conversion</a>.
	 *
	 * @throws java.util.IllegalFormatException
	 *             If a format string contains an illegal syntax, a format
	 *             specifier that is incompatible with the given arguments,
	 *             insufficient arguments given the format string, or other
	 *             illegal conditions. For specification of all possible
	 *             formatting errors, see the <a
	 *             href="../util/Formatter.html#detail">Details</a> section of
	 *             the
	 *             formatter class specification.
	 *
	 * @throws NullPointerException
	 *             If the <tt>format</tt> is <tt>null</tt>
	 *
	 * @return This writer
	 *
	 * @since 1.5 */
	public DualPrintWriter format(Locale l, String format, Object... args) {
		try {
			synchronized(this.lock) {
				ensureOpen();
				if((this.formatter == null) || (this.formatter.locale() != l)) this.formatter = new Formatter(this, l);
				this.formatter.format(l, format, args);
				if(this.autoFlush) this.out.flush();
			}
		} catch(InterruptedIOException x) {
			Thread.currentThread().interrupt();
		} catch(IOException x) {
			this.trouble = true;
		}
		return this;
	}
	
	/** Appends the specified character sequence to this writer.
	 *
	 * <p>
	 * An invocation of this method of the form <tt>out.append(csq)</tt>
	 * behaves in exactly the same way as the invocation
	 *
	 * <pre>
	 * out.write(csq.toString())
	 * </pre>
	 *
	 * <p>
	 * Depending on the specification of <tt>toString</tt> for the
	 * character sequence <tt>csq</tt>, the entire sequence may not be
	 * appended. For instance, invoking the <tt>toString</tt> method of a
	 * character buffer will return a subsequence whose content depends upon
	 * the buffer's position and limit.
	 *
	 * @param csq
	 *            The character sequence to append. If <tt>csq</tt> is
	 *            <tt>null</tt>, then the four characters <tt>"null"</tt> are
	 *            appended to this writer.
	 *
	 * @return This writer
	 *
	 * @since 1.5 */
	@Override
	public DualPrintWriter append(CharSequence csq) {
		if(csq == null) write("null");
		else
			write(csq.toString());
		return this;
	}
	
	/** Appends a subsequence of the specified character sequence to this
	 * writer.
	 *
	 * <p>
	 * An invocation of this method of the form <tt>out.append(csq, start,
	 * end)</tt> when <tt>csq</tt> is not <tt>null</tt>, behaves in
	 * exactly the same way as the invocation
	 *
	 * <pre>
	 * out.write(csq.subSequence(start, end).toString())
	 * </pre>
	 *
	 * @param csq
	 *            The character sequence from which a subsequence will be
	 *            appended. If <tt>csq</tt> is <tt>null</tt>, then characters
	 *            will be appended as if <tt>csq</tt> contained the four
	 *            characters <tt>"null"</tt>.
	 *
	 * @param start
	 *            The index of the first character in the subsequence
	 *
	 * @param end
	 *            The index of the character following the last character in the
	 *            subsequence
	 *
	 * @return This writer
	 *
	 * @throws IndexOutOfBoundsException
	 *             If <tt>start</tt> or <tt>end</tt> are negative,
	 *             <tt>start</tt>
	 *             is greater than <tt>end</tt>, or <tt>end</tt> is greater than
	 *             <tt>csq.length()</tt>
	 *
	 * @since 1.5 */
	@Override
	public DualPrintWriter append(CharSequence csq, int start, int end) {
		CharSequence cs = (csq == null ? "null" : csq);
		write(cs.subSequence(start, end).toString());
		return this;
	}
	
	/** Appends the specified character to this writer.
	 *
	 * <p>
	 * An invocation of this method of the form <tt>out.append(c)</tt>
	 * behaves in exactly the same way as the invocation
	 *
	 * <pre>
	 * out.write(c)
	 * </pre>
	 *
	 * @param c
	 *            The 16-bit character to append
	 *
	 * @return This writer
	 *
	 * @since 1.5 */
	@Override
	public DualPrintWriter append(char c) {
		write(c);
		return this;
	}
	
}
