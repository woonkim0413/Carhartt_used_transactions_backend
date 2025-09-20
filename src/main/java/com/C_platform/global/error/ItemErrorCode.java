package com.C_platform.global.error;

import lombok.Getter;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

@Getter
public enum ItemErrorCode implements ErrorCode {

    I001("I001", "item.not.found"),
    I002("I002", "item.delete.failed");

    private final String code;
    private final String messageKey;

    private static MessageSource messageSource;

    ItemErrorCode(String code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }

    public static void setMessageSource(MessageSource source) {
        messageSource = source;
    }

    @Override
    public String getMessage() {
        if (messageSource == null) {
            return "MessageSource is not initialized";
        }
        return messageSource.getMessage(this.messageKey, null, LocaleContextHolder.getLocale());
    }
}
