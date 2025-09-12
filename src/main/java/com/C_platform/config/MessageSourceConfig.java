package com.C_platform.config;


import com.C_platform.global.error.CategoryErrorCode;
import com.C_platform.global.error.ImageErrorCode;
import com.C_platform.global.error.ProductErrorCode;
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
        CategoryErrorCode.setMessageSource(messageSource);
        ImageErrorCode.setMessageSource(messageSource);

    }
}
