package com.account.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * CSV配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.csv")
public class CsvProperties {
    private String dataDir;
    private String usersFile;
    private String recordsFile;
}
