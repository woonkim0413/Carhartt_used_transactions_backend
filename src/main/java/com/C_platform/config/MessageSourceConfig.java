package com.C_platform.config;

import com.C_platform.global.ProductErrorCode;
import jakarta.annotation.PostConstruct;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageSourceConfig {
    private final MessageSource messageSource;

    public MessageSourceConfig(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @PostConstruct
    public void init() {
        ProductErrorCode.setMessageSource(messageSource);
    }
}
