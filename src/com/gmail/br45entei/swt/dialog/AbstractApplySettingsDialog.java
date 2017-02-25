package com.gmail.br45entei.swt.dialog;

import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.swt.Response;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/** @author Brian_Entei */
public abstract class AbstractApplySettingsDialog extends AbstractDialog {
	
	protected final Shell parent;
	protected Shell shell;
	protected String title = "";
	
	/** <b>Non-API</b> */
	public volatile boolean updatingSettingsUI = true;
	
	/** Flag letting the {@link AbstractApplySettingsDialog#updateUI()} method
	 * know whether or not this dialog's shell images could change and would
	 * therefore need updating each time {@link #updateUI()} is called. */
	@SuppressWarnings(value = {"javadoc"})
	public volatile boolean shellImagesExpectedToChange = false;
	
	/** @param parent This dialog's parent shell */
	public AbstractApplySettingsDialog(Shell parent) {
		this(parent, SWT.DIALOG_TRIM);
	}
	
	/** @param parent This dialog's parent shell
	 * @param style This dialog's SWT style */
	public AbstractApplySettingsDialog(Shell parent, int style) {
		super(parent, style);
		this.parent = parent;
	}
	
	@Override
	public Shell getShell() {
		return this.shell;
	}
	
	/** @return This dialog's title */
	public final String getTitle() {
		return this.title;
	}
	
	/** @param title This dialog's new title */
	public void setTitle(String title) {
		this.title = title == null ? "" : title;
	}
	
	@Override
	public Response open() {
		this.createContents();
		this.restorePreviousSettings();
		this.shell.open();
		this.shell.layout();
		final Display display = this.parent.getDisplay();//change to super.parent.getDisplay(); if overriding open() and copying from these contents!
		while(!this.shell.isDisposed()) {
			if(!this.mainLoop(display)) {
				break;
			}
		}
		return this.response;
	}
	
	@Override
	protected void createContents(String... args) {
		this.shell = new Shell(this.parent, SWT.DIALOG_TRIM);
		this.shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				e.doit = false;
				AbstractApplySettingsDialog.this.response = Response.CLOSE;
			}
		});
		this.shell.setImages(Functions.getDefaultShellImages());
		this.shell.setSize(800, 600);
		this.shell.setText(this.getText());
		this.shell.setText(this.title + this.parent.getText());
		Functions.centerShell2OnShell1(this.parent, this.shell);
	}
	
	@Override
	protected abstract boolean _runClock(Display display);
	
	@Override
	protected void runClock() {
		if(this.shell.isDisposed()) {
			return;
		}
		//this.exitCheck();
		if(this.shell.isVisible()) {
			if(!this.shell.getDisplay().readAndDispatch()) {
				Functions.sleep(1L);//display.sleep();
			}
			return;
		}
		Functions.sleep(10L);
	}
	
	protected abstract void updateSettingsUI();
	
	@Override
	protected void updateUI() {
		Functions.setTextFor(this.shell, this.title + this.parent.getText());
		//Functions.setShellImages(this.shell, Functions.getDefaultShellImages());
	}
	
	@Override
	protected boolean mainLoop(Display display) {
		if(this._runClock(display)) {
			this.updateUI();
			return this._runClock(display);
		}
		return false;
	}
	
	/** Clears this dialog's UI settings, leaving them blank or set to false
	 * where applicable. */
	public abstract void clearSettings();
	
	/** @return Whether or not the current state of this dialog's UI settings
	 *         allows for a 'restore default settings' button to be pressed */
	public abstract boolean canRestoreDefaultSettings();
	
	/** Changes the current state of this dialog's UI settings back to a
	 * pre-defined default state. */
	public abstract void restoreDefaultSettings();
	
	/** @return Whether or not the current state of this dialog's UI settings
	 *         allows for a 'restore previous settings' or 'undo current
	 *         changes' button to be pressed */
	public abstract boolean canRestorePreviousSettings();
	
	/** Changes the current state of this dialog's UI settings back to the state
	 * that they were in when this dialog was opened. */
	public abstract void restorePreviousSettings();
	
	/** @return Whether or not the current state of this dialog's UI settings
	 *         are different from the way they were before this dialog was
	 *         opened.<br>
	 *         In other words, whether or not clicking apply would actually
	 *         change any settings. */
	public abstract boolean canApplySettings();
	
	/** @return Whether or not applying the current state of this dialog's UI
	 *         settings to whatever implementation holding the settings data was
	 *         successful.<br>
	 *         In other words, whether or not the settings from this dialog
	 *         actually made it to their intended destination and made
	 *         changes. */
	public abstract boolean applySettings();
	
}
