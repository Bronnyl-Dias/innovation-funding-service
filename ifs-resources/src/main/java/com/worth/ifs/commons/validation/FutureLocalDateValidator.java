package com.worth.ifs.commons.validation;

import com.worth.ifs.commons.validation.constraints.FutureLocalDate;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.LocalDate;

/**
 * A validator that asserts that a LocalDate value is in the future.
 */
public class FutureLocalDateValidator implements ConstraintValidator<FutureLocalDate, LocalDate> {

    private FutureLocalDate constraintAnnotation;

    @Override
    public void initialize(FutureLocalDate constraintAnnotation) {
        this.constraintAnnotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        LocalDate today = LocalDate.now();

        return value.isEqual(today) || value.isAfter(today);
    }
}