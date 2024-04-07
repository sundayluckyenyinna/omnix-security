package com.accionmfb.omnix.checkpoint.interceptors.config;

import com.accionmfb.omnix.checkpoint.annotation.OmnixValidationInterceptor;
import com.accionmfb.omnix.checkpoint.annotation.ValidationHandler;
import com.accionmfb.omnix.checkpoint.commons.RegistryItem;
import com.accionmfb.omnix.checkpoint.registry.OmnixSecurityRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CustomInterceptorRegistration {

    private final Reflections reflections;

    @Bean
    public String registerCustomInterceptors(){
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(OmnixValidationInterceptor.class);
        List<Method> interceptorHandlers = new ArrayList<>();
        if(!classes.isEmpty()){
            log.info("Found {} class(es) for custom validation interception", classes.size());
            log.info("Registering custom validation interceptor(s)");

            classes.forEach(clazz -> {
                List<Method> methods = Arrays.stream(clazz.getDeclaredMethods())
                        .peek(method -> method.setAccessible(true))
                        .filter(method -> method.isAnnotationPresent(ValidationHandler.class))
                        .toList();
                interceptorHandlers.addAll(methods);
            });
        }
        OmnixSecurityRegistry.registerItem(RegistryItem.INTERCEPTORS_CONFIG, interceptorHandlers);
        return "Success";
    }
}
