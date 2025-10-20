package com.katynova.resto.common_dto_library.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;

public class StartTimeValidator implements ConstraintValidator<CorrectStartTime, LocalDateTime> {
    @Override
    public boolean isValid(LocalDateTime startTime, ConstraintValidatorContext constraintValidatorContext) {
        return startTime.toLocalTime().getMinute() % 30 == 0;
    }
}
