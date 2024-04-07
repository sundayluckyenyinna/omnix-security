package com.accionmfb.omnix.checkpoint.filters;

import com.accionmfb.omnix.checkpoint.commons.AppUserStatus;
import com.accionmfb.omnix.checkpoint.commons.ResponseCodes;
import com.accionmfb.omnix.checkpoint.commons.StringValues;
import com.accionmfb.omnix.checkpoint.config.properties.SecurityProperties;
import com.accionmfb.omnix.checkpoint.exception.SecurityViolationException;
import com.accionmfb.omnix.checkpoint.jwt.AppUserJwtTokenUtil;
import com.accionmfb.omnix.checkpoint.model.AppUserRecord;
import com.accionmfb.omnix.checkpoint.model.AppUserRole;
import com.accionmfb.omnix.checkpoint.payload.OmnixBaseResponse;
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
@Order(value = Ordered.HIGHEST_PRECEDENCE + 2)
@RequiredArgsConstructor
@EnableConfigurationProperties(value = SecurityProperties.class)
public class AppUserExistenceCheckpoint extends OncePerRequestFilter implements OmnixSecurityCheckmate {

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
            try{
                String username = appUserJwtTokenUtil.getAppUserUsernameFromToken(request.getHeader(StringValues.AUTH_KEY));
                AppUserRecord appUserRecord = validateAppUserExistenceAndStatus(username);
                request.setAttribute(StringValues.APP_USER_RECORD, appUserRecord);
                filterChain.doFilter(request, response);
            }catch (SecurityViolationException securityViolationException){
                OmnixBaseResponse res = securityViolationException.getBaseResponse();
                String resJson = getWritableResponse(res, objectMapper, securityProperties, omnixEncryptionService);
                serviceUtil.getResponseWriter(response, HttpStatus.FORBIDDEN).write(resJson);
            }
        }
        else
        {
            filterChain.doFilter(request, response);
        }
    }

    private AppUserRecord validateAppUserExistenceAndStatus(String username){
        log.info("AppUser username: {}", username);
        AppUserRecord appUserRecord = datasourceRecordService.getAppUser(username);
        validateAppUserExistence(appUserRecord, username);
        validateAppUserRole(appUserRecord);
        validateAppUserStatus(appUserRecord);
        return appUserRecord;
    }

    private void validateAppUserExistence(AppUserRecord appUserRecord, String username){
        if(Objects.isNull(appUserRecord)){
            String message = String.format("Omnix could not find AppUser record for the username: %s", username);
            logSecurityViolationMonitor(securityProperties, message);
            throw SecurityViolationException.newInstance()
                    .withCode(ResponseCodes.INVALID_CREDENTIALS.getResponseCode())
                    .withMessage("No profile for this application user. You are forbidden to access this resource!");
        }
    }

    private void validateAppUserRole(AppUserRecord appUserRecord){
        AppUserRole appUserRole = appUserRecord.getAppUserRole();
        log.info("App User role: {}", appUserRole);
        if(Objects.isNull(appUserRole) || appUserRole.getId() == -1L || appUserRole.getId() == 0){
            String message = String.format("Omnix could not find AppUserRole associated with the given AppUser with username: %s", appUserRecord.getUsername());
            logSecurityViolationMonitor(securityProperties, message);
            throw SecurityViolationException.newInstance()
                    .withCode(ResponseCodes.NO_ROLE.getResponseCode())
                    .withMessage("System could not recognize the role of this application user. Contact admin or support!")
                    .addError("Sorry we are unable to continue your request at this moment in this channel. Please contact admin or support.");
        }
    }

    private void validateAppUserStatus(AppUserRecord appUserRecord){
        AppUserStatus appUserStatus = appUserRecord.getStatus();
        if(appUserStatus != AppUserStatus.ACTIVE){
            String message = String.format("Omnix discovered that the AppUser with username: %s is currently not active by virtue of its status.", appUserRecord.getUsername());
            logSecurityViolationMonitor(securityProperties, message);
            throw  SecurityViolationException.newInstance()
                    .withCode(ResponseCodes.APP_USER_INACTIVE.getResponseCode())
                    .withMessage("This application user is currently inactive. Contact administration or support!")
                    .addError("Sorry we are unable to continue your request at this moment in this channel. Please contact admin or support.");
        }
    }

}
