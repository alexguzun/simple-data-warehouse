package com.example.datawarehouse.domain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "warehouse")
public class DataWarehouseConfig {
	private String dbName;
}
