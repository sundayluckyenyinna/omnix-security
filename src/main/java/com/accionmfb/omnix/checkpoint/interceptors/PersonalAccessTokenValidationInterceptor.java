package com.accionmfb.omnix.checkpoint.interceptors;

import com.accionmfb.omnix.checkpoint.config.properties.SecurityProperties;
import com.accionmfb.omnix.checkpoint.commons.ResponseCodes;
import com.accionmfb.omnix.checkpoint.filters.OmnixSecurityCheckmate;
import com.accionmfb.omnix.checkpoint.jwt.PatJwtTokenUtil;
import com.accionmfb.omnix.checkpoint.payload.OmnixBaseResponse;
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
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;


@Slf4j
@Configuration
@RequiredArgsConstructor
@Order(value = Ordered.HIGHEST_PRECEDENCE + 1)
@EnableConfigurationProperties(value = SecurityProperties.class)
public class PersonalAccessTokenValidationInterceptor implements HandlerInterceptor, OmnixSecurityCheckmate {

    private final ObjectMapper objectMapper;
    private final PatJwtTokenUtil patJwtTokenUtil;
    private final OmnixSecurityApplicationServiceUtil serviceUtil;
    private final OmnixEncryptionService omnixEncryptionService;
    private final SecurityProperties securityProperties;

    @Override
    @SneakyThrows
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler){

        if(securityProperties.shouldGoAheadWithCustomerValidation(request.getRequestURI())){
            String patHeader = request.getHeader(securityProperties.getPatKey());
            if(Objects.isNull(patHeader) || patHeader.trim().isEmpty()){
                String message = "No pat found for customer. You are not authorized to access this resource";
                logValidationViolationMonitor(securityProperties, message);
                OmnixBaseResponse res = OmnixBaseResponse.builder()
                        .responseCode(ResponseCodes.INVALID_CREDENTIALS.getResponseCode())
                        .responseMessage(message)
                        .build();
                String resJson = getWritableResponse(res, objectMapper, securityProperties, omnixEncryptionService);
                serviceUtil.getResponseWriter(response, HttpStatus.UNAUTHORIZED).write(resJson);
                return false;
            }
            else{
                patHeader = serviceUtil.resolveJwtToken(patHeader);
                try{
                    String customerQueryFieldValue = patJwtTokenUtil.getCustomerQueryFieldValueFromPat(patHeader);
                    if(Objects.isNull(customerQueryFieldValue) || customerQueryFieldValue.trim().isEmpty()){
                        String message = String.format("Invalid customer %s. You are forbidden to access this resource!", securityProperties.getPatKey());
                        logValidationViolationMonitor(securityProperties, message);
                        OmnixBaseResponse res = OmnixBaseResponse.builder()
                                .responseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode())
                                .responseMessage(message)
                                .build();
                        String resJson = getWritableResponse(res, objectMapper, securityProperties, omnixEncryptionService);
                        serviceUtil.getResponseWriter(response, HttpStatus.FORBIDDEN).write(resJson);
                        return false;
                    }
                }catch (Exception exception){
                    log.error("Exception occurred while trying to validate and retrieve customer mobileNumber from PAT. Exception message is: {}", exception.getMessage());
                    OmnixBaseResponse res = OmnixBaseResponse.builder()
                            .responseCode(ResponseCodes.INVALID_CREDENTIALS.getResponseCode())
                            .responseMessage(exception.getMessage())
                            .build();
                    String resJson = getWritableResponse(res, objectMapper, securityProperties, omnixEncryptionService);
                    serviceUtil.getResponseWriter(response, HttpStatus.INTERNAL_SERVER_ERROR).write(resJson);
                    return false;
                }
            }
        }
        return true;
    }
}
