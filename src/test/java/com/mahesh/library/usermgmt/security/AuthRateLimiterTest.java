package com.mahesh.library.usermgmt.security;

import com.mahesh.library.usermgmt.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthRateLimiterTest {

	@Test
	void rejectsRequestsAfterTheConfiguredWindowBudget() {
		AuthRateLimiter limiter = new AuthRateLimiter(2, 60_000);

		limiter.check("login", "198.51.100.10");
		limiter.check("login", "198.51.100.10");

		assertThrows(RateLimitExceededException.class, () -> limiter.check("login", "198.51.100.10"));
	}

	@Test
	void keepsClientsAndEndpointsOnSeparateBudgets() {
		AuthRateLimiter limiter = new AuthRateLimiter(1, 60_000);

		limiter.check("login", "198.51.100.10");
		limiter.check("refresh", "198.51.100.10");
		limiter.check("login", "198.51.100.11");
	}

}
