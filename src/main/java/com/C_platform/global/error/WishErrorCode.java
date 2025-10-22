package com.C_platform.global.error;

import lombok.Getter;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

@Getter
public enum WishErrorCode implements ErrorCode {

    W001("W001", "wish.not.found"),
    W002("W002", "wish.add.failed"),
    W003("W003","wish.add.already");

    private final String code;
    private final String messageKey;

    private static MessageSource messageSource;

    WishErrorCode(String code, String messageKey) {
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
