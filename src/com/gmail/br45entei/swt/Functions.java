package com.gmail.br45entei.swt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.UUID;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

/** @author Brian_Entei */
public class Functions {
	
	private static final char[]		ILLEGAL_CHARACTERS				= {'\n', '\r', '\t', '\0', '\f', '`', '?', '*', '<', '>', '|', '\"'};
	private static final String[]	ILLEGAL_CHARACTER_REPLACEMENTS	= {"", "", "", "", "", "&#96;", "", "", "&lt;", "&gt;", "", "&quot;"};
	
	/** @param str The string to convert
	 * @return The string with HTML characters converted into normal characters */
	public static String htmlToText(String str) {
		String rtrn;
		try {
			rtrn = StringEscapeUtils.unescapeHtml4(URLDecoder.decode(str, "UTF-8"));
		} catch(UnsupportedEncodingException e) {
			rtrn = StringEscapeUtils.unescapeHtml4(str);
		}
		int i = 0;
		for(char illegalChar : ILLEGAL_CHARACTERS) {
			rtrn = rtrn.replace(illegalChar + "", ILLEGAL_CHARACTER_REPLACEMENTS[i]);
			i++;
		}
		return rtrn;
	}
	
	/** @param shell The shell to center */
	public static final void centerShellOnPrimaryMonitor(Shell shell) {
		Monitor primary = shell.getDisplay().getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle rect = shell.getBounds();
		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;
		shell.setLocation(x, y);
	}
	
	public static final void centerShell2OnShell1(Shell shell1, Shell shell2) {
		Rectangle bounds = shell1.getBounds();
		Rectangle rect = shell2.getBounds();
		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;
		shell2.setLocation(x, y);
	}
	
	/** @param bytes The amount of bytes
	 * @param si Whether or not the bytes are in SI format
	 * @return The readable string
	 * @author aioobe from <a href=
	 *         "http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java/3758880#3758880"
	 *         >stackoverflow.com</a>
	 * @param decimalPlaces The number of decimal places */
	public static String humanReadableByteCount(long bytes, boolean si, int decimalPlaces) {
		int unit = si ? 1000 : 1024;
		if(bytes < unit) {
			return bytes + " B";
		}
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
		return String.format("%." + decimalPlaces + "f %sB", new Double(bytes / Math.pow(unit, exp)), pre);
	}
	
	/** @param file
	 * @return
	 * @throws IOException */
	public static final OutputStream openOutputStream(File file) throws IOException {
		if(file.exists()) {
			if(file.isDirectory()) {
				throw new IOException("The file \"" + file.getAbsolutePath() + "\" is a directory, not a file.\r\nCannot write to folders.");
			}
			if(file.canWrite() == false) {
				throw new IOException("The file \"" + file.getAbsolutePath() + "\" could not be accessed!");
			}
		} else {
			File parent = file.getParentFile();
			if(parent != null) {
				if(!parent.mkdirs() && !parent.isDirectory()) {
					throw new IOException("Parent folder \"" + parent.getAbsolutePath() + "\" could not be created!");
				}
			}
		}
		return new FileOutputStream(file, false);
	}
	
	/** @param url The URL to get the file size of
	 * @return The file size, or -1 if unsuccessful. */
	public static final long getFileSize(URL url) {
		URLConnection conn = null;
		try {
			conn = url.openConnection();
			if(conn instanceof HttpURLConnection) {
				HttpURLConnection conn1 = (HttpURLConnection) conn;
				conn1.setRequestMethod("GET");
				conn1.getInputStream();
				return conn1.getContentLengthLong();
			}
			conn.getInputStream();
			return conn.getContentLengthLong();
		} catch(IOException e) {
			//e.printStackTrace();
			return -1L;
		} finally {
			if(conn != null) {
				if(conn instanceof HttpURLConnection) {
					((HttpURLConnection) conn).disconnect();
				} else {
					//XXX ?
				}
			}
		}
	}
	
	public static final boolean isStrUUID(String uuid) {
		try {
			UUID.fromString(uuid);
			return true;
		} catch(Throwable ignored) {
			return false;
		}
	}
	
	public static final String getElementsFromStringArrayAtIndexesAsString(String[] array, int startIndex, int endIndex) {
		return getElementsFromStringArrayAtIndexesAsString(array, startIndex, endIndex, ' ');
	}
	
	public static final String getElementsFromStringArrayAtIndexesAsString(String[] array, int startIndex, int endIndex, char seperatorChar) {
		if(array == null || array.length == 0) {
			return null;
		}
		if(startIndex < 0 || startIndex > array.length || endIndex < 0 || endIndex > array.length) {
			return null;
		}
		String rtrn = "";
		if(startIndex > endIndex) {
			for(int i = startIndex; i < endIndex; i--) {
				rtrn += seperatorChar + array[i];
			}
		} else {
			for(int i = startIndex; i < endIndex; i++) {
				rtrn += seperatorChar + array[i];
			}
		}
		if(rtrn.startsWith(seperatorChar + "")) {
			rtrn = rtrn.substring(1);
		}
		return rtrn.trim();
	}
	
	public static final String getElementsFromStringArrayAtIndexAsString(String[] array, int index) {
		return getElementsFromStringArrayAtIndexAsString(array, index, ' ');
	}
	
	public static final String getElementsFromStringArrayAtIndexAsString(String[] array, int index, char seperatorChar) {
		if(array == null || index >= array.length) {
			return "";
		}
		String mkArgs = "";
		for(int i = index; i < array.length; i++) {
			mkArgs += array[i] + seperatorChar;
		}
		return mkArgs.trim();
	}
	
	public static final boolean doesArrayContainAnyNullObjects(Object[] array) {
		for(int i = 0; i < array.length; i++) {
			if(array[i] == null) {
				return true;
			}
		}
		return false;
	}
	
	public static final int getNextFreeIndexInArray(Object[] array) {
		if(array == null || !doesArrayContainAnyNullObjects(array)) {
			return -1;
		}
		for(int i = 0; i < array.length; i++) {
			if(array[i] == null) {
				return i;
			}
		}
		return -1;
	}
	
	public static final boolean isWindowsAeroActive() {
		return false;
	}
	
	public static String toHexString(byte[] array) {
		return DatatypeConverter.printHexBinary(array);
	}
	
	public static byte[] toByteArray(String s) {
		return DatatypeConverter.parseHexBinary(s);
	}
	
	public static final InterruptedException sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch(InterruptedException e) {
			return e;
		}
		return null;
	}
	
	public static final InterruptedException sleep(double millis) {
		long wholeNum = (long) millis;
		int nanos = (int) Math.floor((millis - wholeNum) * 1000000);//(int) Math.floor((millis - wholeNum) / 1000000);
		try {
			Thread.sleep(wholeNum, nanos);
		} catch(InterruptedException e) {
			return e;
		}
		return null;
	}
	
}
