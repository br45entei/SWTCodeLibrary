package com.gmail.br45entei.util.exception;

import java.io.IOException;

/** Signals that a pending connection or an existing connection timed out,
 * or a pending read timed out, resulting in this message being thrown.
 * The connection may still be active, however.
 * 
 * @author Brian_Entei
 * @see java.io.IOException */
public class ConnectionTimeoutException extends IOException {
	static final long serialVersionUID = -6972466925100596727L;
	
	/** Constructs an {@code ConnectionTimeoutException} with {@code null}
	 * as its error detail message. */
	public ConnectionTimeoutException() {
		super();
	}
	
	/** Constructs an {@code ConnectionTimeoutException} with the specified
	 * detail message.
	 *
	 * @param message
	 *            The detail message (which is saved for later retrieval
	 *            by the {@link #getMessage()} method) */
	public ConnectionTimeoutException(String message) {
		super(message);
	}
	
	/** Constructs an {@code ConnectionTimeoutException} with the specified
	 * cause and a detail message of {@code (cause==null ? null : cause.toString())}
	 * (which typically contains the class and detail message of {@code cause}).
	 * This constructor is useful for IO exceptions that are little more
	 * than wrappers for other throwables.
	 *
	 * @param cause
	 *            The cause (which is saved for later retrieval by the
	 *            {@link #getCause()} method). (A null value is permitted,
	 *            and indicates that the cause is nonexistent or unknown.) */
	public ConnectionTimeoutException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub (42!)
	}
	
	/** Constructs an {@code ConnectionTimeoutException} with the specified
	 * detail message and cause.
	 *
	 * <p>
	 * Note that the detail message associated with {@code cause} is
	 * <i>not</i> automatically incorporated into this exception's detail
	 * message.
	 *
	 * @param message
	 *            The detail message (which is saved for later retrieval
	 *            by the {@link #getMessage()} method)
	 *
	 * @param cause
	 *            The cause (which is saved for later retrieval by the
	 *            {@link #getCause()} method). (A null value is permitted,
	 *            and indicates that the cause is nonexistent or unknown.) */
	public ConnectionTimeoutException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub (42!)
	}
	
}
