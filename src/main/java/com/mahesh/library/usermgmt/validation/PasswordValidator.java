package com.mahesh.library.usermgmt.validation;

import com.mahesh.library.usermgmt.dto.ValidationRules;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class PasswordValidator implements ConstraintValidator<Password, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return value != null && value.matches(ValidationRules.PASSWORD)
				&& value.getBytes(StandardCharsets.UTF_8).length <= ValidationRules.BCRYPT_MAX_BYTES;
	}

}
