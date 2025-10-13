package com.katynova.resto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Duration;

public class DurationValidator implements ConstraintValidator<CorrectDuration, Duration> {

    @Override
    public boolean isValid(Duration duration, ConstraintValidatorContext constraintValidatorContext) {
        return duration.compareTo(Duration.ZERO) > 0 && duration.toMinutes() % 30 == 0 && duration.toHours() >= 2;
    }
}
