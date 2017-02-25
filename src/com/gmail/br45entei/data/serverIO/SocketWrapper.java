package com.gmail.br45entei.data.serverIO;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public class SocketWrapper extends Socket {
	
	public static final String ephemeralDHKeySize = "2048";
	
	public static final String enabledTLSProtocols = "TLSv1,TLSv1.1,TLSv1.2";//,TLSv1.3";//enabling 1.3 ahead of time yields: javax.net.ssl.SSLException: Received fatal alert: protocol_version
	
	public static final String TLS_DisabledAlgorithms = "MD5, SHA1, DSA, DH, EDH, DHE, RC4, RSA keySize < 4096";//TODO See if "DH, DHE, EDH" even do anything. I have a feeling that they don't... however, it's working like this, so I'm afraid to try XD
	
	/** The enabled TLS protocols */
	public static final String[] TLSProtocols = new String[] {"TLSv1", "TLSv1.1", "TLSv1.2"};//, "TLSv1.3"};//enabling this ahead of time yields an illegal argument exception!
	
	public static final SSLSocketFactory getSSLSocketFactory() {
		//return SSLSocketFactoryEx.getDefault();
		return (SSLSocketFactory) SSLSocketFactory.getDefault();
	}
	
	// Create a trust manager that does not validate certificate chains
	private static final TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
		@Override
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return new java.security.cert.X509Certificate[0];
		}
		
		@Override
		public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
		}
		
		@Override
		public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
		}
	}};
	
	private static volatile SSLSocketFactory noSSLCertCheckingSocketFactory = null;
	private static final SSLSocketFactory httpsURLConnectionDefaultSSLSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
	private static final HostnameVerifier httpsURLConnectionDefaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
	
	// Create all-trusting host name verifier
	private static final HostnameVerifier allHostsValid = new HostnameVerifier() {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};
	
	public static final SSLSocketFactory getSSLSocketFactoryNoSSLCertificateChecking() {
		if(noSSLCertCheckingSocketFactory != null) {
			return noSSLCertCheckingSocketFactory;
		}
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new SecureRandom());
			noSSLCertCheckingSocketFactory = sc.getSocketFactory();
		} catch(GeneralSecurityException e) {
			e.printStackTrace();
		}
		return noSSLCertCheckingSocketFactory;
	}
	
	public static final boolean disableSSLCertificateCheckingForHttpsURLConnections() {
		final SSLSocketFactory noSSLCertCheckingSocketFactory = getSSLSocketFactoryNoSSLCertificateChecking();
		if(noSSLCertCheckingSocketFactory != null) {
			// Install the all-trusting trust manager
			HttpsURLConnection.setDefaultSSLSocketFactory(noSSLCertCheckingSocketFactory);
			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
			return true;
		}
		return false;
	}
	
	public static final void restoreSSLCertificateCheckingForHttpsURLConnections() {
		HttpsURLConnection.setDefaultSSLSocketFactory(httpsURLConnectionDefaultSSLSocketFactory);
		HttpsURLConnection.setDefaultHostnameVerifier(httpsURLConnectionDefaultHostnameVerifier);
	}
	
	//=========================================================================================================================================================
	
	private final Socket socket;
	private InputStreamSSLWrapper inWrapper;
	
	public SocketWrapper(Socket socket) throws IOException {
		this.socket = socket;
		this.inWrapper = new InputStreamSSLWrapper(this.socket.getInputStream());
	}
	
	public final boolean isNextByteClientHello() throws IOException {
		return this.inWrapper.isNextByteClientHello();
	}
	
	public final Socket wrapSSL(String[] TLSProtocols) throws IOException {
		this.insertByte(InputStreamSSLWrapper.sslClientHello);//Put eet baaackkkkkkkkk!!!111111!!!!one!!1 lol, it won't work without the first clienthello byte. That's kinda important. Kinda.
		SSLSocketFactory sslSf = SocketWrapper.getSSLSocketFactory();
		SSLSocket sslSocket = (SSLSocket) sslSf.createSocket(this, null, this.getPort(), false);
		sslSocket.setUseClientMode(false);
		sslSocket.setEnabledProtocols(TLSProtocols);
		sslSocket.startHandshake();//Let the new ssl socket we just 'hackingly' made handle the handshake for us.
		return sslSocket;//... holy crap, I just implemented "HTTP/HTTPS-on-the-same-port" functionality! Yay me! Not impressed? Well, poo on you then. It was hard and fun at the same time. I'm happy. I can explode now.(jk)
	}
	
	public final InputStreamSSLWrapper getInputReader() {
		return this.inWrapper;
	}
	
	@Override
	public final InputStream getInputStream() {
		return this.inWrapper;
	}
	
	public final void insertByte(int b) {
		this.inWrapper.addByteToInternalBuffer(b);
	}
	
	public final void insertBytes(byte[] bytes) {
		this.inWrapper.addBytesToInternalBuffer(bytes);
	}
	
	public final void dispose() {
		if(this.inWrapper == null) {
			return;
		}
		this.inWrapper.dispose();
		this.inWrapper = null;
		System.gc();
	}
	
	//'Pass-through' methods:
	
	@Override
	public final synchronized void close() throws IOException {
		this.dispose();
		this.socket.close();
	}
	
	@Override
	public void connect(SocketAddress endpoint) throws IOException {
		this.socket.connect(endpoint);
	}
	
	@Override
	public void connect(SocketAddress endpoint, int timeout) throws IOException {
		this.socket.connect(endpoint, timeout);
	}
	
	@Override
	public void bind(SocketAddress bindpoint) throws IOException {
		this.socket.bind(bindpoint);
	}
	
	@Override
	public InetAddress getInetAddress() {
		return this.socket.getInetAddress();
	}
	
	@Override
	public InetAddress getLocalAddress() {
		return this.socket.getLocalAddress();
	}
	
	@Override
	public int getPort() {
		return this.socket.getPort();
	}
	
	@Override
	public int getLocalPort() {
		return this.socket.getLocalPort();
	}
	
	@Override
	public SocketAddress getRemoteSocketAddress() {
		return this.socket.getRemoteSocketAddress();
	}
	
	@Override
	public SocketAddress getLocalSocketAddress() {
		return this.socket.getLocalSocketAddress();
	}
	
	@Override
	public SocketChannel getChannel() {
		return this.socket.getChannel();
	}
	
	@Override
	public final OutputStream getOutputStream() throws IOException {
		return this.socket.getOutputStream();
	}
	
	@Override
	public void setTcpNoDelay(boolean on) throws SocketException {
		this.socket.setTcpNoDelay(on);
	}
	
	@Override
	public boolean getTcpNoDelay() throws SocketException {
		return this.socket.getTcpNoDelay();
	}
	
	@Override
	public void setSoLinger(boolean on, int linger) throws SocketException {
		this.socket.setSoLinger(on, linger);
	}
	
	@Override
	public int getSoLinger() throws SocketException {
		return this.socket.getSoLinger();
	}
	
	@Override
	public void sendUrgentData(int data) throws IOException {
		this.socket.sendUrgentData(data);
	}
	
	@Override
	public void setOOBInline(boolean on) throws SocketException {
		this.socket.setOOBInline(on);
	}
	
	@Override
	public boolean getOOBInline() throws SocketException {
		return this.socket.getOOBInline();
	}
	
	@Override
	public synchronized void setSoTimeout(int timeout) throws SocketException {
		this.socket.setSoTimeout(timeout);
	}
	
	@Override
	public synchronized int getSoTimeout() throws SocketException {
		return this.socket.getSoTimeout();
	}
	
	@Override
	public synchronized void setSendBufferSize(int size) throws SocketException {
		this.socket.setSendBufferSize(size);
	}
	
	@Override
	public synchronized int getSendBufferSize() throws SocketException {
		return this.socket.getSendBufferSize();
	}
	
	@Override
	public synchronized void setReceiveBufferSize(int size) throws SocketException {
		this.socket.setReceiveBufferSize(size);
	}
	
	@Override
	public synchronized int getReceiveBufferSize() throws SocketException {
		return this.socket.getReceiveBufferSize();
	}
	
	@Override
	public void setKeepAlive(boolean on) throws SocketException {
		this.socket.setKeepAlive(on);
	}
	
	@Override
	public boolean getKeepAlive() throws SocketException {
		return this.socket.getKeepAlive();
	}
	
	@Override
	public void setTrafficClass(int tc) throws SocketException {
		this.socket.setTrafficClass(tc);
	}
	
	@Override
	public int getTrafficClass() throws SocketException {
		return this.socket.getTrafficClass();
	}
	
	@Override
	public void setReuseAddress(boolean on) throws SocketException {
		this.socket.setReuseAddress(on);
	}
	
	@Override
	public boolean getReuseAddress() throws SocketException {
		return this.socket.getReuseAddress();
	}
	
	@Override
	public void shutdownInput() throws IOException {
		this.socket.shutdownInput();
	}
	
	@Override
	public void shutdownOutput() throws IOException {
		this.socket.shutdownOutput();
	}
	
	@Override
	public String toString() {
		return this.socket.toString();
	}
	
	@Override
	public boolean isConnected() {
		return this.socket.isConnected();
	}
	
	@Override
	public boolean isBound() {
		return this.socket.isBound();
	}
	
	@Override
	public boolean isClosed() {
		return this.socket.isClosed();
	}
	
	@Override
	public boolean isInputShutdown() {
		return this.socket.isInputShutdown();
	}
	
	@Override
	public boolean isOutputShutdown() {
		return this.socket.isOutputShutdown();
	}
	
	@Override
	public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
		this.socket.setPerformancePreferences(connectionTime, latency, bandwidth);
	}
	
}
