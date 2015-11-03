package com.gmail.br45entei.data;

import com.gmail.br45entei.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

/** @author <a href="https://gist.github.com/usamadar/2912088">Usama
 *         Dar(munir.usama@gmail.com)</a> */
public final class HttpDigestAuthorization {
	
	private static final ArrayList<String>	usedNonces	= new ArrayList<>();
	
	private final String					userName;
	private final String					password;
	private final String					realm;
	private final String					authHeader;
	private final String					requestBody;
	private final String					httpMethod;
	
	private final String					nonce;
	
	/** @param realm The HTTP Authentication Realm
	 * @param username The user's username
	 * @param password The user's plain text password(ugh.)
	 * @param authHeader The Authentication header sent by the client
	 * @param requestBody The HTTP request body(if any, set to empty string if
	 *            null) sent by the client
	 * @param httpMethod The HTTP method(GET, HEAD, POST, etc.) used by the
	 *            client */
	public HttpDigestAuthorization(String realm, String username, String password, String authHeader, final String requestBody, final String httpMethod) {
		this.realm = realm;
		this.userName = username;
		this.password = password;
		this.authHeader = authHeader;
		this.requestBody = requestBody == null ? "" : requestBody;
		this.httpMethod = httpMethod;
		this.nonce = nextNonce();
	}
	
	private static final String nextNonce() {
		String nonce = StringUtil.nextSessionId();
		while(usedNonces.contains(nonce)) {
			nonce = StringUtil.nextSessionId();
		}
		usedNonces.add(nonce);
		return nonce;
	}
	
	/** @return The result of the authentication */
	public final AuthorizationResult authenticate() {
		if(StringUtils.isBlank(this.authHeader)) {
			return new AuthorizationResult(false, false, "WWW-Authenticate: " + this.getAuthenticateHeader(), "Authorization required.");
		}
		if(this.authHeader.startsWith("Digest")) {
			// parse the values of the Authentication header into a hashmap
			HashMap<String, String> headerValues = parseHeader(this.authHeader);
			
			String ha1 = DigestUtils.md5Hex(this.userName + ":" + this.realm + ":" + this.password);
			String qop = headerValues.get("qop");
			String reqURI = headerValues.get("uri");
			
			String ha2;
			if(!StringUtils.isBlank(qop) && qop.equals("auth-int")) {
				String entityBodyMd5 = DigestUtils.md5Hex(this.requestBody);
				ha2 = DigestUtils.md5Hex(this.httpMethod + ":" + reqURI + ":" + entityBodyMd5);
			} else {
				ha2 = DigestUtils.md5Hex(this.httpMethod + ":" + reqURI);
			}
			
			String serverResponse;
			String clientRealm = headerValues.get("realm");
			
			if(StringUtils.isBlank(qop)) {
				serverResponse = DigestUtils.md5Hex(ha1 + ":" + this.nonce + ":" + ha2);
			} else {
				String nonceCount = headerValues.get("nc");
				String clientNonce = headerValues.get("cnonce");
				
				serverResponse = DigestUtils.md5Hex(ha1 + ":" + this.nonce + ":" + nonceCount + ":" + clientNonce + ":" + qop + ":" + ha2);
				
			}
			String clientResponse = headerValues.get("response");
			
			if(!serverResponse.equals(clientResponse) || !this.realm.equals(clientRealm)) {
				return new AuthorizationResult(false, true, "WWW-Authenticate: " + this.getAuthenticateHeader(), "Authentication failure: Unknown username or bad password.");
			}
			return new AuthorizationResult(true, true, null, "Login successful.");
		}
		return new AuthorizationResult(false, false, null, "Digest Authorization expected, received \"" + this.authHeader.split(Pattern.quote(" "))[0] + "\".");
	}
	
	private final String getAuthenticateHeader() {
		return "Digest realm=\"" + this.realm + "\",qop=auth,nonce=\"" + this.nonce + "\",opaque=\"" + getOpaque(this.realm, this.nonce) + "\"";
	}
	
	private static final String getOpaque(String domain, String nonce) {
		return DigestUtils.md5Hex(domain + nonce);
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
		/** Status message for the authentication attempt */
		public final String		message;
		
		protected AuthorizationResult(boolean passed, boolean requestUsedCorrectHeader, String resultingAuthenticationHeader, String message) {
			this.passed = passed;
			this.requestUsedCorrectHeader = requestUsedCorrectHeader;
			this.resultingAuthenticationHeader = resultingAuthenticationHeader;
			this.message = message;
		}
		
		/** @return True if the client provided the correct username and password */
		public final boolean passed() {
			return this.passed ? this.requestUsedCorrectHeader : false;
		}
		
	}
	
}
