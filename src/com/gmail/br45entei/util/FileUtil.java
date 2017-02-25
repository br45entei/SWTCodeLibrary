package com.gmail.br45entei.util;

import com.gmail.br45entei.data.DisposableByteArrayOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FilenameUtils;

/** Read file contents up to 2gb, rename files and folders, and log strings of text into files that<br>
 * automatically gzip when log file size reaches 8kb with a configurable root directory for log files.
 * 
 * @author <a href="http://redsandbox.ddns.net/about/author.html">Brian_Entei</a>, <a href="http://www.joapple.ca/fr">Jonathan</a>
 * @see #readFile(File)
 * @see #renameFile(File, String)
 * @see #getRootLogFolder()
 * @see #setRootLogFolder(File)
 * @see #logStr(String, String) */
public class FileUtil {
	
	/** Test to see ratio of file size between gzipped and plain text files; this is not API
	 * 
	 * @param args Program command line arguments */
	public static final void main(String[] args) {
		String logName = "test";
		for(int index = 0; index < 5; index++) {
			DisposableByteArrayOutputStream baos = new DisposableByteArrayOutputStream();
			try {
				GZIPOutputStream gzip = new GZIPOutputStream(baos);
				while(baos.toByteArray().length < 16103) {//14570) {//17636) {//19169) {//12288) {//8192) {
					String random = StringUtil.nextSessionId();
					byte[] r = random.getBytes(StandardCharsets.UTF_8);
					gzip.write(r);
					logStr(logName, random);
				}
				gzip.flush();
				gzip.close();
			} catch(IOException e) {
				baos.close();
				throw new Error("This should not have happened!", e);
			}
		}
	}
	
	private static volatile File rootLogDir = new File(System.getProperty("user.dir"));
	
	/** @param file The file whose contents will be read
	 * @return The file's contents, in a byte array
	 * @throws IOException Thrown if an I/O exception occurs */
	public static final byte[] readFile(File file) throws IOException {
		if(file == null || !file.isFile()) {
			return null;
		}
		try(FileInputStream in = new FileInputStream(file)) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int read;
			while((read = in.read()) >= 0) {
				baos.write(read);
			}
			return baos.toByteArray();
		}
	}
	
	/** @param file The file to rename
	 * @param renameTo The new name for the file
	 * @return Whether or not the renaming was successful
	 * @throws IOException Thrown if an I/O exception occurs */
	public static final boolean renameFile(File file, String renameTo) throws IOException {
		Path source = Paths.get(file.toURI());
		boolean success = true;
		if(file.isDirectory() && file.getName().equalsIgnoreCase(renameTo)) {
			File folder = new File(file.getParentFile(), renameTo);
			success = file.renameTo(folder);
		} else {
			Files.move(source, source.resolveSibling(renameTo), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
		}
		return success;
	}
	
	private static final ConcurrentHashMap<String, File> logFiles = new ConcurrentHashMap<>();
	
	/** @param logName The base name of the log file. Example: if you want a log file named &quot;Commands.log&quot;, then you would only supply the string &quot;Commands&quot; here
	 * @param createIfNotExist Whether or not the log file should be created
	 * @return The log file if it already existed or the createIfNotExist boolean argument was set to {@code true}. If the file did not exist and the argument was {@code false}, then {@code null} is returned.
	 * @throws IOException Thrown if an I/O exception occurs */
	private static final File getLogFile(String logName, boolean createIfNotExist) throws IOException {
		if(logName == null) {
			return null;
		}
		File file = logFiles.get(logName);
		boolean fileDidExist = false;
		if(file != null) {
			fileDidExist = file.isFile();//true;
			//If the file's name doesn't match it's original key(file was renamed into an archive?), or the file's parental directory
			//structure has changed(either the root folder was changed, or the file was moved into a different folder somehow), then:
			if(!file.getName().equals(logName) || !FilenameUtils.normalize(file.getParentFile().getAbsolutePath()).equals(FilenameUtils.normalize(getRootLogFolder().getAbsolutePath()))) {
				logFiles.remove(logName);//unregister the now invalid file object
				file = null;//set the variable to null so that it is re-created below if either createIfNotExist or fileDidExist is true
			}
		}
		if(createIfNotExist || fileDidExist) {
			if(file == null) {
				file = new File(getRootLogFolder(), logName + ".log");
			}
			if(!file.exists()) {
				file.createNewFile();
			}
			logFiles.put(logName, file);
			return file;
		}
		return null;
	}
	
	/** @return The parent directory for any log files created */
	public static final File getRootLogFolder() {
		return rootLogDir;
	}
	
	/** @param folder The new parent directory that will contain all future log files */
	public static final void setRootLogFolder(File folder) {
		if(folder != null) {
			if(!folder.exists()) {
				folder.mkdirs();
			}
			rootLogDir = folder;
		}
	}
	
	private static final File getArchiveFolder() {
		File logs = new File(getRootLogFolder(), "Logs");
		if(!logs.exists()) {
			logs.mkdirs();
		}
		return logs;
	}
	
	/** @param logName The base name of the log file. Example: if you want a log file<br>
	 *            named &quot;Commands.log&quot;, then you would only supply<br>
	 *            the string &quot;Commands&quot; here
	 * @param str The line or lines of text to append to the end of the log file */
	public static final void logStr(String logName, String str) {
		try {
			File file = getLogFile(logName, true);
			BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
			final long lastModifiedTime = attr.lastModifiedTime().toMillis();
			final long fileSize = attr.size();
			boolean fileSizeLimitReached = fileSize >= 8192;
			if(fileSizeLimitReached) {//8kb
				/*if(renameFile(file, "Logs" + File.separator + fileName)) {
					file = getLogFile(fileName, true);
				} else {
					System.err.println("File not renamed!");
				}*/
				String baseName = FilenameUtils.getBaseName(file.getName()) + "_" + StringUtil.getTime(lastModifiedTime, false, true, true);
				String ext = ".log.gz";
				String fileName = baseName + ext;
				File archived = new File(getArchiveFolder(), fileName);
				int duplicates = 0;//juuust in case
				while(archived.exists()) {
					archived = new File(getArchiveFolder(), baseName + "_" + (duplicates++) + ext);
				}
				byte[] r = FileUtil.readFile(file);
				GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(archived, false));
				out.write(r);
				out.flush();
				out.close();
				FileDeleteStrategy.FORCE.deleteQuietly(file);
				file = getLogFile(logName, true);
			}
			FileOutputStream out = new FileOutputStream(file, !fileSizeLimitReached);
			PrintWriter pr = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), true);
			pr.println(str);
			pr.flush();
			pr.close();
		} catch(Error e) {
		} catch(RuntimeException e) {
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
	
}
