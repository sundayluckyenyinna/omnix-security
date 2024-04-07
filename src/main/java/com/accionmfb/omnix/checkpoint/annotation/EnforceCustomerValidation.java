package com.accionmfb.omnix.checkpoint.annotation;

import com.accionmfb.omnix.checkpoint.commons.CustomerValidationParam;

import java.lang.annotation.*;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE, ElementType.METHOD })
public @interface EnforceCustomerValidation {
    CustomerValidationParam validateFor() default CustomerValidationParam.EXISTENCE_AND_STATUS;
    String statusField() default "status";
    String queryField() default "mobileNumber";
    String[] statusAgainst() default { "ACTIVE" };
}
