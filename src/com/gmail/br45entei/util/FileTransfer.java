package com.gmail.br45entei.util;

import com.gmail.br45entei.data.DisposableByteArrayInputStream;
import com.gmail.br45entei.data.DisposableByteArrayOutputStream;
import com.gmail.br45entei.data.OutputInputStream;
import com.gmail.br45entei.data.Property;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

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
			System.out.println("Size: " + data.getSize() + "(available before: " + before + "; available now: " + in.available() + "); Name: " + data.name);
			System.out.flush();
			in.close();
			stream.close();
		} catch(Throwable e) {
			e.printStackTrace();
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
					if(StringUtil.isStrInt(fileSize)) {
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
							while(remaining > 0) {
								remaining = size - count;
								read = in.read(buf, 0, Math.min(buf.length, remaining));
								if(read == -1) {
									break;
								}
								count += read;
								baos.write(buf, 0, read);
								remaining = size - count;
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
	
}
