package com.accionmfb.omnix.checkpoint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class OmnixSecurityApplication {

    public static void main(String[] args) {
        SpringApplication.run(OmnixSecurityApplication.class, args);
    }
}
