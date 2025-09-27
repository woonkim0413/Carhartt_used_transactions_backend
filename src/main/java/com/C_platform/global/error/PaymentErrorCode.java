package com.C_platform.global.error;

import lombok.Getter;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

@Getter
public enum PaymentErrorCode implements ErrorCode {

    P001("P001", "payment.order.status.invalid"),   // 주문 상태 부적합 (READY 아님)
    P002("P002", "payment.order.not.found"),        // 주문 없음
    P003("P003", "payment.order.not.owner"),        // 본인 주문 아님
    P004("P004", "payment.pg.init.failed"),         // PG 초기화 실패
    P005("P005", "payment.request.invalid"),        // 요청 본문 검증 실패
    P006("P006", "payment.idempotent.replay"),      // 멱등 키 재전송
    P007("P007", "payment.attempt.exists"),         // 결제 시도 충돌
    P008("P008", "payment.approve.cancel"), // PG에서 결제 취소 응답
    P009("P009", "payment.approve.fail"); // PG에서 결제 실패 응답

    private final String code;
    private final String messageKey;

    private static MessageSource messageSource;

    PaymentErrorCode(String code, String messageKey) {
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


