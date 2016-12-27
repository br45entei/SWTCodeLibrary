package com.gmail.br45entei.util;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

/** @author Brian_Entei */
public class ThreadedSecureRandom {
	
	private static final ConcurrentHashMap<Thread, SecureRandom> threadedRandoms = new ConcurrentHashMap<>();
	
	/** @return The current thread's SecureRandom object */
	public static final SecureRandom get() {
		Thread thread = Thread.currentThread();
		SecureRandom random = threadedRandoms.get(thread);
		if(random == null) {
			random = new SecureRandom();
			threadedRandoms.put(thread, random);
		}
		return random;
	}
	
	/** @return A securely generated random string */
	public static final String nextSessionId() {
		return new BigInteger(130, get()).toString(32);
	}
	
}
