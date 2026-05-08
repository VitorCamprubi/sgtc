package com.vitorcamprubi.sgtc.security.password;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final int MIN_LENGTH = 8;
    private boolean allowBlank;

    @Override
    public void initialize(StrongPassword constraint) {
        this.allowBlank = constraint.allowBlank();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return allowBlank;
        }
        if (value.length() < MIN_LENGTH) {
            return false;
        }
        boolean temLetra = false;
        boolean temDigito = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isLetter(c)) {
                temLetra = true;
            } else if (Character.isDigit(c)) {
                temDigito = true;
            }
            if (temLetra && temDigito) {
                return true;
            }
        }
        return false;
    }
}
