package com.gmail.br45entei.console;

import com.gmail.br45entei.logging.LogUtils;
import com.gmail.br45entei.swt.dialog.PopupDialogMessage;
import com.gmail.br45entei.util.StringUtil;
import com.gmail.br45entei.util.exception.ConnectionTimeoutException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentLinkedDeque;

/** @author Brian_Entei */
public abstract class ConsoleThread extends Thread {
	private volatile boolean isRunning = false;
	/** Whether or not the exit command was run in the console */
	public volatile boolean exitCommandWasRun = false;
	
	protected volatile BufferedReader br;
	
	private volatile boolean allowConsoleMode = true;
	
	public ConsoleThread(String name, String... args) {
		super(name);
		this.setDaemon(true);
		this.allowConsoleMode = (!StringUtil.containsIgnoreCase("noconsole", args) && !StringUtil.containsIgnoreCase("headless", args) && !StringUtil.containsIgnoreCase("-headless", args) && !StringUtil.containsIgnoreCase("--headless", args));
	}
	
	public final boolean allowConsoleMode() {
		return this.allowConsoleMode;
	}
	
	@Override
	public final synchronized void start() {
		this.isRunning = true;
		if(this.allowConsoleMode) {
			super.start();
		}
		this.isRunning = false;
	}
	
	/** Tells this thread that it should stop running. */
	public final void stopThread() {
		this.isRunning = false;
		try {
			this.interrupt();
		} catch(Throwable ignored) {
		}
	}
	
	public final ConcurrentLinkedDeque<PopupDialogMessage> messages = new ConcurrentLinkedDeque<>();
	
	@Override
	public void run() {
		/*if(System.console() == null) {
			LogUtils.setConsoleMode(this.allowConsoleMode);
			return;
		}*/
		this.br = new BufferedReader(new InputStreamReader(System.in));
		LogUtils.setConsoleMode(this.allowConsoleMode);
		while(this.isRunning) {
			try {
				if(!this.isRunning) {
					break;
				}
				this.handleInput(this.br, this);
			} catch(IOException e) {
				if(e.getMessage() != null && e.getMessage().equals("The handle is invalid")) {
					this.messages.add(new PopupDialogMessage("Console thread shut down", "The internal console listener thread has shut down since System.in could not be read(is javaw being used?)!\nThis means manual commands fed to this application from the terminal or console window belonging to the OS cannot be listened to.\n\nThis is common(and can be ignored) if you started this application by double-clicking on the .jar file, as javaw is used by default on most systems."));
				} else {
					e.printStackTrace();
				}
			} catch(Throwable e) {
				e.printStackTrace();
			}
		}
		LogUtils.setConsoleMode(false);
	}
	
	/** @param in The input source to read from
	 * @param timeout The timeout, in milliseconds
	 * @param console The console thread handling the input
	 * @throws ConnectionTimeoutException Thrown if the input was not read in the given time allowed.<br>
	 *             The input stream may still be read from, but unexpected results can occur.
	 * @throws IOException Thrown if there was an error reading data from the given stream */
	public final void handleInput(InputStream in, long timeout, ConsoleThread console) throws ConnectionTimeoutException, IOException {
		handleInput(StringUtil.readLine(in, timeout), console);
	}
	
	/** @param in The input source to read from
	 * @param console The console thread handling the input
	 * @throws IOException Thrown if there was an error reading data from the given stream */
	public final void handleInput(InputStream in, ConsoleThread console) throws IOException {
		handleInput(StringUtil.readLine(in), console);
	}
	
	/** @param br The input source to read from
	 * @param console The console thread handling the input
	 * @throws IOException Thrown if there was an error reading data from the given stream */
	public final void handleInput(BufferedReader br, ConsoleThread console) throws IOException {
		handleInput(br.readLine(), console);
	}
	
	/** @param input The input to handle
	 * @param console The console thread handling the input
	 * @throws IOException Thrown if there was an error reading further data
	 *             from the console's standard-in stream */
	public final void handleInput(final String input, ConsoleThread console) throws IOException {
		if(input == null) {
			return;
		}
		if(input.trim().isEmpty()) {
			LogUtils.info("What'cha just pressin' enter for? Type a command to do somethin'.");
			return;
		}
		if(input.equalsIgnoreCase("\"help\" for help.") || input.equalsIgnoreCase("\"help\" for help") || input.equalsIgnoreCase("help for help.")) {
			LogUtils.info("Okay smartypants. You got me. I meant for you to type the word help, not all the words following the word \"Type\".");//Trololololol
			return;
		}
		String command = "";
		final String[] args;
		String mkArgs = "";
		for(String arg : input.split(" ")) {
			if(command.isEmpty()) {
				command = arg;
			} else {
				mkArgs += arg + " ";
			}
		}
		mkArgs = mkArgs.trim();
		if(mkArgs.isEmpty()) {
			args = new String[0];
		} else {
			args = mkArgs.split(" ");
		}
		
		if(LogUtils.getSecondaryOut() != null) {
			LogUtils.getSecondaryOut().println(LogUtils.getCarriageReturnConsolePrefix() + input);
		}
		if(LogUtils.getTertiaryOut() != null) {
			LogUtils.getTertiaryOut().println(LogUtils.getCarriageReturnConsolePrefix() + input);
		}
		this.handleInput(command, args);
		LogUtils.printConsole();
	}
	
	/** @param command The command that was issued
	 * @param args The arguments passed to the command */
	public abstract void handleInput(String command, String... args);
	
}
