package com.C_platform.payment.application;

import com.C_platform.exception.PaymentException;
import com.C_platform.global.error.PaymentErrorCode;
import com.C_platform.payment.ui.dto.AttemptPaymentRequest;
import com.C_platform.payment.ui.dto.AttemptPaymentResponse;
import com.C_platform.payment.ui.dto.CompletePaymentRequest;
import com.C_platform.payment.ui.dto.CompletePaymentResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PaymentStubService {
    // 결제 요청(ready): redirect_url만 반환
    public AttemptPaymentResponse ready(AttemptPaymentRequest req) {
        // 1) 주문 존재 확인
        if (!orderExists(req.orderId())) {
            throw new PaymentException(PaymentErrorCode.P002); // ORDER_NOT_FOUND
        }
        // 2) 주문 상태 확인(READY만 허용)
        if (!isReadyOrder(req.orderId())) {
            throw new PaymentException(PaymentErrorCode.P001); // ORDER_STATUS_INVALID
        }
        // 3) PG 초기화 실패 시뮬레이션 (stub)
        if ("FAIL_PG".equalsIgnoreCase(req.paymentMethod())) {
            throw new PaymentException(PaymentErrorCode.P004); // PG_INIT_FAILED
        }

        String redirect = "http://localhost:8080/mock/pg/redirect/"
                + req.paymentMethod() + "/" + UUID.randomUUID();

        // DTO 스펙: redirect_url만
        return new AttemptPaymentResponse(redirect);
    }

    // 결제 완료(approve): order_id만 반환
    public CompletePaymentResponse complete(CompletePaymentRequest req) {
        Long orderId = tryExtractOrderId(req.merchantUid()); // 예: order-202-... -> 202

        // 필요하면 result_hint 검증도 가능 (stub라면 일단 패스)
        // if (!"SUCCESS".equalsIgnoreCase(req.resultHint())) {
        //     throw new PaymentException(PaymentErrorCode.P009); // 예: 승인 거절
        // }

        return new CompletePaymentResponse(orderId); // DTO 스펙: order_id만
    }

    // ------- Stub 헬퍼들 -------

    private boolean orderExists(Long orderId) {
        // TODO: 실제 구현 시 orderRepository.existsById(orderId)
        return true;
    }

    private boolean isReadyOrder(Long orderId) {
        // TODO: 실제 구현 시 주문 상태 == READY 검증
        return true;
    }

    private Long tryExtractOrderId(String merchantUid) {
        if (merchantUid == null) return null;
        Matcher m = Pattern.compile("order-(\\d+)-").matcher(merchantUid);
        return m.find() ? Long.valueOf(m.group(1)) : null;
    }
}
