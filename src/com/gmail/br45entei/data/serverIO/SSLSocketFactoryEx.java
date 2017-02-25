package com.gmail.br45entei.data.serverIO;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

@SuppressWarnings("javadoc")
public final class SSLSocketFactoryEx extends SSLSocketFactory {
	
	public static final void main(String[] args) throws NoSuchAlgorithmException {
		SSLContext context = SSLContext.getDefault();
		SSLSocketFactory sf = context.getSocketFactory();
		String[] cipherSuites = sf.getSupportedCipherSuites();
		for(String cipher : cipherSuites) {
			System.out.println("\t\t\t\t\"" + cipher + "\",//");
		}
	}
	
	private static volatile SSLSocketFactoryEx instance;
	
	public static final synchronized SSLSocketFactoryEx getDefault() {
		if(instance == null) {
			try {
				instance = new SSLSocketFactoryEx(null, null, new SecureRandom());
			} catch(KeyManagementException | NoSuchAlgorithmException e) {
				throw new Error(e);
			}
		}
		return instance;
	}
	
	private SSLContext m_ctx;
	
	private String[] m_ciphers;
	private String[] m_protocols;
	
	public SSLSocketFactoryEx() throws NoSuchAlgorithmException, KeyManagementException {
		initSSLSocketFactoryEx(null, null, null);
	}
	
	public SSLSocketFactoryEx(KeyManager[] km, TrustManager[] tm, SecureRandom random) throws NoSuchAlgorithmException, KeyManagementException {
		initSSLSocketFactoryEx(km, tm, random);
	}
	
	public SSLSocketFactoryEx(SSLContext ctx) {
		initSSLSocketFactoryEx(ctx);
	}
	
	@Override
	public String[] getDefaultCipherSuites() {
		return this.m_ciphers;
	}
	
	@Override
	public String[] getSupportedCipherSuites() {
		return this.m_ciphers;
	}
	
	public String[] getDefaultProtocols() {
		return this.m_protocols;
	}
	
	public String[] getSupportedProtocols() {
		return this.m_protocols;
	}
	
	@Override
	public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
		SSLSocketFactory factory = this.m_ctx.getSocketFactory();
		SSLSocket ss = (SSLSocket) factory.createSocket(s, host, port, autoClose);
		
		ss.setEnabledProtocols(this.m_protocols);
		ss.setEnabledCipherSuites(this.m_ciphers);
		
