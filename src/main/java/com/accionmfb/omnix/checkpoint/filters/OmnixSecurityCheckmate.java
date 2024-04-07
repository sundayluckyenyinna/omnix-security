package com.accionmfb.omnix.checkpoint.filters;

import com.accionmfb.omnix.checkpoint.config.properties.SecurityProperties;
import com.accionmfb.omnix.core.encryption.manager.OmnixEncryptionService;
import com.accionmfb.omnix.core.payload.EncryptionPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.logging.LogLevel;

import java.util.Arrays;
import java.util.logging.Logger;

public interface OmnixSecurityCheckmate {
    Logger logger = Logger.getLogger(OmnixSecurityCheckmate.class.getName());

    @SneakyThrows
    default String getWritableResponse(Object response,
                                       ObjectMapper objectMapper,
                                       SecurityProperties securityProperties,
                                       OmnixEncryptionService encryptionService){
        String responseJson = response instanceof String ? (String) response : objectMapper.writeValueAsString(response);
        if(securityProperties.isEnableEncryptionSecurityResponse()){
            String encryptedResponse = encryptionService.encrypt(responseJson);
            EncryptionPayload payload = new EncryptionPayload();
            payload.setResponse(encryptedResponse);
            responseJson = objectMapper.writeValueAsString(payload);
        }
        return responseJson;
    }

    default void logSecurityViolationMonitor(SecurityProperties properties, String... messages){
        if(properties.isEnableSecurityViolationLogging()){
            logger.info("");
            logger.warning("*************************************** OMNIX - SECURITY VIOLATION ****************************************");
            Arrays.stream(messages).forEach(logger::warning);
            logger.warning("Omnix Security will go ahead to bounce the request.");
            logger.warning(String.format("Will Omnix security filter chain checkpoints encrypt security check response? %s", properties.isEnableEncryptionSecurityResponse()));
            logger.warning("***********************************************************************************************************");
            logger.info("");
        }
    }

    default void logValidationViolationMonitor(SecurityProperties properties, String... messages){
        if(properties.isEnableSecurityViolationLogging()){
            logger.info("");
            logger.warning("*************************************** OMNIX - VALIDATION VIOLATION ****************************************");
            Arrays.stream(messages).forEach(logger::warning);
            logger.warning("Omnix Validation will now go ahead to bounce the request.");
            logger.warning(String.format("Will Omnix validation interceptor chain encrypt security check response? %s", properties.isEnableEncryptionSecurityResponse()));
            logger.warning("***********************************************************************************************************");
            logger.info("");
        }
    }
}
