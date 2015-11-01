package com.gmail.br45entei.data;

import com.gmail.br45entei.util.StringUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;

/** @author Brian_Entei */
public class Credentials {
	
	private static volatile File	rootDir;
	public static final String		ext	= "txt";
	
	public static final void initialize(File rootFolder) {
		rootDir = rootFolder;
	}
	
	private static final ArrayList<Credentials>	instances	= new ArrayList<>();
	
	public static final Credentials getCredentialsIfExists(String name) {
		for(Credentials creds : new ArrayList<>(instances)) {
			if(creds.getName().equals(name)) {
				return creds;
			}
		}
		return null;
	}
	
	public static final Credentials getOrCreateCredentials(String name) {
		for(Credentials creds : new ArrayList<>(instances)) {
			if(creds.getName().equals(name)) {
				return creds;
			}
		}
		Credentials rtrn = new Credentials(name);
		if(rtrn.getSaveFile().exists()) {
			rtrn.loadFromFile();
			return rtrn;
		}
		rtrn.username = "Administrator";
		rtrn.password = StringUtil.nextSessionId();
		rtrn.saveToFile();
		return rtrn;
	}
	
	private final String	name;
	
	private volatile String	username;
	private volatile String	password;
	
	protected Credentials(String name) {
		if(rootDir == null) {
			throw new NullPointerException("Error instantiating class Credentials: \"rootDir\" cannot be null! Have you called \"Credentials.initialize(File rootFolder);\"?");
		}
		if(name == null) {
			throw new NullPointerException("Error instantiating class Credentials: \"name\" cannot be null!");
		}
		this.name = name;
		instances.add(this);
	}
	
	public Credentials(String username, String password) {
		this(username);
		this.username = username;
		this.password = password;
	}
	
	public final String getName() {
		return this.name;
	}
	
	public final void set(String username, String password) {
		if(username != null) {
			this.username = username;
		}
		if(password != null) {
			this.password = password;
		}
	}
	
	public final boolean doCredentialsMatch(String username, String password) {
		return this.username.equalsIgnoreCase(username) && this.password.equals(password);
	}
	
	public static final void loadInstancesFromFile() {
		File folder = new File(rootDir, getSaveFolderName());
		if(!folder.exists()) {
			folder.mkdirs();
		}
		for(String fileName : folder.list()) {
			File file = new File(folder, fileName);
			if(file.isFile()) {
				String baseName = FilenameUtils.getBaseName(fileName);
				String ext = FilenameUtils.getExtension(fileName);
				if(ext.equalsIgnoreCase(ext)) {
					Credentials creds = getCredentialsIfExists(baseName);
					if(creds == null) {
						creds = new Credentials(baseName);
					}
					if(!creds.loadFromFile()) {
						creds.dispose();
					}
				}
			}
		}
	}
	
	public static final void saveInstancesToFile() {
		for(Credentials creds : new ArrayList<>(instances)) {
			creds.saveToFile();
		}
	}
	
	public static String getSaveFolderName() {
		return "Credentials";
	}
	
	public final File getSaveFolder() {
		File folder = new File(rootDir, getSaveFolderName());
		if(!folder.exists()) {
			folder.mkdirs();
		}
		return folder;
	}
	
	public final File getSaveFile() {
		return new File(this.getSaveFolder(), this.name + "." + ext);
	}
	
	public boolean loadFromFile() {
		File file = this.getSaveFile();
		if(!file.exists()) {
			return this.saveToFile();
		}
		try(FileInputStream in = new FileInputStream(file)) {
			String line;
			while((line = StringUtil.readLine(in)) != null) {
				String[] split = line.split(Pattern.quote("="));
				if(!line.isEmpty()) {
					if(split.length >= 2) {
						String pname = split[0];
						String value = StringUtil.stringArrayToString(split, '=', 1);
						if(pname.equalsIgnoreCase("username")) {
							this.username = value;
						} else if(pname.equalsIgnoreCase("password")) {
							this.password = value;
						}
					}
				}
			}
			return true;
		} catch(Throwable ignored) {
			return false;
		}
	}
	
	public boolean saveToFile() {
		File file = this.getSaveFile();
		try(PrintStream pr = new PrintStream(new FileOutputStream(file, false), true)) {
			pr.println("username=" + this.username);
			pr.println("password=" + this.password);
			return true;
		} catch(IOException ignored) {
			return false;
		}
	}
	
	public final void dispose() {
		instances.remove(this);
	}
	
}
