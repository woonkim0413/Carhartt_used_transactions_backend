package com.C_platform.global.error;

import lombok.Getter;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

@Getter
public enum ProductErrorCode implements ErrorCode {

    P001("P001", "product.not.found"),
    P002("P002", "product.stock.insufficient"),
    P003("P003", "product.price.invalid");

    private final String code;
    private final String messageKey;

    private static MessageSource messageSource;

    ProductErrorCode(String code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }

    public static void setMessageSource(MessageSource source) {
        messageSource = source;
    }

    public String getMessage() {
        if (messageSource == null) {
            return "MessageSource is not initialized";
        }
        return messageSource.getMessage(this.messageKey, null, LocaleContextHolder.getLocale());
    }
}
