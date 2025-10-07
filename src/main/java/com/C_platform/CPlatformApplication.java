package com.C_platform;

import com.C_platform.config.FileConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

// main push test중 1
@SpringBootApplication
@EnableConfigurationProperties(FileConfig.class)
@ConfigurationPropertiesScan("com.C_platform.Member_woonkim.domain.Oauth")
public class CPlatformApplication {
	public static void main(String[] args) {
		SpringApplication.run(CPlatformApplication.class, args);
	}
}
