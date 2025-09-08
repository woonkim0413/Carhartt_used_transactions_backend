package com.C_platform.global.error;

import lombok.Getter;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

@Getter
public enum CategoryErrorCode implements ErrorCode {

    C001("C001", "category.not.found");

    private final String code;
    private final String messageKey;

    private static MessageSource messageSource;

    CategoryErrorCode(String code, String messageKey) {
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
