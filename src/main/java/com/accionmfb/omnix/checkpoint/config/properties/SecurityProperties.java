package com.accionmfb.omnix.checkpoint.config.properties;

import com.accionmfb.omnix.checkpoint.commons.RegistryItem;
import com.accionmfb.omnix.checkpoint.registry.OmnixSecurityRegistry;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Objects;

@Data
@ConfigurationProperties(prefix = "omnix.security")
public class SecurityProperties {

    private boolean enableEncryptionSecurityResponse = true;
    private boolean enableSecurityFilter = true;
    private boolean enableCustomerValidationInterception = true;
    private boolean enableSecurityViolationLogging = true;
    private String whiteListSecurityEndpoints;
    private String whiteListCustomerValidationEndpoints;
    private String patKey = "pat";
    private String patSecretKey = "j3H5Ld5nYmGWyULy6xwpOgfSH++NgKXnJMq20vpfd+8=t";
    private String appUserJwtSecretKey = "j3H5Ld5nYmGWyULy6xwpOgfSH++NgKXnJMq20vpfd+8=t";
    private String customerLookupTableName = "customer";
    private String customerQueryFieldName = "mobile_number";
    private String appUserLookupTableName = "app_user";

    @SuppressWarnings("unchecked")
    public boolean shouldGoAheadWithSecurity(String requestUrl){
        List<String> whiteListedUrls = (List<String>) OmnixSecurityRegistry.retrieveItemValue(RegistryItem.WHITE_LISTED_SECURITY_URLS);
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        boolean isNotWhiteListedUrl = Objects.nonNull(whiteListedUrls) && whiteListedUrls.stream().noneMatch(url -> antPathMatcher.match(url, requestUrl));
        return this.isEnableSecurityFilter() && isNotWhiteListedUrl;
    }

    @SuppressWarnings("unchecked")
    public boolean shouldGoAheadWithCustomerValidation(String requestUrl){
        List<String> whiteListedUrls = (List<String>) OmnixSecurityRegistry.retrieveItemValue(RegistryItem.WHITE_LISTED_VALIDATION_URLS);
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        boolean isNotWhiteListedUrl = Objects.nonNull(whiteListedUrls) && whiteListedUrls.stream().noneMatch(url -> antPathMatcher.match(url, requestUrl));
        return this.isEnableCustomerValidationInterception() && isNotWhiteListedUrl;
    }
}
