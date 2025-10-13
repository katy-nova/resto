package com.katynova.resto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Constraint(validatedBy = DurationValidator.class)
public @interface CorrectDuration {
    String message() default "Длительность бронирования должна быть кратна 30 минутам." +
            " Минимальная длительность бронирования 2 часа, максимальная 4 часа." +
            " Для более длительного бронирования свяжитесь с менеджером.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
