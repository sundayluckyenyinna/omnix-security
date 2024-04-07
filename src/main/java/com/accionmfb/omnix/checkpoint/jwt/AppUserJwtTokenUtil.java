package com.accionmfb.omnix.checkpoint.jwt;

import com.accionmfb.omnix.checkpoint.config.properties.SecurityProperties;
import com.accionmfb.omnix.checkpoint.commons.Role;
import com.accionmfb.omnix.checkpoint.commons.StringValues;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.accionmfb.omnix.checkpoint.util.OmnixSecurityApplicationUtil.cleanToken;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AppUserJwtTokenUtil {

    private final ObjectMapper objectMapper;
    private final SecurityProperties securityProperties;
    private static final String APP_USER_USERNAME_KEY = "username";
    private static final String APP_USER_CHANNEL_KEY = "channel";
    private static final String JWT_ISSUER = "Accion Microfinance Bank";
    private static final String APP_USER_TOKEN = "App user bearer token";
    private static final String JWT_CRED_KEY = "X-AMFB-APP-USER-CRED-KEY";


    public String generateToken(String username, String channel, Role role){
        Claims appTokenClaims = generateAppUserClaims(username, channel, role);
        return Jwts.builder()
                .setClaims(appTokenClaims)
                .setSubject(APP_USER_TOKEN)
                .setIssuer(JWT_ISSUER)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .signWith(SignatureAlgorithm.HS256, securityProperties.getAppUserJwtSecretKey())
                .compact();
    }

    public String getAppUserUsernameFromToken(String token){
        return getClaimValueFromKey(APP_USER_USERNAME_KEY, token);
    }

    public String getAppUserChannelFromToken(String token){
        return getClaimValueFromKey(APP_USER_CHANNEL_KEY, token);
    }

    @SneakyThrows
    public String getClaimValueFromKey(String claimKey, String token){
        try {
            token = cleanToken(token);
            Claims claims = getClaimsFromToken(token);
            String base64CredentialKey = Base64.getEncoder().encodeToString(JWT_CRED_KEY.getBytes(StandardCharsets.UTF_8));
            String base64Credentials = (String) claims.get(base64CredentialKey);
            String bareCredentialJson = new String(Base64.getDecoder().decode(base64Credentials));
            Map<String, String> credentialMap = objectMapper.readValue(bareCredentialJson, new TypeReference<HashMap<String, String>>() {
            });
            String value = credentialMap.get(claimKey);
            return Objects.isNull(value) ? null : value;
        }catch (Exception exception){
            log.error("Exception occurred while trying to extract claim from JWT. Exception message is: {}", exception.getMessage());
            return null;
        }
    }

    public Claims getClaimsFromToken(String token){
        return Jwts.parser()
                .setSigningKey(securityProperties.getAppUserJwtSecretKey())
                .parseClaimsJws(token)
                .getBody();
    }

    @SneakyThrows
    private Claims generateAppUserClaims(String username, String channel, Role role){
        String base64Credentials = getBase64EncodedJwtCredentials(username, channel);
        Claims claims = Jwts.claims()
                .setAudience(role.name())
                .setId(String.join(StringValues.STROKE, base64Credentials, UUID.randomUUID().toString()))
                .setIssuedAt(new Date())
                .setIssuer(JWT_ISSUER)
                .setSubject(APP_USER_TOKEN);

        String base64CredentialKey = Base64.getEncoder().encodeToString(JWT_CRED_KEY.getBytes(StandardCharsets.UTF_8));
        claims.put(base64CredentialKey, base64Credentials);
        return claims;
    }

    @SneakyThrows
    private String getBase64EncodedJwtCredentials(String username, String channel){
        Map<String, String> credentialMap = new HashMap<>();
        credentialMap.put(APP_USER_USERNAME_KEY, username);
        credentialMap.put(APP_USER_CHANNEL_KEY, channel);
        String credentialsJson = objectMapper.writeValueAsString(credentialMap);
        return Base64.getEncoder().encodeToString(credentialsJson.getBytes(StandardCharsets.UTF_8));
    }
}
