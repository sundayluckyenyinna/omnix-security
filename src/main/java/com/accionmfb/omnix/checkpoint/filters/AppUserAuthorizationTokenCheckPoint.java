package com.accionmfb.omnix.checkpoint.filters;

import com.accionmfb.omnix.checkpoint.commons.ResponseCodes;
import com.accionmfb.omnix.checkpoint.commons.StringValues;
import com.accionmfb.omnix.checkpoint.config.properties.SecurityProperties;
import com.accionmfb.omnix.checkpoint.payload.OmnixBaseResponse;
import com.accionmfb.omnix.checkpoint.util.OmnixSecurityApplicationServiceUtil;
import com.accionmfb.omnix.core.encryption.manager.OmnixEncryptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Order(value = Ordered.HIGHEST_PRECEDENCE + 1)
@EnableConfigurationProperties(value = SecurityProperties.class)
public class AppUserAuthorizationTokenCheckPoint extends OncePerRequestFilter implements OmnixSecurityCheckmate {

    private final ObjectMapper objectMapper;
    private final OmnixSecurityApplicationServiceUtil serviceUtil;
    private final OmnixEncryptionService omnixEncryptionService;
    private final SecurityProperties securityProperties;


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        if(securityProperties.shouldGoAheadWithSecurity(request.getRequestURI())) {
            String authorizationToken = request.getHeader(StringValues.AUTH_KEY);
            if(Objects.isNull(authorizationToken) || authorizationToken.trim().isEmpty()){
                logSecurityViolationMonitor(securityProperties, "Omnix Error: Empty or undefined authorization bearer token passed for app user!");
                OmnixBaseResponse res = OmnixBaseResponse.builder()
                        .responseCode(ResponseCodes.INVALID_CREDENTIALS.getResponseCode())
                        .responseMessage("You are forbidden to access this resource!").build();
                String resJson = getWritableResponse(res, objectMapper, securityProperties, omnixEncryptionService);
                serviceUtil.getResponseWriter(response, HttpStatus.FORBIDDEN).write(resJson);
            }
            else{
                filterChain.doFilter(request, response);
            }
        }
        else{
            filterChain.doFilter(request, response);
        }
    }

}
