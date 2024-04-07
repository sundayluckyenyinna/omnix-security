package com.accionmfb.omnix.checkpoint.filters;

import com.accionmfb.omnix.checkpoint.config.properties.SecurityProperties;
import com.accionmfb.omnix.checkpoint.commons.ResponseCodes;
import com.accionmfb.omnix.checkpoint.commons.StringValues;
import com.accionmfb.omnix.checkpoint.jwt.AppUserJwtTokenUtil;
import com.accionmfb.omnix.checkpoint.model.AppUserRecord;
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
@RequiredArgsConstructor
@Order(value = Ordered.HIGHEST_PRECEDENCE + 3)
@EnableConfigurationProperties(value = SecurityProperties.class)
public class AppUserConnectingIPCheckpoint extends OncePerRequestFilter implements OmnixSecurityCheckmate {

    private final ObjectMapper objectMapper;
    private final OmnixSecurityApplicationServiceUtil serviceUtil;
    private final AppUserJwtTokenUtil appUserJwtTokenUtil;
    private final SecurityProperties securityProperties;
    private final OmnixEncryptionService omnixEncryptionService;
    private final DatasourceRecordService datasourceRecordService;


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

        if(securityProperties.shouldGoAheadWithSecurity(request.getRequestURI()))
        {
            String username = appUserJwtTokenUtil.getAppUserUsernameFromToken(request.getHeader(StringValues.AUTH_KEY));
            Object appUserObject = request.getAttribute(StringValues.APP_USER_RECORD);
            appUserObject = Objects.isNull(appUserObject) ? datasourceRecordService.getAppUser(username) : appUserObject;
            AppUserRecord appUserRecord = (AppUserRecord) appUserObject;
            String requestIp = request.getRemoteAddr();
            if(Objects.isNull(requestIp) || !appUserRecord.isConnectingIPAllowed(requestIp)){
                String message = String.format("Omnix found that the current IP address of the AppUser with username: %s is %s", appUserRecord.getUsername(), requestIp);
                String action = "Omnix will now bounce AppUser as AppUser client is not from a trusted IP address!";
                logSecurityViolationMonitor(securityProperties, message, action);
                OmnixBaseResponse res = OmnixBaseResponse.builder()
                        .responseCode(ResponseCodes.IP_BANNED_CODE.getResponseCode())
                        .responseMessage("You are not permitted to access this resource from your current client!")
                        .build();
                String resJson = getWritableResponse(res, objectMapper, securityProperties, omnixEncryptionService);
                serviceUtil.getResponseWriter(response, HttpStatus.FORBIDDEN).write(resJson);
            }else{
                filterChain.doFilter(request, response);
            }
        }
        else{
            filterChain.doFilter(request, response);
        }
    }
}
