package com.C_platform.Member.domain;

import com.C_platform.global.error.ErrorCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

@Getter
public enum NameChangeErrorCode implements ErrorCode {
    N001("N001", "name.change.too.frequent"),
    N002("N002", "name.change.invalid.pattern"),
    N003("N003", "name.change.same.as_before");

    private final String code;
    private final String messageKey;

    private static MessageSource messageSource;

    NameChangeErrorCode(String code, String messageKey) {
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
