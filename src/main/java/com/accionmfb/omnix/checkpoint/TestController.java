package com.accionmfb.omnix.checkpoint;

import com.accionmfb.omnix.core.annotation.EncryptionPolicyAdvice;
import com.accionmfb.omnix.core.annotation.HttpLoggingAdvice;
import com.accionmfb.omnix.core.commons.EncryptionPolicy;
import com.accionmfb.omnix.core.commons.LogPolicy;
import com.accionmfb.omnix.core.registry.LocalSourceCacheRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {


    @HttpLoggingAdvice
    @EncryptionPolicyAdvice(value = EncryptionPolicy.RESPONSE)
    @GetMapping(value = "/security/test", produces = MediaType.APPLICATION_JSON_VALUE)
    public String testSomething(){
        System.out.println(LocalSourceCacheRegistry.getConfigurationMap());
        return "Got here";
    }

    @EncryptionPolicyAdvice(value = EncryptionPolicy.REQUEST)
    @HttpLoggingAdvice(direction = LogPolicy.REQUEST)
    @PostMapping(value = "/test-am", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> testam(@RequestBody TestPayload testPayload){
        System.out.println(testPayload);
        return ResponseEntity.ok("testam");
    }
}