		return ss;
	}
	
	@Override
	public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
		SSLSocketFactory factory = this.m_ctx.getSocketFactory();
		SSLSocket ss = (SSLSocket) factory.createSocket(address, port, localAddress, localPort);
		
		ss.setEnabledProtocols(this.m_protocols);
		ss.setEnabledCipherSuites(this.m_ciphers);
		
		return ss;
	}
	
	@Override
	public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
		SSLSocketFactory factory = this.m_ctx.getSocketFactory();
		SSLSocket ss = (SSLSocket) factory.createSocket(host, port, localHost, localPort);
		
		ss.setEnabledProtocols(this.m_protocols);
		ss.setEnabledCipherSuites(this.m_ciphers);
		
		return ss;
	}
	
	@Override
	public Socket createSocket(InetAddress host, int port) throws IOException {
		SSLSocketFactory factory = this.m_ctx.getSocketFactory();
		SSLSocket ss = (SSLSocket) factory.createSocket(host, port);
		
		ss.setEnabledProtocols(this.m_protocols);
		ss.setEnabledCipherSuites(this.m_ciphers);
		
		return ss;
	}
	
	@Override
	public Socket createSocket(String host, int port) throws IOException {
		SSLSocketFactory factory = this.m_ctx.getSocketFactory();
		SSLSocket ss = (SSLSocket) factory.createSocket(host, port);
		
		ss.setEnabledProtocols(this.m_protocols);
		ss.setEnabledCipherSuites(this.m_ciphers);
		
		return ss;
	}
	
	private void initSSLSocketFactoryEx(KeyManager[] km, TrustManager[] tm, SecureRandom random) throws NoSuchAlgorithmException, KeyManagementException {
		this.m_ctx = SSLContext.getInstance("TLSv1.2");
		this.m_ctx.init(km, tm, random);
		
		this.m_protocols = GetProtocolList();
		this.m_ciphers = GetCipherList();
	}
	
	private void initSSLSocketFactoryEx(SSLContext ctx) {
		this.m_ctx = ctx;
		
		this.m_protocols = GetProtocolList();
		this.m_ciphers = GetCipherList();
	}
	
	protected String[] GetProtocolList() {
		String[] preferredProtocols = {"TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"};
		String[] availableProtocols = null;
		
		SSLSocket socket = null;
		
		try {
			SSLSocketFactory factory = this.m_ctx.getSocketFactory();
			socket = (SSLSocket) factory.createSocket();
			
			availableProtocols = socket.getSupportedProtocols();
			Arrays.sort(availableProtocols);
		} catch(Exception e) {
			return new String[] {"TLSv1"};
		} finally {
			if(socket != null) try {
				socket.close();
			} catch(IOException ignored) {
			}
		}
		
		List<String> aa = new ArrayList<>();
		for(int i = 0; i < preferredProtocols.length; i++) {
			int idx = Arrays.binarySearch(availableProtocols, preferredProtocols[i]);
			if(idx >= 0) aa.add(preferredProtocols[i]);
		}
		
		return aa.toArray(new String[0]);
	}
	
	protected String[] GetCipherList() {
		/*String[] preferredCiphers = {
				
				// *_CHACHA20_POLY1305 are 3x to 4x faster than existing cipher suites.
				//   http://googleonlinesecurity.blogspot.com/2014/04/speeding-up-and-strengthening-https.html
				// Use them if available. Normative names can be found at (TLS spec depends on IPSec spec):
				//   http://tools.ietf.org/html/draft-nir-ipsecme-chacha20-poly1305-01
				//   http://tools.ietf.org/html/draft-mavrogiannopoulos-chacha-tls-02
				"TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305", //
				"TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305", //
				"TLS_ECDHE_ECDSA_WITH_CHACHA20_SHA", //
				"TLS_ECDHE_RSA_WITH_CHACHA20_SHA",
				
				"TLS_DHE_RSA_WITH_CHACHA20_POLY1305", //
				"TLS_RSA_WITH_CHACHA20_POLY1305", //
				"TLS_DHE_RSA_WITH_CHACHA20_SHA", //
				"TLS_RSA_WITH_CHACHA20_SHA",
				
				// Done with bleeding edge, back to TLS v1.2 and below
				"TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384", //
				"TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384", //
				"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256", //
				"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
				
				"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", //
				"TLS_DHE_DSS_WITH_AES_256_GCM_SHA384", //
				"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", //
				"TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",
				
				// TLS v1.0 (with some SSLv3 interop)
				"TLS_DHE_RSA_WITH_AES_256_CBC_SHA384", //
				"TLS_DHE_DSS_WITH_AES_256_CBC_SHA256", //
				//"TLS_DHE_RSA_WITH_AES_128_CBC_SHA", //
				"TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
				
				//"TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA", //
				"TLS_DHE_DSS_WITH_3DES_EDE_CBC_SHA", //
				"SSL_DH_RSA_WITH_3DES_EDE_CBC_SHA", //
				"SSL_DH_DSS_WITH_3DES_EDE_CBC_SHA",
				
				// RSA key transport sucks, but they are needed as a fallback.
				// For example, microsoft.com fails under all versions of TLS
				// if they are not included. If only TLS 1.0 is available at
				// the client, then google.com will fail too. TLS v1.3 is
				// trying to deprecate them, so it will be interesteng to see
				// what happens.
				"TLS_RSA_WITH_AES_256_CBC_SHA256", //
				"TLS_RSA_WITH_AES_256_CBC_SHA", //
				"TLS_RSA_WITH_AES_128_CBC_SHA256", //
				"TLS_RSA_WITH_AES_128_CBC_SHA"//
		};*/
		
		String[] preferredCiphers = {"TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA", //weak
				"TLS_DHE_DSS_WITH_AES_128_CBC_SHA", //
				"TLS_DHE_DSS_WITH_AES_256_CBC_SHA", //
				"TLS_DHE_RSA_WITH_AES_256_CBC_SHA", //
				"TLS_RSA_WITH_AES_256_CBC_SHA256", //
				"TLS_RSA_WITH_AES_256_CBC_SHA", //
				"TLS_RSA_WITH_AES_128_CBC_SHA256", //
				"TLS_RSA_WITH_AES_128_CBC_SHA", //
				"TLS_EMPTY_RENEGOTIATION_INFO_SCSV", //
				//
				"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256", //
				"TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", //
				"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA", //
				"TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA", //
				"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", //
				"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", //
				"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", //
				"TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA", //
				"TLS_RSA_WITH_3DES_EDE_CBC_SHA", //added manually
				"TLS_RSA_WITH_AES_128_CBC_SHA256", //
				"TLS_RSA_WITH_AES_128_CBC_SHA", //added manually
				"TLS_RSA_WITH_AES_128_GCM_SHA256", //added manually
				"TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256", //
				"TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256", //
				"TLS_DHE_RSA_WITH_AES_128_CBC_SHA256", //weak
				"TLS_DHE_DSS_WITH_AES_128_CBC_SHA256", //
				"TLS_RSA_WITH_AES_128_CBC_SHA", //
				"TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA", //
				"TLS_ECDH_RSA_WITH_AES_128_CBC_SHA", //
				"TLS_DHE_RSA_WITH_AES_128_CBC_SHA", //weak
				"TLS_DHE_DSS_WITH_AES_128_CBC_SHA", //
				"TLS_RSA_WITH_AES_128_GCM_SHA256", //
				"TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256", //
				"TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256", //
				"TLS_DHE_RSA_WITH_AES_128_GCM_SHA256", //weak
				"TLS_DHE_DSS_WITH_AES_128_GCM_SHA256", //
				"SSL_RSA_WITH_3DES_EDE_CBC_SHA", //
				"TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA", //
				"TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA", //
				"SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA", //
				"SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA", //
				"TLS_ECDHE_ECDSA_WITH_RC4_128_SHA", //
				"TLS_ECDHE_RSA_WITH_RC4_128_SHA", //
				"SSL_RSA_WITH_RC4_128_SHA", //
				"TLS_ECDH_ECDSA_WITH_RC4_128_SHA", //
				"TLS_ECDH_RSA_WITH_RC4_128_SHA", //
				"SSL_RSA_WITH_RC4_128_MD5", //
				"TLS_EMPTY_RENEGOTIATION_INFO_SCSV", //
				"TLS_DH_anon_WITH_AES_128_GCM_SHA256", //
				"TLS_DH_anon_WITH_AES_128_CBC_SHA256", //
				"TLS_ECDH_anon_WITH_AES_128_CBC_SHA", //
				"TLS_DH_anon_WITH_AES_128_CBC_SHA", //
				"TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA", //
				"SSL_DH_anon_WITH_3DES_EDE_CBC_SHA", //
				"TLS_ECDH_anon_WITH_RC4_128_SHA", //
				"SSL_DH_anon_WITH_RC4_128_MD5", //
				"SSL_RSA_WITH_DES_CBC_SHA", //
				"SSL_DHE_RSA_WITH_DES_CBC_SHA", //
				"SSL_DHE_DSS_WITH_DES_CBC_SHA", //
				"SSL_DH_anon_WITH_DES_CBC_SHA", //
				"SSL_RSA_EXPORT_WITH_DES40_CBC_SHA", //
				"SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", //
				"SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA", //
				"SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA", //
				"SSL_RSA_EXPORT_WITH_RC4_40_MD5", //
				"SSL_DH_anon_EXPORT_WITH_RC4_40_MD5", //
				"TLS_RSA_WITH_NULL_SHA256", //
				"TLS_ECDHE_ECDSA_WITH_NULL_SHA", //
				"TLS_ECDHE_RSA_WITH_NULL_SHA", //
				"SSL_RSA_WITH_NULL_SHA", //
				"TLS_ECDH_ECDSA_WITH_NULL_SHA", //
				"TLS_ECDH_RSA_WITH_NULL_SHA", //
				"TLS_ECDH_anon_WITH_NULL_SHA", //
				"SSL_RSA_WITH_NULL_MD5", //
				"TLS_KRB5_WITH_3DES_EDE_CBC_SHA", //
				"TLS_KRB5_WITH_3DES_EDE_CBC_MD5", //
				"TLS_KRB5_WITH_RC4_128_SHA", //
				"TLS_KRB5_WITH_RC4_128_MD5", //
				"TLS_KRB5_WITH_DES_CBC_SHA", //
				"TLS_KRB5_WITH_DES_CBC_MD5", //
				"TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA", //
				"TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5", //
				"TLS_KRB5_EXPORT_WITH_RC4_40_SHA", //
				"TLS_KRB5_EXPORT_WITH_RC4_40_MD5"};
		
		String[] availableCiphers = null;
		
		try {
			SSLSocketFactory factory = this.m_ctx.getSocketFactory();
			availableCiphers = factory.getSupportedCipherSuites();
			Arrays.sort(availableCiphers);
		} catch(Exception e) {
			return new String[] {//
					"TLS_DHE_DSS_WITH_AES_128_CBC_SHA", //
					"TLS_DHE_DSS_WITH_AES_256_CBC_SHA", //
					//"TLS_DHE_RSA_WITH_AES_128_CBC_SHA", //weak
					"TLS_DHE_RSA_WITH_AES_256_CBC_SHA", //
					"TLS_RSA_WITH_AES_256_CBC_SHA256", //
					"TLS_RSA_WITH_AES_256_CBC_SHA", //
					"TLS_RSA_WITH_AES_128_CBC_SHA256", //
					"TLS_RSA_WITH_AES_128_CBC_SHA", //
					"TLS_EMPTY_RENEGOTIATION_INFO_SCSV"};
		}
		
		List<String> aa = new ArrayList<>();
		for(int i = 0; i < preferredCiphers.length; i++) {
			int idx = Arrays.binarySearch(availableCiphers, preferredCiphers[i]);
			if(idx >= 0) aa.add(preferredCiphers[i]);
		}
		
		aa.add("TLS_EMPTY_RENEGOTIATION_INFO_SCSV");
		/*for(String cipher : aa) {
			PrintUtil.println(cipher);
		}
		PrintUtil.printToConsole();*/
		return aa.toArray(new String[0]);
	}
	
}