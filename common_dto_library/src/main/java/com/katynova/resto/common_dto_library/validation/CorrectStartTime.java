package com.katynova.resto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Constraint(validatedBy = StartTimeValidator.class)
public @interface CorrectStartTime {
    String message() default "Время начала должно быть кратно 30 минутам";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
