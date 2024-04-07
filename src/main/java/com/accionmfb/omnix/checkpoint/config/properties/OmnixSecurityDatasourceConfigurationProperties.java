package com.accionmfb.omnix.checkpoint.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "spring.datasource")
public class OmnixSecurityDatasourceConfigurationProperties {
    private String driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    private String url = "jdbc:sqlserver://localhost;databaseName=omnix;encrypt=true;trustServerCertificate=true;";
    private String username = "omnixservice";
    private String password = "Ab123456";
}
