package com.gmail.br45entei.data;

import com.gmail.br45entei.util.StringUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FilenameUtils;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public class Credentials {
	
	private static volatile File	rootDir;
	public static final String		ext	= "txt";
	
	private static final boolean isStrUUID(String str) {
		try {
			UUID.fromString(str);
			return true;
		} catch(IllegalArgumentException ignored) {
			return false;
		}
	}
	
	public static final void initialize(File rootFolder) {
		rootDir = rootFolder;
		loadInstancesFromFile();
	}
	
	public static final ArrayList<Credentials> getInstances() {
		return new ArrayList<>(instances);
	}
	
	private static final ConcurrentLinkedQueue<Credentials> instances = new ConcurrentLinkedQueue<>();
	
	public static final int getNumOfCreds() {
		return instances.size();
	}
	
	public static final Credentials createDefaultCredentials() {
		Credentials rtrn = getCredentialsIfExists("Administrator");
		if(rtrn != null) {
			return rtrn;
		}
		rtrn = new Credentials(UUID.randomUUID());
		while(rtrn.getSaveFile().exists()) {
			rtrn.dispose();
			rtrn = new Credentials(UUID.randomUUID());
		}
		rtrn.username = "Administrator";
		rtrn.password = StringUtil.nextSessionId();
		rtrn.saveToFile();
		return rtrn;
	}
	
	public static final Credentials getCredentialsIfExists(String username) {
		for(Credentials creds : new ArrayList<>(instances)) {
			if(creds.getUsername().equals(username)) {
				return creds;
			}
		}
		return null;
	}
	
	public static final Credentials getOrCreateCredentials(String username) {
		for(Credentials creds : new ArrayList<>(instances)) {
			if(creds.getUsername().equalsIgnoreCase(username)) {
				return creds;
			}
		}
		return createDefaultCredentials();
	}
	
	public static final Credentials getCredentials(String username, String password) {
		if((username == null || username.isEmpty()) || (password == null || password.isEmpty())) {
			return null;
		}
		for(Credentials credentials : instances) {
			if(credentials == null) {
				instances.remove(credentials);
				continue;
			}
			if(credentials.doCredentialsMatch(username, password)) {
				return credentials;
			}
		}
		return null;
	}
	
	private final UUID		uuid;
	
	private volatile String	username;
	private volatile String	password;
	
	/** @param credentials The map of credentials to set and use */
	public static final void setFromMap(Map<? extends String, ? extends String> credentials) {
		for(Credentials cred : new ArrayList<>(instances)) {
			cred.delete();
		}
		instances.clear();
		for(Entry<? extends String, ? extends String> entry : credentials.entrySet()) {
			new Credentials(UUID.randomUUID(), entry.getKey(), entry.getValue()).saveToFile();
		}
	}
	
	private Credentials(UUID uuid) {
		if(rootDir == null) {
			throw new NullPointerException("Error instantiating class Credentials: \"rootDir\" cannot be null! Have you called \"Credentials.initialize(File rootFolder);\"?");
		}
		if(uuid == null) {
			throw new NullPointerException("Error instantiating class Credentials: \"uuid\" cannot be null!");
		}
		this.uuid = uuid;
		instances.add(this);
	}
	
	private Credentials(UUID uuid, String username, String password) {
		this(uuid);
		this.username = username;
		this.password = password;
	}
	
	public final String getUsername() {
		return this.username;
	}
	
	public final String getPassword() {
		return this.password;
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
	
	private static final void loadInstancesFromFile() {
		File folder = new File(rootDir, getSaveFolderName());
		if(!folder.exists()) {
			folder.mkdirs();
		}
		for(String fileName : folder.list()) {
			String name = FilenameUtils.getBaseName(fileName);
			String ext = FilenameUtils.getExtension(fileName);
			if(isStrUUID(name) && ext.equalsIgnoreCase(Credentials.ext)) {
				File file = new File(folder, fileName);
				if(file.isFile()) {
					UUID uuid = UUID.fromString(name);
					Credentials credentials = new Credentials(uuid);
					credentials.loadFromFile();
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
	
	public static final File getSaveFolder() {
		File folder = new File(rootDir, getSaveFolderName());
		if(!folder.exists()) {
			folder.mkdirs();
		}
		return folder;
	}
	
	public final File getSaveFile() {
		return new File(Credentials.getSaveFolder(), this.uuid.toString() + "." + ext);
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
	
	public final void delete() {
		try {
			if(!this.getSaveFile().delete()) {
				FileDeleteStrategy.FORCE.deleteQuietly(getSaveFile());
			}
		} catch(Throwable ignored) {
		}
		this.dispose();
	}
	
	public final void dispose() {
		this.username = null;
		this.password = null;
		instances.remove(this);
		System.gc();
	}
	
}
