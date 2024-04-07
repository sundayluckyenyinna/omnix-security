package com.accionmfb.omnix.checkpoint.filters;

import com.accionmfb.omnix.checkpoint.config.properties.SecurityProperties;
import com.accionmfb.omnix.checkpoint.commons.ResponseCodes;
import com.accionmfb.omnix.checkpoint.commons.StringValues;
import com.accionmfb.omnix.checkpoint.exception.SecurityViolationException;
import com.accionmfb.omnix.checkpoint.jwt.AppUserJwtTokenUtil;
import com.accionmfb.omnix.checkpoint.model.AppUserRecord;
import com.accionmfb.omnix.checkpoint.model.ConnectingService;
import com.accionmfb.omnix.checkpoint.service.DatasourceRecordService;
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
@Order(value = Ordered.HIGHEST_PRECEDENCE + 4)
@RequiredArgsConstructor
@EnableConfigurationProperties(value = SecurityProperties.class)
public class AppUserEndpointPermissionCheckpoint extends OncePerRequestFilter implements OmnixSecurityCheckmate {

    private final ObjectMapper objectMapper;
    private final OmnixSecurityApplicationServiceUtil serviceUtil;
    private final AppUserJwtTokenUtil appUserJwtTokenUtil;
    private final OmnixEncryptionService omnixEncryptionService;
    private final SecurityProperties securityProperties;
    private final DatasourceRecordService datasourceRecordService;


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

        if(securityProperties.shouldGoAheadWithSecurity(request.getRequestURI()))
        {
            String username = appUserJwtTokenUtil.getAppUserUsernameFromToken(request.getHeader(StringValues.AUTH_KEY));
            AppUserRecord appUserRecord = Objects.isNull(request.getAttribute(StringValues.APP_USER_RECORD)) ? datasourceRecordService.getAppUser(username) : (AppUserRecord) request.getAttribute(StringValues.APP_USER_RECORD);
            try{
                validAppUserPermissionForEndpointAndConnectingService(appUserRecord, request.getRequestURI());
                filterChain.doFilter(request, response);
            }catch (SecurityViolationException exception){
                String res = objectMapper.writeValueAsString(exception.getBaseResponse());
                String resJson = getWritableResponse(res, objectMapper, securityProperties, omnixEncryptionService);
                serviceUtil.getResponseWriter(response, HttpStatus.UNAUTHORIZED).write(resJson);
            }
        }
        else{
            filterChain.doFilter(request, response);
        }
    }

    private void validAppUserPermissionForEndpointAndConnectingService(AppUserRecord appUserRecord, String requestUrl){
        validateAppUserPermissionForEndpoint(appUserRecord, requestUrl);
        validateAppUserConnectingService(appUserRecord, requestUrl);
    }

    private void validateAppUserPermissionForEndpoint(AppUserRecord appUserRecord, String requestUrl){
        if(appUserRecord.isEndpointBlackListed(requestUrl)){
            String message = String.format("Omnix resolved that the request endpoint has been blacklisted for AppUser with username: %s", appUserRecord.getUsername());
            logSecurityViolationMonitor(securityProperties, message);
            throw SecurityViolationException.newInstance()
                    .withCode(ResponseCodes.INVALID_CREDENTIALS.getResponseCode())
                    .withMessage("You are not permitted to access this blacklisted resource!");
        }
    }

    private void validateAppUserConnectingService(AppUserRecord appUserRecord, String requestUrl){
        String connectingServiceBaseUrl = requestUrl.trim(); // TODO
        ConnectingService connectingService = datasourceRecordService.getConnectingServiceByBaseUrl(connectingServiceBaseUrl);
        if(Objects.isNull(connectingService)){
            throw SecurityViolationException.newInstance()
                    .withCode(ResponseCodes.RESOURCE_NOT_FOUND.getResponseCode())
                    .withMessage("Invalid request. No resource found for the given request!");
        }
        boolean isServiceEnabled = appUserRecord.isHasServiceEnabled(connectingService);
        if(!isServiceEnabled){
            throw SecurityViolationException.newInstance()
                    .withCode(ResponseCodes.INVALID_CREDENTIALS.getResponseCode())
                    .withMessage("This application user is not currently permitted to access this resource. Please contact admin or support!");
        }
    }
}
