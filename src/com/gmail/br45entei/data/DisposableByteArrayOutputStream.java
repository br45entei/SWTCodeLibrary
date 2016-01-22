package com.gmail.br45entei.data;

import java.io.ByteArrayOutputStream;

/** @author Brian_Entei */
public class DisposableByteArrayOutputStream extends ByteArrayOutputStream {
	
	/** Retrieves this ByteArrayOutputStream's byte[] array, then calls
	 * {@link #dispose()} and returns the retrieved bytes.
	 * 
	 * @return The bytes. */
	public final byte[] getBytesAndDispose() {
		byte[] buf = this.buf;
		this.dispose();
		return buf;
	}
	
	/** Wipes this ByteArrayOutputStream's byte[] array and resets the counter. */
	public final synchronized void dispose() {
		this.buf = new byte[0];
		this.count = 0;
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
