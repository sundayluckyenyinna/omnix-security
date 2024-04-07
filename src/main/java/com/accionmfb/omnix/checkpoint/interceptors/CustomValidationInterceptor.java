package com.accionmfb.omnix.checkpoint.interceptors;

import com.accionmfb.omnix.checkpoint.annotation.ValidationHandler;
import com.accionmfb.omnix.checkpoint.config.properties.SecurityProperties;
import com.accionmfb.omnix.checkpoint.commons.RegistryItem;
import com.accionmfb.omnix.checkpoint.commons.ResponseCodes;
import com.accionmfb.omnix.checkpoint.exception.SecurityViolationException;
import com.accionmfb.omnix.checkpoint.filters.OmnixSecurityCheckmate;
import com.accionmfb.omnix.checkpoint.instrumentation.BufferedRequestWrapper;
import com.accionmfb.omnix.checkpoint.interceptors.data.InterceptionData;
import com.accionmfb.omnix.checkpoint.payload.OmnixBaseResponse;
import com.accionmfb.omnix.checkpoint.registry.OmnixSecurityRegistry;
import com.accionmfb.omnix.checkpoint.util.OmnixSecurityApplicationServiceUtil;
import com.accionmfb.omnix.core.encryption.manager.OmnixEncryptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


@Slf4j
@Configuration
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 3)
@EnableConfigurationProperties(value = SecurityProperties.class)
public class CustomValidationInterceptor implements HandlerInterceptor, OmnixSecurityCheckmate {

    private final ObjectMapper objectMapper;
    private final OmnixSecurityApplicationServiceUtil serviceUtil;
    private final ApplicationContext applicationContext;
    private final OmnixEncryptionService omnixEncryptionService;
    private final SecurityProperties securityProperties;


    @Override
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler){
        Object customInterceptors = OmnixSecurityRegistry.retrieveItemValue(RegistryItem.INTERCEPTORS_CONFIG);
        if(Objects.nonNull(customInterceptors)){
            List<Method> interceptorMethods = (List<Method>) customInterceptors;
            try {
                InterceptionData interceptionData = InterceptionData.builder()
                        .rawHttpServletRequest(request)
                        .rawHttpServletResponse(response)
                        .rawHandler(handler)
                        .build();
                for (Method method : interceptorMethods){
                    ValidationHandler ann = method.getAnnotation(ValidationHandler.class);
                    int invocationCount = Objects.nonNull(ann) ? ann.invocationCount() : 1;
                    Object[] args = resolveMethodArgumentsInjection(method, request, response, handler, interceptionData);
                    for(int i = 0; i < invocationCount; i++){
                        Object bean = applicationContext.getBean(method.getDeclaringClass());
                        ReflectionUtils.invokeMethod(method, bean, args);
                    }
                }
            }catch (SecurityViolationException exception){
                OmnixBaseResponse res = exception.getBaseResponse();
                logValidationViolationMonitor(securityProperties, res.getResponseMessage());
                String resJson = getWritableResponse(res, objectMapper, securityProperties, omnixEncryptionService);
                serviceUtil.getResponseWriter(response, exception.getStatusCode()).write(resJson);
                return false;
            }
            catch (Exception exception){
                OmnixBaseResponse res = OmnixBaseResponse.builder()
                        .responseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode())
                        .responseMessage(exception.getMessage())
                        .build();
                String resJson = getWritableResponse(res, objectMapper, securityProperties, omnixEncryptionService);
                serviceUtil.getResponseWriter(response, HttpStatus.UNAUTHORIZED).write(resJson);
                return false;
            }
        }
        return true;
    }

    public Object[] resolveMethodArgumentsInjection(Method method, Object ... objects){
        List<Parameter> parameters = List.of(method.getParameters());
        Object[] params = new Object[parameters.size()];
        Arrays.fill(params, null);
        for(Object object : objects)
        {
            for(Parameter parameter : parameters) {
                int parameterIndex = parameters.indexOf(parameter);
                if(parameter.getType().isAssignableFrom(object.getClass()) || parameter.getClass().isAssignableFrom(object.getClass())){
                    params[parameterIndex] = object;
                }
            }
        }
        return params;
    }
}
