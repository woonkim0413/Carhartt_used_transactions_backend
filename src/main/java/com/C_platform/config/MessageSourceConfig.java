package com.C_platform.config;


import com.C_platform.Member.domain.Member.NameChangeErrorCode;
import com.C_platform.global.error.CategoryErrorCode;
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
        // Item domain
        ProductErrorCode.setMessageSource(messageSource);
        CategoryErrorCode.setMessageSource(messageSource);
        // member domain
        NameChangeErrorCode.setMessageSource(messageSource);
    }
}
