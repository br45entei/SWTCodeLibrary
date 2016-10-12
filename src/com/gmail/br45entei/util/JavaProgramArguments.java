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
	
	public static final void initializeFromMainClass(Class<?> clazz, String[] args) {
		instance = new JavaProgramArguments(clazz.getPackage().getName(), args);
	}
	
	public static final JavaProgramArguments getArguments() {
		return instance;
	}
	
	public final String	runtimeCommand;
	
	public final String	runtimeArguments;
	public final String	programArguments;
	public final String	javaHome;
	public final String	javaExecutable;
	
	private JavaProgramArguments(final String mainPackageName, final String[] args) {
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
		} else if(mainPackageName.equals(mainClassFileName)) {//-classPath was used
			System.out.println("Launcher was started in a development environment.(Hi there!)");
			startupCommand = (classPathCmdLine.isEmpty() ? "-classpath " : "") + mainClass;
		}
		this.programArguments = programArgsCmdLine.trim();
		this.runtimeArguments = vmArgs;
		this.runtimeCommand = javaCmdLine + " " + vmArgs + classPathCmdLine + startupCommand + programArgsCmdLine;
	}
	
}
