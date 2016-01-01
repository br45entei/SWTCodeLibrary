package com.gmail.br45entei.data;

import com.gmail.br45entei.util.StringUtil;

import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/** @author <a href="https://gist.github.com/usamadar/2912088">Usama
 *         Dar(munir.usama@gmail.com)</a>
 * @author Brian_Entei */
public final class HttpDigestAuthorization {
	
	private static final String									qop					= "auth-int";					//auth-int
	private static final ArrayList<String>						usedNonces			= new ArrayList<>();
	
	private final String										userName;
	private final String										password;
	private final String										realm;
	
	protected volatile String									nonce;												//TODO Make nonces per-client and only use one instance of this class per realm+creds through all the threads in JavaWebServer
	protected static final ConcurrentHashMap<String, String>	authorizedCookies	= new ConcurrentHashMap<>();
	
	//private final ScheduledExecutorService	nonceRefreshExecutor;
	
	static {
		Security.addProvider(new BouncyCastleProvider());
	}
	
	/** @param realm The HTTP Authentication Realm
	 * @param username The user's username
	 * @param password The user's plain text password(ugh.) */
	public HttpDigestAuthorization(String realm, String username, String password) {
		this.realm = realm;
		this.userName = username;
		this.password = password;
		this.nonce = calculateNonce();//"16b1b890cc254d5ff3109cd3a4a12048";
		/*this.nonceRefreshExecutor = Executors.newScheduledThreadPool(1);
		
		this.nonceRefreshExecutor.scheduleAtFixedRate(new Runnable() {
			
			@Override
			public void run() {
				HttpDigestAuthorization.this.nonce = calculateNonce();
			}
		}, 1, 1, TimeUnit.MINUTES);*/
	}
	
	/** Calculate the nonce based on current time-stamp up to the second, and a
	 * random seed
	 *
	 * @return The calculated nonce */
	public static final String calculateNonce() {
		return DigestUtils.md5Hex(new SimpleDateFormat("yyyy:MM:dd:hh:mm:ss").format(new Date()) + Integer.valueOf(new Random(100000).nextInt()).toString());
	}
	
	private static final String getAuthorizedCookieFor(String clientIP) {
		return authorizedCookies.get(clientIP);
	}
	
	private static final void setAuthorizedCookieFor(String clientIP, String cookieData) {
		if(cookieData == null) {
			authorizedCookies.remove(clientIP);//Prevents NPE when putting null objects into concurrent hash maps.
		} else {
			authorizedCookies.put(clientIP, cookieData);
		}
	}
	
	/** @return The result of the authentication
	 * @param authHeader The Authentication header sent by the client
	 * @param requestBody The HTTP request body(if any, set to empty string if
	 *            null) sent by the client
	 * @param httpMethod The HTTP method(GET, HEAD, POST, etc.) used by the
	 *            client
	 * @param cookieHeaders The cookies that the client sent, if any(must not be
	 *            null) */
	public final AuthorizationResult authenticate2(final String authHeader, String requestBody, String httpMethod, String domain, String clientIP, ArrayList<String> cookieHeaders) {
		final String authorizedCookie = getAuthorizedCookieFor(clientIP);
		if(StringUtils.isBlank(authHeader)) {
			return new AuthorizationResult(false, false, "WWW-Authenticate: " + this.getAuthenticateHeader(), null, domain, clientIP, "Authorization required");
		}
		final String authType = authHeader.split(Pattern.quote(" "))[0];
		if(authType.equals("Digest")) {
			if(authorizedCookie != null && !authorizedCookie.isEmpty()) {//Allow session logins
				for(String cookie : cookieHeaders) {
					if(cookie != null && !cookie.isEmpty()) {
						String[] split = cookie.split(Pattern.quote("="));
						if(split.length > 1) {
							String pname = split[0];
							String pvalue = StringUtil.stringArrayToString(split, '=', 1);
							if(pname.equalsIgnoreCase("auth")) {
								if(authorizedCookie.equals(pvalue)) {
									return new AuthorizationResult(true, true, null, authorizedCookie, domain, clientIP, "Login successful.");
								}
							}
						}
					}
				}
			}
			
			// parse the values of the Authentication header into a hashmap
			HashMap<String, String> headerValues = parseHeader(authHeader);
			
			//System.out.println("=============================================");
			//System.out.println("(ha1): (" + this.userName + ":" + this.realm + ":" + this.password + ");");
			String ha1 = DigestUtils.md5Hex(this.userName + ":" + this.realm + ":" + this.password);
			//System.out.println("ha1: \"" + ha1 + "\";");
			String qop = headerValues.get("qop");
			//System.out.println("qop: \"" + qop + "\";");
			String reqURI = headerValues.get("uri");
			//System.out.println("reqURI: \"" + reqURI + "\";");
			
			String ha2;
			if("auth-int".equals(qop)) {
				String entityBodyMd5 = DigestUtils.md5Hex(requestBody);
				//	System.out.println("(ha2[0]): \"" + (httpMethod + ":" + reqURI + ":" + entityBodyMd5) + "\";");
				ha2 = DigestUtils.md5Hex(httpMethod + ":" + reqURI + ":" + entityBodyMd5);
			} else {
				//	System.out.println("(ha2[1]): \"" + (httpMethod + ":" + reqURI) + "\";");
				ha2 = DigestUtils.md5Hex(httpMethod + ":" + reqURI);
			}
			//System.out.println("ha2: \"" + ha2 + "\";");
			
			String serverResponse;
			//String clientRealm = headerValues.get("realm");
			
			if(StringUtils.isBlank(qop)) {
				//	System.out.println("(serverResponse): \"" + (ha1 + ":" + this.nonce + ":" + ha2) + "\";");
				serverResponse = DigestUtils.md5Hex(ha1 + ":" + this.nonce + ":" + ha2);
			} else {
				String nonceCount = headerValues.get("nc");
				String clientNonce = headerValues.get("cnonce");
				//	System.out.println("(serverResponse): \"" + (ha1 + ":" + this.nonce + ":" + nonceCount + ":" + clientNonce + ":" + qop + ":" + ha2) + "\";");
				
				serverResponse = DigestUtils.md5Hex(ha1 + ":" + this.nonce + ":" + nonceCount + ":" + clientNonce + ":" + qop + ":" + ha2);
				
			}
			String clientResponse = headerValues.get("response");
			
			//System.out.println("Server Response: \"" + serverResponse + "\";");
			//System.out.println("Client Response: \"" + clientResponse + "\";");
			//System.out.println("=============================================");
			
			if(!serverResponse.equals(clientResponse)) {
				this.nonce = calculateNonce();
				setAuthorizedCookieFor(clientIP, null);
				return new AuthorizationResult(false, true, "WWW-Authenticate: " + this.getAuthenticateHeader(), null, domain, clientIP, "Authentication failure: Unknown username or bad password.");
			}
			setAuthorizedCookieFor(clientIP, StringUtil.nextSessionId());
			return new AuthorizationResult(true, true, null, getAuthorizedCookieFor(clientIP), domain, clientIP, "Login successful.");
		}
		this.nonce = calculateNonce();
		setAuthorizedCookieFor(clientIP, null);
		return new AuthorizationResult(false, false, "WWW-Authenticate: " + this.getAuthenticateHeader(), null, domain, clientIP, "Digest Authorization expected, received \"" + authType + "\".");
	}
	
