package com.C_platform.config;

import com.C_platform.global.error.*;

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
        ItemErrorCode.setMessageSource(messageSource);
        ImageErrorCode.setMessageSource(messageSource);
        WishErrorCode.setMessageSource(messageSource);
        // member domain
        // NameChangeErrorCode.setMessageSource(messageSource);
        //Order domain
        CreateOrderErrorCode.setMessageSource(messageSource);
        // Payment domain
        PaymentErrorCode.setMessageSource(messageSource);
    }
}
