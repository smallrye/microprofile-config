package io.smallrye.config.validator;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

public class BeanValidationConfigValidatorImpl implements BeanValidationConfigValidator {
    private Validator validator;

    public BeanValidationConfigValidatorImpl() {
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Override
    public Validator getValidator() {
        return validator;
    }
}
