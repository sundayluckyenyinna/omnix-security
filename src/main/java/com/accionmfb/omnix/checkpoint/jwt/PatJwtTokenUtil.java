package com.accionmfb.omnix.checkpoint.jwt;

import com.accionmfb.omnix.checkpoint.config.properties.SecurityProperties;
import com.accionmfb.omnix.checkpoint.commons.StringValues;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.accionmfb.omnix.checkpoint.util.OmnixSecurityApplicationUtil.cleanToken;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatJwtTokenUtil{
    private final ObjectMapper objectMapper;
    private final SecurityProperties securityProperties;
    private static final String APP_USER_CHANNEL_KEY = "channel";
    private static final String JWT_ISSUER = "Accion Microfinance Bank";
    private static final String PAT_USER_TOKEN = "Personal Access Token";
    private static final String PAT_CRED_KEY = "X-AMFB-CRED-KEY";
    private static final String DEFAULT_KEY_SUBJECT = "Customer";


    public String generateToken(String queryField, String channel){
        Claims appTokenClaims = generateCustomerClaims(queryField, channel);
        return Jwts.builder()
                .setClaims(appTokenClaims)
                .setSubject(PAT_USER_TOKEN)
                .setIssuer(JWT_ISSUER)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .signWith(SignatureAlgorithm.HS256, securityProperties.getAppUserJwtSecretKey())
                .compact();
    }

    public String getCustomerQueryFieldValueFromPat(String token){
        return getClaimValueFromKey(securityProperties.getCustomerQueryFieldName(), token);
    }

    public String getCustomerChannelIssuerFromPat(String token){
        return getClaimValueFromKey(APP_USER_CHANNEL_KEY, token);
    }

    @SneakyThrows
    public String getClaimValueFromKey(String claimKey, String token){
        try {
            token = cleanToken(token);
            Claims claims = getClaimsFromToken(token);
            String base64CredentialKey = Base64.getEncoder().encodeToString(PAT_CRED_KEY.getBytes(StandardCharsets.UTF_8));
            String base64Credentials = (String) claims.get(base64CredentialKey);
            String bareCredentialJson = new String(Base64.getDecoder().decode(base64Credentials));
            Map<String, String> credentialMap = objectMapper.readValue(bareCredentialJson, new TypeReference<HashMap<String, String>>() {
            });
            String value = credentialMap.get(claimKey);
            return Objects.isNull(value) ? null : value;
        }catch (Exception exception){
            log.error("Exception occurred while trying to extract claim from PAT. Exception message is: {}", exception.getMessage());
            return null;
        }
    }

    public Claims getClaimsFromToken(String token){
        return Jwts.parser()
                .setSigningKey(securityProperties.getAppUserJwtSecretKey())
                .parseClaimsJws(token)
                .getBody();
    }

    private Claims generateCustomerClaims(String queryField, String channel){
        String base64Credentials = getBase64EncodedPatCredentials(queryField, channel);
        Claims claims = Jwts.claims()
                .setAudience(DEFAULT_KEY_SUBJECT)
                .setId(String.join(StringValues.STROKE, base64Credentials, UUID.randomUUID().toString()))
                .setIssuedAt(new Date())
                .setIssuer(JWT_ISSUER)
                .setSubject(PAT_USER_TOKEN);

        String base64CredentialKey = Base64.getEncoder().encodeToString(PAT_CRED_KEY.getBytes(StandardCharsets.UTF_8));
        claims.put(base64CredentialKey, base64Credentials);
        return claims;
    }

    @SneakyThrows
    private String getBase64EncodedPatCredentials(String mobileNumber, String channel){
        Map<String, String> credentialMap = new HashMap<>();
        credentialMap.put(securityProperties.getCustomerQueryFieldName(), mobileNumber);
        credentialMap.put(APP_USER_CHANNEL_KEY, channel);
        String credentialsJson = objectMapper.writeValueAsString(credentialMap);
        return Base64.getEncoder().encodeToString(credentialsJson.getBytes(StandardCharsets.UTF_8));
    }
}
