package com.accionmfb.omnix.checkpoint.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Component
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE )
public @interface OmnixValidationInterceptor {
}
