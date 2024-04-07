package com.accionmfb.omnix.checkpoint.interceptors;


import com.accionmfb.omnix.checkpoint.config.properties.SecurityProperties;
import com.accionmfb.omnix.checkpoint.commons.CustomerValidationParam;
import com.accionmfb.omnix.checkpoint.commons.ResponseCodes;
import com.accionmfb.omnix.checkpoint.exception.SecurityViolationException;
import com.accionmfb.omnix.checkpoint.filters.OmnixSecurityCheckmate;
import com.accionmfb.omnix.checkpoint.jwt.PatJwtTokenUtil;
import com.accionmfb.omnix.checkpoint.payload.CustomerValidationMatrix;
import com.accionmfb.omnix.checkpoint.payload.OmnixBaseResponse;
import com.accionmfb.omnix.checkpoint.service.DatasourceRecordService;
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
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@EnableConfigurationProperties(value = SecurityProperties.class)
public class CustomerExistenceAndStatusValidationInterceptor implements HandlerInterceptor, OmnixSecurityCheckmate {

    private final ObjectMapper objectMapper;
    private final PatJwtTokenUtil patJwtTokenUtil;
    private final OmnixSecurityApplicationServiceUtil serviceUtil;
    private final SecurityProperties securityProperties;
    private final DatasourceRecordService datasourceRecordService;
    private final OmnixEncryptionService omnixEncryptionService;

    @Override
    @SneakyThrows
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler){

        try {
            if (handler instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                Method method = handlerMethod.getMethod();
                Class<?> containingMethodClass = method.getDeclaringClass();
                if (securityProperties.shouldGoAheadWithCustomerValidation(request.getRequestURI())) {
                    CustomerValidationMatrix matrix = serviceUtil.getDecisionForCustomerValidationEnforcement(method, containingMethodClass);
                    if(matrix.isEnforceCustomerValidation()) {
                        String patHeader = request.getHeader(securityProperties.getPatKey());
                        patHeader = serviceUtil.resolveJwtToken(patHeader);
                        String customerQueryFieldValue = patJwtTokenUtil.getCustomerQueryFieldValueFromPat(patHeader);
                        try {
                            validateCustomerExistenceAndOrStatus(customerQueryFieldValue, matrix);
                        } catch (SecurityViolationException exception) {
                            OmnixBaseResponse res = exception.getBaseResponse();
                            String resJson = getWritableResponse(res, objectMapper, securityProperties, omnixEncryptionService);
                            serviceUtil.getResponseWriter(response, HttpStatus.UNAUTHORIZED).write(resJson);
                            return false;
                        }catch (Exception exception){
                            OmnixBaseResponse res = new OmnixBaseResponse(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode(), exception.getMessage(), new ArrayList<>());
                            String resJson = getWritableResponse(res, objectMapper, securityProperties, omnixEncryptionService);
                            serviceUtil.getResponseWriter(response, HttpStatus.INTERNAL_SERVER_ERROR).write(resJson);
                            return false;
                        }
                    }
                }

            }
        }catch (Exception exception){
            log.error("Exception occurred while validating customer in interceptor: {}", exception.getMessage());
            OmnixBaseResponse res = OmnixBaseResponse.builder()
                    .responseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode())
                    .responseMessage(exception.getMessage())
                    .build();
            String resJson = getWritableResponse(res, objectMapper, securityProperties, omnixEncryptionService);
            serviceUtil.getResponseWriter(response, HttpStatus.INTERNAL_SERVER_ERROR).write(resJson);
            return false;
        }
        return true;
    }

    private void validateCustomerExistenceAndOrStatus(String queryValue, CustomerValidationMatrix matrix) {
        Map<String, Object> customerRecord = datasourceRecordService.getCustomerRecord(queryValue);
        if (Objects.isNull(customerRecord)) {
            String message = "Customer record not found for provided credentials";
            logValidationViolationMonitor(securityProperties, message);
            throw SecurityViolationException.newInstance()
                    .withCode(ResponseCodes.RESOURCE_NOT_FOUND.getResponseCode())
                    .withMessage(message);
        }
        if(matrix.getValidationParam() == CustomerValidationParam.EXISTENCE_AND_STATUS){
            Object statusObject = customerRecord.get(matrix.getStatusField());
            if(Objects.isNull(statusObject) || !List.of(matrix.getStatusAgainst()).contains(String.valueOf(statusObject))){
                String message = "Customer status is not valid for this request";
                logValidationViolationMonitor(securityProperties, message);
                throw SecurityViolationException.newInstance()
                        .withCode(ResponseCodes.CUSTOMER_DISABLED.getResponseCode())
                        .withMessage(message);
            }
        }
    }
}
