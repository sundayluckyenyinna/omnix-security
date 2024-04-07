package com.accionmfb.omnix.checkpoint.annotation;

import java.lang.annotation.*;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD )
public @interface ValidationHandler {
    int invocationCount() default 1;
}
