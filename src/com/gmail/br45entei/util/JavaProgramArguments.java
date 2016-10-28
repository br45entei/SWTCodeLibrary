package com.gmail.br45entei.util;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;

import org.apache.commons.io.FilenameUtils;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public final class JavaProgramArguments {
	
	private static volatile JavaProgramArguments	instance;
	public final ArrayList<String>					arguments	= new ArrayList<>();
	private static volatile File					classPathJar;
	
	public static final void initializeFromMainClass(Class<?> clazz, String[] args) {
		if(instance != null) {
			throw new IllegalStateException("This may only be called once!");
		}
		instance = new JavaProgramArguments(clazz, args);
	}
	
	public static final JavaProgramArguments getArguments() {
		return instance;
	}
	
	public final String	runtimeCommand;
	
	public final String	runtimeArguments;
	public final String	programArguments;
	public final String	javaHome;
	public final String	javaExecutable;
	
	private JavaProgramArguments(final Class<?> clazz, final String[] args) {
		String mainPackageName = clazz.getPackage().getName();
		for(String arg : args) {
			if(arg != null && !arg.trim().isEmpty()) {
				this.arguments.add(arg);
			}
		}
		final String programArgs = StringUtil.stringArrayToString(' ', args).trim();
		String programArgsCmdLine;
		if(programArgs.isEmpty()) {
			programArgsCmdLine = "";//" fromLauncher";
		} else {
			programArgsCmdLine = " " + programArgs;// + " fromLauncher";
		}
		
		this.javaHome = System.getProperty("java.home");
		this.javaExecutable = this.javaHome + File.separatorChar + "bin" + File.separatorChar + "java";
		final String javaCmdLine;
		if(this.javaExecutable.contains(" ")) {
			javaCmdLine = "\"" + this.javaExecutable + "\"";
		} else {
			javaCmdLine = this.javaExecutable;
		}
		RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
		final String arguments = StringUtil.stringArrayToString(' ', runtimeMxBean.getInputArguments());
		final String vmArgs;
		if(arguments.isEmpty()) {
			vmArgs = "";
		} else {
			vmArgs = arguments + " " + (arguments.contains("-Xms") || arguments.contains("-Xmx") ? "" : "-Xms1024m ");//-Xmx2048m ");
		}
		
		final String mainClass;
		String getMainClass = System.getProperty("sun.java.command");
		if(getMainClass.endsWith(programArgs)) {
			mainClass = getMainClass.substring(0, getMainClass.length() - programArgs.length()).trim();
		} else {
			mainClass = getMainClass;
		}
		final String classPath = System.getProperty("java.class.path");
		final String classPathCmdLine;
		if(classPath == null || classPath.isEmpty()) {
			classPathCmdLine = "";
		} else {
			classPathCmdLine = "-classpath " + classPath + " ";
		}
		
		String mainClassFileName = FilenameUtils.getBaseName(mainClass);
		
		String startupCommand = "";
		
		if(mainClass.equals(classPath)) {// -jar was used
			System.out.println("Launcher was started from an executable jar file.");
			startupCommand = "-jar " + mainClass;
			classPathJar = new File(mainClass);
		} else if(mainPackageName.equals(mainClassFileName)) {//-classPath was used
			String currentDir = System.getProperty("user.dir");
			String packageName = mainPackageName.replace(currentDir, "");
			String filePath = FilenameUtils.normalize(currentDir + File.separator + "bin" + File.separator + packageName.replace(".", File.separator) + File.separator + clazz.getSimpleName() + ".class");
			classPathJar = new File(filePath);
			if(!classPathJar.exists()) {
				filePath = FilenameUtils.normalize(currentDir + File.separator + ".." + File.separator + "bin" + File.separator + packageName.replace(".", File.separator) + File.separator + clazz.getSimpleName() + ".class");
				classPathJar = new File(filePath);
			}
			System.out.println("Launcher was started in a development environment.(Hi there!)");
			startupCommand = (classPathCmdLine.isEmpty() ? "-classpath " : "") + mainClass;
		}
		//System.out.println("File: " + classPathJar.getAbsolutePath() + "; exists: " + classPathJar.isFile());
		this.programArguments = programArgsCmdLine.trim();
		this.runtimeArguments = vmArgs;
		this.runtimeCommand = javaCmdLine + " " + vmArgs + classPathCmdLine + startupCommand + programArgsCmdLine;
	}
	
	public static final File getClassPathJarFile() {
		return classPathJar;
	}
	
}
