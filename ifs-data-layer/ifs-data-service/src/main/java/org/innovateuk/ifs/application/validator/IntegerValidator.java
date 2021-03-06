package org.innovateuk.ifs.application.validator;

import org.innovateuk.ifs.application.domain.FormInputResponse;
import org.springframework.validation.Errors;

import java.math.BigDecimal;

import static java.lang.Long.MAX_VALUE;
import static java.lang.Long.MIN_VALUE;
import static java.math.BigDecimal.valueOf;
import static org.innovateuk.ifs.commons.error.ValidationMessages.rejectValue;

public abstract class IntegerValidator extends BaseValidator {

    @Override
    public final void validate(Object target, Errors errors) {
        FormInputResponse response = (FormInputResponse) target;
        String responseValue = response.getValue();
        BigDecimal value = toBigDecimal(responseValue);
        if (value == null){
            rejectValue(errors, "value", "validation.field.must.not.be.blank");
        } else {
            if (fractionalPartLength(value) > 0){
                rejectValue(errors, "value", "validation.standard.integer.non.decimal.format");
            }
            if (value.compareTo(valueOf(MAX_VALUE)) > 0){
                rejectValue(errors, "value", "validation.standard.integer.max.value.format");
            }
            if (value.compareTo(valueOf(MIN_VALUE)) < 0){
                rejectValue(errors, "value", "validation.standard.integer.min.value.format");
            }

            validate(value, errors);
        }
    }

    protected abstract void validate(BigDecimal bd, Errors errors);

    private BigDecimal toBigDecimal(String value) {
        try {
            return new BigDecimal(value);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private int fractionalPartLength(BigDecimal value){
        int fractionPartLength = value.scale() < 0 ? 0 : value.scale();
        return fractionPartLength;
    }
}
