package com.gmail.br45entei.swt.test;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

public class MainClass {
	
	public static void main(String[] a) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new GridLayout());
		
		ProgressBar pb1 = new ProgressBar(shell, SWT.HORIZONTAL | SWT.SMOOTH);
		pb1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		pb1.setMinimum(0);
		pb1.setMaximum(30);
		
		new LongRunningOperation(display, pb1).start();
		
		shell.open();
		while(!shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
}

class LongRunningOperation extends Thread {
	private final Display display;
	
	private final ProgressBar progressBar;
	
	public LongRunningOperation(Display display, ProgressBar progressBar) {
		this.display = display;
		this.progressBar = progressBar;
	}
	
	@Override
	public void run() {
		for(int i = 0; i < 30; i++) {
			try {
				Thread.sleep(1000);
			} catch(InterruptedException e) {
			}
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					if(progressBar.isDisposed()) return;
					progressBar.setSelection(progressBar.getSelection() + 1);
				}
			});
		}
	}
}