	public final String getAuthenticateHeader() {
		return "Digest realm=\"" + this.realm + "\",qop=" + qop + ",nonce=\"" + this.nonce + "\",opaque=\"" + getOpaque(this.realm, this.nonce) + "\"";
	}
	
	private static final String getOpaque(String realm, String nonce) {
		return DigestUtils.md5Hex(realm + nonce);
	}
	
	/** Gets the Authorization header string minus the "AuthType" and returns a
	 * hashMap of keys and values
	 *
	 * @param headerString
	 * @return */
	private static final HashMap<String, String> parseHeader(String headerString) {
		String headerStringWithoutScheme = headerString.substring(headerString.indexOf(" ") + 1).trim();
		HashMap<String, String> values = new HashMap<>();
		String[] split = headerStringWithoutScheme.split(",");
		for(String param : split) {
			String[] entry = param.split(Pattern.quote("="));
			String key = entry[0];
			String value = "";
			if(entry.length > 1) {
				value = StringUtil.stringArrayToString(entry, '=', 1);
			}
			values.put(key.trim(), value.replaceAll("\"", "").trim());
		}
		return values;
	}
	
	/** @author Brian_Entei */
	public static final class AuthorizationResult {
		private final boolean	passed;
		/** True if the client used the correct authentication header type(Basic,
		 * <em>Digest</em>, etc.) */
		public final boolean	requestUsedCorrectHeader;
		/** The header(may be null) that will need to be returned to the client
		 * if authentication failed */
		public final String		resultingAuthenticationHeader;
		/** Cookie created after a successful login to allow a client to log in
		 * after a time out(resets on the next failed authentication) */
		public final String		authorizedCookie;
		/** The domain on which the authorization attempt took place */
		public final String		domain;
		/** The ip address of the client that attempted the authorization */
		public final String		clientIP;
		/** Status message for the authentication attempt */
		public final String		message;
		
		protected AuthorizationResult(boolean passed, boolean requestUsedCorrectHeader, String resultingAuthenticationHeader, String authorizedCookie, String domain, String clientIP, String message) {
			this.passed = passed;
			this.requestUsedCorrectHeader = requestUsedCorrectHeader;
			this.resultingAuthenticationHeader = resultingAuthenticationHeader;
			this.authorizedCookie = "auth=" + authorizedCookie + "; Domain=" + domain + "; Path=/; HttpOnly";
			this.domain = domain;
			this.clientIP = clientIP;
			this.message = message;
		}
		
		/** @return True if the client provided the correct username and password */
		public final boolean passed() {
			return this.passed ? this.requestUsedCorrectHeader : false;
		}
		
	}
	
}
