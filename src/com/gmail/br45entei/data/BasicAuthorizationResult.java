package com.gmail.br45entei.data;

import com.gmail.br45entei.util.StringUtil;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;

public final class BasicAuthorizationResult {
	private static final ConcurrentHashMap<String, SessionID>	sessionIDs		= new ConcurrentHashMap<>();
	private static final ConcurrentLinkedQueue<SessionID>		usedSessionIDs	= new ConcurrentLinkedQueue<>();
	
	private static final SessionID getSessionIDForClient(String clientIP, String domain) {
		return sessionIDs.get(clientIP + ":" + domain.toLowerCase().trim());
	}
	
	public static final boolean isLoggedIn(String clientIP, String domain) {
		return getSessionIDForClient(clientIP, domain) != null;
	}
	
	/** @param clientIP The client whose session id will be removed
	 * @param domain The domain
	 * @return True if the session id was not null and was removed */
	public static final boolean removeSessionIDForClient(String clientIP, String domain) {
		SessionID oldID = sessionIDs.remove(clientIP + ":" + domain.toLowerCase().trim());
		if(oldID != null) {
			usedSessionIDs.add(oldID);
			return true;
		}
		return false;
	}
	
	private static final SessionID nextSessionIDForClient(String clientIP, String domain) {
		SessionID oldID = sessionIDs.remove(clientIP + ":" + domain.toLowerCase().trim());
		if(oldID != null) {
			usedSessionIDs.add(oldID);
		}
		String id = StringUtil.nextSessionId();
		while(usedSessionIDs.contains(id)) {
			id = StringUtil.nextSessionId();
		}
		sessionIDs.put(clientIP + ":" + domain.toLowerCase().trim(), new SessionID(id));
		return getSessionIDForClient(clientIP, domain);//id;
	}
	
	private static final String[] getSessionCookieHeaderForClient(String clientIP, String domain) {
		SessionID id = getSessionIDForClient(clientIP, domain);
		return new String[] {"Set-Cookie", "auth=" + id + "; Domain=" + domain + "; Path=/; " + (id == null ? "Max-Age=0; " : "") + "HttpOnly"};
	}
	
	private static final SessionID getSessionIDFromClient(ArrayList<String> cookies) {
		for(String cookie : cookies) {
			if(cookie != null && !cookie.isEmpty()) {
				System.out.println("Cookie: \"" + cookie + "\";");
				String[] split = cookie.split(Pattern.quote("="));
				if(split.length > 1) {
					String pname = split[0].trim();
					String pvalue = StringUtil.stringArrayToString(split, '=', 1);
					System.out.println("Cookie value: " + pvalue);
					if(pname.equalsIgnoreCase("auth")) {
						if(pvalue.trim().equalsIgnoreCase("null")) {
							return null;
						}
						System.out.println("Cookie is a session id!");
						return new SessionID(pvalue);
					}
				}
			}
		}
		return null;
	}
	
