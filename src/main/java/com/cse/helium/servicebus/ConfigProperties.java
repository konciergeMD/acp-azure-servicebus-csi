package com.cse.helium.servicebus;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "servicebus")
public class ConfigProperties {
    
    private String connectionString;
    private String topic;

}
