package com.oussema.cloud.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FileValidatorConstraint.class)
public @interface ValidFile {

    String message() default "Invalid file. Check file type or content.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String[] allowedTypes() default {};
}