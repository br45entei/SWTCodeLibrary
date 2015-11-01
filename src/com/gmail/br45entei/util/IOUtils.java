package com.gmail.br45entei.util;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;

/** @author Brian_Entei */
public final class IOUtils {
	
	/** @param port The port to check
	 * @return True if the port is valid and is available for TCP and UDP. */
	public static final boolean isPortAvailable(int port) {
		if(port < 0 || port > 65535) {
			return false;
		}
		
		ServerSocket ss = null;
		DatagramSocket ds = null;
		try {
			ss = new ServerSocket(port);
			ss.setReuseAddress(true);
			ds = new DatagramSocket(port);
			ds.setReuseAddress(true);
			return true;
		} catch(IOException ignored) {
		} finally {
			if(ds != null) {
				try {
					ds.close();
				} catch(Throwable ignored) {
				}
			}
			if(ss != null) {
				try {
					ss.close();
				} catch(Throwable ignored) {
				}
			}
		}
		
		return false;
	}
	
	public static final boolean isIPReachable(String ip) {
		InetAddress addr = null;
		try {
			addr = InetAddress.getByName(ip);
		} catch(Throwable ignored) {
		}
		return addr != null;
	}
	
}