	private static final boolean isSessionIDOld(SessionID sessionID) {
		if(sessionID != null) {
			for(SessionID id : usedSessionIDs) {
				if(sessionID.equals(id)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private final boolean	passed;
	public final String		resultingAuthenticationHeader;
	public final String		authorizedCookie;
	
	public static final BasicAuthorizationResult authenticateBasic(final String clientAuthHeader, String realm, String username, String password) {
		if(realm == null) {
			realm = "Authorization required";
		}
		final String authHeader = "WWW-Authenticate: Basic realm=\"" + realm + "\"";
		if(clientAuthHeader == null || clientAuthHeader.trim().isEmpty()) {
			return new BasicAuthorizationResult(false, authHeader, null);
		}
		if(username == null) {
			username = "";
		}
		if(password == null) {
			password = "";
		}
		String clientResponse;
		try {
			clientResponse = new String(Base64.decodeBase64(clientAuthHeader.replace("Basic", "").trim()));
		} catch(IllegalArgumentException ignored) {
			clientResponse = "";
		}
		String[] creds = clientResponse.split(":");
		String clientUser = creds.length == 2 ? creds[0] : "";
		String clientPass = creds.length == 2 ? creds[1] : "";
		return new BasicAuthorizationResult(username.equalsIgnoreCase(clientUser) && password.equals(clientPass), authHeader, null);
	}
	
	public static final BasicAuthorizationResult authenticateBasic(final String clientAuthHeader, String realm, String username, String password, final String clientIP, final String domain, ArrayList<String> cookies) {
		if(realm == null) {
			realm = "Authorization required";
		}
		final String clearClientCookie = "auth=null; Domain=" + domain + "; Path=/; Max-Age=0; HttpOnly";
		final String authHeader = "WWW-Authenticate: Basic realm=\"" + realm + "\"";
		SessionID id = getSessionIDForClient(clientIP, domain);
		final SessionID clientID = getSessionIDFromClient(cookies);
		if(isSessionIDOld(clientID) && id == null) {
			System.out.println("Rejected authentication with old session ID cookie!");// Correct session id is: " + id);
			nextSessionIDForClient(clientIP, domain).rejectNextAttempt = false;
			return new BasicAuthorizationResult(false, authHeader, clearClientCookie);
		}
		if(id != null) {//Returning client
			if(id.rejectNextAttempt) {
				id.rejectNextAttempt = false;
				return new BasicAuthorizationResult(false, authHeader, null);
			}
			if(clientID != null) {//Client provided a session id, let's check it
				if(id.equals(clientID)) {
					System.out.println("Authenticated with cookie!");
					id.incrementTimesUsed();
					return new BasicAuthorizationResult(true, authHeader, getSessionCookieHeaderForClient(clientIP, domain)[1]);
				}
				if(id.timesUsed() != 0) {
					System.out.println("Rejected authentication with invalid session ID cookie!");
					return new BasicAuthorizationResult(false, authHeader, clearClientCookie);
				}
			}
			if(id.timesUsed() != 0) {
				System.out.println("Rejected authentication with no session ID cookie sent!");
				nextSessionIDForClient(clientIP, domain).rejectNextAttempt = false;
				return new BasicAuthorizationResult(false, authHeader, null);
			}
		}
		
		if(clientAuthHeader == null || clientAuthHeader.trim().isEmpty()) {
			return new BasicAuthorizationResult(false, authHeader, null);
		}
		if(username == null) {
			username = "";
		}
		if(password == null) {
			password = "";
		}
		//removeSessionIDForClient(clientIP, domain);
		String clientResponse;
		try {
			clientResponse = new String(Base64.decodeBase64(clientAuthHeader.replace("Basic", "").trim()));
		} catch(IllegalArgumentException ignored) {
			clientResponse = "";
		}
		String[] creds = clientResponse.split(":");
		String clientUser = creds.length == 2 ? creds[0] : "";
		String clientPass = creds.length == 2 ? creds[1] : "";
		boolean passed = username.equalsIgnoreCase(clientUser) && password.equals(clientPass);
		if(passed) {
			if(id == null) {
				id = nextSessionIDForClient(clientIP, domain);
				id.rejectNextAttempt = false;
			} else {
				id.incrementTimesUsed();
			}
		}
		return new BasicAuthorizationResult(passed, authHeader, passed ? getSessionCookieHeaderForClient(clientIP, domain)[1] : null);
	}
	
	protected BasicAuthorizationResult(final boolean passed, final String authHeader, final String authorizedCookie) {
		this.passed = passed;
		this.resultingAuthenticationHeader = authHeader;
		this.authorizedCookie = authorizedCookie;
	}
	
	public final boolean passed() {
		return this.passed;
	}
	
	private static final class SessionID {
		
		protected volatile boolean	rejectNextAttempt	= true;
		public final String			id;
		private volatile int		timesUsed			= 0;
		
		public final SessionID incrementTimesUsed() {
			this.timesUsed++;
			return this;
		}
		
		public final int timesUsed() {
			return this.timesUsed;
		}
		
		public SessionID(String id) {
			this.id = id;
		}
		
		public final boolean equals(SessionID id) {
			return id == null ? false : this.equals(id.toString());
		}
		
		@Override
		public final boolean equals(Object obj) {
			if(obj instanceof SessionID) {//Shouldn't happen, but whatever
				return this.id.equals(((SessionID) obj).toString());
			}
			return this.id.equals(obj);
		}
		
		@Override
		public final int hashCode() {
			return this.id.hashCode();
		}
		
		@Override
		public final String toString() {
			return this.id;
		}
		
	}
	
}
