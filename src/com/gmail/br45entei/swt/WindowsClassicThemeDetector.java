package com.gmail.br45entei.swt;

import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;

@SuppressWarnings("javadoc")
public class WindowsClassicThemeDetector {
	
	protected static final boolean isWindowsLAF() {
		LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
		if(lookAndFeel == null) {
			return false;
		}
		return lookAndFeel.getID().equals("Windows");
	}
	
	protected static final boolean isWindowsClassicLAF() {
		boolean xpStyleThemeActive = false;
		Boolean check = ((Boolean) Toolkit.getDefaultToolkit().getDesktopProperty("win.xpstyle.themeActive"));
		xpStyleThemeActive = (check != null ? check.booleanValue() : xpStyleThemeActive);
		return isWindowsLAF() && !xpStyleThemeActive;
	}
	
	public static final boolean isThemeWindowsAero() {
		if(!isWindowsLAF()) {
			return false;
		}
		return !isWindowsClassicLAF();
	}
	
	public static final boolean isThemeWindowsClassic() {
		if(!isWindowsLAF()) {
			return false;
		}
		return isWindowsClassicLAF();
	}
	
	static {
		// Apply the system look and feel (which will be Windows)
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Throwable ignored) {
		}
	}
	
	public static void main(String... args) throws Exception {
		// Check the initial theme state on startup
		LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
		if(lookAndFeel != null) {
			System.out.println("LookAndFeel is: " + lookAndFeel.getClass().getSimpleName());
		}
		System.out.println("Windows classic is initially: " + isWindowsClassicLAF());
		
		// Register a listener in case the theme changes during runtime.
		Toolkit.getDefaultToolkit().addPropertyChangeListener("win.xpstyle.themeActive", new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				System.out.println("Windows classic is now: " + isWindowsClassicLAF());
			}
		});
		
		// Wait until user presses ENTER in the console and then exit.
		System.in.read();
	}
	
}
