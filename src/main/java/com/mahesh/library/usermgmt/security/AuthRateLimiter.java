package com.mahesh.library.usermgmt.security;

import com.mahesh.library.usermgmt.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/** A small per-process fixed-window limiter for the public authentication endpoints. */
@Component
public class AuthRateLimiter {

	private final int maxRequests;

	private final Duration window;

	private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

	public AuthRateLimiter(@Value("${app.security.auth-rate-limit-max-requests:20}") int maxRequests,
			@Value("${app.security.auth-rate-limit-window-ms:60000}") long windowMs) {
		if (maxRequests < 1 || windowMs < 1)
			throw new IllegalArgumentException("Authentication rate-limit settings must be positive");
		this.maxRequests = maxRequests;
		this.window = Duration.ofMillis(windowMs);
	}

	public void check(String endpoint, String clientAddress) {
		String key = endpoint + ':' + clientAddress;
		Window window = windows.computeIfAbsent(key, ignored -> new Window());
		if (!window.tryConsume())
			throw new RateLimitExceededException();
	}

	private final class Window {

		private Instant started = Instant.now();

		private int requests;

		synchronized boolean tryConsume() {
			Instant now = Instant.now();
			if (!now.isBefore(started.plus(window))) {
				started = now;
				requests = 0;
			}
			return ++requests <= maxRequests;
		}

	}

}
