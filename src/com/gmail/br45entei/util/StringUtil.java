package com.gmail.br45entei.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang3.StringEscapeUtils;

/** @author Brian_Entei */
public strictfp class StringUtil {
	
	public static final byte[] readFile(File file) {
		if(file == null || !file.isFile()) {
			return null;
		}
		try(FileInputStream in = new FileInputStream(file)) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int read;
			while((read = in.read()) != -1) {
				baos.write(read);
			}
			return baos.toByteArray();
		} catch(IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static final String readLine(InputStream in) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int read;
		while((read = in.read()) != -1) {
			String s = new String(new byte[] {(byte) read});
			if(s.equals("\n")) {
				break;
			}
			baos.write(read);
		}
		if(baos.size() == 0 && read == -1) {
			return null;
		}
		String rtrn = new String(baos.toByteArray(), StandardCharsets.UTF_8);
		return rtrn.endsWith("\r") ? rtrn.substring(0, rtrn.length() - 1) : rtrn;
	}
	
	private static final SecureRandom	secureRandom	= new SecureRandom();
	private static final Random			random			= new Random();
	
	public static final int getRandomIntBetween(int min, int max) {
		return random.nextInt(max - min) + min;
	}
	
	public static final String		cacheValidatorTimePattern	= "EEE, dd MMM yyyy HH:mm:ss 'GMT'";
	public static final Locale		cacheValidatorTimeLocale	= Locale.US;
	public static final TimeZone	cacheValidatorTimeFormat	= TimeZone.getTimeZone("GMT");
	
	public static final String nextSessionId() {
		return new BigInteger(130, secureRandom).toString(32);
	}
	
	public static final void main(String[] args) {
		/*String test = "<html>\r\n"//
				+ "\t<head>\r\n"//
				+ "\t\t<title>Hai ther!</title>\r\n"//
				+ "\t</head>\r\n"//
				+ "\t<body>\r\n"//
				+ "\t\t<string>This is a webpage! Do you like it?</string>\r\n"//
				+ "\t</body>\r\n"//
				+ "</html>";
		
		System.out.println(test.length());
		System.out.println("\r\n===\r\n");
		try {
			System.out.println(compressString(test, "UTF-8").length);
			System.out.println(compressString("1234567890123456789012345678901234", "UTF-8").length);
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		try {
			File out = new File(System.getProperty("user.dir") + File.separatorChar + "temp-" + (new Random()).nextLong() + ".txt");
			PrintWriter pr = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out), "UTF-8"), true);
			pr.println("Hello, world!\r\nHow are you?");
			pr.flush();
			pr.close();
			System.out.println("Resulting charset: \"" + getDetectedEncoding(out) + "\"");
			out.deleteOnExit();
		} catch(Throwable e) {
			e.printStackTrace();
		}
		*/
		//long millis = (4 * MILLENNIUM) + (2 * DAY) + (17 * HOUR) + (56 * MINUTE) + (49 * SECOND) + (0 * MILLISECOND);
		//long millis = (4 * MILLENNIUM) + (2 * DAY) + (0 * HOUR) + (0 * MINUTE) + (4 * SECOND) + (0 * MILLISECOND);
		//long millis = -((4 * MILLENNIUM) + (364 * DAY) + (0 * HOUR) + (0 * MINUTE) + (0 * SECOND) + (0 * MILLISECOND));
		//long millis = -1L + YEAR;
		long millis = 0L;
		System.out.println(StringUtil.getElapsedTime(millis));
	}
	
	public static final int getLengthOfLongestLineInStr(String str) {
		int count = 0;
		if(str == null) {
			return -1;
		}
		if(str.contains("\n")) {
			for(String s : str.split(Pattern.quote("\n"))) {
				final int length = s.length();
				if(length > count) {
					count = length;
				}
			}
		}
		return count + 1;
	}
	
	public static final String getSpecificLineInStr(String str, int line) {
		String[] split = str.split(Pattern.quote("\n"));
		if(split.length > line) {
			return split[line];
		}
		return null;
	}
	
	public static final int getNumOfLinesInStr(String str) {
		int count = 0;
		if(str == null) {
			return -1;
		}
		if(str.contains("\n")) {
			count = str.split("\\n").length;
		}
		return count + 1;
	}
	
	public static final String requestArgumentsToString(HashMap<String, String> requestArguments, String... argumentsToIgnore) {
		String rtrn = "?";
		final HashMap<String, String> reqArgs = new HashMap<>(requestArguments);
		for(Entry<String, String> entry : requestArguments.entrySet()) {
			if(entry.getKey() != null) {
				for(String argToIgnore : argumentsToIgnore) {
					if(entry.getKey().equals(argToIgnore)) {
						reqArgs.remove(entry.getKey());
					}
				}
			}
		}
		boolean addedAnyArguments = false;
		for(Entry<String, String> entry : reqArgs.entrySet()) {
			rtrn += (addedAnyArguments ? "&" : "") + entry.getKey() + "=" + entry.getValue();
			addedAnyArguments = true;
		}
		return rtrn.equals("?") ? "" : rtrn;
	}
	
	/** @param getTimeOnly Whether or not time should be included but not date as
	 *            well
	 * @param fileSystemSafe Whether or not the returned string will be used in
	 *            the making of a folder or file
	 * @param milliseconds Whether or not the milliseconds should be included
	 * @return The resulting string */
	public static String getSystemTime(boolean getTimeOnly, boolean fileSystemSafe, boolean milliseconds) {
		return new SimpleDateFormat(getTimeOnly ? (fileSystemSafe ? "HH.mm.ss" + (milliseconds ? ".SSS" : "") : "HH:mm:ss" + (milliseconds ? ":SSS" : "")) : (fileSystemSafe ? "MM-dd-yyyy_HH.mm.ss" + (milliseconds ? ".SSS" : "") : "MM/dd/yyyy_HH:mm:ss" + (milliseconds ? ":SSS" : ""))).format(new Date());
	}
	
	/** @param stackTraceElements The elements to convert
	 * @return The resulting string */
	public static final String stackTraceElementsToStr(StackTraceElement[] stackTraceElements) {
		String str = "";
		if(stackTraceElements != null) {
			for(StackTraceElement stackTrace : stackTraceElements) {
				str += (!stackTrace.toString().startsWith("Caused By") ? "     at " : "") + stackTrace.toString() + "\r\n";
			}
		}
		return str;
	}
	
	/** @param e The {@link Throwable} to convert
	 * @return The resulting String */
	public static final String throwableToStr(Throwable e) {
		if(e == null) {
			return "null";
		}
		String str = e.getClass().getName() + ": ";
		if((e.getMessage() != null) && !e.getMessage().isEmpty()) {
			str += e.getMessage() + "\r\n";
		} else {
			str += "\r\n";
		}
		str += stackTraceElementsToStr(e.getStackTrace());
		if(e.getCause() != null) {
			str += "Caused by:\r\n" + throwableToStr(e.getCause());
		}
		return str;
	}
	
	/** @param <T> The arrays' class
	 * @param arrays The String arrays to combine
	 * @return The combined arrays as one String[] array, or null if none of the
	 *         arrays contained any instances or if null was given */
	@SuppressWarnings("unchecked")
	public static final <T> T[] combine(T[]... arrays) {
		if(arrays == null) {
			return null;
		}
		Class<T> clazz = null;
		int newLength = 0;
		for(T[] array : arrays) {
			newLength += array != null ? array.length : 0;
			if(clazz == null && array != null) {
				for(T element : array) {
					if(element != null) {
						clazz = (Class<T>) element.getClass();
					}
				}
			}
		}
		if(clazz == null) {
			return null;
		}
		T[] rtrn = (T[]) Array.newInstance(clazz, newLength);//new T[newLength];
		int index = 0;
		for(T[] array : arrays) {
			if(array != null) {
				for(T element : array) {
					rtrn[index++] = element;
				}
			}
		}
		return rtrn;
	}
	
	/** @param array The list to read from
	 * @param c The character to use as a separator
	 * @return The resulting string */
	public static final String stringArrayToString(char c, List<String> array) {
		if(array == null) {
			return "null";
		}
		String rtrn = "";
		for(String element : array) {
			rtrn += element + c;
		}
		return rtrn.trim();
	}
	
	/** @param array The array/list/strings to read from
	 * @param c The character to use as a separator
	 * @return The resulting string */
	public static final String stringArrayToString(char c, String... array) {
		if(array == null) {
			return "null";
		}
		String rtrn = "";
		for(String element : array) {
			rtrn += element + c;
		}
		return rtrn.length() >= 2 ? rtrn.substring(0, rtrn.length() - 1) : rtrn;
	}
	
	/** @param array The String[] array to convert
	 * @param c The separator character to use
	 * @return The resulting string */
	public static final String stringArrayToString(String[] array, char c) {
		return stringArrayToString(array, c, 0);
	}
	
	/** @param array The String[] array to convert
	 * @param c The separator character to use
	 * @param startIndex The index to start at
	 * @return The resulting string */
	public static final String stringArrayToString(String[] array, char c, int startIndex) {
		return stringArrayToString(array, c + "", startIndex);
	}
	
	/** @param array The array/list/strings to read from
	 * @param c The character to use as a separator
	 * @param startIndex The index to start at
	 * @return The resulting string */
	public static final String stringArrayToString(String[] array, String c, int startIndex) {
		if(array == null || startIndex >= array.length) {
			return "null";
		}
		String rtrn = "";
		int i = 0;
		for(String element : array) {
			if(i >= startIndex) {
				rtrn += element + c;
			}
			i++;
		}
		if(rtrn.length() > 1) {
			rtrn = rtrn.substring(0, rtrn.length() - 1);
		}
		return rtrn;
	}
	
	/** @param array The String[] array to convert
	 * @param c The separator character to use
	 * @param startIndex The index to start at
	 * @param endIndex The index to stop short at
	 * @return The resulting string */
	public static final String stringArrayToString(String[] array, char c, int startIndex, int endIndex) {
		return stringArrayToString(array, c + "", startIndex, endIndex);
	}
	
	/** @param array The array/list/strings to read from
	 * @param c The character to use as a separator
	 * @param startIndex The index to start at
	 * @param endIndex The index to stop short at
	 * @return The resulting string. If startIndex is greater than or equal to
	 *         the array's size, endIndex is greater than the array's size,
	 *         startIndex is greater than or equal to endIndex, and/or either
	 *         startIndex or endIndex are negative, "null" is returned. */
	public static final String stringArrayToString(String[] array, String c, int startIndex, int endIndex) {
		if(array == null || startIndex >= array.length || endIndex > array.length || startIndex >= endIndex || startIndex < 0 || endIndex < 0) {
			return "null";
		}
		String rtrn = "";
		int i = 0;
		for(String element : array) {
			if(i >= startIndex && i < endIndex) {
				rtrn += element + c;
			}
			i++;
		}
		if(rtrn.length() > 1) {
			rtrn = rtrn.substring(0, rtrn.length() - 1);
		}
		return rtrn;
	}
	
	public static final boolean isStrUUID(String uuid) {
		if(uuid == null) {
			return false;
		}
		try {
			return UUID.fromString(uuid) != null;
		} catch(Throwable ignored) {
			return false;
		}
	}
	
	/** @param str The string to convert
	 * @return The resulting long value
	 * @throws NumberFormatException Thrown if the given String does not
	 *             represent a valid long value */
	public static final Long getLongFromStr(String str) throws NumberFormatException {
		return Long.valueOf(str);
	}
	
	/** @param time The time to convert
	 * @param getTimeOnly Whether or not the result should exclude the date
	 * @return The result */
	public static String getTime(long time, boolean getTimeOnly) {
		return getTime(time, getTimeOnly, false);
	}
	
	/** @param time The time to convert
	 * @param getTimeOnly Whether or not the result should exclude the date
	 * @param fileSystemSafe Whether or not the date should be file system
	 *            safe(does nothing if the date is excluded)
	 * @return The result */
	public static String getTime(long time, boolean getTimeOnly, boolean fileSystemSafe) {
		return new SimpleDateFormat(getTimeOnly ? "HH-mm-ss" : fileSystemSafe ? "MM-dd-yyyy_HH.mm.ss" : "MM/dd/yyyy'\t'h:mm:ss a").format(new Date(time));
	}
	
	public static final SimpleDateFormat getCacheValidatorTimeFormat() {
		SimpleDateFormat rtrn = new SimpleDateFormat(cacheValidatorTimePattern, cacheValidatorTimeLocale);
		rtrn.setTimeZone(cacheValidatorTimeFormat);
		return rtrn;
	}
	
	/** @param millis The amount of milliseconds that have passed since midnight,
	 *            January 1, 1970
	 * @return The resulting string in cache format */
	public static final String getCacheTime(long millis) {
		return getCacheValidatorTimeFormat().format(new Date(millis));
	}
	
	/** @return The current time in cache format */
	public static final String getCurrentCacheTime() {
		return getCacheValidatorTimeFormat().format(new Date());
	}
	
	public static final byte[] compressString(String str, String charsetName) throws IOException {
		if(str == null || str.length() == 0) {
			return new byte[0];
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream(str.length());
		GZIPOutputStream gzip = new GZIPOutputStream(out);
		gzip.write(str.getBytes(charsetName));
		gzip.flush();
		gzip.close();
		byte[] compressedBytes = out.toByteArray();
		return compressedBytes;
	}
	
	public static final boolean containsIgnoreCase(String str, String... list) {
		if(list != null && list.length != 0) {
			for(String s : Arrays.asList(list)) {
				if(s != null && s.equalsIgnoreCase(str)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static final boolean containsIgnoreCase(ArrayList<String> list, String str) {
		if(list != null && !list.isEmpty()) {
			for(String s : new ArrayList<>(list)) {
				if(s != null && s.equalsIgnoreCase(str)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static boolean containsIgnoreCase(Set<String> list, String str) {
		if(list != null && !list.isEmpty()) {
			for(String s : new ArrayList<>(list)) {
				if(s != null && s.equalsIgnoreCase(str)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static String getStringInList(Set<String> list, String str) {
		if(list != null && !list.isEmpty()) {
			for(String s : new ArrayList<>(list)) {
				if(s != null && s.equalsIgnoreCase(str)) {
					return s;
				}
			}
		}
		return null;
	}
	
	public static final String getUrlLinkFromFile(File urlFile) throws IOException {
		String line = "";
		if(urlFile == null || !urlFile.exists() || urlFile.isDirectory()) {
			return line;
		}
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(urlFile));
			while(br.ready()) {
				line = br.readLine();
				if(line.startsWith("URL=")) {
					line = line.substring(4);
					break;
				}
			}
		} finally {
			if(br != null) {
				br.close();
			}
		}
		return line;
	}
	
	public static final String makeFilePathURLSafe(String filePath) {
		return filePath.replace("%", "%25").replace("+", "%2b").replace("#", "%23").replace(" ", "%20");
	}
	
	public static final String encodeHTML(String str) {
		str = str.replaceAll("%(?![0-9a-fA-F]{2})", "%25").replace("+", "%2b").replace("#", "%23").replace(" ", "%20");
		StringEscapeUtils.escapeHtml3("");
		return StringEscapeUtils.escapeHtml4(str).replace(" ", "%20");//.replaceAll("(?i)&mdash;", "—").replaceAll("(?i)&ndash;", "–").replaceAll("(?i)&micro;", "?").replaceAll("(?i)&omega;", "?");
	}
	
	public static final String decodeHTML(String s) {
		final String str = s;
		s = s.replaceAll("%(?![0-9a-fA-F]{2})", "%25").replaceAll("\\+", "%2B");
		try {
			s = URLDecoder.decode(s, "UTF-8");
			return StringEscapeUtils.unescapeHtml4(s);
		} catch(Throwable ignored) {
			return StringEscapeUtils.unescapeHtml4(str);
		}
	}
	
	private static final boolean isCharIllegal(String str) {
		return(str.equals("\n") || str.equals("\r") || str.equals("\t") || str.equals("\0") || str.equals("\f") || str.equals("`") || str.equals("'") || str.equals("?") || str.equals("*") || str.equals("<") || str.equals(">") || str.equals("|") || str.equals("\""));
	}
	
	/** @param value The value to test
	 * @return Whether or not it is a valid long value */
	public static final boolean isStrLong(String value) {
		try {
			Long.valueOf(value).longValue();
			return true;
		} catch(Throwable ignored) {
			return false;
		}
	}
	
	/** @param value The value to test
	 * @return Whether or not it is a valid int value */
	public static final boolean isStrInt(String value) {
		try {
			Integer.valueOf(value).intValue();
			return true;
		} catch(Throwable ignored) {
			return false;
		}
	}
	
	/** @param value The value to test
	 * @return Whether or not it is a valid double value */
	public static final boolean isStrDouble(String value) {
		try {
			Double.valueOf(value).doubleValue();
			return true;
		} catch(Throwable ignored) {
			return false;
		}
	}
	
	/** Compare Strings in alphabetical order */
	public static final Comparator<String>	ALPHABETICAL_ORDER	= new Comparator<String>() {
																	@Override
																	public int compare(String str1, String str2) {
																		if(str1 == null || str2 == null) {
																			return Integer.MAX_VALUE;
																		}
																		int res = String.CASE_INSENSITIVE_ORDER.compare(str1, str2);
																		if(res == 0) {
																			res = str1.compareTo(str2);
																		}
																		return res;
																	}
																};
	
	//==========================
	protected static final long				MILLISECOND			= 1L;
	protected static final long				SECOND				= 1000L;
	protected static final long				MINUTE				= 60 * SECOND;
	protected static final long				HOUR				= 60 * MINUTE;
	protected static final long				DAY					= 24 * HOUR;
	protected static final long				WEEK				= 7 * DAY;
	protected static final long				YEAR				= 365 * DAY;	//(long) (365.2395 * DAY);
	protected static final long				DECADE				= 10 * YEAR;
	protected static final long				CENTURY				= 10 * DECADE;
	protected static final long				MILLENNIUM			= 10 * CENTURY;
	
	/** @param millis The time in milliseconds
	 * @return The time, in String format */
	public static String getElapsedTime(long millis) {
		return getElapsedTime(millis, false);
	}
	
	/** @param millis The time in milliseconds
	 * @param showMilliseconds Whether or not to show milliseconds(...:000)
	 * @return The time, in String format */
	public static String getElapsedTime(long millis, boolean showMilliseconds) {
		boolean negative = millis < 0;
		if(negative) {
			millis = Math.abs(millis);
		}
		String rtrn = "";
		if(millis >= MILLENNIUM) {
			long millenniums = millis / MILLENNIUM;
			millis %= MILLENNIUM;
			rtrn += millenniums + " Millennium" + (millenniums == 1 ? "" : "s") + " ";
		}
		if(millis >= CENTURY) {
			long centuries = millis / CENTURY;
			millis %= CENTURY;
			rtrn += centuries + " Centur" + (centuries == 1 ? "y" : "ies") + " ";
		}
		if(millis >= YEAR) {
			long years = millis / YEAR;
			millis %= YEAR;
			rtrn += years + " Year" + (years == 1 ? "" : "s") + " ";
		}
		if(millis >= WEEK) {
			long weeks = millis / WEEK;
			millis %= WEEK;
			rtrn += weeks + " Week" + (weeks == 1 ? "" : "s") + " ";
		}
		if(millis >= DAY) {
			long days = millis / DAY;
			millis %= DAY;
			rtrn += days + " Day" + (days == 1 ? "" : "s") + " and ";
		}
		long hours = 0L;
		if(millis >= HOUR) {
			hours = millis / HOUR;
			millis %= HOUR;
		}
		long minutes = 0L;
		if(millis >= MINUTE) {
			minutes = millis / MINUTE;
			millis %= MINUTE;
		}
		long seconds = 0L;
		if(millis >= SECOND) {
			seconds = millis / SECOND;
			millis %= SECOND;
		}
		long milliseconds = 0L;
		if(millis >= MILLISECOND && showMilliseconds) {
			milliseconds = millis / MILLISECOND;
			millis %= milliseconds;
		}
		final String hourStr = (hours == 0 ? "" : hours + ":");
		final String minuteStr = (minutes == 0 ? (hours != 0 ? "00:" : (seconds != 0 || milliseconds != 0 ? "0:" : "")) : (minutes < 10 ? "0" : "") + minutes + ":");
		final String secondStr = (hours == 0 && minutes == 0 && seconds == 0 && milliseconds == 0 ? "" : (seconds < 10 ? "0" : "") + seconds);
		rtrn += hourStr + minuteStr + secondStr + (milliseconds != 0 ? ":" + (milliseconds < 100 ? (milliseconds < 10 ? "00" : "0") : "") + milliseconds : "");
		rtrn = rtrn.endsWith("and ") ? rtrn.substring(0, rtrn.length() - 4).trim() : rtrn;
		rtrn += (negative ? " Remaining" : "");
		rtrn = rtrn.replace("  ", " ").trim();
		return rtrn.trim().isEmpty() ? "0:00" : rtrn;
	}
	
	/** @param str The text to edit
	 * @return The given text with its first letter capitalized */
	public static final String captializeFirstLetter(String str) {
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}
	
}
