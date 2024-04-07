package com.accionmfb.omnix.checkpoint.interceptors.config;

import com.accionmfb.omnix.checkpoint.interceptors.CustomValidationInterceptor;
import com.accionmfb.omnix.checkpoint.interceptors.CustomerExistenceAndStatusValidationInterceptor;
import com.accionmfb.omnix.checkpoint.interceptors.PersonalAccessTokenValidationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfigurerInterceptorRegistration implements WebMvcConfigurer {

    private final CustomerExistenceAndStatusValidationInterceptor customerExistenceAndStatusValidationInterceptor;
    private final CustomValidationInterceptor customValidationInterceptor;
    private final PersonalAccessTokenValidationInterceptor personalAccessTokenValidationInterceptor;


    @Override
    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(personalAccessTokenValidationInterceptor); // The order is important!
        registry.addInterceptor(customerExistenceAndStatusValidationInterceptor);
        registry.addInterceptor(customValidationInterceptor);
    }
}
