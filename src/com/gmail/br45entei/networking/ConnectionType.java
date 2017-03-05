package com.gmail.br45entei.networking;

/** @author Brian_Entei */
public enum ConnectionType {
	/** The server is connected/connecting to a client */
	CLIENT,
	/** The server or client is using a file transfer oriented connection */
	FILETRANSFER,
	/** The client is connected/connecting to a server */
	SERVER,
	/** Some other connection type */
	UNKNOWN;
	
}
