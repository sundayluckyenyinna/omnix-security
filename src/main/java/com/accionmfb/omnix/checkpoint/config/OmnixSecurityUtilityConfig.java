package com.accionmfb.omnix.checkpoint.config;

import com.accionmfb.omnix.checkpoint.commons.RegistryItem;
import com.accionmfb.omnix.checkpoint.commons.StringValues;
import com.accionmfb.omnix.checkpoint.config.properties.SecurityProperties;
import com.accionmfb.omnix.checkpoint.registry.OmnixSecurityRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Configuration
@RequiredArgsConstructor
public class OmnixSecurityUtilityConfig {

    private final SecurityProperties securityProperties;

    @Bean
    public List<String> registerSecurityWhiteListedEndpoints(){
        List<String> whiteListedSecurityUrls = new ArrayList<>();
        List<String> whiteListedValidationUrls = new ArrayList<>();
        String whiteListSecurityEndpoints = securityProperties.getWhiteListSecurityEndpoints();
        String whiteListedValidationEndpoints = securityProperties.getWhiteListCustomerValidationEndpoints();
        fillListWithValues(whiteListedSecurityUrls, whiteListSecurityEndpoints);
        fillListWithValues(whiteListedValidationUrls, whiteListedValidationEndpoints);
        OmnixSecurityRegistry.registerItem(RegistryItem.WHITE_LISTED_SECURITY_URLS, whiteListedSecurityUrls);
        OmnixSecurityRegistry.registerItem(RegistryItem.WHITE_LISTED_VALIDATION_URLS, whiteListedValidationUrls);
        return whiteListedSecurityUrls;
    }

    private void fillListWithValues(List<String> whiteListedValidationUrls, String whiteListedValidationEndpoints) {
        if(Objects.nonNull(whiteListedValidationEndpoints) && !whiteListedValidationEndpoints.trim().isEmpty()){
            List<String> paths = Arrays.stream(whiteListedValidationEndpoints.split(StringValues.COMMA))
                    .map(String::trim)
                    .filter(trim -> !trim.isEmpty())
                    .toList();
            whiteListedValidationUrls.addAll(paths);
        }
    }
}
