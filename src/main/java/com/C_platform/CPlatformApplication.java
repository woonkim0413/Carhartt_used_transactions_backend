package com.C_platform;

import com.C_platform.config.FileConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

// test 중 1 2 3 4 5 6
@SpringBootApplication
@EnableConfigurationProperties(FileConfig.class)
public class CPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(CPlatformApplication.class, args);
	}
}
