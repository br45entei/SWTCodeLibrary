package com.gmail.br45entei.util;

import com.gmail.br45entei.data.DisposableByteArrayInputStream;
import com.gmail.br45entei.data.DisposableByteArrayOutputStream;
import com.gmail.br45entei.data.OutputInputStream;
import com.gmail.br45entei.data.Property;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public class FileTransfer {
	
	public static final void main(String[] args) {
		try {
			File test = new File(StringUtil.stringArrayToString(' ', args));
			OutputInputStream stream = new OutputInputStream();
			sendFile(test, stream, null);
			DisposableByteArrayInputStream in = stream.asInputStream();
			final int before = in.available();
			FileData data = readFile(in);
			System.out.println("hash: " + hashBytes(data.data));
			System.out.println("Size: " + data.getSize() + "(available before: " + before + "; available now: " + in.available() + "); Name: " + data.name);
			System.out.flush();
			in.close();
			stream.close();
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
	
	public static final String hashBytes(byte[] data) {
		final MessageDigest hashSum;
		try {
			hashSum = MessageDigest.getInstance("SHA-256");
			hashSum.update(data);
			byte[] mdBytes = hashSum.digest();
			StringBuffer hexString = new StringBuffer();
			for(int i = 0; i < mdBytes.length; i++) {
				hexString.append(Integer.toHexString(0xFF & mdBytes[i]));
			}
			return hexString.toString();
		} catch(Throwable e) {
			e.printStackTrace();
			return "null";
		}
	}
	
	public static final String hashStream(InputStream in) {
		try {
			return hashBytes(readFile(in).data);
		} catch(Throwable e) {
			e.printStackTrace();
			return "null";
		}
	}
	
	public static final String hashFile(File file) {
		try {
			return hashBytes(readFile(file).data);
		} catch(Throwable e) {
			e.printStackTrace();
			return "null";
		}
	}
	
	public static final class FileData {
		public volatile String	name;
		public volatile byte[]	data;
		public volatile long	lastModified;
		
		public final int getSize() {
			return this.data != null ? this.data.length : -1;
		}
		
	}
	
	public static final FileData readFile(InputStream in) throws IOException {
		return readFile(in, null);
	}
	
	public static final FileData readFile(InputStream in, Property<Double> progress) throws IOException {
		DisposableByteArrayOutputStream baos = new DisposableByteArrayOutputStream();
		final String fileNameRead = StringUtil.readLine(in);
		final String fileSizeRead = StringUtil.readLine(in);
		final String fileLastModRead = StringUtil.readLine(in);
		String fileName = null;
		long lastModified = -1L;
		if(fileNameRead.startsWith("FILENAME: ")) {
			fileName = fileNameRead.substring("FILENAME: ".length());
			if(fileSizeRead.startsWith("FILESIZE: ")) {
				if(fileLastModRead.startsWith("FILELASTMOD: ")) {
					String fileSize = fileSizeRead.substring("FILESIZE: ".length());
					if(StringUtil.isStrInt(fileSize)) {//XXX prevents reading files bigger than Integer.MAX_VALUE, i know... need a better way to read file data in case this is ever needed
						String fileLastMod = fileLastModRead.substring("FILELASTMOD: ".length());
						if(StringUtil.isStrLong(fileLastMod)) {
							lastModified = Long.valueOf(fileLastMod).longValue();
							final int size = Integer.valueOf(fileSize).intValue();
							int count = 0;
							byte[] buf = new byte[4096];
							int remaining = size - count;
							int read = in.read(buf, 0, Math.min(buf.length, remaining));
							count += read;
							baos.write(buf, 0, read);
							remaining = size - count;
							if(progress != null) {
								progress.setValue(Double.valueOf(((count + 0.00D) / (size + 0.00D) * 100.0D)));
							}
							while(remaining > 0) {
								remaining = size - count;
								read = in.read(buf, 0, Math.min(buf.length, remaining));
								if(read == -1) {
									break;
								}
								count += read;
								baos.write(buf, 0, read);
								remaining = size - count;
								if(progress != null) {
									progress.setValue(Double.valueOf(((count + 0.00D) / (size + 0.00D) * 100.0D)));
								}
							}
						} else {
							//System.err.println("fileLastMod isLong: " + StringUtil.isStrLong(fileLastMod) + ": " + fileLastMod);
						}
					} else {
						//System.err.println("fileSize isInt: " + StringUtil.isStrInt(fileSize) + ": " + fileSize);
					}
				} else {
					//System.err.println("fileLastModRead.startsWith: " + fileLastModRead);
				}
			} else {
				//System.err.println("fileSizeRead.startsWith: " + fileSizeRead);
			}
		} else {
			//System.err.println("fileNameRead.startsWith: " + fileNameRead);
		}
		FileData data = new FileData();
		data.name = fileName;
		data.data = baos.toByteArray();
		data.lastModified = lastModified == -1L ? System.currentTimeMillis() : lastModified;
		baos.close();
		if(progress != null) {
			progress.setValue(Double.valueOf(100.0D));
		}
		return data;
	}
	
	public static final void sendFile(File file, OutputStream outStream, Property<Double> progress) throws IOException {
		if(progress != null) {
			progress.setValue(Double.valueOf(0.0D));
		}
		URLConnection url = file.toURI().toURL().openConnection();
		int fileSize = url.getContentLength();
		InputStream fis = url.getInputStream();
		PrintWriter pr = new PrintWriter(new OutputStreamWriter(outStream, StandardCharsets.UTF_8), true);
		pr.println("FILENAME: " + file.getName());
		pr.println("FILESIZE: " + fileSize);
		pr.println("FILELASTMOD: " + url.getLastModified());
		//System.out.println("Sending file size: " + fileSize);
		pr.flush();
		outStream.flush();
		
		long sent = 0;
		final double fileSizeD = fileSize + 0.00D;
		
		byte[] b = new byte[4096];
		int len;
		while((len = fis.read(b)) >= 0) {
			outStream.write(b, 0, len);
			sent += len;
			if(progress != null) {
				progress.setValue(Double.valueOf(sent / fileSizeD));
			}
		}
		outStream.flush();
		fis.close();
		if(progress != null) {
			progress.setValue(Double.valueOf(1.0D));
		}
	}
	
	public static final FileData readFile(File file) throws IOException {
		DisposableByteArrayOutputStream baos = new DisposableByteArrayOutputStream();
		final String fileName = file.getName();
		final String fileSize = Long.toString(file.length());
		final FileInputStream fis = new FileInputStream(file);
		final long lastModified = Files.getLastModifiedTime(Paths.get(file.toURI())).toMillis();
		if(StringUtil.isStrInt(fileSize)) {//XXX prevents reading files bigger than Integer.MAX_VALUE, i know... need a better way to read file data in case this is ever needed
			final int size = Integer.parseInt(fileSize);
			int count = 0;
			byte[] buf = new byte[4096];
			int remaining = size - count;
			int read = fis.read(buf, 0, Math.min(buf.length, remaining));
			count += read;
			baos.write(buf, 0, read);
			remaining = size - count;
			while(remaining > 0) {
				remaining = size - count;
				read = fis.read(buf, 0, Math.min(buf.length, remaining));
				if(read == -1) {
					break;
				}
				count += read;
				baos.write(buf, 0, read);
				remaining = size - count;
			}
		}
		FileData data = new FileData();
		data.name = fileName;
		data.data = baos.toByteArray();
		data.lastModified = lastModified == -1L ? System.currentTimeMillis() : lastModified;
		baos.close();
		try {
			fis.close();
		} catch(IOException ignored) {
		}
		return data;
	}
	
}
