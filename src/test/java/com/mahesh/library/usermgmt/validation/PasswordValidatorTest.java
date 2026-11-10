package com.mahesh.library.usermgmt.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordValidatorTest {

	private Validator validator;

	@BeforeEach
	void setUp() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void rejectsPasswordsThatFitTheCharacterLimitButExceedBcryptBytes() {
		String password = "Aa1!" + "é".repeat(35);

		assertFalse(validator.validate(new PasswordRequest(password)).isEmpty());
	}

	@Test
	void acceptsAValidAsciiPasswordWithinTheBcryptLimit() {
		assertTrue(validator.validate(new PasswordRequest("StrongPass1!")).isEmpty());
	}

	private record PasswordRequest(@Password String password) {
	}

}
