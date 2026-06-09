package com.sdpm.workitem.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 校验字段值在指定的枚举 code 集合内（用于替代 DTO 上裸 String 字段）
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EnumValueValidator.class)
public @interface EnumValue {
    String message() default "值不在允许的枚举范围内";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    Class<? extends Enum<?>> enumClass();

    /**
     * 是否使用枚举的 code() 方法取值（true=按 code 字符串匹配；false=按枚举 name() 匹配）
     */
    boolean useCode() default true;
}
