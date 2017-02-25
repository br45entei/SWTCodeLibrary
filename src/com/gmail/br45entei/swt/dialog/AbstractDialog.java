package com.gmail.br45entei.swt.dialog;

import com.gmail.br45entei.swt.Response;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/** @author Brian_Entei */
public abstract class AbstractDialog extends Dialog {
	
	/** The user's response to the dialog, or
	 * {@link Response#NO_RESPONSE} if the dialog was left unanswered,
	 * was closed programmatically, etc. */
	public volatile Response response;
	
	/** @param parent This dialog's parent shell */
	public AbstractDialog(Shell parent) {
		this(parent, SWT.DIALOG_TRIM);
	}
	
	/** @param parent This dialog's parent shell
	 * @param style This dialog's SWT style */
	public AbstractDialog(Shell parent, int style) {
		super(parent, style);
	}
	
	/** @return This dialog's shell */
	public abstract Shell getShell();
	
	/** Open the dialog.
	 * 
	 * @return The user's response to the dialog, or
	 *         {@link Response#NO_RESPONSE} if the dialog was left unanswered,
	 *         was closed programmatically, etc. */
	public abstract Response open();
	
	protected abstract void createContents(String... args);
	
	protected abstract boolean _runClock(Display display);
	
	/** Runs {@link Display#readAndDispatch()},
	 * then attempts to sleep. */
	protected abstract void runClock();
	
	/** Updates this dialog's user interface and various settings, states,
	 * etc. */
	protected abstract void updateUI();
	
	protected abstract boolean mainLoop(Display display);
	
	/** Causes the receiver to have the <em>keyboard focus</em>,
	 * such that all keyboard events will be delivered to it. Focus
	 * reassignment will respect applicable platform constraints.
	 *
	 * @return <code>true</code> if the control got focus, and
	 *         <code>false</code> if it was unable to.
	 *
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
	 *                disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *                thread that created the receiver</li>
	 *                </ul>
	 *
	 * @see Control#forceFocus */
	public final boolean setFocus() {//Also could be named "bring to front"
		boolean success = false;
		if(!this.getShell().isVisible()) {
			this.getShell().setVisible(true);
			this.getShell().open();
		}
		/*if(!this.getShell().getMaximized()) {//only enable if dialog can be resized, but even then, this will maximize it, so probably not really needed anyway
			this.getShell().setMaximized(true);
		}*/
		if(!(success |= this.getShell().setFocus())) {
			success |= this.getShell().forceFocus();
		}
		this.getShell().forceActive();
		return success;
	}
	
	/** Disposes of the operating system resources associated with
	 * the receiver and all its descendants. After this method has
	 * been invoked, the receiver and all descendants will answer
	 * <code>true</code> when sent the message <code>isDisposed()</code>.
	 * Any internal connections between the widgets in the tree will
	 * have been removed to facilitate garbage collection.
	 * This method does nothing if the widget is already disposed.
	 * <p>
	 * NOTE: This method is not called recursively on the descendants
	 * of the receiver. This means that, widget implementers can not
	 * detect when a widget is being disposed of by re-implementing
	 * this method, but should instead listen for the <code>Dispose</code>
	 * event.
	 * </p>
	 *
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *                thread that created the receiver</li>
	 *                </ul>
	 *
	 * @see Shell#addDisposeListener
	 * @see Shell#removeDisposeListener
	 * @see Shell#checkWidget */
	public final void dispose() {
		this.getShell().dispose();
	}
	
	/** Returns <code>true</code> if the widget has been disposed,
	 * and <code>false</code> otherwise.
	 * <p>
	 * This method gets the dispose state for the widget.
	 * When a widget has been disposed, it is an error to
	 * invoke any other method (except {@link #dispose()}) using the widget.
	 * </p>
	 *
	 * @return <code>true</code> when the widget is disposed and
	 *         <code>false</code> otherwise */
	public final boolean isDisposed() {
		return this.getShell().isDisposed();
	}
	
}
