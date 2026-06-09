package com.sdpm.workitem.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

public class EnumValueValidator implements ConstraintValidator<EnumValue, String> {

    private Class<? extends Enum<?>> enumClass;
    private boolean useCode;
    private String allowedValues;

    @Override
    public void initialize(EnumValue constraintAnnotation) {
        this.enumClass = constraintAnnotation.enumClass();
        this.useCode = constraintAnnotation.useCode();
        this.allowedValues = Arrays.stream(enumClass.getEnumConstants())
                .map(this::extract)
                .collect(Collectors.joining(", "));
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        boolean valid = Arrays.stream(enumClass.getEnumConstants())
                .map(this::extract)
                .anyMatch(v -> v.equals(value));
        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "值 " + value + " 不在允许范围内，允许值: " + allowedValues)
                .addConstraintViolation();
        }
        return valid;
    }

    private String extract(Enum<?> e) {
        if (useCode) {
            try {
                Method m = e.getClass().getMethod("getCode");
                Object code = m.invoke(e);
                return code == null ? null : code.toString();
            } catch (Exception ex) {
                return e.name();
            }
        }
        return e.name();
    }
}
