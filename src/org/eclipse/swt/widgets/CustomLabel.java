package org.eclipse.swt.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.internal.win32.OS;

/** @author <a href="http://stackoverflow.com/questions/23402993/swt-how-to-make-rounded-border-label/23404151#23404151">Baz</a> from <a href="https://stackoverflow.com/users/1449199/baz">https://stackoverflow.com/</a>
 * @author Brian_Entei */
public final class CustomLabel extends Canvas {
	
	private volatile String text = "";
	
	/** This label's border margin size */
	public volatile int borderMargin = 3;
	
	/** @param parent This label's parent */
	public CustomLabel(Composite parent) {
		super(parent, SWT.NONE);
		this.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				Point rect = CustomLabel.this.getSize();
				e.gc.fillRectangle(0, 0, rect.x, rect.x);
				e.gc.drawRoundRectangle(CustomLabel.this.borderMargin, CustomLabel.this.borderMargin, rect.x - 2 * CustomLabel.this.borderMargin - 1, rect.y - 2 * CustomLabel.this.borderMargin - 1, CustomLabel.this.borderMargin, CustomLabel.this.borderMargin);
				e.gc.drawText(CustomLabel.this.getText(), 2 * CustomLabel.this.borderMargin, 2 * CustomLabel.this.borderMargin);
			}
		});
	}
	
	/** Sets the receiver's text.
	 * <p>
	 * This method sets the widget label. The label may include
	 * the mnemonic character and line delimiters.
	 * </p>
	 * <p>
	 * Mnemonics are indicated by an '&amp;' that causes the next
	 * character to be the mnemonic. When the user presses a
	 * key sequence that matches the mnemonic, focus is assigned
	 * to the control that follows the label. On most platforms,
	 * the mnemonic appears underlined but may be emphasised in a
	 * platform specific manner. The mnemonic indicator character
	 * '&amp;' can be escaped by doubling it in the string, causing
	 * a single '&amp;' to be displayed.
	 * </p>
	 * 
	 * @param string the new text
	 *
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the text is null</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
	 *                </ul>
	*/
	public final void setText(String text) {
		this.text = text;
	}
	
	public final String getText() {
		return this.text;
	}
	
	private int superWidgetExtStyle() {
		int bits = 0;
		if(!OS.IsPPC) {
			if((this.style & SWT.BORDER) != 0) {
				bits |= OS.WS_EX_CLIENTEDGE;
			}
		}
		//(if, when running this to test, the label has strange style flag issues, try commenting out the below
		/**/if((this.style & SWT.BORDER) != 0) {
			if((this.style & SWT.FLAT) == 0) {
				bits |= OS.WS_EX_CLIENTEDGE;
			}
		} /**/
		/*
		* Feature in Windows NT.  When CreateWindowEx() is called with
		* WS_EX_LAYOUTRTL or WS_EX_NOINHERITLAYOUT, CreateWindowEx()
		* fails to create the HWND. The fix is to not use these bits.
		*/
		if(OS.WIN32_VERSION < OS.VERSION(4, 10)) {
			return bits;
		}
		bits |= OS.WS_EX_NOINHERITLAYOUT;
		if((this.style & SWT.RIGHT_TO_LEFT) != 0) {
			bits |= OS.WS_EX_LAYOUTRTL;
		}
		if((this.style & SWT.FLIP_TEXT_DIRECTION) != 0) {
			bits |= OS.WS_EX_RTLREADING;
		}
		return bits;
	}
	
	@Override
	int widgetExtStyle() {
		int bits = this.superWidgetExtStyle() & ~OS.WS_EX_CLIENTEDGE;
		if((this.style & SWT.BORDER) != 0) {
			return bits | OS.WS_EX_STATICEDGE;
		}
		return bits;
	}
	
	private int superWidgetStyle() {
		/* Force clipping of siblings by setting WS_CLIPSIBLINGS */
		int bits = OS.WS_CHILD | OS.WS_VISIBLE | OS.WS_CLIPSIBLINGS;
		//(if, when running this to test, the label has strange style flag issues, try commenting out the below then adding border to the widgetStyle() method below at the bottom)
		/**/if((this.style & SWT.BORDER) != 0) {
			if((this.style & SWT.FLAT) != 0) {
				bits |= OS.WS_BORDER;
			}
		} /**/
		if(OS.IsPPC) {
			if((this.style & SWT.BORDER) != 0) {
				bits |= OS.WS_BORDER;
			}
		}
		return bits;
	}
	
	@Override
	int widgetStyle() {
		int bits = this.superWidgetStyle() | OS.SS_NOTIFY;
		if((this.style & SWT.SEPARATOR) != 0) return bits | OS.SS_OWNERDRAW;
		if(OS.WIN32_VERSION >= OS.VERSION(5, 0)) {
			if((this.style & SWT.WRAP) != 0) bits |= OS.SS_EDITCONTROL;
		}
		if((this.style & SWT.CENTER) != 0) return bits | OS.SS_CENTER;
		if((this.style & SWT.RIGHT) != 0) return bits | OS.SS_RIGHT;
		if((this.style & SWT.WRAP) != 0) return bits | OS.SS_LEFT;
		return bits | OS.SS_LEFTNOWORDWRAP;
	}
	
	@Override
	protected final void checkSubclass() {
	}
	
}
