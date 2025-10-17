package com.C_platform.global.error;

import lombok.Getter;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

@Getter
public enum CreateOrderErrorCode implements ErrorCode{

    O004("O004", "order.not.found"),          // 주문 없음
    O005("O005", "order.forbidden"),          // 권한 없음
    O006("O006", "order.buyer.not.found"),    // 구매자 없음 추가
    O007("O007", "order.seller.not.found");   // 판매자 없음 추가

    private final String code;
    private final String messageKey;

    private static MessageSource messageSource;

    CreateOrderErrorCode(String code, String messageKey) {
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
